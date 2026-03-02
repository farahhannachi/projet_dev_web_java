<?php

namespace App\Tests\Service;

use PHPUnit\Framework\TestCase;
use App\Service\ProductManager;

class ProductManagerTest extends TestCase
{
    public function testValidProduct(): void
    {
        $manager = new ProductManager();
        $result = $manager->createProduct("Laptop", 1000, 5);

        $this->assertTrue($result);
    }

    public function testProductWithoutName(): void
    {
        $manager = new ProductManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createProduct("", 1000, 5);
    }

    public function testProductWithInvalidPrice(): void
    {
        $manager = new ProductManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createProduct("Laptop", -100, 5);
    }

    public function testProductWithNegativeQuantity(): void
    {
        $manager = new ProductManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createProduct("Laptop", 1000, -2);
    }
}