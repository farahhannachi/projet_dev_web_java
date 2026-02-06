<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Repository\UtilisateurRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;

class AdminClientController extends AbstractController
{
    #[Route('/admin/clients', name: 'admin_clients')]
    public function index(UtilisateurRepository $utilisateurRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer tous les utilisateurs non admin
        $allUsers = $utilisateurRepository->findAll();
        $clientsArray = array_filter($allUsers, function($user) {
            $roles = $user->getRoles();
            return !in_array('ROLE_ADMIN', $roles);
        });
        
        // Convertir en tableau indexé pour Twig
        $clients = array_values($clientsArray);
        
        // Recherche
        $search = $request->query->get('search', '');
        if (!empty($search)) {
            $clients = $utilisateurRepository->createQueryBuilder('u')
                ->where('u.email LIKE :search OR u.nom LIKE :search OR u.prenom LIKE :search')
                ->andWhere('u.roles NOT LIKE :adminRole')
                ->setParameter('search', '%' . $search . '%')
                ->setParameter('adminRole', '%ROLE_ADMIN%')
                ->getQuery()
                ->getResult();
        }
        
        return $this->render('Admin/clients/index.html.twig', [
            'clients' => $clients,
            'search' => $search
        ]);
    }

    #[Route('/admin/client/new', name: 'admin_client_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $passwordHasher, UtilisateurRepository $utilisateurRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if ($request->isMethod('POST')) {
            $email = $request->request->get('email');
            
            // Vérifier si l'email existe déjà
            $existingUser = $utilisateurRepository->findOneBy(['email' => $email]);
            if ($existingUser) {
                $this->addFlash('error', 'Cet email est déjà utilisé par un autre utilisateur.');
                return $this->redirectToRoute('admin_client_new');
            }
            
            $client = new Utilisateur();
            $client->setNom($request->request->get('nom'));
            $client->setPrenom($request->request->get('prenom'));
            $client->setEmail($email);
            $client->setTelephone($request->request->get('telephone'));
            $client->setAdresse($request->request->get('adresse'));
            $client->setRole('client');
            $client->setEtatCompte('actif');
            $client->setDateCreation(new \DateTimeImmutable());
            
            // Hasher le mot de passe
            $password = $request->request->get('mot_de_passe');
            $client->setMotDePasse($passwordHasher->hashPassword($client, $password));
            
            // Assigner les rôles
            $client->setRoles(['ROLE_USER']);

            $entityManager->persist($client);
            $entityManager->flush();
            
            $this->addFlash('success', 'Client créé avec succès');
            return $this->redirectToRoute('admin_clients');
        }

        return $this->render('Admin/clients/new.html.twig');
    }

    #[Route('/admin/client/{id}/edit', name: 'admin_client_edit', methods: ['GET', 'POST'])]
    public function edit(Utilisateur $client, Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $passwordHasher): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if ($request->isMethod('POST')) {
            $client->setNom($request->request->get('nom'));
            $client->setPrenom($request->request->get('prenom'));
            $client->setEmail($request->request->get('email'));
            $client->setTelephone($request->request->get('telephone'));
            $client->setAdresse($request->request->get('adresse'));
            $client->setRole($request->request->get('role'));
            $client->setEtatCompte($request->request->get('etat_compte'));
            
            // Si un nouveau mot de passe est fourni
            $newPassword = $request->request->get('mot_de_passe');
            if (!empty($newPassword)) {
                $client->setMotDePasse($passwordHasher->hashPassword($client, $newPassword));
            }

            $entityManager->flush();
            $this->addFlash('success', 'Client modifié avec succès');

            return $this->redirectToRoute('admin_clients');
        }

        return $this->render('Admin/clients/edit.html.twig', [
            'client' => $client
        ]);
    }

    #[Route('/admin/client/{id}/delete', name: 'admin_client_delete', methods: ['POST'])]
    public function delete(Utilisateur $client, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($client);
        $entityManager->flush();
        $this->addFlash('success', 'Client supprimé avec succès');

        return $this->redirectToRoute('admin_clients');
    }

    #[Route('/admin/client/{id}/toggle-status', name: 'admin_client_toggle_status', methods: ['POST'])]
    public function toggleStatus(Utilisateur $client, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Inverser l'état du compte
        $newStatus = $client->getEtatCompte() === 'actif' ? 'bloque' : 'actif';
        $client->setEtatCompte($newStatus);

        $entityManager->flush();
        $this->addFlash('success', 'Statut du client modifié avec succès');

        return $this->redirectToRoute('admin_clients');
    }
}
