<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Form\ResetPasswordRequestType;
use App\Form\ResetPasswordType;
use App\Repository\UtilisateurRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Psr\Log\LoggerInterface;
use Symfony\Component\Mime\Email;

class ResetPasswordController extends AbstractController
{
    #[Route('/forgot-password', name: 'app_forgot_password')]
    public function request(
        Request $request, 
        UtilisateurRepository $userRepository, 
        EntityManagerInterface $entityManager,
        MailerInterface $mailer, 
        LoggerInterface $logger
    ): Response {
        // If user is already logged in, redirect to home
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        $form = $this->createForm(ResetPasswordRequestType::class);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $email = $form->get('email')->getData();

            $user = $userRepository->findOneBy(['email' => $email]);

            if ($user) {
                // Generate reset token
                $token = bin2hex(random_bytes(32));
                
                // Set token expiration (1 hour from now)
                $expiresAt = new \DateTime('+1 hour');
                
                $user->setResetToken($token);
                $user->setResetTokenExpiresAt($expiresAt);
                
                $entityManager->flush();

                // Generate reset URL
                $resetUrl = $this->generateUrl('app_reset_password', ['token' => $token], UrlGeneratorInterface::ABSOLUTE_URL);

                // Send email
                try {
                    $emailMessage = (new Email())
                        ->from(new Address($this->getParameter('mailer_from_email'), 'CURAVITA Support'))
                        ->to($user->getEmail())
                        ->subject('Réinitialisation de votre mot de passe - CURAVITA')
                        ->html($this->renderView('emails/password_reset.html.twig', [
                            'user' => $user,
                            'resetUrl' => $resetUrl,
                        ]));

                    $mailer->send($emailMessage);
                    
                    $logger->info('Password reset email sent to: ' . $email);
                } catch (\Exception $e) {
                    $logger->error('Failed to send password reset email: ' . $e->getMessage());
                }
            }

            // Always show success message to prevent email enumeration
            $this->addFlash('success', 'Si un compte existe avec cette adresse email, vous recevrez un lien de réinitialisation dans quelques minutes.');
            
            return $this->redirectToRoute('app_forgot_password');
        }

        return $this->render('security/forgot_password.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    #[Route('/reset-password/{token}', name: 'app_reset_password')]
    public function reset(
        Request $request, 
        string $token, 
        EntityManagerInterface $entityManager, 
        UserPasswordHasherInterface $passwordHasher, 
        LoggerInterface $logger
    ): Response {
        // If user is already logged in, redirect to home
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        // Find user by token
        $user = $entityManager->getRepository(Utilisateur::class)->findOneBy(['resetToken' => $token]);

        if (!$user) {
            $this->addFlash('error', 'Le lien de réinitialisation est invalide ou a expiré.');
            return $this->redirectToRoute('app_forgot_password');
        }

        // Check if token is valid (not expired)
        if (!$user->isResetTokenValid()) {
            $this->addFlash('error', 'Le lien de réinitialisation a expiré. Veuillez refaire une demande.');
            return $this->redirectToRoute('app_forgot_password');
        }

        $form = $this->createForm(ResetPasswordType::class);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $newPassword = $form->get('newPassword')->getData();

            // Hash and update password
            $hashedPassword = $passwordHasher->hashPassword($user, $newPassword);
            $user->setMotDePasse($hashedPassword);
            
            // Clear reset token
            $user->setResetToken(null);
            $user->setResetTokenExpiresAt(null);
            
            $entityManager->flush();
            
            $logger->info('Password reset successfully for user: ' . $user->getEmail());

            // Add flash message and redirect to login
            $this->addFlash('success', 'Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant vous connecter.');
            
            return $this->redirectToRoute('app_login');
        }

        return $this->render('security/reset_password.html.twig', [
            'token' => $token,
            'form' => $form->createView(),
        ]);
    }
}
