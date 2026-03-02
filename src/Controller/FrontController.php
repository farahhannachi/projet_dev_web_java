<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Entity\Stock;
use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Entity\Produit;
use App\Entity\Commande;
use App\Entity\Coupon;
use App\Entity\Address;
use App\Entity\Utilisateur;
use App\Entity\Question;
use App\Form\OrdonnanceFrontType;
use App\Form\TraitementFrontType;
use App\Form\AddressValidatedType;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use App\Repository\CommandeRepository;
use App\Repository\ProduitRepository;
use App\Repository\AddressRepository;
use App\Repository\QuestionRepository;
use App\Service\MailerService;
use App\Service\StockAssistantService;
use App\Service\PanierService;
use App\Service\CouponService;
use App\Service\DeliveryEstimatorService;
use App\Service\FraudDetectionService;
use App\Service\LoyaltyService;
use App\Service\OrderSplitService;
use App\Service\ShippingCalculatorService;
use App\Service\DepotHealthScoreService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Form\FormInterface;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class FrontController extends AbstractController
{
    #[Route('/', name: 'app_home', methods: ['GET'])]
    public function home(PanierService $panierService): Response
    {
        return $this->render('front/home.html.twig', [
            'nombre_articles_panier' => $panierService->getNombreArticles()
        ]);
    }

    #[Route('/products', name: 'app_products', methods: ['GET'])]
    public function products(ProduitRepository $produitRepository, PanierService $panierService, Request $request): Response
    {
        $produits = $produitRepository->createQueryBuilder('p')
            ->where('p.statut IN (:statuts)')
            ->setParameter('statuts', ['disponible', 'stock_critique'])
            ->orderBy('p.nom', 'ASC')
            ->getQuery()
            ->getResult();

        $categories = $produitRepository->createQueryBuilder('p')
            ->select('DISTINCT p.categorie')
            ->where('p.statut IN (:statuts)')
            ->setParameter('statuts', ['disponible', 'stock_critique'])
            ->orderBy('p.categorie', 'ASC')
            ->getQuery()
            ->getArrayResult();

        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);

        return $this->render('front/products.html.twig', [
            'produits' => $produits,
            'categories' => array_map(static fn (array $row) => $row['categorie'], $categories),
            'nombre_articles_panier' => $panierDetails['count'],
            'panier' => $panierDetails,
        ]);
    }
    
    #[Route('/about', name: 'app_about', methods: ['GET'])]
    public function about(PanierService $panierService): Response
    {
        return $this->render('front/about.html.twig', [
            'nombre_articles_panier' => $panierService->getNombreArticles()
        ]);
    }

    #[Route('/contact', name: 'app_contact', methods: ['GET', 'POST'])]
    public function contact(Request $request, EntityManagerInterface $entityManager, QuestionRepository $questionRepository, MailerService $mailerService): Response
    {
        $editMode = false;
        $editQuestionId = $request->query->get('edit');
        
        // Mode edition : charger la question existante
        if ($editQuestionId && $this->getUser()) {
            $question = $questionRepository->find($editQuestionId);
            
            // Verifier que la question existe et appartient a l'utilisateur
            if ($question && $question->getUtilisateur() === $this->getUser()) {
                $editMode = true;
            } else {
                $this->addFlash('error', 'Question introuvable ou acces non autorise.');
                return $this->redirectToRoute('app_contact');
            }
        } else {
            // Mode creation : nouvelle question
            $question = new \App\Entity\Question();
            $question->setStatut('ouvert');
            
            $user = $this->getUser();
            if ($user instanceof \App\Entity\Utilisateur) {
                $question->setUtilisateur($user);
            }
        }
        
        $form = $this->createForm(\App\Form\QuestionType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted()) {
            // Si l'utilisateur n'est pas connecte, bloquer la soumission
            if (!$this->getUser()) {
                $this->addFlash('error', 'Vous devez etre connecte pour envoyer un ticket.');
                return $this->redirectToRoute('app_login');
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            // Gerer le fichier uploadé
            $fichier = $form->get('fichier')->getData();
            if ($fichier) {
                $originalName = $fichier->getClientOriginalName();
                $mimeType = $fichier->getMimeType();
                $fileSize = $fichier->getSize();
                
                $slugger = new \Symfony\Component\String\Slugger\AsciiSlugger();
                $originalFilename = pathinfo($originalName, PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename.'-'.uniqid().'.'.$fichier->guessExtension();

                try {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $uploadDir = (is_string($projectDir) ? $projectDir : '').'/public/uploads/questions';
                    if (!is_dir($uploadDir)) {
                        mkdir($uploadDir, 0777, true);
                    }
                    
                    $fichier->move($uploadDir, $newFilename);
                    
                    $question->setFileName($originalName);
                    $question->setFilePath('/uploads/questions/'.$newFilename);
                    $question->setFileType($mimeType);
                    $question->setFileSize($fileSize);
                } catch (\Symfony\Component\HttpFoundation\File\Exception\FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l\'upload du fichier');
                }
            }

            $entityManager->persist($question);
            $entityManager->flush();

            if ($editMode) {
                $this->addFlash('success', 'Votre ticket a ete modifie avec succes !');
            } else {
                $mailerService->sendTicketCreatedEmail($question);
                $this->addFlash('success', 'Votre message a ete envoye avec succes ! Un email de confirmation vous a ete envoye.');
            }

            return $this->redirectToRoute('app_contact');
        }

        $mesQuestions = [];
        $user = $this->getUser();
        if ($user instanceof \App\Entity\Utilisateur) {
            $userId = $user->getId();
            if ($userId !== null) {
                $mesQuestions = $questionRepository->findByUtilisateur($userId);
            }
        }

        return $this->render('front/contact.html.twig', [
            'form' => $form,
            'mesQuestions' => $mesQuestions,
            'editMode' => $editMode,
            'editQuestion' => $editMode ? $question : null,
        ]);
    }

    #[Route('/ordonnance', name: 'app_ordonnance', methods: ['GET', 'POST'])]
    #[Route('/formulaire-ordonnance', name: 'app_formulaire_ordonnance', methods: ['GET', 'POST'])]
    public function ordonnance(
        Request $request, 
        EntityManagerInterface $entityManager,
        TraitementRepository $traitementRepository
    ): Response
    {
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez etre connecte pour acceder a cette page');
            return $this->redirectToRoute('app_login');
        }

        $produitsIds = $request->query->get('produits');
        $produitsSelectionnes = [];
        
        if ($produitsIds && is_string($produitsIds)) {
            $idsArray = explode(',', $produitsIds);
            foreach ($idsArray as $id) {
                $produit = $entityManager->getRepository(Produit::class)->find($id);
                if ($produit) {
                    $produitsSelectionnes[] = $produit;
                }
            }
        }

        $ordonnance = new Ordonnance();
        $ordonnance->setStatut('en attente');
        $user = $this->getUser();
        if ($user instanceof \App\Entity\Utilisateur) {
            $ordonnance->setUtilisateur($user);
        }
        
        $numeroOrdonnance = 'ORD-' . date('Y') . '-' . str_pad((string) rand(1, 9999), 4, '0', STR_PAD_LEFT);
        $ordonnance->setNumeroOrdonnance($numeroOrdonnance);
        
        $dateExpiration = new \DateTime();
        $dateExpiration->modify('+3 months');
        $ordonnance->setDateExpiration($dateExpiration);
        
        $formOrdonnance = $this->createForm(OrdonnanceFrontType::class, $ordonnance);
        $formOrdonnance->handleRequest($request);
        
        if ($formOrdonnance->isSubmitted() && $formOrdonnance->isValid()) {
            $dateOrdonnance = $ordonnance->getDateOrdonnance();
            $dateExpiration = $ordonnance->getDateExpiration();
            
            if ($dateExpiration <= $dateOrdonnance) {
                $this->addFlash('error', 'La date d\'expiration doit etre posterieure a la date de l\'ordonnance');
            } else {
                $entityManager->persist($ordonnance);
                $entityManager->flush();
                
                if (!empty($produitsSelectionnes)) {
                    $currentUser = $this->getUser();
                    if (!$currentUser instanceof Utilisateur) {
                        $currentUser = null;
                    }
                    foreach ($produitsSelectionnes as $produit) {
                        $traitement = new Traitement();
                        $traitement->setOrdonnance($ordonnance);
                        $traitement->setUtilisateur($currentUser);
                        $traitement->setProduit($produit);
                        $traitement->setStatus('en attente');
                        $traitement->setNotes('Demande de traitement pour: ' . $produit->getNom());
                        
                        $entityManager->persist($traitement);
                    }
                    $entityManager->flush();
                }
                
                $this->addFlash('success', 'Votre ordonnance a ete envoyee avec succes ! Un pharmacien va la verifier.');
                return $this->redirectToRoute('app_mes_traitements');
            }
        }

        return $this->render('front/ordonnance.html.twig', [
            'form' => $formOrdonnance->createView(),
            'produitsSelectionnes' => $produitsSelectionnes
        ]);
    }


    #[Route('/mes-traitements', name: 'app_mes_traitements', methods: ['GET'])]
    #[Route('/ordonnances', name: 'app_ordonnances', methods: ['GET'])]
    public function mesTraitements(
        Request $request,
        TraitementRepository $traitementRepository,
        OrdonnanceRepository $ordonnanceRepository
    ): Response
    {
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez etre connecte pour acceder a cette page');
            return $this->redirectToRoute('app_login');
        }

        $searchTerm = $request->query->get('search', '');

        if ($searchTerm) {
            $traitements = $traitementRepository->createQueryBuilder('t')
                ->leftJoin('t.ordonnance', 'o')
                ->where('t.utilisateur = :user')
                ->andWhere('o.numeroOrdonnance LIKE :search OR t.dosage LIKE :search OR t.frequence LIKE :search OR t.notes LIKE :search OR t.status LIKE :search')
                ->setParameter('user', $this->getUser())
                ->setParameter('search', '%' . $searchTerm . '%')
                ->orderBy('t.id', 'DESC')
                ->getQuery()
                ->getResult();
        } else {
            $traitements = $traitementRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['id' => 'DESC']
            );
        }

        if ($searchTerm) {
            $ordonnances = $ordonnanceRepository->createQueryBuilder('o')
                ->where('o.utilisateur = :user')
                ->andWhere('o.numeroOrdonnance LIKE :search OR o.noteMedical LIKE :search OR o.statut LIKE :search')
                ->setParameter('user', $this->getUser())
                ->setParameter('search', '%' . $searchTerm . '%')
                ->orderBy('o.dateOrdonnance', 'DESC')
                ->getQuery()
                ->getResult();
        } else {
            $ordonnances = $ordonnanceRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['dateOrdonnance' => 'DESC']
            );
        }

        return $this->render('front/mes_traitements.html.twig', [
            'traitements' => $traitements,
            'ordonnances' => $ordonnances,
            'searchTerm' => $searchTerm
        ]);
    }

    #[Route('/mes-traitements/search', name: 'app_mes_traitements_search', methods: ['GET'])]
    public function mesTraitementsSearch(
        Request $request,
        TraitementRepository $traitementRepository,
        OrdonnanceRepository $ordonnanceRepository
    ): Response
    {
        if (!$this->getUser()) {
            return $this->json(['error' => 'Non autorise'], 401);
        }

        /** @var string $searchTerm */
        $searchTerm = (string) $request->query->get('search', '');

        if ($searchTerm) {
            $isNumericSearch = ctype_digit($searchTerm);
            
            if ($isNumericSearch && strlen($searchTerm) == 4) {
                $allOrdonnances = $ordonnanceRepository->findBy(
                    ['utilisateur' => $this->getUser()],
                    ['dateOrdonnance' => 'DESC']
                );
                
                $ordonnances = array_filter($allOrdonnances, function($ordonnance) use ($searchTerm) {
                    $numero = $ordonnance->getNumeroOrdonnance();
                    return $numero !== null && substr($numero, -4) === $searchTerm;
                });
            } else {
                $qb = $ordonnanceRepository->createQueryBuilder('o')
                    ->where('o.utilisateur = :user')
                    ->setParameter('user', $this->getUser());
                
                if ($isNumericSearch) {
                    $qb->andWhere('o.numeroOrdonnance LIKE :search')
                       ->setParameter('search', '%' . $searchTerm . '%');
                } else {
                    $qb->andWhere('o.numeroOrdonnance LIKE :search OR o.noteMedical LIKE :search OR o.statut LIKE :search')
                       ->setParameter('search', '%' . $searchTerm . '%');
                }
                
                $ordonnances = $qb->orderBy('o.dateOrdonnance', 'DESC')
                    ->getQuery()
                    ->getResult();
            }
        } else {
            $ordonnances = $ordonnanceRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['dateOrdonnance' => 'DESC']
            );
        }

        if ($searchTerm) {
            $isNumericSearch = ctype_digit($searchTerm);
            
            if ($isNumericSearch && strlen((string) $searchTerm) == 4) {
                $allTraitements = $traitementRepository->createQueryBuilder('t')
                    ->leftJoin('t.ordonnance', 'o')
                    ->leftJoin('t.produit', 'p')
                    ->where('t.utilisateur = :user')
                    ->setParameter('user', $this->getUser())
                    ->orderBy('t.id', 'DESC')
                    ->getQuery()
                    ->getResult();
                
                $traitements = array_filter($allTraitements, function($traitement) use ($searchTerm) {
                    $numero = $traitement->getOrdonnance()->getNumeroOrdonnance();
                    return substr($numero, -4) === $searchTerm;
                });
            } else {
                $qb = $traitementRepository->createQueryBuilder('t')
                    ->leftJoin('t.ordonnance', 'o')
                    ->leftJoin('t.produit', 'p')
                    ->where('t.utilisateur = :user')
                    ->setParameter('user', $this->getUser());
                
                if ($isNumericSearch) {
                    $qb->andWhere('o.numeroOrdonnance LIKE :search')
                       ->setParameter('search', '%' . $searchTerm . '%');
                } else {
                    $qb->andWhere('o.numeroOrdonnance LIKE :search OR t.dosage LIKE :search OR t.frequence LIKE :search OR t.notes LIKE :search OR t.status LIKE :search OR p.nom LIKE :search')
                       ->setParameter('search', '%' . $searchTerm . '%');
                }
                
                $traitements = $qb->orderBy('t.id', 'DESC')
                    ->getQuery()
                    ->getResult();
            }
        } else {
            $traitements = $traitementRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['id' => 'DESC']
            );
        }

        $html = $this->renderView('front/_traitements_list.html.twig', [
            'traitements' => $traitements,
            'ordonnances' => $ordonnances,
            'searchTerm' => $searchTerm
        ]);

        return $this->json([
            'html' => $html,
            'count' => count($ordonnances)
        ]);
    }

    #[Route('/ordonnance/{id}/pdf', name: 'app_ordonnance_pdf', methods: ['GET'])]
    public function ordonnancePdf(Ordonnance $ordonnance): Response
    {
        if (!$this->getUser() || $ordonnance->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas acces a cette ordonnance');
        }

        $html = $this->renderView('front/pdf/ordonnance_pdf.html.twig', [
            'ordonnance' => $ordonnance
        ]);

        $options = new Options();
        $options->set('defaultFont', 'Arial');
        $options->set('isRemoteEnabled', true);
        $options->set('isHtml5ParserEnabled', true);
        
        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $filename = 'Ordonnance_' . $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"'
        ]);
    }

    #[Route('/ordonnance/{id}/complete-pdf', name: 'app_ordonnance_complete_pdf', methods: ['GET'])]
    public function ordonnanceCompletePdf(Ordonnance $ordonnance): Response
    {
        if (!$this->getUser() || $ordonnance->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas acces a cette ordonnance');
        }

        $html = $this->renderView('front/pdf/ordonnance_complete_pdf.html.twig', [
            'ordonnance' => $ordonnance
        ]);

        $options = new Options();
        $options->set('defaultFont', 'Arial');
        $options->set('isRemoteEnabled', true);
        $options->set('isHtml5ParserEnabled', true);
        
        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $filename = 'Ordonnance_Complete_' . $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"'
        ]);
    }

    #[Route('/traitement/{id}/pdf', name: 'app_traitement_pdf', methods: ['GET'])]
    public function traitementPdf(Traitement $traitement): Response
    {
        if (!$this->getUser() || $traitement->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas acces a ce traitement');
        }

        $html = $this->renderView('front/pdf/traitement_pdf.html.twig', [
            'traitement' => $traitement
        ]);

        $options = new Options();
        $options->set('defaultFont', 'Arial');
        $options->set('isRemoteEnabled', true);
        $options->set('isHtml5ParserEnabled', true);
        
        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $ordonnance = $traitement->getOrdonnance();
        $numeroOrdonnance = $ordonnance !== null ? $ordonnance->getNumeroOrdonnance() : 'N/A';
        $filename = 'Traitement_' . $numeroOrdonnance . '_' . date('Y-m-d') . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"'
        ]);
    }

    #[Route('/guide-sante', name: 'app_guide_sante', methods: ['GET'])]
    public function guideSante(): Response
    {
        return $this->render('front/guide_sante.html.twig');
    }

    #[Route('/mes-commandes', name: 'app_mes_commandes', methods: ['GET'])]
    public function mesCommandes(
        CommandeRepository $commandeRepository,
        ProduitRepository $produitRepository,
        PanierService $panierService,
        Request $request
    ): Response {
        if (!$this->getUser()) {
            return $this->redirectToRoute('app_login');
        }

        $search = trim((string) $request->query->get('search', ''));
        $statut = (string) $request->query->get('statut', 'all');
        $sort = (string) $request->query->get('sort', 'date_desc');

        $queryBuilder = $commandeRepository->createQueryBuilder('c')
            ->where('c.utilisateur = :user')
            ->setParameter('user', $this->getUser());

        if ($search !== '') {
            $queryBuilder
                ->andWhere('c.nom LIKE :search OR c.email LIKE :search OR c.telephone LIKE :search OR c.adresseLivraison LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        if ($statut !== 'all') {
            $queryBuilder
                ->andWhere('c.statut = :statut')
                ->setParameter('statut', $statut);
        }

        switch ($sort) {
            case 'amount_asc':
                $queryBuilder->orderBy('c.total', 'ASC');
                break;
            case 'amount_desc':
                $queryBuilder->orderBy('c.total', 'DESC');
                break;
            case 'date_asc':
                $queryBuilder->orderBy('c.dateCommande', 'ASC');
                break;
            case 'date_desc':
            default:
                $queryBuilder->orderBy('c.dateCommande', 'DESC');
                break;
        }

        $commandes = $queryBuilder->getQuery()->getResult();

        $detailsParCommande = [];
        $stats = [
            'total' => count($commandes),
            'en_attente' => 0,
            'confirmee' => 0,
            'annulee' => 0,
            'livree' => 0,
            'review' => 0,
            'bloquee' => 0,
            'montant_total' => 0.0,
        ];

        foreach ($commandes as $commande) {
            $stats['montant_total'] += (float) $commande->getTotal();
            $currentStatus = (string) $commande->getStatut();
            if (array_key_exists($currentStatus, $stats)) {
                $stats[$currentStatus]++;
            }

            $rows = [];
            $decoded = $commande->getProduitsArray();
            if (!is_array($decoded)) {
                $decoded = [];
            }

            foreach ($decoded as $row) {
                $productId = null;
                $quantity = 1;
                $unitPrice = null;

                if (is_array($row)) {
                    $productId = isset($row['id']) ? (int) $row['id'] : null;
                    $quantity = isset($row['quantity']) ? max(1, (int) $row['quantity']) : 1;
                    $unitPrice = isset($row['unitPrice']) ? (float) $row['unitPrice'] : null;
                } elseif (is_numeric($row)) {
                    $productId = (int) $row;
                }

                if (!$productId) {
                    continue;
                }

                $produit = $produitRepository->find($productId);
                if (!$produit) {
                    continue;
                }

                $price = $unitPrice ?? (float) $produit->getPrix();
                $rows[] = [
                    'nom' => $produit->getNom(),
                    'quantity' => $quantity,
                    'unitPrice' => $price,
                    'lineTotal' => $price * $quantity,
                ];
            }

            $detailsParCommande[$commande->getId()] = $rows;
        }

        return $this->render('front/mes_commandes.html.twig', [
            'commandes' => $commandes,
            'details_par_commande' => $detailsParCommande,
            'stats' => $stats,
            'nombre_articles_panier' => $panierService->getNombreArticles(),
            'filters' => [
                'search' => $search,
                'statut' => $statut,
                'sort' => $sort,
            ],
        ]);
    }

    #[Route('/mes-adresses', name: 'app_mes_adresses', methods: ['GET'])]
    public function mesAdresses(EntityManagerInterface $entityManager, PanierService $panierService): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            return $this->redirectToRoute('app_login');
        }

        $addresses = $entityManager->getRepository(Address::class)->findBy(
            ['utilisateur' => $user],
            ['id' => 'DESC']
        );

        return $this->render('front/mes_adresses.html.twig', [
            'addresses' => $addresses,
            'nombre_articles_panier' => $panierService->getNombreArticles(),
        ]);
    }

    #[Route('/mes-adresses/new', name: 'app_mes_adresses_new', methods: ['GET', 'POST'])]
    public function newAdresse(Request $request, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            return $this->redirectToRoute('app_login');
        }

        $address = new Address();
        $address->setUtilisateur($user);
        $form = $this->createForm(AddressValidatedType::class, $address);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($address);
            $entityManager->flush();
            $this->addFlash('success', 'Adresse ajoutee.');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Adresse ajoutee avec succes.',
                    'redirect_url' => $this->generateUrl('app_mes_adresses'),
                ]);
            }

            return $this->redirectToRoute('app_mes_adresses');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
        }

        return $this->render('front/adresse_form.html.twig', [
            'form' => $form->createView(),
            'is_edit' => false,
        ]);
    }

    #[Route('/mes-adresses/{id}/edit', name: 'app_mes_adresses_edit', methods: ['GET', 'POST'])]
    public function editAdresse(Address $address, Request $request, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            return $this->redirectToRoute('app_login');
        }
        if ($address->getUtilisateur()?->getId() !== $user->getId()) {
            throw $this->createAccessDeniedException();
        }

        $form = $this->createForm(AddressValidatedType::class, $address);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Adresse modifiee.');

            if ($request->isXmlHttpRequest()) {
                return $this->json([
                    'ok' => true,
                    'message' => 'Adresse modifiee avec succes.',
                    'redirect_url' => $this->generateUrl('app_mes_adresses'),
                ]);
            }

            return $this->redirectToRoute('app_mes_adresses');
        }

        if ($request->isXmlHttpRequest() && $form->isSubmitted()) {
            return $this->json([
                'ok' => false,
                'errors' => $this->extractFormErrors($form),
            ], 422);
        }

        return $this->render('front/adresse_form.html.twig', [
            'form' => $form->createView(),
            'is_edit' => true,
        ]);
    }

    #[Route('/mes-adresses/{id}/delete', name: 'app_mes_adresses_delete', methods: ['POST'])]
    public function deleteAdresse(Address $address, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            return $this->redirectToRoute('app_login');
        }
        if ($address->getUtilisateur()?->getId() !== $user->getId()) {
            throw $this->createAccessDeniedException();
        }

        $entityManager->remove($address);
        $entityManager->flush();
        $this->addFlash('success', 'Adresse supprimee.');

        return $this->redirectToRoute('app_mes_adresses');
    }

    #[Route('/commande', name: 'app_commande', methods: ['GET', 'POST'])]
    public function commande(
        Request $request,
        EntityManagerInterface $entityManager,
        PanierService $panierService,
        ProduitRepository $produitRepository,
        MailerService $mailerService,
        CouponService $couponService,
        LoyaltyService $loyaltyService,
        DeliveryEstimatorService $deliveryEstimatorService,
        FraudDetectionService $fraudDetectionService,
        OrderSplitService $orderSplitService,
        ShippingCalculatorService $shippingCalculatorService,
        ValidatorInterface $validator
    ): Response {
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez vous connecter pour passer une commande.');
            return $this->redirectToRoute('app_login');
        }

        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);
        $currentUser = $this->getUser() instanceof Utilisateur ? $this->getUser() : null;
        $addresses = $currentUser
            ? $entityManager->getRepository(Address::class)->findBy(['utilisateur' => $currentUser], ['id' => 'DESC'])
            : [];
        $couponCodeFromQuery = trim((string) $request->query->get('coupon_code', ''));
        $pricing = $this->buildCheckoutPricing(
            $panierDetails,
            $couponCodeFromQuery,
            $currentUser,
            $couponService,
            $loyaltyService
        );

        if ($request->isMethod('POST')) {
            try {
                if ($panierDetails['count'] === 0) {
                    $this->addFlash('error', 'Votre panier est vide.');
                    return $this->redirectToRoute('app_commande');
                }

                $payload = [
                    'nom' => trim((string) $request->request->get('nom')),
                    'email' => trim((string) $request->request->get('email')),
                    'telephone' => trim((string) $request->request->get('telephone')),
                    'adresse' => trim((string) $request->request->get('adresse_livraison')),
                    'message' => trim((string) $request->request->get('message')),
                    'mode_paiement' => (string) $request->request->get('mode_paiement', 'livraison'),
                    'coupon_code' => trim((string) $request->request->get('coupon_code', '')),
                    'address_id' => (string) $request->request->get('address_id', ''),
                ];
                file_put_contents('checkout_payload_debug.log', sprintf("[%s] RECEIVED EMAIL: %s\n", date('Y-m-d H:i:s'), $payload['email']), FILE_APPEND);


                $addressId = (int) $payload['address_id'];
                if ($addressId > 0 && $currentUser) {
                    $selectedAddress = $entityManager->getRepository(Address::class)->findOneBy([
                        'id' => $addressId,
                        'utilisateur' => $currentUser,
                    ]);
                    if ($selectedAddress instanceof Address) {
                        $payload['adresse'] = $selectedAddress->toSingleLine();
                    }
                }

                $pricing = $this->buildCheckoutPricing(
                    $panierDetails,
                    $payload['coupon_code'],
                    $currentUser,
                    $couponService,
                    $loyaltyService
                );

                $validationErrors = $this->validateCommandePayload($payload, $validator);
                if ($payload['coupon_code'] !== '' && !$pricing['couponValid']) {
                    $validationErrors['coupon_code'][] = $pricing['couponMessage'] ?: 'Code promo invalide.';
                }

                if (!empty($validationErrors)) {
                    error_log("Validation failed for commande: " . print_r($validationErrors, true));
                    if ($request->isXmlHttpRequest()) {
                        return $this->json([
                            'ok' => false,
                            'errors' => $validationErrors,
                        ], 422);
                    }

                    $first = null;
                    foreach ($validationErrors as $messages) {
                        if (!empty($messages)) {
                            $first = $messages[0];
                            break;
                        }
                    }
                    $this->addFlash('error', $first ?: 'Merci de corriger les informations saisies.');
                } else {
                    $request->getSession()->set('checkout_data', $payload);

                    if ($payload['mode_paiement'] === 'en_ligne') {
                        if ($request->isXmlHttpRequest()) {
                            return $this->json([
                                'ok' => true,
                                'redirect_url' => $this->generateUrl('app_commande_paiement'),
                            ]);
                        }
                        return $this->redirectToRoute('app_commande_paiement');
                    }

                    error_log("--- Starting Checkout Processing ---");
                    error_log("PAYLOAD EMAIL: " . $payload['email']);
                    $commande = $this->createCommandeFromPanier(
                        $payload,
                        $panierDetails,
                        $pricing,
                        $entityManager,
                        $orderSplitService,
                        $shippingCalculatorService
                    );
                    error_log("Commande created. ID (before flush): " . $commande->getId());

                    $fraudScore = $fraudDetectionService->calculateFraudScore($commande);
                    error_log("Fraud score: " . $fraudScore);
                    $fraudDetectionService->applyFraudDecision($commande, $fraudScore);
                    
                    $commande->setEstimatedDeliveryDate($deliveryEstimatorService->estimateDeliveryDate($commande));
                    error_log("Estimated delivery: " . ($commande->getEstimatedDeliveryDate()?->format('Y-m-d H:i:s') ?: 'N/A'));

                    $entityManager->flush();
                    error_log("Main flush successful. ID (after flush): " . $commande->getId());

                    $mailerItems = $this->buildMailerItems($panierDetails, $pricing);
                    error_log("Mailer items built.");

                    $emailSent = false;
                    $emailError = null;

                    try {
                        error_log("Sending invoice email...");
                        $emailSent = $mailerService->sendCommandeInvoiceEmail($commande, $mailerItems);
                        if ($emailSent) {
                            error_log("Invoice email sent successfully.");
                        } else {
                            error_log("Invoice email failed to send.");
                        }

                        error_log("Sending admin notification...");
                        $mailerService->sendAdminCommandeNotification($commande, $mailerItems);
                        error_log("Admin notification sent.");
                    } catch (\Throwable $e) {
                        error_log("NON-CRITICAL: Mailer failed: " . $e->getMessage());
                        $emailError = $e->getMessage();
                    }

                    $panierService->viderPanierProduits();
                    $request->getSession()->remove('checkout_data');
                    error_log("Panier cleared and session updated. Checkout complete.");

                    if ($emailSent) {
                        $this->addFlash('success', sprintf('Commande enregistrée. Une facture a été envoyée à : %s', $commande->getEmail()));
                    } else {
                        $this->addFlash('warning', sprintf('Commande enregistrée mais l\'email de confirmation n\'a pas pu être envoyé à %s. Veuillez contacter le support si vous ne recevez pas votre facture.', $commande->getEmail()));
                    }
                    
                    if ($request->isXmlHttpRequest()) {
                        return $this->json([
                            'ok' => true,
                            'message' => 'Commande enregistree avec succes.',
                            'redirect_url' => $this->generateUrl('app_mes_commandes'),
                        ]);
                    }
                    return $this->redirectToRoute('app_mes_commandes');
                }
            } catch (\Throwable $e) {
                error_log("CRASH in checkout: " . $e->getMessage() . " at " . $e->getFile() . ":" . $e->getLine());
                if ($request->isXmlHttpRequest()) {
                    return $this->json([
                        'ok' => false,
                        'errors' => ['_form' => ['Erreur systeme: ' . $e->getMessage()]],
                    ], 500);
                }
                $this->addFlash('error', 'Une erreur est survenue lors de la validation de votre commande.');
                return $this->redirectToRoute('app_commande');
            }
        }

        return $this->render('front/commande.html.twig', [
            'panier' => $panierDetails,
            'pricing' => $pricing,
            'addresses' => $addresses,
            'nombre_articles_panier' => $panierDetails['count'],
            'prefill' => $this->buildPrefillData(),
        ]);
    }

    #[Route('/commande/paiement', name: 'app_commande_paiement', methods: ['GET', 'POST'])]
    public function paiementCommande(
        Request $request,
        EntityManagerInterface $entityManager,
        PanierService $panierService,
        ProduitRepository $produitRepository,
        MailerService $mailerService,
        CouponService $couponService,
        LoyaltyService $loyaltyService,
        DeliveryEstimatorService $deliveryEstimatorService,
        FraudDetectionService $fraudDetectionService,
        OrderSplitService $orderSplitService,
        ShippingCalculatorService $shippingCalculatorService
    ): Response {
        $checkout = $request->getSession()->get('checkout_data');
        if (!is_array($checkout)) {
            return $this->redirectToRoute('app_commande');
        }

        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);
        if ($panierDetails['count'] === 0) {
            return $this->redirectToRoute('app_products');
        }

        $pricing = $this->buildCheckoutPricing(
            $panierDetails,
            trim((string) ($checkout['coupon_code'] ?? '')),
            $this->getUser() instanceof Utilisateur ? $this->getUser() : null,
            $couponService,
            $loyaltyService
        );

        if (($checkout['coupon_code'] ?? '') !== '' && !$pricing['couponValid']) {
            $this->addFlash('error', $pricing['couponMessage'] ?: 'Code promo invalide.');
            return $this->redirectToRoute('app_commande');
        }

        if ($request->isMethod('POST')) {
            $cardHolder = trim((string) $request->request->get('card_holder'));
            $cardNumber = preg_replace('/\s+/', '', (string) $request->request->get('card_number'));
            $expDate = trim((string) $request->request->get('exp_date'));
            $cvv = trim((string) $request->request->get('cvv'));

            if ($cardHolder === '' || $cardNumber === '' || $expDate === '' || $cvv === '') {
                $this->addFlash('error', 'Merci de remplir les informations de paiement.');
            } else {
                $commande = $this->createCommandeFromPanier(
                    $checkout,
                    $panierDetails,
                    $pricing,
                    $entityManager,
                    $orderSplitService,
                    $shippingCalculatorService
                );
                $commande->setModePaiement('en_ligne');
                $commande->setStatut('confirmee');
                $fraudScore = $fraudDetectionService->calculateFraudScore($commande);
                $fraudDetectionService->applyFraudDecision($commande, $fraudScore);
                $commande->setEstimatedDeliveryDate($deliveryEstimatorService->estimateDeliveryDate($commande));

                $user = $this->getUser();
                if ($user instanceof Utilisateur) {
                    $loyaltyService->addPoints($user, $commande);
                    $entityManager->persist($user);
                }
                $entityManager->flush();

                $mailerItems = $this->buildMailerItems($panierDetails, $pricing);
                $emailSent = $mailerService->sendCommandeInvoiceEmail($commande, $mailerItems);
                $mailerService->sendAdminCommandeNotification($commande, $mailerItems);
                $panierService->viderPanierProduits();
                $request->getSession()->remove('checkout_data');

                if ($emailSent) {
                    $this->addFlash('success', sprintf('Paiement valide. Une facture a été envoyée à : %s', $commande->getEmail()));
                } else {
                    $this->addFlash('warning', sprintf('Paiement valide mais l\'email de confirmation n\'a pas pu être envoyé à %s. Veuillez contacter le support si vous ne recevez pas votre facture.', $commande->getEmail()));
                }
                return $this->redirectToRoute('app_mes_commandes');
            }
        }

        return $this->render('front/paiement.html.twig', [
            'panier' => $panierDetails,
            'pricing' => $pricing,
            'nombre_articles_panier' => $panierDetails['count'],
        ]);
    }

    #[Route('/panier', name: 'app_panier_view', methods: ['GET'])]
    #[Route('/panier', name: 'app_panier', methods: ['GET'])]
    public function panierView(PanierService $panierService, ProduitRepository $produitRepository): Response
    {
        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);

        return $this->render('front/panier.html.twig', [
            'panier' => $panierDetails,
            'nombre_articles_panier' => $panierDetails['count'],
        ]);
    }

    #[Route('/panier/ajouter/{id}', name: 'app_panier_add', methods: ['POST'])]
    public function addToCart(Produit $produit, PanierService $panierService, ProduitRepository $produitRepository): JsonResponse
    {
        $stock = $this->resolveProduitStock($produit);
        if ($stock <= 0 || $produit->getStatut() === 'indisponible') {
            return $this->json(['ok' => false, 'message' => 'Produit indisponible'], 400);
        }

        $produitId = $produit->getId();
        if ($produitId === null) {
            return $this->json(['ok' => false, 'message' => 'Produit invalide'], 400);
        }

        $current = $panierService->getProduitsPanier();
        $nextQuantity = ($current[$produitId] ?? 0) + 1;
        if ($nextQuantity > $stock) {
            return $this->json(['ok' => false, 'message' => 'Stock insuffisant'], 400);
        }

        $panierService->ajouterProduit($produitId, 1);
        return $this->json(array_merge(['ok' => true], $this->buildPanierDetails($panierService, $produitRepository)));
    }

    #[Route('/panier/quantite/{id}', name: 'app_panier_quantity', methods: ['POST'])]
    public function updateCartQuantity(
        Produit $produit,
        Request $request,
        PanierService $panierService,
        ProduitRepository $produitRepository
    ): JsonResponse {
        $quantity = max(0, (int) $request->request->get('quantity', 1));
        $stock = $this->resolveProduitStock($produit);

        $produitId = $produit->getId();
        if ($produitId === null) {
            return $this->json(['ok' => false, 'message' => 'Produit invalide'], 400);
        }

        if ($quantity > $stock) {
            return $this->json(['ok' => false, 'message' => 'Quantite superieure au stock'], 400);
        }

        $panierService->setQuantiteProduit($produitId, $quantity);
        return $this->json(array_merge(['ok' => true], $this->buildPanierDetails($panierService, $produitRepository)));
    }

    #[Route('/panier/supprimer/{id}', name: 'app_panier_remove', methods: ['POST'])]
    public function removeCartProduct(Produit $produit, PanierService $panierService, ProduitRepository $produitRepository): JsonResponse
    {
        $produitId = $produit->getId();
        if ($produitId === null) {
            return $this->json(['ok' => false, 'message' => 'Produit invalide'], 400);
        }

        $panierService->retirerProduit($produitId);
        return $this->json(array_merge(['ok' => true], $this->buildPanierDetails($panierService, $produitRepository)));
    }

    #[Route('/panier/vider', name: 'app_panier_clear', methods: ['POST'])]
    public function clearCart(PanierService $panierService, ProduitRepository $produitRepository): JsonResponse
    {
        $panierService->viderPanierProduits();
        return $this->json(array_merge(['ok' => true], $this->buildPanierDetails($panierService, $produitRepository)));
    }

    #[Route('/panier/resume', name: 'app_panier_summary', methods: ['GET'])]
    public function cartSummary(PanierService $panierService, ProduitRepository $produitRepository): JsonResponse
    {
        return $this->json(array_merge(['ok' => true], $this->buildPanierDetails($panierService, $produitRepository)));
    }

    private function buildPanierDetails(PanierService $panierService, ProduitRepository $produitRepository): array
    {
        $rawItems = $panierService->getProduitsPanier();
        $items = [];
        $total = 0.0;
        $count = 0;

        foreach ($rawItems as $productId => $quantity) {
            $produit = $produitRepository->find((int) $productId);
            if (!$produit) {
                continue;
            }

            $stock = $this->resolveProduitStock($produit);
            $qty = min((int) $quantity, max(0, $stock));
            if ($qty <= 0) {
                continue;
            }

            $unitPrice = (float) ($produit->getActivePromotion() ? $produit->getPromotionalPrice() : $produit->getPrix());
            $lineTotal = $unitPrice * $qty;

            $items[] = [
                'id' => $produit->getId(),
                'name' => $produit->getNom(),
                'image' => $produit->getImage(),
                'price' => $unitPrice,
                'quantity' => $qty,
                'stock' => $stock,
                'lineTotal' => $lineTotal,
            ];

            $count += $qty;
            $total += $lineTotal;
        }

        return [
            'items' => $items,
            'count' => $count,
            'total' => $total,
        ];
    }

    private function resolveProduitStock(Produit $produit): int
    {
        try {
            $stock = $produit->getStockTotal();
            if ($stock >= 0) {
                return $stock;
            }
        } catch (\Throwable $e) {
        }

        return max(0, (int) $produit->getQuantiteStock());
    }

    /**
     * @param array<string, mixed> $payload
     * @param array<string, mixed> $panierDetails
     * @param array<string, mixed> $pricing
     */
    private function createCommandeFromPanier(
        array $payload,
        array $panierDetails,
        array $pricing,
        EntityManagerInterface $entityManager,
        OrderSplitService $orderSplitService,
        ShippingCalculatorService $shippingCalculatorService
    ): Commande
    {
        $commande = new Commande();
        $commande->setDateCommande(new \DateTime());
        $commande->setStatut('en_attente');
        $commande->setNom($payload['nom']);
        $commande->setEmail($payload['email']);
        $commande->setTelephone($payload['telephone']);
        $commande->setAdresseLivraison($payload['adresse']);
        $commande->setModePaiement($payload['mode_paiement'] ?? 'livraison');
        $commande->setMessage($payload['message'] ?? null);
        $commande->setTotal((float) ($pricing['grandTotal'] ?? $panierDetails['total']));
        $commande->setCouponDiscount((float) ($pricing['totalDiscount'] ?? 0.0));
        $commande->setCouponCode((string) ($pricing['couponCode'] ?? null) ?: null);

        $user = $this->getUser();
        if ($user instanceof Utilisateur) {
            $commande->setUtilisateur($user);
        }

        $storedItems = array_map(static function (array $item): array {
            return [
                'id' => $item['id'],
                'quantity' => $item['quantity'],
                'unitPrice' => $item['price'],
            ];
        }, $panierDetails['items']);

        $jsonProduitsIds = json_encode($storedItems);
        $commande->setProduitsIds($jsonProduitsIds !== false ? $jsonProduitsIds : null);

        $selectedAddress = null;
        $addressId = isset($payload['address_id']) ? (int) $payload['address_id'] : 0;
        if ($addressId > 0) {
            $selectedAddress = $entityManager->getRepository(Address::class)->find($addressId);
            if ($selectedAddress && $user instanceof Utilisateur && $selectedAddress->getUtilisateur()?->getId() !== $user->getId()) {
                $selectedAddress = null;
            }
        }

        if (!$selectedAddress) {
            $selectedAddress = new Address();
            $selectedAddress->setUtilisateur($user instanceof Utilisateur ? $user : null);
            $selectedAddress->setFullName((string) $payload['nom']);
            $selectedAddress->setLine1((string) $payload['adresse']);
            $selectedAddress->setCity('N/A');
            $selectedAddress->setRegion('N/A');
            $selectedAddress->setPostalCode('0000');
            $selectedAddress->setCountry('Tunisie');
            $selectedAddress->setPhone((string) ($payload['telephone'] ?? null));
            $entityManager->persist($selectedAddress);
        }

        $allocations = $this->resolveShipmentsAllocations(
            $payload,
            $panierDetails,
            $entityManager,
            $selectedAddress,
            $user instanceof Utilisateur ? $user : null
        );
        $orderSplitService->splitOrderByAddress($commande, $allocations, $shippingCalculatorService);

        $entityManager->persist($commande);

        $coupon = $pricing['coupon'] ?? null;
        if ($coupon instanceof Coupon) {
            $coupon->incrementUsage();
            $entityManager->persist($coupon);
        }

        return $commande;
    }

    /**
     * @param array<string, mixed> $panierDetails
     * @return array<string, mixed>
     */
    private function buildCheckoutPricing(
        array $panierDetails,
        string $couponCode,
        ?Utilisateur $user,
        CouponService $couponService,
        LoyaltyService $loyaltyService
    ): array {
        $subTotal = round((float) ($panierDetails['total'] ?? 0.0), 2);
        $level = $user?->getLoyaltyLevel() ?? 'BRONZE';
        $levelDiscountPercent = $loyaltyService->getDiscountByLevel($level);
        $loyaltyDiscount = round($subTotal * ($levelDiscountPercent / 100), 2);
        $afterLoyalty = max(0.0, $subTotal - $loyaltyDiscount);

        $coupon = null;
        $couponValid = false;
        $couponMessage = '';
        $couponDiscount = 0.0;
        $normalizedCode = strtoupper(trim($couponCode));

        if ($normalizedCode !== '') {
            $validation = $couponService->validateCoupon($normalizedCode, $afterLoyalty);
            $couponValid = (bool) ($validation['valid'] ?? false);
            $couponMessage = (string) ($validation['message'] ?? '');
            $coupon = $validation['coupon'] ?? null;

            if ($couponValid && $coupon instanceof Coupon) {
                $applied = $couponService->applyCoupon($afterLoyalty, $coupon);
                $couponDiscount = (float) ($applied['discount'] ?? 0.0);
                $afterLoyalty = (float) ($applied['finalTotal'] ?? $afterLoyalty);
            }
        }

        return [
            'subTotal' => $subTotal,
            'loyaltyLevel' => $level,
            'loyaltyDiscount' => $loyaltyDiscount,
            'couponCode' => $normalizedCode ?: null,
            'couponValid' => $normalizedCode === '' ? true : $couponValid,
            'couponMessage' => $couponMessage,
            'couponDiscount' => round($couponDiscount, 2),
            'totalDiscount' => round($loyaltyDiscount + $couponDiscount, 2),
            'grandTotal' => round($afterLoyalty, 2),
            'coupon' => $coupon instanceof Coupon ? $coupon : null,
        ];
    }

    /**
     * @param array<string, mixed> $panierDetails
     * @param array<string, mixed> $pricing
     * @return array<int, array<string, mixed>>
     */
    private function buildMailerItems(array $panierDetails, array $pricing): array
    {
        $items = $panierDetails['items'] ?? [];
        $items[] = [
            'name' => 'Sous-total',
            'quantity' => 1,
            'price' => (float) ($pricing['subTotal'] ?? 0.0),
            'lineTotal' => (float) ($pricing['subTotal'] ?? 0.0),
            'meta' => true,
        ];

        if (((float) ($pricing['loyaltyDiscount'] ?? 0.0)) > 0) {
            $items[] = [
                'name' => sprintf('Remise fidelite (%s)', (string) ($pricing['loyaltyLevel'] ?? 'BRONZE')),
                'quantity' => 1,
                'price' => -1 * (float) $pricing['loyaltyDiscount'],
                'lineTotal' => -1 * (float) $pricing['loyaltyDiscount'],
                'meta' => true,
            ];
        }

        if (((float) ($pricing['couponDiscount'] ?? 0.0)) > 0) {
            $items[] = [
                'name' => sprintf('Coupon %s', (string) ($pricing['couponCode'] ?? '')),
                'quantity' => 1,
                'price' => -1 * (float) $pricing['couponDiscount'],
                'lineTotal' => -1 * (float) $pricing['couponDiscount'],
                'meta' => true,
            ];
        }

        return $items;
    }

    /**
     * @return array<string, string>
     */
    private function buildPrefillData(): array
    {
        $user = $this->getUser();
        if ($user instanceof Utilisateur) {
            return [
                'nom' => $user->getNom() . ' ' . $user->getPrenom(),
                'email' => $user->getEmail() ?? '',
                'telephone' => (string) $user->getTelephone(),
                'adresse_livraison' => '',
                'message' => '',
                'mode_paiement' => 'livraison',
                'coupon_code' => '',
                'address_id' => '',
            ];
        }

        return [
            'nom' => '',
            'email' => '',
            'telephone' => '',
            'adresse_livraison' => '',
            'message' => '',
            'mode_paiement' => 'livraison',
            'coupon_code' => '',
            'address_id' => '',
        ];
    }

    private function extractFormErrors(FormInterface $form): array
    {
        $errors = [];
        foreach ($form->getErrors(true, true) as $error) {
            if (!$error instanceof \Symfony\Component\Form\FormError) {
                continue;
            }
            $origin = $error->getOrigin();
            $field = $origin ? $origin->getName() : '_form';
            $errors[$field] ??= [];
            $errors[$field][] = $error->getMessage();
        }

        return $errors;
    }

    /**
     * @param array<string, mixed> $payload
     * @return array<string, array<int, string>>
     */
    private function validateCommandePayload(array $payload, ValidatorInterface $validator): array
    {
        $constraints = new Assert\Collection([
            'nom' => [
                new Assert\NotBlank(message: 'Le nom est obligatoire.'),
                new Assert\Length(min: 2, max: 255, minMessage: 'Le nom est trop court.'),
            ],
            'email' => [
                new Assert\NotBlank(message: 'L email est obligatoire.'),
                new Assert\Email(message: 'Adresse email invalide.'),
            ],
            'telephone' => [
                new Assert\NotBlank(message: 'Le telephone est obligatoire.'),
                new Assert\Length(min: 6, max: 20),
            ],
            'adresse' => [
                new Assert\NotBlank(message: 'L adresse de livraison est obligatoire.'),
                new Assert\Length(min: 5, max: 255),
            ],
            'message' => [
                new Assert\Optional([
                    new Assert\Length(max: 1000),
                ]),
            ],
            'mode_paiement' => [
                new Assert\NotBlank(),
                new Assert\Choice(choices: ['livraison', 'en_ligne'], message: 'Mode de paiement invalide.'),
            ],
            'coupon_code' => [
                new Assert\Optional([
                    new Assert\Length(max: 64),
                ]),
            ],
            'address_id' => [
                new Assert\Optional([
                    new Assert\Regex(pattern: '/^\d*$/', message: 'Adresse invalide.'),
                ]),
            ],
        ]);

        $violations = $validator->validate($payload, $constraints);
        $errors = [];
        foreach ($violations as $violation) {
            $path = trim((string) $violation->getPropertyPath());
            $field = $path;
            if (preg_match('/^\[(.+)\]$/', $path, $matches)) {
                $field = $matches[1];
            }
            if ($field === '') {
                $field = '_form';
            }
            $errors[$field] ??= [];
            $errors[$field][] = $violation->getMessage();
        }

        return $errors;
    }

    /**
     * @param array<string, mixed> $payload
     * @param array<string, mixed> $panierDetails
     * @return array<int, array{address: Address, items: array<int, array{id: int, quantity: int, unitPrice: float}>}>
     */
    private function resolveShipmentsAllocations(
        array $payload,
        array $panierDetails,
        EntityManagerInterface $entityManager,
        Address $defaultAddress,
        ?Utilisateur $user
    ): array {
        $productPriceMap = [];
        foreach (($panierDetails['items'] ?? []) as $item) {
            $productPriceMap[(int) $item['id']] = (float) $item['price'];
        }

        $json = trim((string) ($payload['split_allocations'] ?? ''));
        if ($json !== '') {
            $decoded = json_decode($json, true);
            if (is_array($decoded)) {
                $allocations = [];
                foreach ($decoded as $block) {
                    $addressId = isset($block['addressId']) ? (int) $block['addressId'] : 0;
                    $address = $addressId > 0 ? $entityManager->getRepository(Address::class)->find($addressId) : null;
                    if (!$address instanceof Address) {
                        continue;
                    }
                    if ($user instanceof Utilisateur && $address->getUtilisateur()?->getId() !== $user->getId()) {
                        continue;
                    }

                    $items = [];
                    foreach (($block['items'] ?? []) as $entry) {
                        $productId = (int) ($entry['id'] ?? 0);
                        $qty = max(1, (int) ($entry['quantity'] ?? 1));
                        if ($productId <= 0 || !isset($productPriceMap[$productId])) {
                            continue;
                        }
                        $items[] = [
                            'id' => $productId,
                            'quantity' => $qty,
                            'unitPrice' => $productPriceMap[$productId],
                        ];
                    }

                    if (!empty($items)) {
                        $allocations[] = ['address' => $address, 'items' => $items];
                    }
                }

                if (!empty($allocations)) {
                    return $allocations;
                }
            }
        }

        $defaultItems = [];
        foreach (($panierDetails['items'] ?? []) as $item) {
            $defaultItems[] = [
                'id' => (int) $item['id'],
                'quantity' => (int) $item['quantity'],
                'unitPrice' => (float) $item['price'],
            ];
        }

        return [[
            'address' => $defaultAddress,
            'items' => $defaultItems,
        ]];
    }

    #[Route('/depots', name: 'front_depots')]
    public function depots(DepotRepository $depotRepository, Request $request): Response
    {
        $search = $request->query->get('search');
        $ville = $request->query->get('ville');
        $capaciteMin = $request->query->get('capacite_min');
        $capaciteMax = $request->query->get('capacite_max');
        $sort = $request->query->get('sort', 'name');
        
        $queryBuilder = $depotRepository->createQueryBuilder('d');
        
        if ($search) {
            $queryBuilder->andWhere('d.nomDepot LIKE :search OR d.adresseDepot LIKE :search OR d.responsableDepot LIKE :search')
                        ->setParameter('search', '%' . $search . '%');
        }
        
        if ($ville && $ville !== 'all') {
            $queryBuilder->andWhere('d.adresseDepot LIKE :ville OR d.ville LIKE :ville')
                        ->setParameter('ville', '%' . $ville . '%');
        }
        
        if ($capaciteMin !== null && $capaciteMin !== '') {
            $queryBuilder->andWhere('d.capaciteDepot >= :capaciteMin')
                        ->setParameter('capaciteMin', $capaciteMin);
        }
        
        if ($capaciteMax !== null && $capaciteMax !== '') {
            $queryBuilder->andWhere('d.capaciteDepot <= :capaciteMax')
                        ->setParameter('capaciteMax', $capaciteMax);
        }
        
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
        
        $villes = $depotRepository->createQueryBuilder('d')
            ->select('DISTINCT d.adresseDepot')
            ->getQuery()
            ->getResult();
        
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
        
        return $this->render('front/depots.html.twig', [
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

    #[Route('/stocks', name: 'front_stocks')]
    public function stocks(StockRepository $stockRepository, Request $request): Response
    {
        $search = $request->query->get('search');
        $etat = $request->query->get('etat');
        $quantiteMin = $request->query->get('quantite_min');
        $quantiteMax = $request->query->get('quantite_max');
        $sort = $request->query->get('sort', 'produit');
        
        $queryBuilder = $stockRepository->createQueryBuilder('s')
            ->leftJoin('s.produit', 'p')
            ->addSelect('p');
        
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
        
        return $this->render('front/stocks.html.twig', [
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

    #[Route('/api/stock/assistant', name: 'api_stock_assistant', methods: ['POST'])]
    public function stockAssistant(Request $request, StockAssistantService $assistant): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $payload = json_decode((string) $request->getContent(), true);
        if (!is_array($payload)) {
            $payload = $request->request->all();
        }

        $question = trim((string) ($payload['question'] ?? ''));
        if ($question === '') {
            return $this->json(['ok' => false, 'message' => 'Question vide'], 422);
        }

        try {
            $result = $assistant->analyserQuestion($question);
            return $this->json(array_merge(['ok' => true], $result));
        } catch (\Throwable $e) {
            return $this->json(['ok' => false, 'message' => 'Erreur serveur'], 500);
        }
    }

    #[Route('/api/depots/{id}/health', name: 'api_depot_health', methods: ['GET'])]
    public function depotHealth(Depot $depot, DepotHealthScoreService $healthScoreService): JsonResponse
    {
        try {
            $result = $healthScoreService->calculerScoreDepot($depot);
            return $this->json(array_merge(['ok' => true], $result));
        } catch (\Throwable $e) {
            return $this->json(['ok' => false, 'message' => 'Erreur serveur'], 500);
        }
    }
    #[Route('/api/geoproxy', name: 'api_geoproxy', methods: ['GET'])]
    public function geoProxy(Request $request, HttpClientInterface $httpClient): JsonResponse
    {
        $q = $request->query->get('q');
        $lat = $request->query->get('lat');
        $lon = $request->query->get('lon');
        $format = $request->query->get('format', 'jsonv2');
        $addressdetails = $request->query->get('addressdetails', '1');
        $limit = $request->query->get('limit', '6');

        $baseUrl = 'https://nominatim.openstreetmap.org/';
        $params = [
            'format' => $format,
            'addressdetails' => $addressdetails,
        ];

        if ($q) {
            $endpoint = 'search';
            $params['q'] = $q;
            $params['limit'] = $limit;
        } elseif ($lat && $lon) {
            $endpoint = 'reverse';
            $params['lat'] = $lat;
            $params['lon'] = $lon;
        } else {
            return $this->json(['error' => 'Missing parameters'], 400);
        }

        try {
            $response = $httpClient->request('GET', $baseUrl . $endpoint, [
                'query' => $params,
                'headers' => [
                    'User-Agent' => 'CURAVITA-Pharmacy-App/1.0',
                ],
            ]);

            return new JsonResponse($response->toArray(), $response->getStatusCode());
        } catch (\Throwable $e) {
            return $this->json(['error' => $e->getMessage()], 500);
        }
    }

    /**
     * Air Mouse - Experimental feature with hand gesture control
     * Uses MediaPipe for real-time hand tracking and gesture recognition
     */
    #[Route('/air-mouse', name: 'app_air_mouse')]
    public function airMouse(PanierService $panierService): Response
    {
        return $this->render('front/air_mouse.html.twig', [
            'nombre_articles_panier' => $panierService->getNombreArticles()
        ]);
    }
}
