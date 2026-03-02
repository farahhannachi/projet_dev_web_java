<?php

namespace App\Service;

class PromotionManager
{
    public function applyDiscount(float $price, float $percentage): float
    {
        if ($price <= 0) {
            throw new \InvalidArgumentException("Price must be positive");
        }

        if ($percentage < 0 || $percentage > 100) {
            throw new \InvalidArgumentException("Percentage must be between 0 and 100");
        }

        $discount = $price * ($percentage / 100);

        return $price - $discount;
    }
}