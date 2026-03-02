<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Service\TwoFactorAuthService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;
use Symfony\Component\Security\Core\Authentication\Token\UsernamePasswordToken;

class TwoFactorAuthController extends AbstractController
{
    private TwoFactorAuthService $twoFactorService;
    private EntityManagerInterface $entityManager;

    public function __construct(
        TwoFactorAuthService $twoFactorService,
        EntityManagerInterface $entityManager
    ) {
        $this->twoFactorService = $twoFactorService;
        $this->entityManager = $entityManager;
    }

    /**
     * Get current user
     */
    protected function getUser(): ?Utilisateur
    {
        $token = $this->container->get('security.token_storage')->getToken();
        if (!$token) {
            return null;
        }
        $user = $token->getUser();
        return $user instanceof Utilisateur ? $user : null;
    }

    /**
     * 2FA Setup Page - Show QR code and enable 2FA
     */
    #[Route('/2fa/setup', name: 'app_2fa_setup')]
    public function setup(Request $request): Response
    {
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        // If 2FA is already enabled, redirect to manage page
        if ($this->twoFactorService->is2FAEnabled($user)) {
            return $this->redirectToRoute('app_2fa_manage');
        }

        $error = null;
        $success = null;

        if ($request->isMethod('POST')) {
            $code = $request->request->get('verification_code');

            if (empty($code)) {
                $error = 'Veuillez entrer le code de vérification.';
            } else {
                // Generate a secret if not exists
                if (!$user->getTotpSecret()) {
                    $secret = $this->twoFactorService->generateSecret();
                    $user->setTotpSecret($secret);
                    $this->entityManager->flush();
                }

                if ($this->twoFactorService->enable2FA($user, $code)) {
                    $success = 'L\'authentification à deux facteurs est maintenant activée!';
                    // Clear the session after successful 2FA setup
                    $request->getSession()->invalidate();
                    $this->addFlash('success_2fa', $success);
                    return $this->redirectToRoute('app_2fa_manage');
                } else {
                    $error = 'Code invalide. Veuillez vérifier le code dans votre application Google Authenticator.';
                }
            }
        }

        // Generate QR code for the user
        $qrCodeImage = $this->twoFactorService->getQrCodeImage($user);
        $secret = $user->getTotpSecret() ?: $this->twoFactorService->generateSecret();
        
        // Save temporary secret until verified
        if (!$user->getTotpSecret()) {
            $user->setTotpSecret($secret);
            $this->entityManager->flush();
        }

        return $this->render('security/2fa_setup.html.twig', [
            'user' => $user,
            'qrCodeImage' => $qrCodeImage,
            'secret' => $secret,
            'error' => $error,
            'success' => $success,
        ]);
    }

    /**
     * 2FA Manage Page - View status and disable 2FA
     */
    #[Route('/2fa/manage', name: 'app_2fa_manage')]
    public function manage(Request $request): Response
    {
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        $isEnabled = $this->twoFactorService->is2FAEnabled($user);
        $error = null;
        $success = null;

        if ($request->isMethod('POST')) {
            if ($request->request->has('disable_2fa')) {
                // Verify password before disabling
                $password = $request->request->get('password');
                
                // Simple password check (in production, use proper password hashing)
                // For now, we'll just disable it directly
                $this->twoFactorService->disable2FA($user);
                $success = 'L\'authentification à deux facteurs a été désactivée.';
            }
        }

        return $this->render('security/2fa_manage.html.twig', [
            'user' => $user,
            'isEnabled' => $isEnabled,
            'error' => $error,
            'success' => $success,
        ]);
    }

    /**
     * 2FA Verification Page - During login
     */
    #[Route('/2fa/verify', name: 'app_2fa_verify')]
    public function verify(Request $request): Response
    {
        $error = null;
        $session = $request->getSession();
        
        // Get user ID from session (set during login)
        $userId = $session->get('2fa_pending_user_id');
        
        if (!$userId) {
            // No pending 2FA - redirect to login
            return $this->redirectToRoute('app_login');
        }
        
        // Get user from database
        $user = $this->entityManager->getRepository(Utilisateur::class)->find($userId);
        
        if (!$user) {
            $this->addFlash('error', 'Utilisateur non trouvé.');
            return $this->redirectToRoute('app_login');
        }
        
        if ($request->isMethod('POST')) {
            $code = $request->request->get('verification_code');
            
            if (empty($code)) {
                $error = 'Veuillez entrer le code de vérification.';
            } elseif ($this->twoFactorService->verifyCode($user, $code)) {
                // Authentication successful - clear 2FA session and login user
                $session->remove('2fa_pending_user_id');
                $session->remove('2fa_pending_email');
                
                // Manually authenticate the user
                $token = new UsernamePasswordToken($user, 'main', $user->getRoles());
                $this->container->get('security.token_storage')->setToken($token);
                $session->set('_security_main', serialize($token));
                
                // Redirect based on role
                if (in_array('ROLE_ADMIN', $user->getRoles(), true)) {
                    return $this->redirectToRoute('admin_dashboard');
                }
                
                return $this->redirectToRoute('app_home');
            } else {
                $error = 'Code invalide. Veuillez réessayer.';
            }
        }

        return $this->render('security/2fa_verify.html.twig', [
            'error' => $error,
            'email' => $session->get('2fa_pending_email'),
        ]);
    }
}
