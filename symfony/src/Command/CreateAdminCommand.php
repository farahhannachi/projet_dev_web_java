<?php

namespace App\Command;

use App\Entity\Utilisateur;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;

#[AsCommand(
    name: 'app:create-admin',
    description: 'Crée un utilisateur administrateur'
)]
class CreateAdminCommand extends Command
{
    public function __construct(
        private EntityManagerInterface $entityManager,
        private UserPasswordHasherInterface $passwordHasher
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        // Pas de configuration supplémentaire nécessaire
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);

        // Vérifier si l'admin existe déjà
        $existingAdmin = $this->entityManager->getRepository(Utilisateur::class)
            ->findOneBy(['email' => 'curavita@gmail.com']);

        if ($existingAdmin) {
            // Mettre à jour le mot de passe et le rôle
            $hashedPassword = $this->passwordHasher->hashPassword($existingAdmin, '123456');
            $existingAdmin->setMotDePasse($hashedPassword);
            $existingAdmin->setRoles(['ROLE_ADMIN']);
            $this->entityManager->flush();

            $io->success('L\'administrateur curavita@gmail.com a été mis à jour avec succès !');
            $io->info('Email: curavita@gmail.com');
            $io->info('Mot de passe: 123456');
            $io->info('Rôle: ROLE_ADMIN');
        } else {
            // Créer un nouvel admin
            $admin = new Utilisateur();
            $admin->setNom('CuraVita');
            $admin->setPrenom('Admin');
            $admin->setEmail('curavita@gmail.com');
            
            // Hasher le mot de passe
            $hashedPassword = $this->passwordHasher->hashPassword($admin, '123456');
            $admin->setMotDePasse($hashedPassword);
            
            // Définir le rôle admin et le compte actif
            $admin->setRoles(['ROLE_ADMIN']);
            $admin->setEtatCompte('actif');
            $admin->setDateCreation(new \DateTimeImmutable());
            
            // Sauvegarder en base de données
            $this->entityManager->persist($admin);
            $this->entityManager->flush();

            $io->success('Administrateur créé avec succès !');
            $io->info('Email: curavita@gmail.com');
            $io->info('Mot de passe: 123456');
            $io->info('Rôle: ROLE_ADMIN');
        }

        return Command::SUCCESS;
    }
}
