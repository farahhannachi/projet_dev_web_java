<?php

namespace App\Tests\Service;

use PHPUnit\Framework\TestCase;
use App\Service\PromotionManager;

class PromotionManagerTest extends TestCase
{
    public function testApplyValidDiscount(): void
    {
        $manager = new PromotionManager();
        $newPrice = $manager->applyDiscount(100, 20);

        $this->assertEquals(80, $newPrice);
    }

    public function testInvalidPrice(): void
    {
        $manager = new PromotionManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->applyDiscount(-50, 20);
    }

    public function testInvalidPercentage(): void
    {
        $manager = new PromotionManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->applyDiscount(100, 150);
    }
}