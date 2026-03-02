<?php

namespace App\Service;

class StockManager
{
    public function addStock(int $currentStock, int $quantity): int
    {
        if ($quantity <= 0) {
            throw new \InvalidArgumentException("La quantité doit être positive");
        }

        return $currentStock + $quantity;
    }

    public function removeStock(int $currentStock, int $quantity): int
    {
        if ($quantity <= 0) {
            throw new \InvalidArgumentException("La quantité doit être positive");
        }

        if ($quantity > $currentStock) {
            throw new \InvalidArgumentException("Stock insuffisant");
        }

        return $currentStock - $quantity;
    }
}