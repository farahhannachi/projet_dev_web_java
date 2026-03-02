<?php

namespace App\Command;

use App\Entity\Utilisateur;
use App\Repository\UtilisateurRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

#[AsCommand(
    name: 'app:update-admin',
    description: 'Update admin user roles'
)]
class UpdateAdminCommand extends Command
{
    public function __construct(
        private UtilisateurRepository $utilisateurRepository,
        private EntityManagerInterface $entityManager,
        private string $adminEmail
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $user = $this->utilisateurRepository->findOneBy(['email' => $this->adminEmail]);

        if ($user) {
            $user->setRoles(['ROLE_ADMIN']);
            $this->entityManager->flush();
            $output->writeln('Admin user updated successfully!');
            $output->writeln('Email: ' . $user->getEmail());
            $output->writeln('Roles: ' . json_encode($user->getRoles()));
        } else {
            $output->writeln('Admin user not found!');
        }

        return Command::SUCCESS;
    }
}
