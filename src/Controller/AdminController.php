<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Entity\Produit;
use App\Entity\Commande;
use App\Entity\Depot;
use App\Entity\Stock;
use App\Repository\UtilisateurRepository;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\ProduitRepository;
use App\Repository\CommandeRepository;
use App\Repository\OrdonnanceRepository;
use App\Repository\TraitementRepository;
use App\Form\ProduitType;
use App\Form\CommandeType;
use App\Form\DepotType;
use App\Form\StockType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\ResponseHeaderBag;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Dompdf\Dompdf;
use Dompdf\Options;

class AdminController extends AbstractController
{
    #[Route('/admin', name: 'admin_dashboard')]
    public function dashboard(Request $request, UtilisateurRepository $utilisateurRepository, DepotRepository $depotRepository, StockRepository $stockRepository, ProduitRepository $produitRepository, CommandeRepository $commandeRepository, OrdonnanceRepository $ordonnanceRepository, TraitementRepository $traitementRepository): Response
    {
        // Sécurité : seul l'admin peut entrer
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Get current admin user
        $currentUser = $this->getUser();
        
        // Récupérer les statistiques
        $allUsers = $utilisateurRepository->findAll();
        $clients = array_filter($allUsers, function($user) {
            return !in_array('ROLE_ADMIN', $user->getRoles());
        });
        
        $depots = $depotRepository->findAll();
        $stocks = $stockRepository->findAll();
        $produits = $produitRepository->findAll();
        $commandes = $commandeRepository->findAll();
        $ordonnances = $ordonnanceRepository->findAll();
        $traitements = $traitementRepository->findAll();
        $promotions = []; // TODO: Ajouter PromotionRepository quand disponible

        return $this->render('Admin/dashboard.html.twig', [
            'totalClients' => count($clients),
            'totalDepots' => count($depots),
            'totalStocks' => count($stocks),
            'totalProduits' => count($produits),
            'totalCommandes' => count($commandes),
            'totalOrdonnances' => count($ordonnances),
            'totalTraitements' => count($traitements),
            'totalPromotions' => count($promotions),
            'current_admin' => $currentUser
        ]);
    }

    #[Route('/admin/download-pdf', name: 'admin_download_pdf')]
    public function downloadPdf(UtilisateurRepository $utilisateurRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Récupérer tous les clients (utilisateurs non admin)
        $allClients = $utilisateurRepository->findAll();
        $clients = array_filter($allClients, function($user) {
            return !in_array('ROLE_ADMIN', $user->getRoles());
        });

        // Configuration de DomPDF
        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->set('isRemoteEnabled', false);
        $pdfOptions->set('isHtml5ParserEnabled', true);

        // Instance de DomPDF
        $dompdf = new Dompdf($pdfOptions);

        // Génération du HTML
        $html = $this->renderView('Admin/clients_pdf.html.twig', [
            'clients' => $clients,
            'date' => new \DateTime()
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        // Génération du nom de fichier
        $fileName = 'clients_' . date('Y-m-d_H-i-s') . '.pdf';

        // Envoi de la réponse
        $response = new Response($dompdf->output());
        $response->headers->set('Content-Type', 'application/pdf');
        $response->headers->set('Content-Disposition', $response->headers->makeDisposition(
            ResponseHeaderBag::DISPOSITION_ATTACHMENT,
            $fileName
        ));
        
        return $response;
    }

    #[Route('/admin/client/{id}/edit', name: 'admin_client_edit', methods: ['GET', 'POST'])]
    public function editClient(Utilisateur $client, Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $passwordHasher): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if ($request->isMethod('POST')) {
            $client->setNom($request->request->get('nom'));
            $client->setPrenom($request->request->get('prenom'));
            $client->setEmail($request->request->get('email'));
            
            // Si un nouveau mot de passe est fourni
            $newPassword = $request->request->get('mot_de_passe');
            if (!empty($newPassword)) {
                $client->setMotDePasse($passwordHasher->hashPassword($client, $newPassword));
            }

            $entityManager->flush();
            $this->addFlash('success', 'Client modifié avec succès');

            return $this->redirectToRoute('admin_dashboard');
        }

        return $this->render('Admin/edit_client.html.twig', [
            'client' => $client
        ]);
    }

    #[Route('/admin/client/{id}/delete', name: 'admin_client_delete', methods: ['POST'])]
    public function deleteClient(Utilisateur $client, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($client);
        $entityManager->flush();
        $this->addFlash('success', 'Client supprimé avec succès');

        return $this->redirectToRoute('admin_dashboard');
    }

    #[Route('/admin/produits', name: 'admin_produits')]
    public function produits(ProduitRepository $produitRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $produits = $produitRepository->findAll();
        
        return $this->render('Admin/produits.html.twig', [
            'produits' => $produits
        ]);
    }

    #[Route('/admin/produit/new', name: 'admin_produit_new', methods: ['GET', 'POST'])]
    public function newProduit(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $produit = new Produit();
        $form = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($produit);
            $entityManager->flush();
            $this->addFlash('success', 'Produit ajouté avec succès');
            
            return $this->redirectToRoute('admin_produits');
        }
        
        return $this->render('Admin/produit_form.html.twig', [
            'form' => $form->createView(),
            'produit' => null
        ]);
    }

    #[Route('/admin/produit/{id}/edit', name: 'admin_produit_edit', methods: ['GET', 'POST'])]
    public function editProduit(Produit $produit, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Produit modifié avec succès');
            
            return $this->redirectToRoute('admin_produits');
        }
        
        return $this->render('Admin/produit_form.html.twig', [
            'form' => $form->createView(),
            'produit' => $produit
        ]);
    }

    #[Route('/admin/produit/{id}/delete', name: 'admin_produit_delete', methods: ['POST'])]
    public function deleteProduit(Produit $produit, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $entityManager->remove($produit);
        $entityManager->flush();
        $this->addFlash('success', 'Produit supprimé avec succès');
        
        return $this->redirectToRoute('admin_produits');
    }

    #[Route('/admin/commandes', name: 'admin_commandes')]
    public function commandes(CommandeRepository $commandeRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $commandes = $commandeRepository->findAll();
        
        return $this->render('Admin/commandes.html.twig', [
            'commandes' => $commandes
        ]);
    }

    #[Route('/admin/commande/new', name: 'admin_commande_new', methods: ['GET', 'POST'])]
    public function newCommande(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $commande = new Commande();
        // Initialiser la date de commande avec la date et heure actuelles
        $commande->setDateCommande(new \DateTime());
        
        $form = $this->createForm(CommandeType::class, $commande);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($commande);
            $entityManager->flush();
            $this->addFlash('success', 'Commande ajoutée avec succès');
            
            return $this->redirectToRoute('admin_commandes');
        }
        
        return $this->render('Admin/commande_form.html.twig', [
            'form' => $form->createView(),
            'commande' => null
        ]);
    }

    #[Route('/admin/commande/{id}/edit', name: 'admin_commande_edit', methods: ['GET', 'POST'])]
    public function editCommande(Commande $commande, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(CommandeType::class, $commande);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Commande modifiée avec succès');
            
            return $this->redirectToRoute('admin_commandes');
        }
        
        return $this->render('Admin/commande_form.html.twig', [
            'form' => $form->createView(),
            'commande' => $commande
        ]);
    }

    #[Route('/admin/commande/{id}/delete', name: 'admin_commande_delete', methods: ['POST'])]
    public function deleteCommande(Commande $commande, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $entityManager->remove($commande);
        $entityManager->flush();
        $this->addFlash('success', 'Commande supprimée avec succès');
        
        return $this->redirectToRoute('admin_commandes');
    }

    // CRUD pour les Dépôts
    #[Route('/admin/depots', name: 'admin_depots')]
    public function depots(DepotRepository $depotRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $depots = $depotRepository->findAll();
        
        return $this->render('Admin/depots.html.twig', [
            'depots' => $depots
        ]);
    }

    #[Route('/admin/depot/new', name: 'admin_depot_new', methods: ['GET', 'POST'])]
    public function newDepot(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $depot = new Depot();
        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($depot);
            $entityManager->flush();
            $this->addFlash('success', 'Dépôt ajouté avec succès');
            
            return $this->redirectToRoute('admin_depots');
        }
        
        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => null
        ]);
    }

    #[Route('/admin/depot/{id}/edit', name: 'admin_depot_edit', methods: ['GET', 'POST'])]
    public function editDepot(Depot $depot, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Dépôt modifié avec succès');
            
            return $this->redirectToRoute('admin_depots');
        }
        
        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => $depot
        ]);
    }

    #[Route('/admin/depot/{id}/delete', name: 'admin_depot_delete', methods: ['POST'])]
    public function deleteDepot(Depot $depot, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $entityManager->remove($depot);
        $entityManager->flush();
        $this->addFlash('success', 'Dépôt supprimé avec succès');
        
        return $this->redirectToRoute('admin_depots');
    }

    // CRUD pour les Stocks
    #[Route('/admin/stocks', name: 'admin_stocks')]
    public function stocks(StockRepository $stockRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $stocks = $stockRepository->findAll();
        
        return $this->render('Admin/stocks.html.twig', [
            'stocks' => $stocks
        ]);
    }

    #[Route('/admin/stock/new', name: 'admin_stock_new', methods: ['GET', 'POST'])]
    public function newStock(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $stock = new Stock();
        // Initialiser la date de dernière mise à jour avec la date actuelle
        $stock->setDateDerniereMiseAJour(new \DateTime());
        
        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($stock);
            $entityManager->flush();
            $this->addFlash('success', 'Stock ajouté avec succès');
            
            return $this->redirectToRoute('admin_stocks');
        }
        
        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => null
        ]);
    }

    #[Route('/admin/stock/{id}/edit', name: 'admin_stock_edit', methods: ['GET', 'POST'])]
    public function editStock(Stock $stock, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Stock modifié avec succès');
            
            return $this->redirectToRoute('admin_stocks');
        }
        
        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => $stock
        ]);
    }

    #[Route('/admin/stock/{id}/delete', name: 'admin_stock_delete', methods: ['POST'])]
    public function deleteStock(Stock $stock, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $entityManager->remove($stock);
        $entityManager->flush();
        $this->addFlash('success', 'Stock supprimé avec succès');
        
        return $this->redirectToRoute('admin_stocks');
    }
}
