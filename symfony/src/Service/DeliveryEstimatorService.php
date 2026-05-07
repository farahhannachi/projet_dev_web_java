<?php

namespace App\Service;

use App\Entity\Commande;

class DeliveryEstimatorService
{
    public function estimateDeliveryDate(Commande $order): \DateTimeImmutable
    {
        $hours = 48;

        $address = mb_strtolower((string) $order->getAdresseLivraison());
        if ($address !== '' && !str_contains($address, 'tunis')) {
            $hours += 24;
        }

        if ($order->getBaseShippingCost() >= 15) {
            $hours += 12;
        }

        if ($order->getStatut() === 'review') {
            $hours += 24;
        }

        return (new \DateTimeImmutable('now'))->modify(sprintf('+%d hours', $hours));
    }
}

