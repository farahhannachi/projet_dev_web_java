<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Doctrine\ORM\EntityManagerInterface;

#[Route('/profile/avatar')]
#[IsGranted('ROLE_USER')]
class AvatarController extends AbstractController
{
    private const AVATAR_STYLES = [
        'avataaars' => 'Personnage Cartoon',
        'avataaars-neutral' => 'Personnage Neutre',
        'pixel-art' => 'Style Pixel Art',
        'adventurer' => 'Aventurier',
        'fun-emoji' => 'Emoji Amusant',
        'lorelei' => 'Lorelei',
        'bottts' => 'Robot',
    ];

    #[Route('', name: 'app_avatar_builder', methods: ['GET'])]
    public function builder(): Response
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        return $this->render('front/avatar/builder.html.twig', [
            'user' => $user,
            'styles' => self::AVATAR_STYLES,
            'currentStyle' => $user->getAvatarSeed() ? explode('|', $user->getAvatarSeed())[0] : 'avataaars',
        ]);
    }

    #[Route('/generate', name: 'app_avatar_generate', methods: ['POST'])]
    public function generate(Request $request, EntityManagerInterface $em): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        /** @var string $style */
        $style = (string) $request->request->get('style', 'avataaars');
        $seed = (string) ($request->request->get('seed') ?? uniqid('avatar_'));

        // Validate style
        if (!isset(self::AVATAR_STYLES[$style])) {
            $style = 'avataaars';
        }

        // Generate DiceBear avatar URL - using avataaars style which is closest to Snapchat Bitmoji
        $avatarUrl = $this->generateAvatarUrl($style, $seed);

        return new JsonResponse([
            'success' => true,
            'avatarUrl' => $avatarUrl,
            'style' => $style,
            'seed' => $seed,
        ]);
    }

    #[Route('/save', name: 'app_avatar_save', methods: ['POST'])]
    public function save(Request $request, EntityManagerInterface $em): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        /** @var string|null $avatarUrl */
        $avatarUrl = $request->request->get('avatarUrl');
        /** @var string $style */
        $style = (string) $request->request->get('style', 'avataaars');
        $seed = $request->request->get('seed');

        if (!$avatarUrl || !$seed) {
            return new JsonResponse(['error' => 'Invalid avatar data'], 400);
        }

        // Save avatar to user
        $user->setAvatarUrl($avatarUrl);
        $user->setAvatarSeed($style . '|' . $seed);

        $em->flush();

        return new JsonResponse([
            'success' => true,
            'message' => 'Avatar sauvegardé avec succès!',
        ]);
    }

    #[Route('/regenerate', name: 'app_avatar_regenerate', methods: ['POST'])]
    public function regenerate(Request $request): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        /** @var string $style */
        $style = (string) $request->request->get('style', 'avataaars');
        $seed = uniqid('avatar_');

        // Validate style
        if (!isset(self::AVATAR_STYLES[$style])) {
            $style = 'avataaars';
        }

        // Generate new random avatar
        $avatarUrl = $this->generateAvatarUrl($style, $seed);

        return new JsonResponse([
            'success' => true,
            'avatarUrl' => $avatarUrl,
            'style' => $style,
            'seed' => $seed,
        ]);
    }

    private function generateAvatarUrl(string $style, string $seed): string
    {
        // Using DiceBear Avatars API - free and no authentication needed
        // avataaars style is closest to Snapchat Bitmoji style
        return sprintf(
            'https://api.dicebear.com/7.x/%s/svg?seed=%s&scale=80',
            urlencode($style),
            urlencode($seed)
        );
    }
}
