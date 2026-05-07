<?php

namespace App\Service;

use App\Entity\Commande;
use App\Repository\CommandeRepository;

class FraudDetectionService
{
    public function __construct(private readonly CommandeRepository $commandeRepository)
    {
    }

    public function calculateFraudScore(Commande $order, int $paymentFailures = 0): int
    {
        $score = 0;

        $recentOrders = $this->commandeRepository->countRecentOrdersByEmail((string) $order->getEmail(), 20);
        if ($recentOrders >= 4) {
            $score += 35;
        } elseif ($recentOrders >= 2) {
            $score += 20;
        }

        $amount = (float) $order->getTotal();
        if ($amount >= 1200) {
            $score += 35;
        } elseif ($amount >= 600) {
            $score += 20;
        }

        $address = mb_strtolower((string) $order->getAdresseLivraison());
        $suspiciousTerms = ['boite postale', 'p.o. box', 'unknown', 'test'];
        foreach ($suspiciousTerms as $term) {
            if (str_contains($address, $term)) {
                $score += 20;
                break;
            }
        }

        if ($paymentFailures >= 3) {
            $score += 30;
        } elseif ($paymentFailures >= 1) {
            $score += 15;
        }

        return max(0, min(100, $score));
    }

    public function applyFraudDecision(Commande $order, int $score): void
    {
        $order->setFraudScore($score);

        if ($score > 90) {
            $order->setStatut('bloquee');
            return;
        }

        if ($score > 70) {
            $order->setStatut('review');
        }
    }
}

