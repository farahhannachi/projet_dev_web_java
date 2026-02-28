<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use App\Service\TwoFactorAuthService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Component\Validator\Constraints as Assert;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;
use App\Service\OllamaService;

#[Route('/profile')]
#[IsGranted('ROLE_USER')]
class ProfileController extends AbstractController
{
    
    public function __construct(
        private TwoFactorAuthService $twoFactorAuthService,
        private EntityManagerInterface $entityManager,
        private HttpClientInterface $httpClient,
        private LoggerInterface $logger,
        private OllamaService $ollamaService
    ) {
    }
    
    #[Route('', name: 'app_profile', methods: ['GET', 'POST'])]
    public function show(Request $request): Response
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        // Handle 2FA actions via POST
        $error = null;
        $success = null;
        $qrCodeImage = null;
        $secret = null;
        
        if ($request->isMethod('POST')) {
            // Disable 2FA
            if ($request->request->has('disable_2fa')) {
                $this->twoFactorAuthService->disable2FA($user);
                $success = 'L\'authentification à deux facteurs a été désactivée.';
            }
            
            // Enable 2FA - verify code
            if ($request->request->has('verification_code') && !$this->twoFactorAuthService->is2FAEnabled($user)) {
                $code = $request->request->get('verification_code');
                
                if (empty($code)) {
                    $error = 'Veuillez entrer le code de vérification.';
                } else {
                    // Generate secret if not exists
                    if (!$user->getTotpSecret()) {
                        $secret = $this->twoFactorAuthService->generateSecret();
                        $user->setTotpSecret($secret);
                        $this->entityManager->flush();
                    }
                    
                    if ($this->twoFactorAuthService->enable2FA($user, (string) $code)) {
                        $success = 'L\'authentification à deux facteurs est maintenant activée!';
                    } else {
                        $error = 'Code invalide. Veuillez vérifier le code dans votre application Google Authenticator.';
                    }
                }
            }
            
            // Generate QR code for setup
            if ($request->request->has('generate_qr')) {
                $secret = $user->getTotpSecret() ?: $this->twoFactorAuthService->generateSecret();
                $user->setTotpSecret($secret);
                $this->entityManager->flush();
                $qrCodeImage = $this->twoFactorAuthService->getQrCodeImage($user);
            }
        }
        
        // Get 2FA status
        $isEnabled = $this->twoFactorAuthService->is2FAEnabled($user);
        
        // If QR code not generated yet, prepare it for modal
        if (!$qrCodeImage && !$isEnabled) {
            $secret = $user->getTotpSecret() ?: $this->twoFactorAuthService->generateSecret();
            if (!$user->getTotpSecret()) {
                $user->setTotpSecret($secret);
                $this->entityManager->flush();
            }
            try {
                $qrCodeImage = $this->twoFactorAuthService->getQrCodeImage($user);
            } catch (\Exception $e) {
                $qrCodeImage = null;
            }
        }

        // Calculate account age
        $createdAt = $user->getDateCreation();
        $now = new \DateTimeImmutable();
        $accountAge = $createdAt ? $createdAt->diff($now) : null;

        // Format account standing text
        $accountStatus = match ($user->getEtatCompte()) {
            'actif' => 'Compte Actif',
            'suspendu' => 'Compte Suspendu',
            'desactive' => 'Compte Désactivé',
            default => 'Compte Actif',
        };

        // Get loyalty information
        $loyaltyLevel = $user->getLoyaltyLevel();
        $loyaltyPoints = $user->getLoyaltyPoints();
        $segment = $user->getSegment();

        return $this->render('front/profile/show.html.twig', [
            'user' => $user,
            'accountStatus' => $accountStatus,
            'accountAge' => $accountAge,
            'loyaltyLevel' => $loyaltyLevel,
            'loyaltyPoints' => $loyaltyPoints,
            'segment' => $segment,
            'isEnabled' => $isEnabled,
            'qrCodeImage' => $qrCodeImage,
            'secret' => $secret ?: ($user->getTotpSecret() ?: ''),
            'error' => $error,
            'success' => $success,
        ]);
    }

    #[Route('/edit', name: 'app_profile_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, ValidatorInterface $validator, EntityManagerInterface $em): Response
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        $errors = [];

        if ($request->isMethod('POST')) {
            $nom = trim((string) $request->request->get('nom', ''));
            $prenom = trim((string) $request->request->get('prenom', ''));
            $email = trim((string) $request->request->get('email', ''));

            // Validation constraints
            $constraints = new Assert\Collection([
                'nom' => [
                    new Assert\NotBlank(['message' => 'Le nom est obligatoire']),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le nom ne peut pas dépasser {{ limit }} caractères'
                    ]),
                ],
                'prenom' => [
                    new Assert\NotBlank(['message' => 'Le prénom est obligatoire']),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Le prénom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le prénom ne peut pas dépasser {{ limit }} caractères'
                    ]),
                ],
                'email' => [
                    new Assert\NotBlank(['message' => 'L\'email est obligatoire']),
                    new Assert\Email(['message' => 'L\'email "{{ value }}" n\'est pas valide']),
                ],
            ]);

            // Validate the data
            $violations = $validator->validate([
                'nom' => $nom,
                'prenom' => $prenom,
                'email' => $email,
            ], $constraints);

            if (count($violations) == 0) {
                // Check if email is already taken by another user
                $existingUser = $em->getRepository(Utilisateur::class)->findOneBy(['email' => $email]);
                if ($existingUser && $existingUser->getId() !== $user->getId()) {
                    $errors['email'] = 'Cet email est déjà utilisé par un autre compte';
                } else {
                    // Update user data
                    $user->setNom($nom);
                    $user->setPrenom($prenom);
                    $user->setEmail($email);

                    $em->flush();
                    $this->addFlash('success', 'Votre profil a été mis à jour avec succès!');

                    return $this->redirectToRoute('app_profile');
                }
            } else {
                // Convert violations to error array
                foreach ($violations as $violation) {
                    $propertyPath = $violation->getPropertyPath();
                    $errors[$propertyPath] = $violation->getMessage();
                }
            }
        }

        return $this->render('front/profile/edit.html.twig', [
            'user' => $user,
            'errors' => $errors,
        ]);
    }

    #[Route('/ai-health-summary', name: 'app_profile_ai_summary', methods: ['GET', 'POST'])]
    public function aiHealthSummary(
        Request $request,
        TraitementRepository $traitementRepository,
        OrdonnanceRepository $ordonnanceRepository
    ): JsonResponse {
        // Prevent any warning/notice from leaking into JSON output
        error_reporting(0);
        ini_set('display_errors', 0);
        
        // Ensure this slow AI task doesn't hit PHP's max_execution_time prematurely
        set_time_limit(300);
        
        // Aggressively clear any existing output buffers (e.g. from auto-prepend or previous errors)
        while (ob_get_level() > 0) {
            ob_end_clean();
        }
        
        // Start our isolated output buffering
        ob_start();

        try {
            /** @var Utilisateur $user */
            $user = $this->getUser();
            if (!$user) {
                if (ob_get_length()) ob_end_clean();
                return $this->json(['error' => 'Non authentifié'], 401);
            }

            // Read age and height from browser (sent as JSON body)
            $body = json_decode($request->getContent(), true) ?? [];
            $ageFromClient = isset($body['age']) && $body['age'] ? (int)$body['age'] : null;
            $tailleFromClient = isset($body['taille']) && $body['taille'] ? (int)$body['taille'] : null;

            // Fallback: calculate age from dateNaissance if not provided by client
            $age = $ageFromClient;
            if (!$age && $user->getDateNaissance()) {
                $age = $user->getDateNaissance()->diff(new \DateTime())->y;
            }
            $taille = $tailleFromClient;

            // Get ordonnances
            $ordonnances = $ordonnanceRepository->findBy(['utilisateur' => $user], ['dateOrdonnance' => 'DESC'], 5);
            $ordonnanceDetails = [];
            foreach ($ordonnances as $ord) {
                $ordonnanceDetails[] = [
                    'numero' => $ord->getNumeroOrdonnance(),
                    'date' => $ord->getDateOrdonnance() ? $ord->getDateOrdonnance()->format('Y-m-d') : 'N/A',
                    'statut' => $ord->getStatut(),
                    'note' => mb_substr($ord->getNoteMedical() ?? '', 0, 300),
                ];
            }

            // Get traitements
            $traitements = $traitementRepository->findBy(['utilisateur' => $user], ['dateDebut' => 'DESC'], 5);
            $traitementDetails = [];
            foreach ($traitements as $t) {
                $traitementDetails[] = [
                    'produit' => $t->getProduit() ? $t->getProduit()->getNom() : 'Inconnu',
                    'statut' => $t->getStatus(),
                    'debut' => $t->getDateDebut() ? $t->getDateDebut()->format('Y-m-d') : 'N/A',
                    'fin' => $t->getDateFin() ? $t->getDateFin()->format('Y-m-d') : 'N/A',
                    'duree' => ($t->getDureeJours() ?: 0) . ' jours',
                ];
            }

            // Build summary context
            $context = [
                'Nom' => $user->getNom() . ' ' . $user->getPrenom(),
                'Email' => $user->getEmail(),
                'Age' => $age ? $age . ' ans' : 'Non renseigné',
                'Taille' => $taille ? $taille . ' cm' : 'Non renseignée',
                'Telephone' => $user->getTelephone() ?? 'Non renseigné',
                'Fidélité' => $user->getLoyaltyLevel() . ' (' . $user->getLoyaltyPoints() . ' points)',
                'Ordonnances récentes' => $ordonnanceDetails,
                'Traitements récents' => $traitementDetails,
            ];

            $contextJson = json_encode($context, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

            $prompt = "Tu es un assistant médical intelligent et bienveillant pour la pharmacie CuraVita.

Voici les données complètes du patient:\n\n" . $contextJson . "\n\n
Analyse ces données et génère exactement 3 conseils de santé personnalisés pour ce patient.
Chaque conseil doit être:
- Basé sur les traitements, ordonnances et historique réel du patient
- Pratique et actionnable
- Rédigé en français, en tutoyant le patient
- Commençant par une emoji médicale pertinente

Réponds UNIQUEMENT avec un tableau JSON de 3 objets, chaque objet ayant:
- 'titre': un titre court (max 5 mots)
- 'conseil': le conseil complet (max 2 phrases)
- 'priorite': 'haute', 'moyenne', ou 'basse'

Exemple de format: [{\"titre\": \"Hydratation\", \"conseil\": \"...\", \"priorite\": \"haute\"}]";

            $rawText = $this->ollamaService->generateResponse(
                message: "Analyse les données du patient.",
                conversationHistory: [],
                systemPrompt: $prompt,
                timeout: 110
            );

            // --- Robust JSON extraction ---
            $insights = null;

            // Case 1: response is a plain JSON array [ {...}, ... ]
            if (preg_match('/\[.*\]/s', $rawText, $m)) {
                $decoded = json_decode($m[0], true);
                if (is_array($decoded) && isset($decoded[0])) {
                    $insights = $decoded;
                }
            }

            // Case 2: check if response is already an array or an object
            if (!$insights) {
                $decoded = json_decode($rawText, true);
                if (is_array($decoded)) {
                    // Check if it's a direct list
                    if (isset($decoded[0])) {
                        $insights = $decoded;
                    } else {
                        // It's an object — find the first array value inside
                        foreach ($decoded as $v) {
                            if (is_array($v) && !empty($v) && isset($v[0])) {
                                $insights = $v;
                                break;
                            }
                        }
                    }
                }
            }

            // Case 3: extract any JSON object array buried in text
            if (!$insights && preg_match_all('/\{[^{}]+\}/s', $rawText, $objects)) {
                $parsed = [];
                foreach ($objects[0] as $obj) {
                    $d = json_decode($obj, true);
                    if (is_array($d) && isset($d['titre'])) {
                        $parsed[] = $d;
                    }
                }
                if (!empty($parsed)) {
                    $insights = $parsed;
                }
            }

            if (empty($insights)) {
                throw new \Exception('Réponse invalide de Ollama. Raw: ' . mb_substr($rawText, 0, 200));
            }

            // Calculate overall health verdict from priorities
            $priorityScore = 0;
            $insightsSlice = array_slice($insights, 0, 3);
            foreach ($insightsSlice as $insight) {
                $p = strtolower($insight['priorite'] ?? 'moyenne');
                if ($p === 'haute') $priorityScore += 2;
                elseif ($p === 'moyenne') $priorityScore += 1;
                // basse = 0
            }
            // 0-1 = bon, 2-3 = moyen, 4+ = mauvais
            $verdict = $priorityScore <= 1 ? 'bon' : ($priorityScore <= 3 ? 'moyen' : 'mauvais');

            // Discard any captured accidental output to ensure clean JSON
            if (ob_get_length()) ob_end_clean();

            return $this->json([
                'success' => true,
                'insights' => $insightsSlice,
                'verdict' => $verdict,
                'userName' => $user->getNom() . ' ' . $user->getPrenom(),
                'avatar' => $user->getAvatarUrl() ?? 'https://api.dicebear.com/7.x/initials/svg?seed=' . urlencode($user->getNom() . '+' . $user->getPrenom()) . '&backgroundColor=16563f&fontColor=ffffff',
            ]);

        } catch (\Throwable $e) {
            // Log the error for server-side debugging
            error_log('AI Summary Critical Error: ' . $e->getMessage() . ' in ' . $e->getFile() . ' on line ' . $e->getLine());
            error_log('Stack trace: ' . $e->getTraceAsString());

            // Return JSON error instead of allowing a 500 HTML page
            if (ob_get_length()) ob_end_clean();

            return $this->json([
                'success' => false,
                'error' => 'Erreur lors de la génération de l\'analyse.',
                'details' => $e->getMessage(),
                'file' => basename($e->getFile()),
                'line' => $e->getLine(),
            ], 200);
        }
    }
}
