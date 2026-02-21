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
use App\Form\QuestionType;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use App\Repository\CommandeRepository;
use App\Repository\ProduitRepository;
use App\Repository\AddressRepository;
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
use App\Exception\FileUploadException;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Form\FormInterface;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Component\String\Slugger\AsciiSlugger;
use Dompdf\Dompdf;
use Dompdf\Options;
use Psr\Log\LoggerInterface;
use App\Repository\QuestionRepository;

/**
 * FrontController - Main controller for public-facing pages
 * 
 * This controller handles:
 * - Home and static pages
 * - Product catalog
 * - Contact form with ticket system
 * - Prescription (ordonnance) management
 * - Shopping cart and checkout
 * - Depot/pharmacy locator
 * 
 * @package App\Controller
 */
class FrontController extends AbstractController
{
    // =========================================================================
    // Constants
    // =========================================================================

    private const AVAILABLE_STATUSES = ['disponible', 'stock_critique'];
    private const MAX_FILE_SIZE = 5242880; // 5MB
    private const ALLOWED_MIME_TYPES = [
        'image/jpeg',
        'image/png', 
        'image/gif',
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ];
    private const ITEMS_PER_PAGE = 20;
    private const DEFAULT_SORT = 'name';

    // =========================================================================
    // Properties
    // =========================================================================

    private ?LoggerInterface $logger = null;

    // =========================================================================
    // Constructor
    // =========================================================================

    public function __construct(
        private readonly EntityManagerInterface $entityManager,
    ) {}

    // =========================================================================
    // Public Methods - Routes
    // =========================================================================

    /**
     * Home page - Main landing page
     */
    #[Route('/', name: 'app_home', methods: ['GET'])]
    public function home(PanierService $panierService): Response
    {
        try {
            return $this->render('front/home.html.twig', [
                'nombre_articles_panier' => $panierService->getNombreArticles()
            ]);
        } catch (\Throwable $e) {
            $this->logError('home', $e);
            throw $this->createNotFoundException('Page not found');
        }
    }

    /**
     * Products catalog page with filtering and pagination
     */
    #[Route('/products', name: 'app_products', methods: ['GET'])]
    public function products(
        ProduitRepository $produitRepository,
        PanierService $panierService,
        Request $request
    ): Response {
        try {
            // Get filter parameters
            $category = $request->query->get('category');
            $sortBy = $request->query->get('sort', 'nom');
            $page = max(1, (int) $request->query->get('page', 1));
            $search = $request->query->get('search', '');

            // Build optimized query
            $queryBuilder = $produitRepository->createQueryBuilder('p')
                ->where('p.statut IN (:statuts)')
                ->setParameter('statuts', self::AVAILABLE_STATUSES);

            // Apply search filter
            if ($search) {
                $queryBuilder->andWhere('p.nom LIKE :search OR p.description LIKE :search')
                    ->setParameter('search', '%' . $search . '%');
            }

            // Apply category filter
            if ($category) {
                $queryBuilder->andWhere('p.categorie = :category')
                    ->setParameter('category', $category);
            }

            // Apply sorting
            $sortField = match ($sortBy) {
                'price_asc' => 'p.prix',
                'price_desc' => 'p.prix',
                default => 'p.nom',
            };
            $sortOrder = in_array($sortBy, ['price_desc']) ? 'DESC' : 'ASC';
            $queryBuilder->orderBy($sortField, $sortOrder);

            // Paginate results
            $totalItems = count($queryBuilder->getQuery()->getResult());
            $totalPages = (int) ceil($totalItems / self::ITEMS_PER_PAGE);
            $page = min($page, max(1, $totalPages));

            $produits = $queryBuilder
                ->setFirstResult(($page - 1) * self::ITEMS_PER_PAGE)
                ->setMaxResults(self::ITEMS_PER_PAGE)
                ->getQuery()
                ->getResult();

            // Get categories separately (cached)
            $categories = $this->getCategories($produitRepository);

            // Get cart details
            $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);

            return $this->render('front/products.html.twig', [
                'produits' => $produits,
                'categories' => $categories,
                'currentCategory' => $category,
                'currentSort' => $sortBy,
                'currentPage' => $page,
                'totalPages' => $totalPages,
                'totalItems' => $totalItems,
                'searchTerm' => $search,
                'nombre_articles_panier' => $panierDetails['count'],
                'panier' => $panierDetails,
            ]);
        } catch (\Throwable $e) {
            $this->logError('products', $e);
            $this->addFlash('error', 'Error loading products. Please try again.');
            return $this->redirectToRoute('app_home');
        }
    }

    /**
     * About page
     */
    #[Route('/about', name: 'app_about', methods: ['GET'])]
    public function about(PanierService $panierService): Response
    {
        return $this->render('front/about.html.twig', [
            'nombre_articles_panier' => $panierService->getNombreArticles()
        ]);
    }

    /**
     * Ordonnance form - Create new prescription request
     * Note: This is a placeholder. Full implementation should be in a dedicated controller.
     */
    #[Route('/ordonnance/new', name: 'app_formulaire_ordonnance', methods: ['GET', 'POST'])]
    public function formulaireOrdonnance(
        Request $request,
        EntityManagerInterface $entityManager
    ): Response {
        // Redirect to ordonnances page - full implementation pending
        return $this->redirectToRoute('app_ordonnances');
    }

    /**
     * My treatments page - lists user's treatments
     * Note: This is a placeholder. Full implementation should be in a dedicated controller.
     */
    #[Route('/mes-traitements', name: 'app_mes_traitements', methods: ['GET'])]
    public function mesTraitements(): Response
    {
        // Redirect to treatments page
        return $this->redirectToRoute('app_ordonnances');
    }

    /**
     * Search treatments - AJAX endpoint
     */
    #[Route('/mes-traitements/search', name: 'app_mes_traitements_search', methods: ['GET'])]
    public function searchTraitements(
        Request $request,
        OrdonnanceRepository $ordonnanceRepository,
        TraitementRepository $traitementRepository
    ): JsonResponse {
        $searchTerm = $request->query->get('search', '');
        $user = $this->getUser();

        if (!$user instanceof Utilisateur) {
            return $this->json(['html' => '', 'count' => 0]);
        }

        // Search logic - reuse ordonnances method logic
        $ordonnances = $this->buildFilteredOrdonnances(
            $ordonnanceRepository,
            $user,
            $searchTerm,
            '',
            ''
        );

        $traitements = $traitementRepository->findBy(
            ['utilisateur' => $user],
            ['id' => 'DESC']
        );

        $html = $this->renderView('front/_traitements_list.html.twig', [
            'ordonnances' => $ordonnances,
            'traitements' => $traitements,
            'searchTerm' => $searchTerm
        ]);

        return $this->json([
            'html' => $html,
            'count' => count($ordonnances)
        ]);
    }

    /**
     * Contact page with ticket creation and management
     */
    #[Route('/contact', name: 'app_contact', methods: ['GET', 'POST'])]
    public function contact(
        Request $request,
        EntityManagerInterface $entityManager,
        QuestionRepository $questionRepository,
        MailerService $mailerService
    ): Response {
        try {
            $editMode = false;
            $editQuestionId = $request->query->get('edit');
            
            // Handle edit mode - load existing question
            $question = $this->handleEditMode($editQuestionId, $questionRepository, $editMode);
            
            // Create and handle form
            $form = $this->createForm(QuestionType::class, $question);
            $form->handleRequest($request);

            // Process form submission
            if ($form->isSubmitted() && $form->isValid()) {
                return $this->processContactForm(
                    $form,
                    $question,
                    $editMode,
                    $entityManager,
                    $mailerService
                );
            }

            // Get user's questions
            $mesQuestions = $this->getUserQuestions($questionRepository);

            return $this->render('front/contact.html.twig', [
                'form' => $form,
                'mesQuestions' => $mesQuestions,
                'editMode' => $editMode,
                'editQuestion' => $editMode ? $question : null,
            ]);
        } catch (\Throwable $e) {
            $this->logError('contact', $e);
            $this->addFlash('error', 'An error occurred. Please try again.');
            return $this->redirectToRoute('app_contact');
        }
    }

    // =========================================================================
    // Prescription (Ordonnance) Methods
    // =========================================================================

    /**
     * List user's prescriptions with search and filtering
     * Note: This is a wrapper around the existing mes_traitements functionality
     */
    #[Route('/ordonnances', name: 'app_ordonnances', methods: ['GET', 'POST'])]
    public function ordonnances(
        Request $request,
        EntityManagerInterface $entityManager,
        OrdonnanceRepository $ordonnanceRepository,
        TraitementRepository $traitementRepository,
        ProduitRepository $produitRepository
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            return $this->redirectToRoute('app_login');
        }

        $searchTerm = $request->query->get('search', '');
        $dateFilter = $request->query->get('date', '');
        $statusFilter = $request->query->get('status', '');

        // Build filtered query
        $ordonnances = $this->buildFilteredOrdonnances(
            $ordonnanceRepository,
            $user,
            $searchTerm,
            $dateFilter,
            $statusFilter
        );

        // Get associated traitements
        $traitements = $traitementRepository->findBy(
            ['utilisateur' => $user],
            ['id' => 'DESC']
        );

        return $this->render('front/mes_traitements.html.twig', [
            'ordonnances' => $ordonnances,
            'traitements' => $traitements,
            'searchTerm' => $searchTerm,
            'dateFilter' => $dateFilter,
            'statusFilter' => $statusFilter,
        ]);
    }

    /**
     * Generate prescription PDF
     */
    #[Route('/ordonnance/{id}/pdf', name: 'app_ordonnance_pdf', methods: ['GET'])]
    public function ordonnancePdf(Ordonnance $ordonnance): Response
    {
        // Security check - verify ownership
        $this->verifyOwnership($ordonnance, $ordonnance->getUtilisateur());

        try {
            $pdfContent = $this->generatePdf(
                'front/pdf/ordonnance_pdf.html.twig',
                ['ordonnance' => $ordonnance],
                'Ordonnance_' . $ordonnance->getNumeroOrdonnance()
            );

            return new Response($pdfContent, 200, [
                'Content-Type' => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="Ordonnance_' . 
                    $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf"',
            ]);
        } catch (\Throwable $e) {
            $this->logError('ordonnance_pdf', $e);
            $this->addFlash('error', 'Error generating PDF. Please try again.');
            return $this->redirectToRoute('app_ordonnances');
        }
    }

    /**
     * Generate complete prescription PDF with treatments
     */
    #[Route('/ordonnance/{id}/complete-pdf', name: 'app_ordonnance_complete_pdf', methods: ['GET'])]
    public function ordonnanceCompletePdf(Ordonnance $ordonnance): Response
    {
        $this->verifyOwnership($ordonnance, $ordonnance->getUtilisateur());

        try {
            $pdfContent = $this->generatePdf(
                'front/pdf/ordonnance_complete_pdf.html.twig',
                ['ordonnance' => $ordonnance],
                'Ordonnance_Complete_' . $ordonnance->getNumeroOrdonnance()
            );

            return new Response($pdfContent, 200, [
                'Content-Type' => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="Ordonnance_Complete_' . 
                    $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf"',
            ]);
        } catch (\Throwable $e) {
            $this->logError('ordonnance_complete_pdf', $e);
            $this->addFlash('error', 'Error generating PDF. Please try again.');
            return $this->redirectToRoute('app_ordonnances');
        }
    }

    // =========================================================================
    // Cart and Checkout Methods
    // =========================================================================

    /**
     * Shopping cart page
     */
    #[Route('/panier', name: 'app_panier', methods: ['GET'])]
    public function panier(
        PanierService $panierService,
        ProduitRepository $produitRepository
    ): Response {
        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);

        return $this->render('front/panier.html.twig', [
            'panier' => $panierDetails,
            'nombre_articles_panier' => $panierDetails['count'],
        ]);
    }

    /**
     * Checkout page
     */
    #[Route('/commande', name: 'app_commande', methods: ['GET', 'POST'])]
    public function commande(
        Request $request,
        EntityManagerInterface $entityManager,
        PanierService $panierService,
        ProduitRepository $produitRepository,
        AddressRepository $addressRepository,
        CouponService $couponService,
        LoyaltyService $loyaltyService
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            $this->addFlash('warning', 'Please login to continue.');
            return $this->redirectToRoute('app_login');
        }

        // Get cart details
        $panierDetails = $this->buildPanierDetails($panierService, $produitRepository);
        
        // Check if cart is empty
        if ($panierDetails['count'] === 0) {
            $this->addFlash('info', 'Your cart is empty.');
            return $this->redirectToRoute('app_products');
        }

        // Get user's addresses
        $addresses = $addressRepository->findBy(['utilisateur' => $user]);

        // Get applied coupon from session
        $couponCodeFromQuery = $request->query->get('coupon', '');
        
        // Calculate pricing
        $pricing = $this->buildCheckoutPricing(
            $panierDetails,
            $couponCodeFromQuery,
            $user,
            $couponService,
            $loyaltyService
        );

        // Handle form submission
        if ($request->isMethod('POST')) {
            return $this->processCheckout(
                $request,
                $panierDetails,
                $pricing,
                $entityManager,
                $user
            );
        }

        return $this->render('front/commande.html.twig', [
            'panier' => $panierDetails,
            'pricing' => $pricing,
            'addresses' => $addresses,
            'nombre_articles_panier' => $panierDetails['count'],
            'prefill' => [
                'nom' => $user->getNom() . ' ' . $user->getPrenom(),
                'email' => $user->getEmail(),
                'telephone' => '',
                'adresse_livraison' => '',
                'message' => '',
                'mode_paiement' => 'livraison',
                'coupon_code' => $couponCodeFromQuery,
                'address_id' => '',
            ],
        ]);
    }

    // =========================================================================
    // Depot (Pharmacy) Methods
    // =========================================================================

    /**
     * List depots with filtering
     */
    #[Route('/depots', name: 'front_depots')]
    public function depots(DepotRepository $depotRepository, Request $request): Response
    {
        $filters = $this->getDepotFilters($request);
        $queryBuilder = $this->buildDepotQuery($depotRepository, $filters);
        
        // Apply sorting
        $this->applyDepotSorting($queryBuilder, $filters['sort']);
        
        $depots = $queryBuilder->getQuery()->getResult();

        return $this->render('front/depots.html.twig', [
            'depots' => $depots,
            'filters' => $filters,
        ]);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Handle edit mode for contact form
     */
    private function handleEditMode(
        ?string $editQuestionId,
        QuestionRepository $questionRepository,
        bool &$editMode
    ): Question {
        if (!$editQuestionId || !$this->getUser()) {
            return new Question();
        }

        $question = $questionRepository->find($editQuestionId);
        
        if ($question && $question->getUtilisateur() === $this->getUser()) {
            $editMode = true;
            return $question;
        }

        $this->addFlash('error', 'Question not found or access denied.');
        $this->redirectToRoute('app_contact');
        
        return new Question();
    }

    /**
     * Process contact form submission
     */
    private function processContactForm(
        FormInterface $form,
        Question $question,
        bool $editMode,
        EntityManagerInterface $entityManager,
        MailerService $mailerService
    ): Response {
        // Check authentication
        if (!$this->getUser()) {
            $this->addFlash('error', 'You must be logged in to submit a ticket.');
            return $this->redirectToRoute('app_login');
        }

        // Handle file upload
        $this->handleFileUpload($form, $question);

        // Set question metadata
        $question->setUtilisateur($this->getUser());
        if (!$editMode) {
            $question->setStatut('ouvert');
        }

        // Save to database
        $entityManager->persist($question);
        $entityManager->flush();

        // Send confirmation email
        if (!$editMode) {
            try {
                $mailerService->sendTicketCreatedEmail($question);
                $this->addFlash('success', 'Your message has been sent successfully. A confirmation email has been sent.');
            } catch (\Throwable $e) {
                $this->logError('send_ticket_email', $e);
                $this->addFlash('warning', 'Ticket created but confirmation email could not be sent.');
            }
        } else {
            $this->addFlash('success', 'Your ticket has been updated successfully.');
        }

        return $this->redirectToRoute('app_contact');
    }

    /**
     * Handle file upload with validation
     */
    private function handleFileUpload(FormInterface $form, Question $question): void
    {
        $fichier = $form->get('fichier')->getData();
        
        if (!$fichier) {
            return;
        }

        // Validate file
        $this->validateFile($fichier);

        try {
            $originalName = $fichier->getClientOriginalName();
            $mimeType = $fichier->getMimeType();
            $fileSize = $fichier->getSize();
            
            // Generate safe filename
            $slugger = new AsciiSlugger();
            $safeFilename = $slugger->slug(pathinfo($originalName, PATHINFO_FILENAME));
            $newFilename = $safeFilename . '-' . uniqid() . '.' . $fichier->guessExtension();

            // Move file to upload directory
            $uploadDir = $this->getParameter('kernel.project_dir') . '/public/uploads/questions';
            $this->ensureDirectoryExists($uploadDir);
            
            $fichier->move($uploadDir, $newFilename);

            // Set file metadata on question
            $question->setFileName($originalName);
            $question->setFilePath('/uploads/questions/' . $newFilename);
            $question->setFileType($mimeType);
            $question->setFileSize($fileSize);
        } catch (\Symfony\Component\HttpFoundation\File\Exception\FileException $e) {
            $this->logError('file_upload', $e);
            throw new FileUploadException('Error uploading file. Please try again.');
        }
    }

    /**
     * Validate uploaded file
     */
    private function validateFile($fichier): void
    {
        // Check file size
        if ($fichier->getSize() > self::MAX_FILE_SIZE) {
            throw new FileUploadException('File size exceeds maximum allowed size of 5MB.');
        }

        // Check mime type
        $mimeType = $fichier->getMimeType();
        if (!in_array($mimeType, self::ALLOWED_MIME_TYPES, true)) {
            throw new FileUploadException('File type not allowed. Allowed types: JPEG, PNG, GIF, PDF, DOC, DOCX.');
        }
    }

    /**
     * Ensure directory exists
     */
    private function ensureDirectoryExists(string $path): void
    {
        if (!is_dir($path)) {
            mkdir($path, 0777, true);
        }
    }

    /**
     * Get user's questions
     */
    private function getUserQuestions(QuestionRepository $questionRepository): array
    {
        if (!$this->getUser()) {
            return [];
        }

        return $questionRepository->findByUtilisateur($this->getUser()->getId());
    }

    /**
     * Get available categories
     */
    private function getCategories(ProduitRepository $produitRepository): array
    {
        $categories = $produitRepository->createQueryBuilder('p')
            ->select('DISTINCT p.categorie')
            ->where('p.statut IN (:statuts)')
            ->setParameter('statuts', self::AVAILABLE_STATUSES)
            ->orderBy('p.categorie', 'ASC')
            ->getQuery()
            ->getArrayResult();

        return array_map(static fn(array $row) => $row['categorie'], $categories);
    }

    /**
     * Build filtered ordonnances query
     */
    private function buildFilteredOrdonnances(
        OrdonnanceRepository $repository,
        Utilisateur $user,
        string $searchTerm,
        string $dateFilter,
        string $statusFilter
    ): array {
        $queryBuilder = $repository->createQueryBuilder('o')
            ->where('o.utilisateur = :user')
            ->setParameter('user', $user)
            ->orderBy('o.dateOrdonnance', 'DESC');

        if ($searchTerm) {
            $queryBuilder->andWhere(
                'o.numeroOrdonnance LIKE :search OR o.noteMedical LIKE :search'
            )
            ->setParameter('search', '%' . $searchTerm . '%');
        }

        if ($dateFilter) {
            $queryBuilder->andWhere('DATE(o.dateOrdonnance) = :date')
                ->setParameter('date', $dateFilter);
        }

        if ($statusFilter) {
            $queryBuilder->andWhere('o.statut = :status')
                ->setParameter('status', $statusFilter);
        }

        return $queryBuilder->getQuery()->getResult();
    }

    /**
     * Generate PDF from template
     */
    private function generatePdf(string $template, array $data, string $filename): string
    {
        $html = $this->renderView($template, $data);

        $options = new Options();
        $options->set('defaultFont', 'Arial')
            ->set('isRemoteEnabled', true)
            ->set('isHtml5ParserEnabled', true);

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return $dompdf->output();
    }

    /**
     * Verify resource ownership
     */
    private function verifyOwnership($resource, ?Utilisateur $owner): void
    {
        if (!$this->getUser() || $owner !== $this->getUser()) {
            throw $this->createAccessDeniedException(
                'You do not have permission to access this resource.'
            );
        }
    }

    /**
     * Get depot filters from request
     */
    private function getDepotFilters(Request $request): array
    {
        return [
            'search' => $request->query->get('search'),
            'ville' => $request->query->get('ville'),
            'capaciteMin' => $request->query->get('capacite_min'),
            'capaciteMax' => $request->query->get('capacite_max'),
            'sort' => $request->query->get('sort', self::DEFAULT_SORT),
        ];
    }

    /**
     * Build depot query with filters
     */
    private function buildDepotQuery(DepotRepository $repository, array $filters): \Doctrine\ORM\QueryBuilder
    {
        $queryBuilder = $repository->createQueryBuilder('d');

        if ($filters['search']) {
            $queryBuilder->andWhere(
                'd.nomDepot LIKE :search OR d.adresseDepot LIKE :search OR d.responsableDepot LIKE :search'
            )->setParameter('search', '%' . $filters['search'] . '%');
        }

        if ($filters['ville']) {
            $queryBuilder->andWhere('d.adresseDepot LIKE :ville')
                ->setParameter('ville', '%' . $filters['ville'] . '%');
        }

        if ($filters['capaciteMin']) {
            $queryBuilder->andWhere('d.capaciteDepot >= :capaciteMin')
                ->setParameter('capaciteMin', $filters['capaciteMin']);
        }

        if ($filters['capaciteMax']) {
            $queryBuilder->andWhere('d.capaciteDepot <= :capaciteMax')
                ->setParameter('capaciteMax', $filters['capaciteMax']);
        }

        return $queryBuilder;
    }

    /**
     * Apply sorting to depot query
     */
    private function applyDepotSorting(\Doctrine\ORM\QueryBuilder $queryBuilder, string $sort): void
    {
        $sortMap = [
            'name' => ['d.nomDepot', 'ASC'],
            'capacity_desc' => ['d.capaciteDepot', 'DESC'],
            'capacity_asc' => ['d.capaciteDepot', 'ASC'],
            'date_desc' => ['d.dateCreation', 'DESC'],
            'date_asc' => ['d.dateCreation', 'ASC'],
        ];

        [$field, $order] = $sortMap[$sort] ?? ['d.nomDepot', 'ASC'];
        $queryBuilder->orderBy($field, $order);
    }

    /**
     * Build cart details with product information
     * 
     * @deprecated Use PanierService::getPanierDetails() instead
     */
    private function buildPanierDetails(
        PanierService $panierService,
        ProduitRepository $produitRepository
    ): array {
        return $panierService->getPanierDetails();
    }

    /**
     * Build checkout pricing
     */
    private function buildCheckoutPricing(
        array $panierDetails,
        string $couponCode,
        ?Utilisateur $user,
        CouponService $couponService,
        LoyaltyService $loyaltyService
    ): array {
        $subtotal = $panierDetails['subtotal'] ?? 0;
        $shipping = $panierDetails['shipping'] ?? 0;
        $couponDiscount = 0;
        $couponValid = false;
        $couponMessage = '';

        // Apply coupon if provided
        if ($couponCode) {
            $couponResult = $couponService->applyCoupon($couponCode, $panierDetails);
            $couponValid = $couponResult['valid'] ?? false;
            $couponDiscount = $couponResult['discount'] ?? 0;
            $couponMessage = $couponResult['message'] ?? '';
        }

        // Calculate loyalty points
        $loyaltyPoints = 0;
        $loyaltyDiscount = 0;
        if ($user) {
            $loyaltyPoints = $user->getPointsFidelite() ?? 0;
            $loyaltyDiscount = $loyaltyService->calculateDiscount($loyaltyPoints);
        }

        $total = max(0, $subtotal + $shipping - $couponDiscount - $loyaltyDiscount);

        return [
            'subtotal' => $subtotal,
            'shipping' => $shipping,
            'couponDiscount' => $couponDiscount,
            'loyaltyDiscount' => $loyaltyDiscount,
            'loyaltyPoints' => $loyaltyPoints,
            'total' => $total,
            'couponValid' => $couponValid,
            'couponMessage' => $couponMessage,
        ];
    }

    /**
     * Process checkout form submission
     */
    private function processCheckout(
        Request $request,
        array $panierDetails,
        array $pricing,
        EntityManagerInterface $entityManager,
        Utilisateur $user
    ): Response {
        // Get form data
        $nom = trim($request->request->get('nom', ''));
        $email = trim($request->request->get('email', ''));
        $telephone = trim($request->request->get('telephone', ''));
        $adresse = trim($request->request->get('adresse_livraison', ''));
        $message = trim($request->request->get('message', ''));
        $modePaiement = $request->request->get('mode_paiement', 'livraison');
        $addressId = $request->request->get('address_id');

        // Validate required fields
        $errors = [];
        if (empty($nom)) {
            $errors[] = 'Name is required.';
        }
        if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $errors[] = 'Valid email is required.';
        }
        if (empty($adresse)) {
            $errors[] = 'Delivery address is required.';
        }

        if (!empty($errors)) {
            foreach ($errors as $error) {
                $this->addFlash('error', $error);
            }
            return $this->redirectToRoute('app_commande');
        }

        // TODO: Create commande and process payment
        // This is a placeholder for the actual checkout logic

        $this->addFlash('success', 'Order placed successfully!');
        // Note: Replace with actual order history route when available
        return $this->redirectToRoute('app_home');
    }

    /**
     * Log error with context
     */
    private function logError(string $context, \Throwable $e): void
    {
        if ($this->logger) {
            $this->logger->error(sprintf(
                '[FrontController::%s] %s: %s',
                $context,
                get_class($e),
                $e->getMessage()
            ), [
                'exception' => $e,
                'trace' => $e->getTraceAsString(),
            ]);
        }
    }

    /**
     * Set logger (can be injected via DI)
     */
    public function setLogger(LoggerInterface $logger): void
    {
        $this->logger = $logger;
    }
}
