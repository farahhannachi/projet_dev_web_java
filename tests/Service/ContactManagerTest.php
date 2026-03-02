<?php

namespace App\Tests\Service;

use PHPUnit\Framework\TestCase;
use App\Service\ContactManager;

class ContactManagerTest extends TestCase
{
    public function testValidContact(): void
    {
        $manager = new ContactManager();
        $result = $manager->sendContact(
            "Iheb",
            "iheb@email.com",
            "Bonjour, je voudrais plus d'informations."
        );

        $this->assertTrue($result);
    }

    public function testContactSansNom(): void
    {
        $manager = new ContactManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->sendContact(
            "",
            "iheb@email.com",
            "Message valide ici."
        );
    }

    public function testEmailInvalide(): void
    {
        $manager = new ContactManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->sendContact(
            "Iheb",
            "email-invalide",
            "Message valide ici."
        );
    }

    public function testMessageTropCourt(): void
    {
        $manager = new ContactManager();

        $this->expectException(\InvalidArgumentException::class);
        $manager->sendContact(
            "Iheb",
            "iheb@email.com",
            "Court"
        );
    }
}