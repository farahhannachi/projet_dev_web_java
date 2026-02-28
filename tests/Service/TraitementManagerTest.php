<?php

namespace App\Tests\Service;

use App\Entity\Traitement;
use App\Entity\Ordonnance;
use App\Entity\Utilisateur;
use App\Service\TraitementManager;
use PHPUnit\Framework\TestCase;

class TraitementManagerTest extends TestCase
{
    /**
     * Test 1 : Validation d'un traitement valide
     */
    public function testValidTraitement()
    {
        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(7);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $this->assertTrue($manager->validate($traitement));
    }

    /**
     * Test 2 : La durée ne peut pas être négative
     */
    public function testTraitementWithNegativeDuration()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La durée du traitement doit être positive');

        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(-5);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 3 : La durée ne peut pas être zéro
     */
    public function testTraitementWithZeroDuration()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La durée du traitement doit être positive');

        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(0);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 4 : La durée ne peut pas dépasser 365 jours
     */
    public function testTraitementWithExcessiveDuration()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La durée du traitement ne peut pas dépasser 365 jours');

        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(400);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 5 : La date de fin doit être postérieure à la date de début
     */
    public function testTraitementWithInvalidDateRange()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La date de fin doit être postérieure à la date de début');

        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(7);
        $traitement->setStatus('actif');
        
        // Utilisation de Reflection pour accéder aux méthodes protected
        $reflection = new \ReflectionClass($traitement);
        
        $dateDebutMethod = $reflection->getMethod('setDateDebut');
        $dateDebutMethod->setAccessible(true);
        $dateDebutMethod->invoke($traitement, new \DateTimeImmutable('2026-03-01'));
        
        $dateFinMethod = $reflection->getMethod('setDateFin');
        $dateFinMethod->setAccessible(true);
        $dateFinMethod->invoke($traitement, new \DateTimeImmutable('2026-02-28'));

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 6 : Le statut doit être valide
     */
    public function testTraitementWithInvalidStatus()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le statut du traitement est invalide');

        $ordonnance = $this->createMock(Ordonnance::class);
        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(7);
        $traitement->setStatus('statut_invalide');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 7 : L'ordonnance est obligatoire
     */
    public function testTraitementWithoutOrdonnance()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('L\'ordonnance est obligatoire');

        $utilisateur = $this->createMock(Utilisateur::class);

        $traitement = new Traitement();
        $traitement->setUtilisateur($utilisateur);
        $traitement->setDureeJours(7);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }

    /**
     * Test 8 : Le patient est obligatoire
     */
    public function testTraitementWithoutPatient()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le patient est obligatoire');

        $ordonnance = $this->createMock(Ordonnance::class);

        $traitement = new Traitement();
        $traitement->setOrdonnance($ordonnance);
        $traitement->setDureeJours(7);
        $traitement->setStatus('actif');

        $manager = new TraitementManager();
        $manager->validate($traitement);
    }
}
