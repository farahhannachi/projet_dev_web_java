<?php

namespace App\Service;

use App\Entity\Utilisateur;
use App\Repository\CommandeRepository;

class CustomerSegmentationService
{
    public function __construct(private readonly CommandeRepository $commandeRepository)
    {
    }

    public function computeSegment(Utilisateur $user): string
    {
        $stats = $this->commandeRepository->getUserOrderStatsByEmail((string) $user->getEmail());
        $ordersCount = (int) ($stats['count'] ?? 0);
        $total = (float) ($stats['total'] ?? 0.0);
        $lastOrderAt = $stats['lastOrderAt'];

        if ($ordersCount === 0) {
            return 'NEW_CUSTOMER';
        }

        $daysSinceLastOrder = 9999;
        if ($lastOrderAt instanceof \DateTimeInterface) {
            $daysSinceLastOrder = (int) $lastOrderAt->diff(new \DateTimeImmutable('now'))->format('%a');
        }

        if ($ordersCount >= 12 && $total >= 1500) {
            return 'VIP';
        }

        if ($daysSinceLastOrder > 120) {
            return 'INACTIVE';
        }

        if ($daysSinceLastOrder > 60 && $ordersCount <= 2) {
            return 'RISKY';
        }

        return 'ACTIVE';
    }

    public function updateUserSegment(Utilisateur $user): string
    {
        $segment = $this->computeSegment($user);
        $user->setSegment($segment);
        return $segment;
    }
}

