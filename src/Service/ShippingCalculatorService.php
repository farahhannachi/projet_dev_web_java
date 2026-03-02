<?php

namespace App\Service;

use App\Entity\Address;

class ShippingCalculatorService
{
    public function calculateForAddress(Address $address, int $itemsCount, float $subtotal): float
    {
        $base = 7.5;

        $region = mb_strtolower((string) ($address->getRegion() ?? ''));
        $regionMultiplier = str_contains($region, 'tunis') ? 1.0 : 1.25;
        $itemsFee = max(0, $itemsCount - 1) * 1.2;
        $valueFee = $subtotal >= 250 ? 0.0 : 2.0;

        return round(($base * $regionMultiplier) + $itemsFee + $valueFee, 2);
    }
}

