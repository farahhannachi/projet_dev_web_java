<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Service\TwoFactorAuthService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Component\Validator\Constraints as Assert;
use Doctrine\ORM\EntityManagerInterface;

#[Route('/profile')]
#[IsGranted('ROLE_USER')]
class ProfileController extends AbstractController
{
    private TwoFactorAuthService $twoFactorAuthService;
    private EntityManagerInterface $entityManager;
    
    public function __construct(TwoFactorAuthService $twoFactorAuthService, EntityManagerInterface $entityManager)
    {
        $this->twoFactorAuthService = $twoFactorAuthService;
        $this->entityManager = $entityManager;
    }
    
    #[Route('', name: 'app_profile', methods: ['GET', 'POST'])]
    public function show(Request $request): Response
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        // Handle 2FA actions via POST
        $error = null;
        $success = null;
        $qrCodeImage = null;
        $secret = null;
        
        if ($request->isMethod('POST')) {
            // Disable 2FA
            if ($request->request->has('disable_2fa')) {
                $this->twoFactorAuthService->disable2FA($user);
                $success = 'L\'authentification à deux facteurs a été désactivée.';
            }
            
            // Enable 2FA - verify code
            if ($request->request->has('verification_code') && !$this->twoFactorAuthService->is2FAEnabled($user)) {
                $code = $request->request->get('verification_code');
                
                if (empty($code)) {
                    $error = 'Veuillez entrer le code de vérification.';
                } else {
                    // Generate secret if not exists
                    if (!$user->getTotpSecret()) {
                        $secret = $this->twoFactorAuthService->generateSecret();
                        $user->setTotpSecret($secret);
                        $this->entityManager->flush();
                    }
                    
                    if ($this->twoFactorAuthService->enable2FA($user, $code)) {
                        $success = 'L\'authentification à deux facteurs est maintenant activée!';
                    } else {
                        $error = 'Code invalide. Veuillez vérifier le code dans votre application Google Authenticator.';
                    }
                }
            }
            
            // Generate QR code for setup
            if ($request->request->has('generate_qr')) {
                $secret = $user->getTotpSecret() ?: $this->twoFactorAuthService->generateSecret();
                $user->setTotpSecret($secret);
                $this->entityManager->flush();
                $qrCodeImage = $this->twoFactorAuthService->getQrCodeImage($user);
            }
        }
        
        // Get 2FA status
        $isEnabled = $this->twoFactorAuthService->is2FAEnabled($user);
        
        // If QR code not generated yet, prepare it for modal
        if (!$qrCodeImage && !$isEnabled) {
            $secret = $user->getTotpSecret() ?: $this->twoFactorAuthService->generateSecret();
            if (!$user->getTotpSecret()) {
                $user->setTotpSecret($secret);
                $this->entityManager->flush();
            }
            try {
                $qrCodeImage = $this->twoFactorAuthService->getQrCodeImage($user);
            } catch (\Exception $e) {
                $qrCodeImage = null;
            }
        }

        // Calculate account age
        $createdAt = $user->getDateCreation();
        $now = new \DateTimeImmutable();
        $accountAge = $createdAt->diff($now);

        // Format account standing text
        $accountStatus = match ($user->getEtatCompte()) {
            'actif' => 'Compte Actif',
            'suspendu' => 'Compte Suspendu',
            'desactive' => 'Compte Désactivé',
            default => 'Compte Actif',
        };

        // Get loyalty information
        $loyaltyLevel = $user->getLoyaltyLevel();
        $loyaltyPoints = $user->getLoyaltyPoints();
        $segment = $user->getSegment();

        return $this->render('front/profile/show.html.twig', [
            'user' => $user,
            'accountStatus' => $accountStatus,
            'accountAge' => $accountAge,
            'loyaltyLevel' => $loyaltyLevel,
            'loyaltyPoints' => $loyaltyPoints,
            'segment' => $segment,
            'isEnabled' => $isEnabled,
            'qrCodeImage' => $qrCodeImage,
            'secret' => $secret ?: ($user->getTotpSecret() ?: ''),
            'error' => $error,
            'success' => $success,
        ]);
    }

    #[Route('/edit', name: 'app_profile_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, ValidatorInterface $validator, EntityManagerInterface $em): Response
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        $errors = [];

        if ($request->isMethod('POST')) {
            $nom = trim($request->request->get('nom', ''));
            $prenom = trim($request->request->get('prenom', ''));
            $email = trim($request->request->get('email', ''));

            // Validation constraints
            $constraints = new Assert\Collection([
                'nom' => [
                    new Assert\NotBlank(['message' => 'Le nom est obligatoire']),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le nom ne peut pas dépasser {{ limit }} caractères'
                    ]),
                ],
                'prenom' => [
                    new Assert\NotBlank(['message' => 'Le prénom est obligatoire']),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Le prénom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le prénom ne peut pas dépasser {{ limit }} caractères'
                    ]),
                ],
                'email' => [
                    new Assert\NotBlank(['message' => 'L\'email est obligatoire']),
                    new Assert\Email(['message' => 'L\'email "{{ value }}" n\'est pas valide']),
                ],
            ]);

            // Validate the data
            $violations = $validator->validate([
                'nom' => $nom,
                'prenom' => $prenom,
                'email' => $email,
            ], $constraints);

            if (count($violations) == 0) {
                // Check if email is already taken by another user
                $existingUser = $em->getRepository(Utilisateur::class)->findOneBy(['email' => $email]);
                if ($existingUser && $existingUser->getId() !== $user->getId()) {
                    $errors['email'] = 'Cet email est déjà utilisé par un autre compte';
                } else {
                    // Update user data
                    $user->setNom($nom);
                    $user->setPrenom($prenom);
                    $user->setEmail($email);

                    $em->flush();
                    $this->addFlash('success', 'Votre profil a été mis à jour avec succès!');

                    return $this->redirectToRoute('app_profile');
                }
            } else {
                // Convert violations to error array
                foreach ($violations as $violation) {
                    $propertyPath = $violation->getPropertyPath();
                    $errors[$propertyPath] = $violation->getMessage();
                }
            }
        }

        return $this->render('front/profile/edit.html.twig', [
            'user' => $user,
            'errors' => $errors,
        ]);
    }
}
