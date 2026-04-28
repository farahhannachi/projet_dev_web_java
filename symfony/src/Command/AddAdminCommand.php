<?php

namespace App\Command;

use App\Entity\Utilisateur;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;

#[AsCommand(
    name: 'app:add-admin',
    description: 'Add a new admin user'
)]
class AddAdminCommand extends Command
{
    public function __construct(
        private EntityManagerInterface $entityManager,
        private UserPasswordHasherInterface $passwordHasher
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this
            ->addArgument('email', InputArgument::REQUIRED, 'Admin email address')
            ->addArgument('password', InputArgument::REQUIRED, 'Admin password')
            ->addArgument('name', InputArgument::REQUIRED, 'Admin display name')
            ->setHelp('This command allows you to add a new admin user...');
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $email = $input->getArgument('email');
        $password = $input->getArgument('password');
        $name = $input->getArgument('name');
        
        $userRepository = $this->entityManager->getRepository(Utilisateur::class);
        
        // Check if user already exists
        $existingUser = $userRepository->findOneBy(['email' => $email]);
        
        if ($existingUser) {
            // Update existing user to admin
            $existingUser->setRoles(['ROLE_ADMIN', 'ROLE_USER']);
            $this->entityManager->flush();
            
            $output->writeln('<info>Existing user updated to admin successfully!</info>');
            $output->writeln("<info>Email: {$email}</info>");
            $output->writeln('<info>Roles: ["ROLE_ADMIN","ROLE_USER"]</info>');
        } else {
            // Create new admin user
            $user = new Utilisateur();
            $user->setNom($name);
            $user->setPrenom('');
            $user->setEmail($email);
            $user->setMotDePasse($this->passwordHasher->hashPassword($user, $password));
            $user->setRoles(['ROLE_ADMIN', 'ROLE_USER']);
            $user->setDateCreation(new \DateTimeImmutable());
            
            $this->entityManager->persist($user);
            $this->entityManager->flush();
            
            $output->writeln('<info>New admin user created successfully!</info>');
            $output->writeln("<info>Email: {$email}</info>");
            $output->writeln("<info>Password: {$password}</info>");
            $output->writeln("<info>Name: {$name}</info>");
            $output->writeln('<info>Roles: ["ROLE_ADMIN","ROLE_USER"]</info>');
        }
        
        return Command::SUCCESS;
    }
}
