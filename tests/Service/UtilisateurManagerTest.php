<?php

namespace App\Tests\Service;

use PHPUnit\Framework\TestCase;
use App\Service\UtilisateurManager;

class UtilisateurManagerTest extends TestCase
{
    public function testValidUtilisateur(): void
    {
        $manager = new UtilisateurManager();
        $result = $manager->createUtilisateur("Iheb", "iheb@email.com", 25);

        $this->assertTrue($result);
    }

    public function testUtilisateurSansNom(): void
    {
        $manager = new UtilisateurManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createUtilisateur("", "iheb@email.com", 25);
    }

    public function testEmailInvalide(): void
    {
        $manager = new UtilisateurManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createUtilisateur("Iheb", "email-invalide", 25);
    }

    public function testUtilisateurMineur(): void
    {
        $manager = new UtilisateurManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->createUtilisateur("Iheb", "iheb@email.com", 16);
    }
}