<?php

namespace App\Service;

use App\Entity\Ordonnance;
use App\Entity\Produit;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

/**
 * Service pour analyser la note médicale d'une ordonnance avec l'IA
 * et suggérer automatiquement le dosage, la fréquence et la durée
 */
class IAAnalyseNoteMedicaleService
{
    private HttpClientInterface $httpClient;
    private LoggerInterface $logger;
    private string $apiKey;

    public function __construct(
        HttpClientInterface $httpClient,
        LoggerInterface $logger,
        ?string $apiKey = ''
    ) {
        $this->httpClient = $httpClient;
        $this->logger = $logger;
        $this->apiKey = $apiKey ?? '';
    }

    /**
     * Analyse la note médicale d'une ordonnance et suggère les informations de traitement
     * 
     * @param Ordonnance $ordonnance L'ordonnance contenant la note médicale
     * @param Produit|null $produit Le produit prescrit (optionnel)
     * @return array Suggestions de dosage, fréquence et durée
     */
    public function analyserNoteMedicale(Ordonnance $ordonnance, ?Produit $produit = null): array
    {
        $noteMedicale = $ordonnance->getNoteMedical();
        
        if (empty($noteMedicale)) {
            return [
                'success' => false,
                'error' => 'Aucune note médicale à analyser'
            ];
        }

        // Essayer OpenAI en priorité
        if (!empty($this->apiKey) && strpos($this->apiKey, 'sk-') === 0) {
            $this->logger->info('🤖 Analyse de la note médicale avec OpenAI');
            $resultat = $this->analyserAvecOpenAI($noteMedicale, $produit);
            if ($resultat !== null) {
                return $resultat;
            }
        }

        // Fallback : Analyse basique
        $this->logger->warning('⚠️ IA non disponible, utilisation de l\'analyse basique');
        return $this->analyseBasique($noteMedicale, $produit);
    }

    /**
     * Analyse avec OpenAI
     */
    private function analyserAvecOpenAI(string $noteMedicale, ?Produit $produit): ?array
    {
        try {
            $this->logger->info('🤖 === ANALYSE NOTE MEDICALE AVEC OPENAI ===');

            $prompt = $this->construirePrompt($noteMedicale, $produit);

            $response = $this->httpClient->request('POST', 'https://api.openai.com/v1/chat/completions', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'model' => 'gpt-3.5-turbo',
                    'messages' => [
                        [
                            'role' => 'system',
                            'content' => 'Tu es un pharmacien expert. Tu analyses les notes médicales pour extraire et suggérer le dosage, la fréquence et la durée de traitement appropriés. Réponds UNIQUEMENT en JSON.'
                        ],
                        [
                            'role' => 'user',
                            'content' => $prompt
                        ]
                    ],
                    'temperature' => 0.3,
                    'max_tokens' => 800
                ],
                'timeout' => 30
            ]);

            $statusCode = $response->getStatusCode();
            if ($statusCode !== 200) {
                $this->logger->error('❌ Erreur HTTP OpenAI: ' . $statusCode);
                return null;
            }

            $data = $response->toArray();

            if (isset($data['error'])) {
                $this->logger->error('❌ Erreur API OpenAI', ['error' => $data['error']]);
                return null;
            }

            if (isset($data['choices'][0]['message']['content'])) {
                $content = $data['choices'][0]['message']['content'];
                $this->logger->info('✅ Réponse OpenAI reçue', ['content' => substr($content, 0, 200)]);

                // Parser le JSON
                $result = json_decode($content, true);

                if ($result && isset($result['dosage'], $result['frequence'], $result['duree_jours'])) {
                    $this->logger->info('✅ Analyse note médicale réussie', $result);
                    return [
                        'success' => true,
                        'dosage' => $result['dosage'],
                        'frequence' => $result['frequence'],
                        'duree_jours' => $result['duree_jours'],
                        'explication' => $result['explication'] ?? '',
                        'precautions' => $result['precautions'] ?? '',
                        'source' => 'OpenAI GPT-3.5'
                    ];
                }
            }

            return null;

        } catch (\Exception $e) {
            $this->logger->error('❌ Exception OpenAI', [
                'error' => $e->getMessage()
            ]);
            return null;
        }
    }

    /**
     * Construit le prompt pour OpenAI
     */
    private function construirePrompt(string $noteMedicale, ?Produit $produit): string
    {
        $produitInfo = '';
        if ($produit) {
            $produitInfo = "\n**MÉDICAMENT PRESCRIT:**\n- Nom: {$produit->getNom()}\n- Description: {$produit->getDescription()}\n";
        }

        return <<<PROMPT
Tu es un pharmacien expert. Analyse cette note médicale et suggère un dosage, une fréquence et une durée de traitement appropriés.
{$produitInfo}
**NOTE MÉDICALE DU PATIENT:**
{$noteMedicale}

**INSTRUCTIONS:**
1. Analyse attentivement la note médicale
2. Identifie les symptômes, antécédents et conditions du patient
3. Suggère un dosage précis et adapté
4. Détermine la fréquence de prise optimale
5. Calcule la durée de traitement recommandée
6. Fournis des précautions importantes si nécessaire

**RÉPONDS UNIQUEMENT EN JSON avec cette structure exacte:**
{
    "dosage": "exemple: 500mg par prise ou 2 comprimés",
    "frequence": "exemple: 3 fois par jour (matin, midi, soir) ou toutes les 8 heures",
    "duree_jours": 7,
    "explication": "Explication claire de la posologie recommandée basée sur la note médicale",
    "precautions": "Précautions importantes à prendre (interactions, contre-indications, etc.)"
}

**RÈGLES IMPORTANTES:**
- Le dosage doit être précis (mg, ml, comprimés, gélules, etc.)
- La fréquence doit indiquer clairement quand prendre le médicament
- La durée doit être en jours (nombre entier entre 1 et 365)
- L'explication doit justifier tes recommandations
- Les précautions doivent mentionner les points d'attention importants
- Base tes recommandations sur les standards médicaux et pharmaceutiques
PROMPT;
    }

    /**
     * Analyse basique (fallback sans IA)
     */
    private function analyseBasique(string $noteMedicale, ?Produit $produit): array
    {
        $this->logger->info('📋 Analyse basique de la note médicale');

        $noteL = strtolower($noteMedicale);
        
        // Détection de mots-clés pour suggérer une durée
        $dureeJours = 7; // Par défaut
        
        if (preg_match('/(\d+)\s*(jour|jours|day|days)/i', $noteMedicale, $matches)) {
            $dureeJours = (int)$matches[1];
        } elseif (preg_match('/(\d+)\s*(semaine|semaines|week|weeks)/i', $noteMedicale, $matches)) {
            $dureeJours = (int)$matches[1] * 7;
        } elseif (preg_match('/(\d+)\s*(mois|month|months)/i', $noteMedicale, $matches)) {
            $dureeJours = (int)$matches[1] * 30;
        }
        
        // Détection de la fréquence
        $frequence = '2 à 3 fois par jour';
        if (strpos($noteL, '3 fois') !== false || strpos($noteL, 'trois fois') !== false) {
            $frequence = '3 fois par jour (matin, midi, soir)';
        } elseif (strpos($noteL, '2 fois') !== false || strpos($noteL, 'deux fois') !== false) {
            $frequence = '2 fois par jour (matin et soir)';
        } elseif (strpos($noteL, '1 fois') !== false || strpos($noteL, 'une fois') !== false) {
            $frequence = '1 fois par jour';
        } elseif (strpos($noteL, 'toutes les') !== false) {
            if (preg_match('/toutes les (\d+) heures/i', $noteMedicale, $matches)) {
                $frequence = 'Toutes les ' . $matches[1] . ' heures';
            }
        }

        // Détection du dosage
        $dosage = 'Selon prescription médicale';
        if ($produit) {
            $nomProduit = strtolower($produit->getNom());
            if (strpos($nomProduit, 'paracetamol') !== false || strpos($nomProduit, 'doliprane') !== false) {
                $dosage = '500mg par prise (1 comprimé)';
            } elseif (strpos($nomProduit, 'ibuprofene') !== false) {
                $dosage = '400mg par prise (1 comprimé)';
            }
        }
        
        // Recherche de dosage dans la note
        if (preg_match('/(\d+)\s*(mg|ml|g|comprimé|comprimés|gélule|gélules)/i', $noteMedicale, $matches)) {
            $dosage = $matches[1] . $matches[2] . ' par prise';
        }

        return [
            'success' => true,
            'dosage' => $dosage,
            'frequence' => $frequence,
            'duree_jours' => $dureeJours,
            'explication' => 'Suggestions basées sur l\'analyse automatique de la note médicale',
            'precautions' => 'Vérifier avec un professionnel de santé',
            'source' => 'Analyse basique'
        ];
    }
}
