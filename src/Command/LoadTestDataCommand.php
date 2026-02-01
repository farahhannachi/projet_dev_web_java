<?php

namespace App\Command;

use App\Entity\Ordonnance;
use App\Entity\Traitement;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:load-test-data',
    description: 'Charge des données de test pour les ordonnances et traitements',
)]
class LoadTestDataCommand extends Command
{
    public function __construct(
        private EntityManagerInterface $entityManager
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);

        $io->title('Chargement des données de test');

        // Ordonnance 1 - En attente de validation
        $ordonnance1 = new Ordonnance();
        $ordonnance1->setClientId(1);
        $ordonnance1->setFileName('ordonnance_001.pdf');
        $ordonnance1->setFilePath('prescriptions/2026/02/ordonnance_001.pdf');
        $ordonnance1->setStatus(Ordonnance::STATUS_PENDING_VALIDATION);
        $ordonnance1->setUploadedAt(new \DateTime('2026-02-01 10:00:00'));
        $this->entityManager->persist($ordonnance1);

        // Ordonnance 2 - Validée avec traitements
        $ordonnance2 = new Ordonnance();
        $ordonnance2->setClientId(1);
        $ordonnance2->setValidatedById(2);
        $ordonnance2->setFileName('ordonnance_002.pdf');
        $ordonnance2->setFilePath('prescriptions/2026/02/ordonnance_002.pdf');
        $ordonnance2->setStatus(Ordonnance::STATUS_VALIDATED);
        $ordonnance2->setUploadedAt(new \DateTime('2026-01-28 14:30:00'));
        $ordonnance2->setValidatedAt(new \DateTime('2026-01-29 09:15:00'));
        $this->entityManager->persist($ordonnance2);

        // Traitement 1 pour ordonnance 2
        $traitement1 = new Traitement();
        $traitement1->setOrdonnance($ordonnance2);
        $traitement1->setClientId(1);
        $traitement1->setDosage('500mg');
        $traitement1->setFrequency('3 fois par jour');
        $traitement1->setDurationDays(7);
        $traitement1->setStartDate(new \DateTime('2026-01-29 00:00:00'));
        $traitement1->setEndDate(new \DateTime('2026-02-05 00:00:00'));
        $traitement1->setIsActive(true);
        $traitement1->setIsCompleted(false);
        $traitement1->setNotes('Prendre après les repas');
        $this->entityManager->persist($traitement1);

        // Traitement 2 pour ordonnance 2
        $traitement2 = new Traitement();
        $traitement2->setOrdonnance($ordonnance2);
        $traitement2->setClientId(1);
        $traitement2->setDosage('10mg');
        $traitement2->setFrequency('1 fois par jour le soir');
        $traitement2->setDurationDays(30);
        $traitement2->setStartDate(new \DateTime('2026-01-29 00:00:00'));
        $traitement2->setEndDate(new \DateTime('2026-02-28 00:00:00'));
        $traitement2->setIsActive(true);
        $traitement2->setIsCompleted(false);
        $traitement2->setNotes('Avant le coucher');
        $this->entityManager->persist($traitement2);

        // Ordonnance 3 - Validée avec traitements
        $ordonnance3 = new Ordonnance();
        $ordonnance3->setClientId(2);
        $ordonnance3->setValidatedById(2);
        $ordonnance3->setFileName('ordonnance_003.jpg');
        $ordonnance3->setFilePath('prescriptions/2026/01/ordonnance_003.jpg');
        $ordonnance3->setStatus(Ordonnance::STATUS_VALIDATED);
        $ordonnance3->setUploadedAt(new \DateTime('2026-01-25 11:20:00'));
        $ordonnance3->setValidatedAt(new \DateTime('2026-01-26 10:00:00'));
        $this->entityManager->persist($ordonnance3);

        // Traitement 3 pour ordonnance 3
        $traitement3 = new Traitement();
        $traitement3->setOrdonnance($ordonnance3);
        $traitement3->setClientId(2);
        $traitement3->setDosage('250mg');
        $traitement3->setFrequency('2 fois par jour');
        $traitement3->setDurationDays(14);
        $traitement3->setStartDate(new \DateTime('2026-01-26 00:00:00'));
        $traitement3->setEndDate(new \DateTime('2026-02-09 00:00:00'));
        $traitement3->setIsActive(true);
        $traitement3->setIsCompleted(false);
        $traitement3->setNotes('Matin et soir');
        $this->entityManager->persist($traitement3);

        // Traitement 4 pour ordonnance 3
        $traitement4 = new Traitement();
        $traitement4->setOrdonnance($ordonnance3);
        $traitement4->setClientId(2);
        $traitement4->setDosage('5ml');
        $traitement4->setFrequency('3 fois par jour');
        $traitement4->setDurationDays(10);
        $traitement4->setStartDate(new \DateTime('2026-01-26 00:00:00'));
        $traitement4->setEndDate(new \DateTime('2026-02-05 00:00:00'));
        $traitement4->setIsActive(true);
        $traitement4->setIsCompleted(false);
        $traitement4->setNotes('Sirop contre la toux');
        $this->entityManager->persist($traitement4);

        // Traitement 5 - Complété
        $traitement5 = new Traitement();
        $traitement5->setOrdonnance($ordonnance3);
        $traitement5->setClientId(2);
        $traitement5->setDosage('100mg');
        $traitement5->setFrequency('1 fois par jour');
        $traitement5->setDurationDays(5);
        $traitement5->setStartDate(new \DateTime('2026-01-15 00:00:00'));
        $traitement5->setEndDate(new \DateTime('2026-01-20 00:00:00'));
        $traitement5->setIsActive(false);
        $traitement5->setIsCompleted(true);
        $traitement5->setNotes('Traitement terminé avec succès');
        $this->entityManager->persist($traitement5);

        // Ordonnance 4 - En attente
        $ordonnance4 = new Ordonnance();
        $ordonnance4->setClientId(3);
        $ordonnance4->setFileName('ordonnance_004.pdf');
        $ordonnance4->setFilePath('prescriptions/2026/02/ordonnance_004.pdf');
        $ordonnance4->setStatus(Ordonnance::STATUS_PENDING_VALIDATION);
        $ordonnance4->setUploadedAt(new \DateTime('2026-02-01 15:45:00'));
        $this->entityManager->persist($ordonnance4);

        // Ordonnance 5 - Rejetée
        $ordonnance5 = new Ordonnance();
        $ordonnance5->setClientId(2);
        $ordonnance5->setValidatedById(2);
        $ordonnance5->setFileName('ordonnance_005.png');
        $ordonnance5->setFilePath('prescriptions/2026/01/ordonnance_005.png');
        $ordonnance5->setStatus(Ordonnance::STATUS_REJECTED);
        $ordonnance5->setRejectionReason('Document illisible, veuillez téléverser une meilleure qualité');
        $ordonnance5->setUploadedAt(new \DateTime('2026-01-20 16:00:00'));
        $ordonnance5->setValidatedAt(new \DateTime('2026-01-21 08:30:00'));
        $this->entityManager->persist($ordonnance5);

        $this->entityManager->flush();

        $io->success('Données de test chargées avec succès !');
        $io->table(
            ['Type', 'Nombre'],
            [
                ['Ordonnances', 5],
                ['Traitements', 5],
            ]
        );

        return Command::SUCCESS;
    }
}
