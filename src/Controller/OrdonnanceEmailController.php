<?php

namespace App\Controller;

use App\Entity\Ordonnance;
use App\Service\OrdonnanceMailerService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class OrdonnanceEmailController extends AbstractController
{
    #[Route('/ordonnance/{id}/send-email', name: 'app_ordonnance_send_email', methods: ['POST'])]
    public function sendOrdonnanceEmail(Ordonnance $ordonnance, OrdonnanceMailerService $ordonnanceMailerService): Response
    {
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour envoyer une ordonnance par email');
            return $this->redirectToRoute('app_login');
        }

        // Vérifier que l'ordonnance appartient à l'utilisateur connecté
        if ($ordonnance->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à cette ordonnance');
        }

        // Vérifier que l'ordonnance est validée
        if ($ordonnance->getStatut() !== 'validé') {
            $this->addFlash('error', 'Seules les ordonnances validées peuvent être envoyées par email');
            return $this->redirectToRoute('app_mes_traitements');
        }

        // Envoyer l'email
        $emailEnvoye = $ordonnanceMailerService->sendOrdonnanceValideeEmail($ordonnance);

        if ($emailEnvoye) {
            $this->addFlash('success', '📧 Ordonnance envoyée par email avec succès à ' . $this->getUser()->getEmail());
        } else {
            $this->addFlash('error', '❌ Erreur lors de l\'envoi de l\'email. Veuillez réessayer plus tard.');
        }

        return $this->redirectToRoute('app_mes_traitements');
    }
}
