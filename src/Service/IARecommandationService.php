<?php

namespace App\Service;

use App\Repository\ProduitRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class IARecommandationService
{
    private ProduitRepository $produitRepository;
    private HttpClientInterface $httpClient;
    private ?string $openaiApiKey;
    private ?string $huggingfaceApiKey;

    public function __construct(
        ProduitRepository $produitRepository,
        HttpClientInterface $httpClient,
        ?string $openaiApiKey = null,
        ?string $huggingfaceApiKey = null
    ) {
        $this->produitRepository = $produitRepository;
        $this->httpClient = $httpClient;
        $this->openaiApiKey = $openaiApiKey;
        $this->huggingfaceApiKey = $huggingfaceApiKey;
    }

    /**
     * Analyse les symptômes avec l'IA et recommande des produits
     */
    public function analyserEtRecommander(string $symptomes, ?string $antecedents = null, int $limite = 5): array
    {
        // Récupérer tous les produits disponibles
        $produits = $this->produitRepository->createQueryBuilder('p')
            ->where('p.statut IN (:statuts)')
            ->setParameter('statuts', ['disponible', 'stock_critique'])
            ->getQuery()
            ->getResult();

        if (empty($produits)) {
            return [];
        }

        // Préparer la liste des produits pour l'IA
        $listeProduits = $this->preparerListeProduits($produits);

        // Analyser avec l'IA
        $analyse = $this->analyserAvecIA($symptomes, $antecedents, $listeProduits);

        // Mapper les recommandations de l'IA avec les produits réels
        return $this->mapperRecommandations($analyse, $produits, $limite);
    }

    /**
     * Prépare la liste des produits pour l'IA
     */
    private function preparerListeProduits(array $produits): string
    {
        $liste = [];
        foreach ($produits as $produit) {
            $liste[] = sprintf(
                "ID: %d | Nom: %s | Catégorie: %s | Description: %s",
                $produit->getId(),
                $produit->getNom(),
                $produit->getCategorie(),
                substr($produit->getDescription() ?? '', 0, 100)
            );
        }
        return implode("\n", $liste);
    }

    /**
     * Analyse les symptômes avec l'IA
     */
    private function analyserAvecIA(string $symptomes, ?string $antecedents, string $listeProduits): array
    {
        // Essayer d'abord avec OpenAI si disponible
        if (!empty($this->openaiApiKey)) {
            try {
                return $this->analyserAvecOpenAI($symptomes, $antecedents, $listeProduits);
            } catch (\Exception $e) {
                // Fallback vers l'analyse locale
            }
        }

        // Sinon, utiliser l'analyse locale intelligente
        return $this->analyserLocalement($symptomes, $antecedents, $listeProduits);
    }

    /**
     * Analyse avec OpenAI GPT
     */
    private function analyserAvecOpenAI(string $symptomes, ?string $antecedents, string $listeProduits): array
    {
        $prompt = $this->construirePrompt($symptomes, $antecedents, $listeProduits);

        $response = $this->httpClient->request('POST', 'https://api.openai.com/v1/chat/completions', [
            'headers' => [
                'Authorization' => 'Bearer ' . $this->openaiApiKey,
                'Content-Type' => 'application/json',
            ],
            'json' => [
                'model' => 'gpt-3.5-turbo',
                'messages' => [
                    [
                        'role' => 'system',
                        'content' => 'Tu es un assistant pharmaceutique expert. Analyse les symptômes et recommande les produits les plus appropriés.'
                    ],
                    [
                        'role' => 'user',
                        'content' => $prompt
                    ]
                ],
                'temperature' => 0.3,
                'max_tokens' => 500
            ],
            'timeout' => 10
        ]);

        $data = $response->toArray();
        $reponseIA = $data['choices'][0]['message']['content'] ?? '';

        return $this->extraireRecommandations($reponseIA);
    }

    /**
     * Construit le prompt pour l'IA
     */
    private function construirePrompt(string $symptomes, ?string $antecedents, string $listeProduits): string
    {
        $prompt = "SYMPTÔMES DU PATIENT:\n{$symptomes}\n\n";
        
        if (!empty($antecedents)) {
            $prompt .= "ANTÉCÉDENTS MÉDICAUX:\n{$antecedents}\n\n";
        }

        $prompt .= "PRODUITS DISPONIBLES:\n{$listeProduits}\n\n";
        $prompt .= "INSTRUCTIONS:\n";
        $prompt .= "Analyse les symptômes et recommande les 3 meilleurs produits de la liste.\n";
        $prompt .= "Pour chaque produit, fournis:\n";
        $prompt .= "- L'ID du produit\n";
        $prompt .= "- Un score de pertinence (0-100)\n";
        $prompt .= "- Une raison courte (max 50 mots)\n\n";
        $prompt .= "Format de réponse: ID|SCORE|RAISON (une ligne par produit)";

        return $prompt;
    }

    /**
     * Extrait les recommandations de la réponse de l'IA
     */
    private function extraireRecommandations(string $reponseIA): array
    {
        $recommandations = [];
        $lignes = explode("\n", $reponseIA);

        foreach ($lignes as $ligne) {
            if (preg_match('/(\d+)\|(\d+)\|(.+)/', $ligne, $matches)) {
                $recommandations[] = [
                    'id' => (int)$matches[1],
                    'score' => (int)$matches[2],
                    'raison' => trim($matches[3])
                ];
            }
        }

        return $recommandations;
    }

    /**
     * Analyse locale intelligente (sans API externe)
     */
    private function analyserLocalement(string $symptomes, ?string $antecedents, string $listeProduits): array
    {
        $symptomesNormalises = $this->normaliserTexte($symptomes);
        $recommandations = [];

        // Mots-clés avancés avec synonymes et variations
        $motsClés = [
            'tete' => ['tete', 'crane', 'migraine', 'cephalee', 'mal de tete', 'maux de tete'],
            'gorge' => ['gorge', 'angine', 'pharyngite', 'mal a la gorge', 'avaler', 'toux', 'enroue'],
            'respiration' => ['respiration', 'poumon', 'bronche', 'asthme', 'souffle', 'respirer', 'rhume', 'nez', 'bouche'],
            'fievre' => ['fievre', 'temperature', 'chaud', 'chaleur', 'transpiration', 'grippe', 'etat grippal'],
            'douleur' => ['douleur', 'mal', 'souffrance', 'douloureux', 'fait mal', 'courbature', 'muscle', 'articulation'],
            'ventre' => ['ventre', 'estomac', 'digestion', 'nausee', 'diarrhee', 'spasme', 'ballonnement'],
            'allergie' => ['allergie', 'rhinite', 'yeux', 'demangeaison', 'urticaire', 'eternuement'],
            'peau' => ['peau', 'brulure', 'irritation', 'rougeur', 'plaie', 'cicatrice', 'bouton'],
        ];

        // Analyser chaque catégorie
        $scoresCategories = [];
        foreach ($motsClés as $categorie => $synonymes) {
            $score = 0;
            foreach ($synonymes as $synonyme) {
                if (strpos($symptomesNormalises, $synonyme) !== false) {
                    $score += 10;
                }
            }
            if ($score > 0) {
                $scoresCategories[$categorie] = $score;
            }
        }

        // Si aucune catégorie détectée, chercher des mots-clés génériques
        if (empty($scoresCategories)) {
            $mots = explode(' ', $symptomesNormalises);
            foreach ($mots as $mot) {
                if (strlen($mot) > 3) {
                    foreach ($motsClés as $categorie => $synonymes) {
                        foreach ($synonymes as $synonyme) {
                            if (levenshtein($mot, $synonyme) <= 2) { // Distance de Levenshtein
                                $scoresCategories[$categorie] = ($scoresCategories[$categorie] ?? 0) + 5;
                            }
                        }
                    }
                }
            }
        }

        // Extraire les détails des produits de la liste
        preg_match_all('/ID: (\d+) \| Nom: ([^\|]+) \| Catégorie: ([^\|]+) \| Description: ([^\|]+)/', $listeProduits, $matches, PREG_SET_ORDER);

        foreach ($matches as $match) {
            $id = (int)$match[1];
            $nom = $this->normaliserTexte($match[2]);
            $categorie = $this->normaliserTexte($match[3]);

            $score = 0;
            $raisons = [];

            // Score basé sur la catégorie
            foreach ($scoresCategories as $cat => $scoreCategorie) {
                if (strpos($categorie, $cat) !== false || strpos($cat, $categorie) !== false) {
                    $score += $scoreCategorie * 3;
                    $raisons[] = "Adapté pour les problèmes de {$cat}";
                }
            }

            // Score basé sur le nom du produit
            foreach ($scoresCategories as $cat => $scoreCategorie) {
                if (strpos($nom, $cat) !== false) {
                    $score += $scoreCategorie * 2;
                }
            }

            // Score basé sur les mots des symptômes
            $motsSymptomes = explode(' ', $symptomesNormalises);
            foreach ($motsSymptomes as $mot) {
                if (strlen($mot) > 3) {
                    if (strpos($nom, $mot) !== false) {
                        $score += 8; // Increased from 5
                    }
                    if (strpos($categorie, $mot) !== false) {
                        $score += 5; // Increased from 3
                    }
                    if (strpos($this->normaliserTexte($match[4] ?? ''), $mot) !== false) {
                        $score += 3; // Match in description
                    }
                }
            }

            if ($score > 0) {
                $recommandations[] = [
                    'id' => $id,
                    'score' => min($score, 100), // Limiter à 100
                    'raison' => !empty($raisons) ? implode('. ', $raisons) : 'Recommandé pour vos symptômes'
                ];
            }
        }

        // Trier par score décroissant
        usort($recommandations, function($a, $b) {
            return $b['score'] <=> $a['score'];
        });

        return $recommandations;
    }

    /**
     * Normalise le texte
     */
    private function normaliserTexte(string $texte): string
    {
        $texte = mb_strtolower($texte, 'UTF-8');
        $accents = [
            'à' => 'a', 'á' => 'a', 'â' => 'a', 'ã' => 'a', 'ä' => 'a',
            'è' => 'e', 'é' => 'e', 'ê' => 'e', 'ë' => 'e',
            'ì' => 'i', 'í' => 'i', 'î' => 'i', 'ï' => 'i',
            'ò' => 'o', 'ó' => 'o', 'ô' => 'o', 'õ' => 'o', 'ö' => 'o',
            'ù' => 'u', 'ú' => 'u', 'û' => 'u', 'ü' => 'u',
            'ç' => 'c', 'ñ' => 'n'
        ];
        return strtr($texte, $accents);
    }

    /**
     * Mappe les recommandations de l'IA avec les produits réels
     */
    private function mapperRecommandations(array $analyse, array $produits, int $limite): array
    {
        $resultat = [];
        $produitsParId = [];

        // Indexer les produits par ID
        foreach ($produits as $produit) {
            $produitsParId[$produit->getId()] = $produit;
        }

        // Mapper les recommandations
        foreach ($analyse as $recommandation) {
            if (isset($produitsParId[$recommandation['id']])) {
                $produit = $produitsParId[$recommandation['id']];
                $resultat[] = [
                    'produit' => $produit,
                    'score' => $recommandation['score'],
                    'raison' => $recommandation['raison']
                ];
            }

            if (count($resultat) >= $limite) {
                break;
            }
        }

        return $resultat;
    }
}
