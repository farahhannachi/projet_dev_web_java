<?php

namespace App\Command;

use App\Repository\UtilisateurRepository;
use App\Service\CustomerSegmentationService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(name: 'app:customers:refresh-segments', description: 'Recalcule automatiquement les segments clients.')]
class RefreshCustomerSegmentsCommand extends Command
{
    public function __construct(
        private readonly UtilisateurRepository $utilisateurRepository,
        private readonly CustomerSegmentationService $customerSegmentationService,
        private readonly EntityManagerInterface $entityManager
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $users = $this->utilisateurRepository->findAll();

        $updated = 0;
        foreach ($users as $user) {
            $old = $user->getSegment();
            $new = $this->customerSegmentationService->updateUserSegment($user);
            if ($old !== $new) {
                $updated++;
            }
        }

        $this->entityManager->flush();
        $io->success(sprintf('Segments recalculés. Utilisateurs mis à jour: %d', $updated));

        return Command::SUCCESS;
    }
}

