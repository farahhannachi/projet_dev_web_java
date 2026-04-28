<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Entity\Produit;
use App\Entity\Commande;
use App\Entity\Depot;
use App\Entity\Stock;
use App\Entity\Coupon;
use App\Repository\UtilisateurRepository;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\ProduitRepository;
use App\Repository\CommandeRepository;
use App\Repository\CouponRepository;
use App\Repository\OrdonnanceRepository;
use App\Repository\TraitementRepository;
use App\Service\StockService;
use App\Service\MailerService;
use App\Service\SmsService;
use App\Form\ProduitType;
use App\Form\CommandeType;
use App\Form\DepotType;
use App\Form\StockType;
use App\Form\CouponType;
use App\Service\DepotService;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\ResponseHeaderBag;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\String\Slugger\AsciiSlugger;
use Symfony\Component\Form\FormInterface;
use Dompdf\Dompdf;
use Dompdf\Options;
use App\Entity\Stock as StockEntity;

class AdminController extends AbstractController
{
    #[Route('/admin', name: 'admin_dashboard')]
    public function dashboard(Request $request, UtilisateurRepository $utilisateurRepository, DepotRepository $depotRepository, StockRepository $stockRepository, ProduitRepository $produitRepository, CommandeRepository $commandeRepository, OrdonnanceRepository $ordonnanceRepository, TraitementRepository $traitementRepository): Response
    {
        // SÃ©curitÃ© : seul l'admin peut entrer
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Get current admin user
        $currentUser = $this->getUser();
        
        // RÃ©cupÃ©rer les filtres
        $stockSearch = $request->query->get('stock_search');
        $stockEtat = $request->query->get('stock_etat');
        $commandeSearch = $request->query->get('commande_search');
        $commandeStatut = $request->query->get('commande_statut');
        $depotSearch = $request->query->get('depot_search');
        
        // RÃ©cupÃ©rer les statistiques
        $allUsers = $utilisateurRepository->findAll();
        $clients = array_filter($allUsers, function($user) {
            return !in_array('ROLE_ADMIN', $user->getRoles());
        });
        
        // Filtrer les stocks
        $stocksQuery = $stockRepository->createQueryBuilder('s')
            ->leftJoin('s.produit', 'p')
            ->addSelect('p');
        
        if ($stockSearch) {
            $stocksQuery->andWhere('p.nom LIKE :stockSearch')
                      ->setParameter('stockSearch', '%' . $stockSearch . '%');
        }
        
        if ($stockEtat && $stockEtat !== 'all') {
            $stocksQuery->andWhere('s.etatStock = :stockEtat')
                      ->setParameter('stockEtat', $stockEtat);
        }
        
        $stocks = $stocksQuery->getQuery()->getResult();
        
        // Filtrer les commandes
        $commandesQuery = $commandeRepository->createQueryBuilder('c');
        
        if ($commandeSearch) {
            $commandesQuery->andWhere('c.nom LIKE :commandeSearch OR c.email LIKE :commandeSearch')
                         ->setParameter('commandeSearch', '%' . $commandeSearch . '%');
        }
        
        if ($commandeStatut && $commandeStatut !== 'all') {
            $commandesQuery->andWhere('c.statut = :commandeStatut')
                         ->setParameter('commandeStatut', $commandeStatut);
        }
        
        $commandes = $commandesQuery->getQuery()->getResult();
        
        // Filtrer les dÃ©pÃ´ts
        $depotsQuery = $depotRepository->createQueryBuilder('d');
        
        if ($depotSearch) {
            $depotsQuery->andWhere('d.nomDepot LIKE :depotSearch OR d.adresseDepot LIKE :depotSearch OR d.responsableDepot LIKE :depotSearch')
                      ->setParameter('depotSearch', '%' . $depotSearch . '%');
        }
        
        $depots = $depotsQuery->getQuery()->getResult();
        
        $produits = $produitRepository->findAll();
        $ordonnances = $ordonnanceRepository->findAll();
        $traitements = $traitementRepository->findAll();
        $promotions = []; // Promotions are now managed by PromotionController

        return $this->render('Admin/dashboard.html.twig', [
            'totalClients' => count($clients),
            'totalDepots' => count($depots),
            'totalStocks' => count($stocks),
            'totalProduits' => count($produits),
            'totalCommandes' => count($commandes),
            'totalOrdonnances' => count($ordonnances),
            'totalTraitements' => count($traitements),
            'totalPromotions' => count($promotions),
            'current_admin' => $currentUser,
            // DonnÃ©es filtrÃ©es pour le dashboard
            'stocks' => $stocks,
            'commandes' => $commandes,
            'depots' => $depots,
            // Filtres actifs
            'filters' => [
                'stock_search' => $stockSearch,
                'stock_etat' => $stockEtat,
                'commande_search' => $commandeSearch,
                'commande_statut' => $commandeStatut,
                'depot_search' => $depotSearch
            ]
        ]);
    }

    #[Route('/admin/download-pdf', name: 'admin_download_pdf')]
    public function downloadPdf(UtilisateurRepository $utilisateurRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // RÃ©cupÃ©rer tous les clients (utilisateurs non admin)
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

        // GÃ©nÃ©ration du HTML
        $html = $this->renderView('Admin/clients_pdf.html.twig', [
            'clients' => $clients,
            'date' => new \DateTime()
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        // GÃ©nÃ©ration du nom de fichier
        $fileName = 'clients_' . date('Y-m-d_H-i-s') . '.pdf';

        // Envoi de la rÃ©ponse
        $response = new Response($dompdf->output());
        $response->headers->set('Content-Type', 'application/pdf');
        $response->headers->set('Content-Disposition', $response->headers->makeDisposition(
            ResponseHeaderBag::DISPOSITION_ATTACHMENT,
            $fileName
        ));
        
        return $response;
    }

    #[Route('/admin/clients', name: 'admin_clients')]
    public function clients(UtilisateurRepository $utilisateurRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $search = $request->query->get('search', '');
        $sort = $request->query->get('sort', '');
        $role = $request->query->get('role', '');
        $status = $request->query->get('status', '');
        $dateFrom = $request->query->get('date_from', '');
        $dateTo = $request->query->get('date_to', '');
        
        // Build query for users
        $queryBuilder = $utilisateurRepository->createQueryBuilder('u')
            ->where('u.roles NOT LIKE :adminRole')
            ->setParameter('adminRole', '%ROLE_ADMIN%');
        
        // Apply search filter
        if (!empty($search)) {
            $queryBuilder->andWhere('u.email LIKE :search OR u.nom LIKE :search OR u.prenom LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }
        
        // Apply role filter
        if (!empty($role)) {
            if ($role === 'client') {
                $queryBuilder->andWhere('u.roles NOT LIKE :adminRoleFilter')
                    ->setParameter('adminRoleFilter', '%ROLE_ADMIN%');
            }
        }
        
        // Apply status filter
        if (!empty($status)) {
            $queryBuilder->andWhere('u.etatCompte = :status')
                ->setParameter('status', $status);
        }
        
        // Apply date range filter
        if (!empty($dateFrom)) {
            $dateFromObj = new \DateTime($dateFrom);
            $queryBuilder->andWhere('u.dateCreation >= :dateFrom')
                ->setParameter('dateFrom', $dateFromObj);
        }
        
        if (!empty($dateTo)) {
            $dateToObj = new \DateTime($dateTo);
            $dateToObj->setTime(23, 59, 59); // End of day
            $queryBuilder->andWhere('u.dateCreation <= :dateTo')
                ->setParameter('dateTo', $dateToObj);
        }
        
        $clients = $queryBuilder->getQuery()->getResult();
        
        // Apply alphabetical sorting if requested
        if ($sort === 'alpha') {
            usort($clients, function($a, $b) {
                return strcmp($a->getNom(), $b->getNom());
            });
        }
        
        return $this->render('Admin/clients/index.html.twig', [
            'clients' => $clients,
            'search' => $search,
            'sort' => $sort,
            'role' => $role,
            'status' => $status,
            'date_from' => $dateFrom,
            'date_to' => $dateTo
        ]);
    }

    #[Route('/admin/client/new', name: 'admin_client_new', methods: ['GET', 'POST'])]
    public function newClient(Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $passwordHasher, UtilisateurRepository $utilisateurRepository, \Symfony\Component\Validator\Validator\ValidatorInterface $validator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Symfony Validator is injected

        if ($request->isMethod('POST')) {
            $email = $request->request->get('email');
            $nom = $request->request->get('nom');
            $prenom = $request->request->get('prenom');
            $password = $request->request->get('mot_de_passe');
            $role = $request->request->get('role', 'ROLE_USER');

            $input = [
                'email' => $email,
                'nom' => $nom,
                'prenom' => $prenom,
                'mot_de_passe' => $password,
                'role' => $role,
            ];

            $constraints = new \Symfony\Component\Validator\Constraints\Collection([
                'email' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Email()],
                'nom' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 2])],
                'prenom' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 2])],
                'mot_de_passe' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 6])],
                'role' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
            ]);

            $violations = $validator->validate($input, $constraints);
            if (count($violations) > 0) {
                foreach ($violations as $violation) {
                    $this->addFlash('error', $violation->getMessage());
                }
                return $this->redirectToRoute('admin_client_new');
            }

            // VÃ©rifier si l'email existe dÃ©jÃ 
            $existingUser = $utilisateurRepository->findOneBy(['email' => $email]);
            if ($existingUser) {
                $this->addFlash('error', 'Cet email est dÃ©jÃ  utilisÃ© par un autre utilisateur.');
                return $this->redirectToRoute('admin_client_new');
            }

            $client = new Utilisateur();
            $client->setNom($nom);
            $client->setPrenom($prenom);
            $client->setEmail($email);
            $client->setEtatCompte('actif');
            $client->setDateCreation(new \DateTimeImmutable());
            $client->setMotDePasse($passwordHasher->hashPassword($client, $password));
            $client->setRoles([$role]);

            $entityManager->persist($client);
            $entityManager->flush();

            $this->addFlash('success', 'Client crÃ©Ã© avec succÃ¨s');
            return $this->redirectToRoute('admin_clients');
        }

        return $this->render('Admin/clients/new.html.twig');
    }

    #[Route('/admin/client/{id}/toggle-status', name: 'admin_client_toggle_status', methods: ['POST'])]
    public function toggleClientStatus(Utilisateur $client, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Inverser l'Ã©tat du compte
        $newStatus = $client->getEtatCompte() === 'actif' ? 'bloque' : 'actif';
        $client->setEtatCompte($newStatus);

        $entityManager->flush();
        $this->addFlash('success', 'Statut du client modifiÃ© avec succÃ¨s');

        return $this->redirectToRoute('admin_clients');
    }

    #[Route('/admin/client/{id}/edit', name: 'admin_client_edit', methods: ['GET', 'POST'])]
    public function editClient(Utilisateur $client, Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $passwordHasher, \Symfony\Component\Validator\Validator\ValidatorInterface $validator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if ($request->isMethod('POST')) {
            $nom = $request->request->get('nom');
            $prenom = $request->request->get('prenom');
            $email = $request->request->get('email');
            $role = $request->request->get('role', 'ROLE_USER');
            $etatCompte = $request->request->get('etat_compte');
            $newPassword = $request->request->get('mot_de_passe');

            $input = [
                'nom' => $nom,
                'prenom' => $prenom,
                'email' => $email,
                'role' => $role,
                'etat_compte' => $etatCompte,
                'mot_de_passe' => $newPassword,
            ];

            $constraints = new \Symfony\Component\Validator\Constraints\Collection([
                'nom' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 2])],
                'prenom' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 2])],
                'email' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Email()],
                'role' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'etat_compte' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'mot_de_passe' => [new \Symfony\Component\Validator\Constraints\Length(['min' => 6])],
            ]);

            $violations = $validator->validate($input, $constraints);
            if (count($violations) > 0) {
                foreach ($violations as $violation) {
                    $this->addFlash('error', $violation->getMessage());
                }
                return $this->redirectToRoute('admin_client_edit', ['id' => $client->getId()]);
            }

            $client->setNom($nom);
            $client->setPrenom($prenom);
            $client->setEmail($email);
            $client->setRoles([$role]);
            $client->setEtatCompte($etatCompte);

            // Si un nouveau mot de passe est fourni
            if (!empty($newPassword)) {
                $client->setMotDePasse($passwordHasher->hashPassword($client, $newPassword));
            }

            $entityManager->flush();
            $this->addFlash('success', 'Client modifiÃ© avec succÃ¨s');

            return $this->redirectToRoute('admin_clients');
        }

        return $this->render('Admin/clients/edit.html.twig', [
            'client' => $client
        ]);
    }

    #[Route('/admin/client/{id}/delete', name: 'admin_client_delete', methods: ['POST'])]
    public function deleteClient(Utilisateur $client, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($client);
        $entityManager->flush();
        $this->addFlash('success', 'Client supprimÃ© avec succÃ¨s');

        return $this->redirectToRoute('admin_clients');
    }

    // CRUD pour les Produits
    #[Route('/admin/produits', name: 'admin_produits')]
    public function produits(ProduitRepository $produitRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // RÃ©cupÃ©rer les filtres
        $search = $request->query->get('search');
        $categorie = $request->query->get('categorie');
        $statut = $request->query->get('statut');
        $prixMin = $request->query->get('prix_min');
        $prixMax = $request->query->get('prix_max');
        $stockMin = $request->query->get('stock_min');
        $stockMax = $request->query->get('stock_max');
        $sort = $request->query->get('sort', 'name');
        
        // Construire la requÃªte avec filtres
        $queryBuilder = $produitRepository->createQueryBuilder('p');
        
        // Filtrer par recherche (nom, description)
        if ($search) {
            $queryBuilder->andWhere('p.nom LIKE :search OR p.description LIKE :search')
                        ->setParameter('search', '%' . $search . '%');
        }
        
        // Filtrer par catÃ©gorie
        if ($categorie && $categorie !== 'all') {
            $queryBuilder->andWhere('p.categorie = :categorie')
                        ->setParameter('categorie', $categorie);
        }
        
        // Filtrer par statut
        if ($statut && $statut !== 'all') {
            $queryBuilder->andWhere('p.statut = :statut')
                        ->setParameter('statut', $statut);
        }
        
        // Filtrer par prix minimum
        if ($prixMin) {
            $queryBuilder->andWhere('p.prix >= :prixMin')
                        ->setParameter('prixMin', $prixMin);
        }
        
        // Filtrer par prix maximum
        if ($prixMax) {
            $queryBuilder->andWhere('p.prix <= :prixMax')
                        ->setParameter('prixMax', $prixMax);
        }
        
        // Filtrer par stock minimum
        if ($stockMin) {
            $queryBuilder->andWhere('p.quantiteStock >= :stockMin')
                        ->setParameter('stockMin', $stockMin);
        }
        
        // Filtrer par stock maximum
        if ($stockMax) {
            $queryBuilder->andWhere('p.quantiteStock <= :stockMax')
                        ->setParameter('stockMax', $stockMax);
        }
        
        // Trier
        switch ($sort) {
            case 'name':
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
            case 'price_asc':
                $queryBuilder->orderBy('p.prix', 'ASC');
                break;
            case 'price_desc':
                $queryBuilder->orderBy('p.prix', 'DESC');
                break;
            case 'stock_desc':
                $queryBuilder->orderBy('p.quantiteStock', 'DESC');
                break;
            case 'stock_asc':
                $queryBuilder->orderBy('p.quantiteStock', 'ASC');
                break;
            case 'date_desc':
                $queryBuilder->orderBy('p.id', 'DESC');
                break;
            case 'date_asc':
                $queryBuilder->orderBy('p.id', 'ASC');
                break;
            default:
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
        }
        
        $produits = $queryBuilder->getQuery()->getResult();
        
        // RÃ©cupÃ©rer toutes les catÃ©gories pour le filtre
        $categories = $produitRepository->createQueryBuilder('p')
            ->select('DISTINCT p.categorie')
            ->getQuery()
            ->getResult();
        
        $categories = array_map(function($cat) {
            return $cat['categorie'];
        }, $categories);
        
        $stats = [
            'total' => count($produits),
            'disponible' => 0,
            'indisponible' => 0,
            'rupture' => 0,
        ];

        foreach ($produits as $produit) {
            $status = (string) $produit->getStatut();
            if (array_key_exists($status, $stats)) {
                $stats[$status]++;
            }
        }

        return $this->render('Admin/produits.html.twig', [
            'produits' => $produits,
            'categories' => $categories,
            'stats' => $stats,
            'filters' => [
                'search' => $search,
                'categorie' => $categorie,
                'statut' => $statut,
                'prix_min' => $prixMin,
                'prix_max' => $prixMax,
                'stock_min' => $stockMin,
                'stock_max' => $stockMax,
                'sort' => $sort
            ]
        ]);
    }

    #[Route('/admin/produit/{id}/toggle-disponibilite', name: 'admin_produit_toggle_disponibilite', methods: ['POST'])]
    public function toggleProduitDisponibilite(Produit $produit, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $current = (string) $produit->getStatut();
        if ($current === 'disponible') {
            $produit->setStatut('indisponible');
        } else {
            $produit->setStatut('disponible');
        }

        $entityManager->flush();
        $this->addFlash('success', 'Disponibilite du produit mise a jour.');
        return $this->redirectToRoute('admin_produits');
    }

    #[Route('/admin/produit/new', name: 'admin_produit_new', methods: ['GET', 'POST'])]
    public function newProduit(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $produit = new Produit();
        $form = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Gestion de l'upload d'image
            $imageFile = $form->get('image')->getData();
            
            if ($imageFile instanceof UploadedFile) {
                try {
                    $produit->setImage($this->uploadProduitImage($imageFile));
                } catch (\Throwable $e) {
                    $this->addFlash('error', 'Upload image impossible: ' . $e->getMessage());
                }
            }
            
            // Forcer le statut si null
            if ($produit->getStatut() === null) {
                $produit->setStatut('disponible');
            }
            
            // CrÃ©er automatiquement un stock pour le produit
            $stock = new StockEntity();
            $stock->setProduit($produit);
            $stock->setQuantite($produit->getQuantiteStock() ?: 0);
            $stock->setSeuilAlerte(5);
            $stock->setDateExpiration($produit->getDateExpiration());
            $stock->setDateDerniereMiseAJour(new \DateTime());
            
            // DÃ©finir l'Ã©tat du stock
            if ($stock->getQuantite() <= 0) {
                $stock->setEtatStock('rupture');
            } elseif ($stock->getQuantite() <= 5) {
                $stock->setEtatStock('alerte');
            } else {
                $stock->setEtatStock('disponible');
            }
            
            $entityManager->persist($stock);
            $entityManager->persist($produit);
            $entityManager->flush();
            $this->addFlash('success', 'Produit ajoutÃ© avec succÃ¨s (stock crÃ©Ã© automatiquement)');
            
            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Produit enregistre avec succes.',
                    'redirect_url' => $this->generateUrl('admin_produits'),
                ]);
            }

            return $this->redirectToRoute('admin_produits');
        }
        
        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
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
        
        // Sauvegarder l'ancienne image
        $ancienneImage = $produit->getImage();
        
        $form = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Gestion de l'upload d'image
            $imageFile = $form->get('image')->getData();
            
            if ($imageFile instanceof UploadedFile) {
                try {
                    $newFilename = $this->uploadProduitImage($imageFile);
                    $produit->setImage($newFilename);
                    $this->removeProduitImage($ancienneImage);
                } catch (\Throwable $e) {
                    $this->addFlash('error', 'Upload image impossible: ' . $e->getMessage());
                }
            }
            
            $entityManager->flush();
            $this->addFlash('success', 'Produit modifiÃ© avec succÃ¨s');
            
            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Produit modifie avec succes.',
                    'redirect_url' => $this->generateUrl('admin_produits'),
                ]);
            }

            return $this->redirectToRoute('admin_produits');
        }
        
        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
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
        $this->addFlash('success', 'Produit supprimÃ© avec succÃ¨s');
        
        return $this->redirectToRoute('admin_produits');
    }

    #[Route('/admin/commandes', name: 'admin_commandes')]
    public function commandes(CommandeRepository $commandeRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // RÃ©cupÃ©rer les filtres
        $search = $request->query->get('search');
        $statut = $request->query->get('statut');
        $dateMin = $request->query->get('date_min');
        $dateMax = $request->query->get('date_max');
        $montantMin = $request->query->get('montant_min');
        $montantMax = $request->query->get('montant_max');
        $fraudOnly = (string) $request->query->get('fraud_only', '0');
        $sort = $request->query->get('sort', 'date_desc');
        
        // Construire la requÃªte avec filtres
        $queryBuilder = $commandeRepository->createQueryBuilder('c');
        
        // Filtrer par recherche (nom, email, tÃ©lÃ©phone)
        if ($search) {
            $queryBuilder->andWhere('c.nom LIKE :search OR c.email LIKE :search OR c.telephone LIKE :search')
                        ->setParameter('search', '%' . $search . '%');
        }
        
        // Filtrer par statut
        if ($statut && $statut !== 'all') {
            $queryBuilder->andWhere('c.statut = :statut')
                        ->setParameter('statut', $statut);
        }
        
        // Filtrer par date minimum
        if ($dateMin) {
            $dateMinObj = new \DateTime($dateMin);
            $queryBuilder->andWhere('c.dateCommande >= :dateMin')
                        ->setParameter('dateMin', $dateMinObj);
        }
        
        // Filtrer par date maximum
        if ($dateMax) {
            $dateMaxObj = new \DateTime($dateMax);
            $dateMaxObj->setTime(23, 59, 59); // Inclure toute la journÃ©e
            $queryBuilder->andWhere('c.dateCommande <= :dateMax')
                        ->setParameter('dateMax', $dateMaxObj);
        }
        
        // Filtrer par montant minimum
        if ($montantMin) {
            $queryBuilder->andWhere('c.total >= :montantMin')
                        ->setParameter('montantMin', $montantMin);
        }
        
        // Filtrer par montant maximum
        if ($montantMax) {
            $queryBuilder->andWhere('c.total <= :montantMax')
                        ->setParameter('montantMax', $montantMax);
        }

        if ($fraudOnly === '1') {
            $queryBuilder->andWhere('c.fraudScore >= :fraudThreshold')
                ->setParameter('fraudThreshold', 70);
        }
        
        // Trier
        switch ($sort) {
            case 'date_asc':
                $queryBuilder->orderBy('c.dateCommande', 'ASC');
                break;
            case 'montant_desc':
                $queryBuilder->orderBy('c.total', 'DESC');
                break;
            case 'montant_asc':
                $queryBuilder->orderBy('c.total', 'ASC');
                break;
            case 'name_asc':
                $queryBuilder->orderBy('c.nom', 'ASC');
                break;
            case 'name_desc':
                $queryBuilder->orderBy('c.nom', 'DESC');
                break;
            case 'date_desc':
            default:
                $queryBuilder->orderBy('c.dateCommande', 'DESC');
                break;
        }
        
        $commandes = $queryBuilder->getQuery()->getResult();
        $stats = [
            'total' => count($commandes),
            'en_attente' => 0,
            'confirmee' => 0,
            'annulee' => 0,
            'livree' => 0,
            'review' => 0,
            'bloquee' => 0,
            'suspectes' => 0,
            'montant_total' => 0.0,
        ];

        foreach ($commandes as $commande) {
            $status = (string) $commande->getStatut();
            if (array_key_exists($status, $stats)) {
                $stats[$status]++;
            }
            if ((int) $commande->getFraudScore() >= 70) {
                $stats['suspectes']++;
            }
            $stats['montant_total'] += (float) $commande->getTotal();
        }
        
        return $this->render('Admin/commandes.html.twig', [
            'commandes' => $commandes,
            'stats' => $stats,
            'filters' => [
                'search' => $search,
                'statut' => $statut,
                'date_min' => $dateMin,
                'date_max' => $dateMax,
                'montant_min' => $montantMin,
                'montant_max' => $montantMax,
                'fraud_only' => $fraudOnly,
                'sort' => $sort
            ]
        ]);
    }

    #[Route('/admin/commande/{id}/status', name: 'admin_commande_status', methods: ['POST'])]
    public function updateCommandeStatus(
        Commande $commande,
        Request $request,
        EntityManagerInterface $entityManager,
        MailerService $mailerService,
        SmsService $smsService
    ): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $newStatus = (string) $request->request->get('status', '');
        $allowed = ['en_attente', 'confirmee', 'annulee', 'livree', 'review', 'bloquee'];

        if (!in_array($newStatus, $allowed, true)) {
            $this->addFlash('error', 'Statut invalide.');
            return $this->redirectToRoute('admin_commandes');
        }

        $oldStatus = (string) $commande->getStatut();
        $commande->setStatut($newStatus);
        $entityManager->flush();

        if ($oldStatus !== $newStatus) {
            try {
                $mailerService->sendCommandeStatusUpdateEmail($commande, $oldStatus, $newStatus);
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Statut modifie, mais email non envoye.');
            }

            if ($newStatus === 'livree') {
                try {
                    $smsSent = $smsService->sendSms(
                        (string) $commande->getTelephone(),
                        sprintf('CURAVITA: votre commande #%d est livree. Merci pour votre confiance.', (int) $commande->getId())
                    );
                    if (!$smsSent) {
                        $detail = $smsService->getLastError();
                        $this->addFlash('error', 'Statut livre, mais SMS non envoye.' . ($detail ? ' Detail: ' . $detail : ''));
                    }
                } catch (\Throwable $e) {
                    $this->addFlash('error', 'Statut livre, mais erreur lors de l envoi SMS: ' . $e->getMessage());
                }
            }
        }

        $this->addFlash('success', 'Statut de commande mis a jour.');
        return $this->redirectToRoute('admin_commandes');
    }

    #[Route('/admin/commandes/export/csv', name: 'admin_commandes_export_csv', methods: ['GET'])]
    public function exportCommandesCsv(CommandeRepository $commandeRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $commandes = $commandeRepository->findBy([], ['dateCommande' => 'DESC']);
        $rows = ["ID;Date;Nom;Email;Telephone;Adresse;ModePaiement;Statut;Coupon;Remise;FraudScore;LivraisonEstimee;Total"];

        foreach ($commandes as $commande) {
            $rows[] = implode(';', [
                $commande->getId(),
                $commande->getDateCommande()?->format('Y-m-d H:i:s'),
                $commande->getNom(),
                $commande->getEmail(),
                $commande->getTelephone(),
                str_replace(';', ',', (string) $commande->getAdresseLivraison()),
                $commande->getModePaiement(),
                $commande->getStatut(),
                (string) ($commande->getCouponCode() ?? ''),
                number_format((float) $commande->getCouponDiscount(), 2, '.', ''),
                (string) $commande->getFraudScore(),
                $commande->getEstimatedDeliveryDate()?->format('Y-m-d H:i:s'),
                number_format((float) $commande->getTotal(), 2, '.', ''),
            ]);
        }

        $content = "\xEF\xBB\xBF" . implode("\n", $rows);
        $response = new Response($content);
        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', 'attachment; filename=\"commandes_' . date('Ymd_His') . '.csv\"');
        return $response;
    }

    #[Route('/admin/commandes/export/pdf', name: 'admin_commandes_export_pdf', methods: ['GET'])]
    public function exportCommandesPdf(CommandeRepository $commandeRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $commandes = $commandeRepository->findBy([], ['dateCommande' => 'DESC']);

        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->set('isRemoteEnabled', false);
        $pdfOptions->set('isHtml5ParserEnabled', true);

        $dompdf = new Dompdf($pdfOptions);
        $html = $this->renderView('Admin/commandes_pdf.html.twig', [
            'commandes' => $commandes,
            'date' => new \DateTime(),
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $response = new Response($dompdf->output());
        $response->headers->set('Content-Type', 'application/pdf');
        $response->headers->set('Content-Disposition', $response->headers->makeDisposition(
            ResponseHeaderBag::DISPOSITION_ATTACHMENT,
            'commandes_' . date('Y-m-d_H-i-s') . '.pdf'
        ));

        return $response;
    }

    #[Route('/admin/coupons', name: 'admin_coupons', methods: ['GET'])]
    public function coupons(CouponRepository $couponRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $search = trim((string) $request->query->get('search', ''));
        $active = (string) $request->query->get('active', 'all');
        $sort = (string) $request->query->get('sort', 'date_desc');

        $qb = $couponRepository->createQueryBuilder('cp');
        if ($search !== '') {
            $qb->andWhere('cp.code LIKE :search')
                ->setParameter('search', '%' . strtoupper($search) . '%');
        }
        if ($active !== 'all') {
            $qb->andWhere('cp.actif = :active')->setParameter('active', $active === '1');
        }

        switch ($sort) {
            case 'code_asc':
                $qb->orderBy('cp.code', 'ASC');
                break;
            case 'usage_desc':
                $qb->orderBy('cp.usageCount', 'DESC');
                break;
            case 'value_desc':
                $qb->orderBy('cp.valeur', 'DESC');
                break;
            case 'date_asc':
                $qb->orderBy('cp.dateExpiration', 'ASC');
                break;
            case 'date_desc':
            default:
                $qb->orderBy('cp.dateExpiration', 'DESC');
                break;
        }

        $coupons = $qb->getQuery()->getResult();

        return $this->render('Admin/coupons.html.twig', [
            'coupons' => $coupons,
            'filters' => [
                'search' => $search,
                'active' => $active,
                'sort' => $sort,
            ],
        ]);
    }

    #[Route('/admin/coupon/new', name: 'admin_coupon_new', methods: ['GET', 'POST'])]
    public function newCoupon(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $coupon = new Coupon();
        $form = $this->createForm(CouponType::class, $coupon);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($coupon);
            $entityManager->flush();
            $this->addFlash('success', 'Coupon cree avec succes.');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Coupon enregistre avec succes.',
                    'redirect_url' => $this->generateUrl('admin_coupons'),
                ]);
            }

            return $this->redirectToRoute('admin_coupons');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
        }

        return $this->render('Admin/coupon_form.html.twig', [
            'form' => $form->createView(),
            'coupon' => null,
        ]);
    }

    #[Route('/admin/coupon/{id}/edit', name: 'admin_coupon_edit', methods: ['GET', 'POST'])]
    public function editCoupon(Coupon $coupon, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $form = $this->createForm(CouponType::class, $coupon);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Coupon modifie avec succes.');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Coupon modifie avec succes.',
                    'redirect_url' => $this->generateUrl('admin_coupons'),
                ]);
            }

            return $this->redirectToRoute('admin_coupons');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
        }

        return $this->render('Admin/coupon_form.html.twig', [
            'form' => $form->createView(),
            'coupon' => $coupon,
        ]);
    }

    #[Route('/admin/coupon/{id}/delete', name: 'admin_coupon_delete', methods: ['POST'])]
    public function deleteCoupon(Coupon $coupon, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($coupon);
        $entityManager->flush();
        $this->addFlash('success', 'Coupon supprime avec succes.');

        return $this->redirectToRoute('admin_coupons');
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
            $this->addFlash('success', 'Commande ajoutÃ©e avec succÃ¨s');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Commande enregistree avec succes.',
                    'redirect_url' => $this->generateUrl('admin_commandes'),
                ]);
            }

            return $this->redirectToRoute('admin_commandes');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
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
            $this->addFlash('success', 'Commande modifiÃ©e avec succÃ¨s');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Commande modifiee avec succes.',
                    'redirect_url' => $this->generateUrl('admin_commandes'),
                ]);
            }

            return $this->redirectToRoute('admin_commandes');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
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
        $this->addFlash('success', 'Commande supprimÃ©e avec succÃ¨s');
        
        return $this->redirectToRoute('admin_commandes');
    }

    // CRUD pour les DÃ©pÃ´ts
    #[Route('/admin/depots', name: 'admin_depots')]
    public function depots(DepotRepository $depotRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // RÃ©cupÃ©rer les filtres
        $search = $request->query->get('search');
        $ville = $request->query->get('ville');
        $capaciteMin = $request->query->get('capacite_min');
        $capaciteMax = $request->query->get('capacite_max');
        $sort = $request->query->get('sort', 'name');
        
        // Construire la requÃªte avec filtres
        $queryBuilder = $depotRepository->createQueryBuilder('d');
        
        // Filtrer par recherche (nom, adresse, responsable)
        if ($search) {
            $queryBuilder->andWhere('d.nomDepot LIKE :search OR d.adresseDepot LIKE :search OR d.responsableDepot LIKE :search')
                        ->setParameter('search', '%' . $search . '%');
        }
        
        // Filtrer par ville (extraite de l'adresse)
        if ($ville) {
            $queryBuilder->andWhere('d.adresseDepot LIKE :ville')
                        ->setParameter('ville', '%' . $ville . '%');
        }
        
        // Filtrer par capacitÃ© minimum
        if ($capaciteMin) {
            $queryBuilder->andWhere('d.capaciteDepot >= :capaciteMin')
                        ->setParameter('capaciteMin', $capaciteMin);
        }
        
        // Filtrer par capacitÃ© maximum
        if ($capaciteMax) {
            $queryBuilder->andWhere('d.capaciteDepot <= :capaciteMax')
                        ->setParameter('capaciteMax', $capaciteMax);
        }
        
        // Trier
        switch ($sort) {
            case 'name':
                $queryBuilder->orderBy('d.nomDepot', 'ASC');
                break;
            case 'capacity_desc':
                $queryBuilder->orderBy('d.capaciteDepot', 'DESC');
                break;
            case 'capacity_asc':
                $queryBuilder->orderBy('d.capaciteDepot', 'ASC');
                break;
            case 'date_desc':
                $queryBuilder->orderBy('d.dateCreation', 'DESC');
                break;
            case 'date_asc':
                $queryBuilder->orderBy('d.dateCreation', 'ASC');
                break;
            default:
                $queryBuilder->orderBy('d.nomDepot', 'ASC');
                break;
        }
        
        $depots = $queryBuilder->getQuery()->getResult();
        
        // RÃ©cupÃ©rer toutes les villes uniques pour le filtre
        $villes = $depotRepository->createQueryBuilder('d')
            ->select('DISTINCT d.adresseDepot')
            ->getQuery()
            ->getResult();
        
        // Extraire les villes des adresses (simplifiÃ©)
        $villesList = [];
        foreach ($villes as $villeData) {
            $parts = explode(',', $villeData['adresseDepot']);
            if (count($parts) > 1) {
                $villeName = trim($parts[count($parts) - 1]);
                if (!in_array($villeName, $villesList)) {
                    $villesList[] = $villeName;
                }
            }
        }
        
        return $this->render('Admin/depots.html.twig', [
            'depots' => $depots,
            'villes' => $villesList,
            'filters' => [
                'search' => $search,
                'ville' => $ville,
                'capacite_min' => $capaciteMin,
                'capacite_max' => $capaciteMax,
                'sort' => $sort
            ]
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
            $this->addFlash('success', 'DÃ©pÃ´t ajoutÃ© avec succÃ¨s');
            
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
            $this->addFlash('success', 'DÃ©pÃ´t modifiÃ© avec succÃ¨s');
            
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
        $this->addFlash('success', 'DÃ©pÃ´t supprimÃ© avec succÃ¨s');
        
        return $this->redirectToRoute('admin_depots');
    }

    // Dashboard avancÃ© des stocks
    #[Route('/admin/stocks/dashboard', name: 'admin_stocks_dashboard')]
    public function stocksDashboard(StockService $stockService): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Obtenir les statistiques globales
        $statistiques = $stockService->getStatistiquesGlobales();
        
        // Obtenir les alertes
        $alertes = $stockService->getAlertesStock();
        
        // Obtenir les suggestions de rÃ©approvisionnement
        $suggestions = $stockService->getSuggestionsReapprovisionnement();
        
        // GÃ©nÃ©rer le rapport complet
        $rapport = $stockService->genererRapportInventaire();
        
        return $this->render('Admin/stocks_dashboard.html.twig', [
            'statistiques' => $statistiques,
            'alertes' => $alertes,
            'suggestions' => $suggestions,
            'rapport' => $rapport
        ]);
    }

    // CRUD pour les Stocks
    #[Route('/admin/stocks', name: 'admin_stocks')]
    public function stocks(StockRepository $stockRepository, Request $request, StockService $stockService): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // RÃ©cupÃ©rer les filtres
        $search = $request->query->get('search');
        $etat = $request->query->get('etat');
        $quantiteMin = $request->query->get('quantite_min');
        $quantiteMax = $request->query->get('quantite_max');
        $sort = $request->query->get('sort', 'produit');
        
        $queryBuilder = $stockRepository->createQueryBuilder('s')
            ->leftJoin('s.produit', 'p')
            ->leftJoin('s.depot', 'd')
            ->addSelect('p')
            ->addSelect('d');
        
        if ($search) {
            $queryBuilder->andWhere('p.nom LIKE :search')
                        ->setParameter('search', '%' . $search . '%');
        }
        
        if ($etat && $etat !== 'all') {
            $queryBuilder->andWhere('s.etatStock = :etat')
                        ->setParameter('etat', $etat);
        }
        
        if ($quantiteMin) {
            $queryBuilder->andWhere('s.quantite >= :quantiteMin')
                        ->setParameter('quantiteMin', $quantiteMin);
        }
        
        if ($quantiteMax) {
            $queryBuilder->andWhere('s.quantite <= :quantiteMax')
                        ->setParameter('quantiteMax', $quantiteMax);
        }
        
        switch ($sort) {
            case 'produit':
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
            case 'quantity_desc':
                $queryBuilder->orderBy('s.quantite', 'DESC');
                break;
            case 'quantity_asc':
                $queryBuilder->orderBy('s.quantite', 'ASC');
                break;
            case 'date_desc':
                $queryBuilder->orderBy('s.dateDerniereMiseAJour', 'DESC');
                break;
            case 'date_asc':
                $queryBuilder->orderBy('s.dateDerniereMiseAJour', 'ASC');
                break;
            default:
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
        }
        
        $stocks = $queryBuilder->getQuery()->getResult();
        
        return $this->render('Admin/stocks_advanced.html.twig', [
            'stocks' => $stocks,
            'filters' => [
                'search' => $search,
                'etat' => $etat,
                'quantite_min' => $quantiteMin,
                'quantite_max' => $quantiteMax,
                'sort' => $sort
            ]
        ]);
    }

    #[Route('/admin/stock/new', name: 'admin_stock_new', methods: ['GET', 'POST'])]
    public function newStock(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $stock = new Stock();
        // Initialiser la date de derniÃ¨re mise Ã  jour avec la date actuelle
        $stock->setDateDerniereMiseAJour(new \DateTime());
        
        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($stock);
            $entityManager->flush();
            $this->addFlash('success', 'Stock ajoutÃ© avec succÃ¨s');
            
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
            $this->addFlash('success', 'Stock modifiÃ© avec succÃ¨s');
            
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
        $this->addFlash('success', 'Stock supprimÃ© avec succÃ¨s');
        
        return $this->redirectToRoute('admin_stocks');
    }

    private function uploadProduitImage(UploadedFile $imageFile): string
    {
        $uploadDir = $this->getParameter('kernel.project_dir') . '/public/uploads/produits';
        if (!is_dir($uploadDir) && !mkdir($uploadDir, 0777, true) && !is_dir($uploadDir)) {
            throw new \RuntimeException('Impossible de creer le dossier de destination.');
        }

        $slugger = new AsciiSlugger();
        $originalFilename = pathinfo((string) $imageFile->getClientOriginalName(), PATHINFO_FILENAME);
        $safeFilename = strtolower((string) $slugger->slug($originalFilename));
        if ($safeFilename === '') {
            $safeFilename = 'produit';
        }

        $extension = $imageFile->guessExtension();
        if (!$extension) {
            $mimeMap = [
                'image/jpeg' => 'jpg',
                'image/png' => 'png',
                'image/gif' => 'gif',
                'image/webp' => 'webp',
            ];
            $extension = $mimeMap[(string) $imageFile->getMimeType()] ?? 'bin';
        }

        $newFilename = sprintf('%s-%s.%s', $safeFilename, bin2hex(random_bytes(6)), $extension);
        $imageFile->move($uploadDir, $newFilename);

        return $newFilename;
    }

    private function removeProduitImage(?string $imageName): void
    {
        if (!$imageName) {
            return;
        }

        $path = $this->getParameter('kernel.project_dir') . '/public/uploads/produits/' . $imageName;
        if (is_file($path)) {
            @unlink($path);
        }
    }

    /**
     * @return array<string, array<int, string>>
     */
    private function extractFormErrors(FormInterface $form): array
    {
        $errors = [];
        foreach ($form->getErrors(true, true) as $error) {
            $origin = $error->getOrigin();
            $field = $origin ? $origin->getName() : '_form';
            $errors[$field] ??= [];
            $errors[$field][] = $error->getMessage();
        }

        return $errors;
    }
}

