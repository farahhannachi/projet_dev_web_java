<?php

namespace App\Tests\Service;

use PHPUnit\Framework\TestCase;
use App\Service\StockManager;

class StockManagerTest extends TestCase
{
    public function testAddStock(): void
    {
        $manager = new StockManager();
        $result = $manager->addStock(10, 5);

        $this->assertEquals(15, $result);
    }

    public function testRemoveStock(): void
    {
        $manager = new StockManager();
        $result = $manager->removeStock(10, 3);

        $this->assertEquals(7, $result);
    }

    public function testRemoveTooMuchStock(): void
    {
        $manager = new StockManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->removeStock(5, 10);
    }

    public function testAddInvalidQuantity(): void
    {
        $manager = new StockManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->addStock(10, -2);
    }
}