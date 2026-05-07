<?php

namespace App\Service;

use App\Entity\Commande;
use App\Entity\Utilisateur;

class LoyaltyService
{
    public function addPoints(Utilisateur $user, Commande $commande): int
    {
        if (!in_array($commande->getStatut(), ['confirmee', 'livree'], true)) {
            return 0;
        }

        $points = (int) floor($commande->getTotal() / 10);
        if ($commande->getTotal() >= 300) {
            $points += 20;
        }

        $user->setLoyaltyPoints($user->getLoyaltyPoints() + $points);
        $user->setLoyaltyLevel($this->calculateLevel($user->getLoyaltyPoints()));
        $user->setLastActivityAt(new \DateTimeImmutable('now'));

        return $points;
    }

    public function calculateLevel(int $points): string
    {
        return match (true) {
            $points >= 2000 => 'PLATINUM',
            $points >= 1000 => 'GOLD',
            $points >= 400 => 'SILVER',
            default => 'BRONZE',
        };
    }

    public function getDiscountByLevel(string $level): float
    {
        return match ($level) {
            'PLATINUM' => 12.0,
            'GOLD' => 8.0,
            'SILVER' => 4.0,
            default => 0.0,
        };
    }
}

