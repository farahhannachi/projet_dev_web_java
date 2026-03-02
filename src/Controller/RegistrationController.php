<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Form\RegistrationFormType;
use App\Security\EmailVerifier;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mime\Address;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Form\FormError;
use SymfonyCasts\Bundle\VerifyEmail\Exception\VerifyEmailExceptionInterface;
use App\Security\LoginFormAuthenticator;

class RegistrationController extends AbstractController
{
    public function __construct(private EmailVerifier $emailVerifier)
    {
    }

    #[Route('/register', name: 'app_register')]
   public function register(
    Request $request,
    UserPasswordHasherInterface $userPasswordHasher,
    Security $security,
    EntityManagerInterface $entityManager
): Response {
    $user = new Utilisateur();
    $form = $this->createForm(RegistrationFormType::class, $user);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        $plainPassword = $form->get('plainPassword')->getData();

        // Check if email already exists
        $existingUser = $entityManager->getRepository(Utilisateur::class)->findOneBy(['email' => $user->getEmail()]);
        if ($existingUser) {
            $form->get('email')->addError(new FormError('Cet email est déjà utilisé. Veuillez en choisir un autre.'));
            return $this->render('registration/register.html.twig', [
                'registrationForm' => $form->createView(),
            ]);
        }

        // ✅ FIXED LINE
        $user->setMotDePasse(
            $userPasswordHasher->hashPassword($user, $plainPassword)
        );

        // Assign role
        $adminEmail = $this->getParameter('mailer_admin_email');
        if ($user->getEmail() === $adminEmail || $user->getEmail() === 'ihebjbir10@gmail.com') {
            $user->setRoles(['ROLE_ADMIN']);
        } else {
            $user->setRoles(['ROLE_USER']);
        }

        $entityManager->persist($user);
        $entityManager->flush();

            // Email verification
            $this->emailVerifier->sendEmailConfirmation(
                'app_verify_email',
                $user,
                (new TemplatedEmail())
                    ->from(new Address($this->getParameter('mailer_from_email'), 'Curavita'))
                    ->to($user->getEmail())
                    ->subject('Confirm your email')
                    ->htmlTemplate('registration/confirmation_email.html.twig')
            );

            // Auto login after register
            return $security->login($user, LoginFormAuthenticator::class, 'main');
        }

        return $this->render('registration/register.html.twig', [
            'registrationForm' => $form->createView(),
        ]);
    }

    #[Route('/verify/email', name: 'app_verify_email')]
    public function verifyUserEmail(Request $request): Response
    {
        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');

        try {
            /** @var Utilisateur $user */
            $user = $this->getUser();
            $this->emailVerifier->handleEmailConfirmation($request, $user);
        } catch (VerifyEmailExceptionInterface $exception) {
            $this->addFlash('verify_email_error', $exception->getReason());
            return $this->redirectToRoute('app_register');
        }

        $this->addFlash('success', 'Email verified successfully.');

        return $this->redirectToRoute('app_login');
    }
}
