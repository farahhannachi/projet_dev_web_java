<?php

namespace App\Service;

class UtilisateurManager
{
    public function createUtilisateur(string $nom, string $email, int $age): bool
    {
        if (empty($nom)) {
            throw new \InvalidArgumentException("Le nom est obligatoire");
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException("Email invalide");
        }

        if ($age < 18) {
            throw new \InvalidArgumentException("L'utilisateur doit avoir au moins 18 ans");
        }

        return true;
    }
}