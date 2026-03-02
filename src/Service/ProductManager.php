<?php

namespace App\Service;

class ProductManager
{
    public function createProduct(string $name, float $price, int $quantity): bool
    {
        if (empty($name)) {
            throw new \InvalidArgumentException("Name cannot be empty");
        }

        if ($price <= 0) {
            throw new \InvalidArgumentException("Price must be positive");
        }

        if ($quantity < 0) {
            throw new \InvalidArgumentException("Quantity cannot be negative");
        }

        return true;
    }
}