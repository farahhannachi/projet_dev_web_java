<?php

namespace App\Service;

use App\Entity\Traitement;

class TraitementManager
{
    /**
     * Valide les règles métier d'un traitement
     */
    public function validate(Traitement $traitement): bool
    {
        // Règle 1 : La durée doit être positive et entre 1 et 365 jours
        if ($traitement->getDureeJours() !== null) {
            if ($traitement->getDureeJours() <= 0) {
                throw new \InvalidArgumentException('La durée du traitement doit être positive');
            }
            if ($traitement->getDureeJours() > 365) {
                throw new \InvalidArgumentException('La durée du traitement ne peut pas dépasser 365 jours');
            }
        }

        // Règle 2 : La date de fin doit être postérieure à la date de début
        if ($traitement->getDateDebut() !== null && $traitement->getDateFin() !== null) {
            if ($traitement->getDateFin() <= $traitement->getDateDebut()) {
                throw new \InvalidArgumentException('La date de fin doit être postérieure à la date de début');
            }
        }

        // Règle 3 : Le statut doit être valide
        $statutsValides = ['en attente', 'validé', 'rejeté', 'actif', 'terminé', 'suspendu', 'annulé'];
        if (!in_array($traitement->getStatus(), $statutsValides, true)) {
            throw new \InvalidArgumentException('Le statut du traitement est invalide');
        }

        // Règle 4 : L'ordonnance est obligatoire
        try {
            $ordonnance = $traitement->getOrdonnance();
        } catch (\Error $e) {
            throw new \InvalidArgumentException('L\'ordonnance est obligatoire');
        }

        // Règle 5 : Le patient est obligatoire
        try {
            $utilisateur = $traitement->getUtilisateur();
        } catch (\TypeError $e) {
            throw new \InvalidArgumentException('Le patient est obligatoire');
        }

        return true;
    }
}
