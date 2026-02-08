<?php

namespace App\Twig;

use App\Entity\Utilisateur;
use App\Repository\ResponseQuestionRepository;
use Symfony\Bundle\SecurityBundle\Security;
use Twig\Extension\AbstractExtension;
use Twig\Extension\GlobalsInterface;

class NotificationExtension extends AbstractExtension implements GlobalsInterface
{
    private ResponseQuestionRepository $responseQuestionRepository;
    private Security $security;

    public function __construct(
        ResponseQuestionRepository $responseQuestionRepository,
        Security $security
    ) {
        $this->responseQuestionRepository = $responseQuestionRepository;
        $this->security = $security;
    }

    public function getGlobals(): array
    {
        $user = $this->security->getUser();
        $unreadCount = 0;
        $allNotifications = [];

        if ($user instanceof Utilisateur && !$this->security->isGranted('ROLE_ADMIN')) {
            $unreadCount = $this->responseQuestionRepository->countUnreadResponsesForUser($user->getId());
            // Récupérer les 15 dernières réponses d'agents (lues + non lues) pour le dropdown
            // Les réponses restent visibles, seul le compteur se décrémente quand le client accède au ticket
            $allNotifications = $this->responseQuestionRepository->findRecentAgentResponsesForUser($user->getId(), 15);
        }

        return [
            'unread_notifications_count' => $unreadCount,
            'all_notifications' => $allNotifications,
        ];
    }
}
