<?php

namespace App\Command;

use App\Repository\OrdonnanceRepository;
use App\Repository\TraitementRepository;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:show-data',
    description: 'Affiche les données des ordonnances et traitements avec leurs relations',
)]
class ShowDataCommand extends Command
{
    public function __construct(
        private OrdonnanceRepository $ordonnanceRepository,
        private TraitementRepository $traitementRepository
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);

        $io->title('Données des Ordonnances et Traitements');

        // Afficher toutes les ordonnances
        $ordonnances = $this->ordonnanceRepository->findAll();
        
        $io->section('Ordonnances (' . count($ordonnances) . ')');
        
        $ordonnancesData = [];
        foreach ($ordonnances as $ordonnance) {
            $ordonnancesData[] = [
                $ordonnance->getId(),
                $ordonnance->getFileName(),
                $ordonnance->getStatus(),
                $ordonnance->getClientId(),
                count($ordonnance->getTraitements()),
                $ordonnance->getUploadedAt()->format('Y-m-d H:i'),
            ];
        }
        
        $io->table(
            ['ID', 'Fichier', 'Statut', 'Client ID', 'Nb Traitements', 'Date Upload'],
            $ordonnancesData
        );

        // Afficher tous les traitements
        $traitements = $this->traitementRepository->findAll();
        
        $io->section('Traitements (' . count($traitements) . ')');
        
        $traitementsData = [];
        foreach ($traitements as $traitement) {
            $traitementsData[] = [
                $traitement->getId(),
                $traitement->getOrdonnance()->getId(),
                $traitement->getOrdonnance()->getFileName(),
                $traitement->getDosage(),
                $traitement->getFrequency(),
                $traitement->getDurationDays(),
                $traitement->isActive() ? 'Oui' : 'Non',
                $traitement->isCompleted() ? 'Oui' : 'Non',
            ];
        }
        
        $io->table(
            ['ID', 'Ordonnance ID', 'Fichier Ordonnance', 'Dosage', 'Fréquence', 'Durée (j)', 'Actif', 'Complété'],
            $traitementsData
        );

        // Afficher les ordonnances en attente
        $pending = $this->ordonnanceRepository->findPendingValidation();
        $io->section('Ordonnances en attente de validation (' . count($pending) . ')');
        foreach ($pending as $ord) {
            $io->writeln('- ' . $ord->getFileName() . ' (Client ' . $ord->getClientId() . ')');
        }

        // Afficher les traitements actifs par client
        $io->section('Traitements actifs par client');
        $clientIds = [1, 2, 3];
        foreach ($clientIds as $clientId) {
            $activeTraitements = $this->traitementRepository->findActiveByClientId($clientId);
            if (count($activeTraitements) > 0) {
                $io->writeln('Client ' . $clientId . ': ' . count($activeTraitements) . ' traitement(s) actif(s)');
                foreach ($activeTraitements as $t) {
                    $io->writeln('  - ' . $t->getDosage() . ' - ' . $t->getFrequency() . ' (Ordonnance #' . $t->getOrdonnance()->getId() . ')');
                }
            }
        }

        $io->success('Affichage terminé !');

        return Command::SUCCESS;
    }
}
