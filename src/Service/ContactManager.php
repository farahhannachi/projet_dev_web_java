<?php

namespace App\Service;

class ContactManager
{
    public function sendContact(string $nom, string $email, string $message): bool
    {
        if (empty($nom)) {
            throw new \InvalidArgumentException("Le nom est obligatoire");
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException("Email invalide");
        }

        if (strlen($message) < 10) {
            throw new \InvalidArgumentException("Le message doit contenir au moins 10 caractères");
        }

        return true;
    }
}