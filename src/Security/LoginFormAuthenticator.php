<?php

namespace App\Security;

use App\Entity\Utilisateur;
use App\Service\TwoFactorAuthService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\Security\Core\Security;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\Security\Http\Authenticator\AbstractLoginFormAuthenticator;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\CsrfTokenBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\RememberMeBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Credentials\PasswordCredentials;
use Symfony\Component\Security\Http\Authenticator\Passport\Passport;

class LoginFormAuthenticator extends AbstractLoginFormAuthenticator
{
    public const LOGIN_ROUTE = 'app_login';

    private UrlGeneratorInterface $urlGenerator;
    private EntityManagerInterface $entityManager;
    private TwoFactorAuthService $twoFactorAuthService;
    private \Symfony\Contracts\HttpClient\HttpClientInterface $httpClient;
    private string $recaptchaSecret;

    public function __construct(
        UrlGeneratorInterface $urlGenerator,
        EntityManagerInterface $entityManager,
        TwoFactorAuthService $twoFactorAuthService,
        \Symfony\Contracts\HttpClient\HttpClientInterface $httpClient,
        string $recaptchaSecret
    ) {
        $this->urlGenerator = $urlGenerator;
        $this->entityManager = $entityManager;
        $this->twoFactorAuthService = $twoFactorAuthService;
        $this->httpClient = $httpClient;
        $this->recaptchaSecret = $recaptchaSecret;
    }

    public function authenticate(Request $request): Passport
    {
        $email = $request->request->get('_username', '');
        $recaptchaResponse = $request->request->get('g-recaptcha-response');

        if (!$recaptchaResponse) {
            throw new \Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException('Veuillez valider le reCAPTCHA.');
        }

        try {
            $response = $this->httpClient->request('POST', 'https://www.google.com/recaptcha/api/siteverify', [
                'body' => [
                    'secret'   => $this->recaptchaSecret,
                    'response' => $recaptchaResponse,
                    'remoteip' => $request->getClientIp(),
                ],
            ]);

            $data = $response->toArray();

            if (!($data['success'] ?? false)) {
                throw new \Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException('Échec de la validation reCAPTCHA. Veuillez réessayer.');
            }
        } catch (\Exception $e) {
            throw new \Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException('Erreur lors de la validation reCAPTCHA : ' . $e->getMessage());
        }

        $request->getSession()->set(Security::LAST_USERNAME, $email);

        return new Passport(
            new UserBadge($email),
            new PasswordCredentials($request->request->get('_password', '')),
            [
                new CsrfTokenBadge('authenticate', $request->request->get('_csrf_token')),
                new RememberMeBadge(),
            ]
        );
    }

    public function onAuthenticationSuccess(
        Request $request,
        $token,
        string $firewallName
    ): RedirectResponse {
        /** @var Utilisateur $user */
        $user = $token->getUser();

        // Check if 2FA is enabled for this user
        if ($this->twoFactorAuthService->is2FAEnabled($user)) {
            // Store user ID in session for 2FA verification
            $request->getSession()->set('2fa_pending_user_id', $user->getId());
            $request->getSession()->set('2fa_pending_email', $user->getEmail());
            
            // Redirect to 2FA verification page
            return new RedirectResponse($this->urlGenerator->generate('app_2fa_verify'));
        }

        // Vérifier si l'utilisateur a le rôle ROLE_ADMIN
        $roles = $user->getRoles();
        
        if (in_array('ROLE_ADMIN', $roles, true)) {
            return new RedirectResponse('/index.php/admin');
        }

        return new RedirectResponse(
            $this->urlGenerator->generate('app_home')
        );
    }

    protected function getLoginUrl(Request $request): string
    {
        return $this->urlGenerator->generate(self::LOGIN_ROUTE);
    }
}
