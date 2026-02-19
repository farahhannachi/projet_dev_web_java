<?php

namespace App\Controller;

use App\Service\PusherNotificationService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class TestPusherController extends AbstractController
{
    #[Route('/admin/test-pusher', name: 'admin_test_pusher')]
    public function testPage(): Response
    {
        return $this->render('Admin/test_pusher.html.twig');
    }

    #[Route('/admin/test-pusher/send', name: 'admin_test_pusher_send', methods: ['POST'])]
    public function sendTestNotification(PusherNotificationService $pusherService): JsonResponse
    {
        if (!$pusherService->isEnabled()) {
            return new JsonResponse([
                'success' => false,
                'message' => 'Pusher n\'est pas configuré. Veuillez configurer les clés dans le fichier .env'
            ], 400);
        }

        $result = $pusherService->notifyNewOrdonnance([
            'id' => 999,
            'numero' => 'TEST-' . date('YmdHis'),
            'patient' => 'Patient Test',
            'date' => date('d/m/Y H:i')
        ]);

        if ($result) {
            return new JsonResponse([
                'success' => true,
                'message' => 'Notification de test envoyée avec succès! Vérifiez le backoffice.'
            ]);
        }

        return new JsonResponse([
            'success' => false,
            'message' => 'Erreur lors de l\'envoi de la notification. Vérifiez les logs.'
        ], 500);
    }
}
