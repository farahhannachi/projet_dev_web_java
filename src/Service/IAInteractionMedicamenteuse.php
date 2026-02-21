<?php

namespace App\Service;

use App\Entity\Produit;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class IAInteractionMedicamenteuse
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
     * Analyse les interactions médicamenteuses et contre-indications
     */
    public function analyserInteractions(
        Produit $produit,
        string $symptomes,
        string $antecedentsMedicaux
    ): array {
        // PRIORITÉ 1 : Utiliser Hugging Face IA (gratuit)
        if (!empty($this->apiKey) && strpos($this->apiKey, 'hf_') === 0) {
            $this->logger->info('🤖 Utilisation de Hugging Face IA en priorité');
            $resultat = $this->analyseAvecHuggingFace($produit, $symptomes, $antecedentsMedicaux);
            // Si l'IA a réussi, retourner son résultat
            if ($resultat !== null) {
                return $resultat;
            }
        }
        
        // PRIORITÉ 2 : Utiliser OpenAI si clé disponible
        if (!empty($this->apiKey) && strpos($this->apiKey, 'sk-') === 0) {
            $this->logger->info('🤖 Utilisation de OpenAI GPT en priorité');
            $resultat = $this->analyseAvecOpenAI($produit, $symptomes, $antecedentsMedicaux);
            if ($resultat !== null) {
                return $resultat;
            }
        }

        // FALLBACK : Analyse locale seulement si IA échoue
        $this->logger->warning('⚠️ IA non disponible, utilisation de l\'analyse locale');
        return $this->analyseLocale($produit, $symptomes, $antecedentsMedicaux);
    }

    /**
     * Analyse avec Hugging Face (IA gratuite)
     */
    private function analyseAvecHuggingFace(Produit $produit, string $symptomes, string $antecedentsMedicaux): ?array
    {
        try {
            $this->logger->info('🤖 === ANALYSE AVEC HUGGING FACE IA ===');
            $this->logger->info('Produit: ' . $produit->getNom());
            $this->logger->info('Symptômes: ' . substr($symptomes, 0, 100));
            $this->logger->info('Antécédents: ' . substr($antecedentsMedicaux, 0, 100));

            $prompt = $this->construirePromptSimple($produit, $symptomes, $antecedentsMedicaux);
            
            $this->logger->info('📝 Prompt envoyé à Hugging Face', ['longueur' => strlen($prompt)]);

            // Utiliser un modèle plus stable et récent
            $response = $this->httpClient->request('POST', 'https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'inputs' => $prompt,
                    'parameters' => [
                        'max_new_tokens' => 800,  // Augmenté pour plus de détails
                        'temperature' => 0.2,      // Réduit pour plus de précision
                        'top_p' => 0.9,
                        'do_sample' => true,
                        'return_full_text' => false
                    ],
                    'options' => [
                        'wait_for_model' => true,  // Attendre que le modèle soit chargé
                        'use_cache' => false       // Ne pas utiliser le cache
                    ]
                ],
                'timeout' => 35  // Augmenté pour laisser le temps au modèle
            ]);

            $statusCode = $response->getStatusCode();
            $this->logger->info('📥 Réponse Hugging Face - Status: ' . $statusCode);

            if ($statusCode !== 200) {
                $this->logger->error('❌ Erreur HTTP Hugging Face: ' . $statusCode);
                return null;
            }

            $data = $response->toArray();
            $this->logger->info('📦 Données reçues', ['structure' => array_keys($data)]);
            
            if (isset($data['error'])) {
                $this->logger->error('❌ Erreur API Hugging Face', ['error' => $data['error']]);
                return null;
            }
            
            if (isset($data[0]['generated_text'])) {
                $texte = $data[0]['generated_text'];
                $this->logger->info('✅ Texte généré par IA', ['texte' => substr($texte, 0, 300)]);
                
                $resultat = $this->parserReponseIA($texte, $produit, $symptomes, $antecedentsMedicaux);
                
                $this->logger->info('🎯 Résultat final de l\'analyse IA', [
                    'dangereux' => $resultat['dangereux'],
                    'niveau_risque' => $resultat['niveau_risque'],
                    'nombre_raisons' => count($resultat['raisons'])
                ]);
                
                return $resultat;
            }

            $this->logger->warning('⚠️ Format de réponse inattendu de Hugging Face');
            return null;

        } catch (\Symfony\Contracts\HttpClient\Exception\TimeoutException $e) {
            $this->logger->error('⏱️ Timeout Hugging Face (35s dépassé)', ['error' => $e->getMessage()]);
            return null;
        } catch (\Exception $e) {
            $this->logger->error('❌ Exception Hugging Face', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ]);
            return null;
        }
    }

    /**
     * Analyse avec OpenAI (si clé disponible)
     */
    private function analyseAvecOpenAI(Produit $produit, string $symptomes, string $antecedentsMedicaux): array
    {
        try {
            $this->logger->info('🔍 Analyse des interactions médicamenteuses', [
                'produit' => $produit->getNom(),
                'symptomes' => substr($symptomes, 0, 100),
                'antecedents' => substr($antecedentsMedicaux, 0, 100)
            ]);

            $prompt = $this->construirePrompt($produit, $symptomes, $antecedentsMedicaux);

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
                            'content' => 'Tu es un pharmacien expert spécialisé dans la détection des interactions médicamenteuses et des contre-indications. Tu dois analyser si un médicament est approprié pour un patient en fonction de ses symptômes et antécédents médicaux. Réponds UNIQUEMENT en JSON.'
                        ],
                        [
                            'role' => 'user',
                            'content' => $prompt
                        ]
                    ],
                    'temperature' => 0.3,
                    'max_tokens' => 500
                ]
            ]);

            $data = $response->toArray();
            $content = $data['choices'][0]['message']['content'] ?? '';

            // Parser la réponse JSON
            $result = json_decode($content, true);

            if (!$result) {
                $this->logger->error('Erreur parsing JSON de l\'IA', ['content' => $content]);
                return $this->getDefaultSafeResponse();
            }

            $this->logger->info('✓ Analyse terminée', [
                'dangereux' => $result['dangereux'] ?? false,
                'niveau_risque' => $result['niveau_risque'] ?? 'inconnu'
            ]);

            return $result;

        } catch (\Exception $e) {
            $this->logger->error('Erreur lors de l\'analyse des interactions', [
                'error' => $e->getMessage()
            ]);
            // En cas d'erreur, utiliser l'analyse locale
            return $this->analyseLocale($produit, $symptomes, $antecedentsMedicaux);
        }
    }

    /**
     * Construit un prompt simple pour Hugging Face
     */
    private function construirePromptSimple(Produit $produit, string $symptomes, string $antecedentsMedicaux): string
    {
        return <<<PROMPT
Tu es un pharmacien expert en interactions médicamenteuses. Analyse cette situation médicale avec attention.

MÉDICAMENT DEMANDÉ: {$produit->getNom()}
DESCRIPTION: {$produit->getDescription()}

PATIENT:
- Symptômes actuels: {$symptomes}
- Antécédents médicaux: {$antecedentsMedicaux}

ANALYSE DEMANDÉE:
Détecte TOUTES les interactions dangereuses, contre-indications, allergies ou risques potentiels.

INSTRUCTIONS:
1. Analyse les antécédents pour détecter:
   - Allergies médicamenteuses (paracétamol, ibuprofène, aspirine, etc.)
   - Conditions médicales (ulcère, grossesse, diabète, hypertension, etc.)
   - Traitements en cours qui peuvent interagir

2. Évalue la compatibilité du médicament avec:
   - Les symptômes décrits
   - Les antécédents médicaux
   - Les conditions particulières (grossesse, allaitement, etc.)

3. Réponds en français avec ce format EXACT:
   - Si DANGEREUX: Commence par "DANGEREUX" puis explique pourquoi en détail
   - Si CRITIQUE (allergie, grossesse, etc.): Utilise le mot "CRITIQUE" dans ta réponse
   - Si ÉLEVÉ (contre-indication importante): Utilise le mot "ÉLEVÉ" ou "IMPORTANT"
   - Si SAFE: Commence par "SAFE" puis donne des recommandations

EXEMPLES:
- "DANGEREUX - CRITIQUE: Allergie au paracétamol détectée. Ce médicament contient du paracétamol. Risque de choc anaphylactique."
- "DANGEREUX - CRITIQUE: Grossesse détectée. L'ibuprofène est contre-indiqué pendant la grossesse. Risque de malformations."
- "DANGEREUX - ÉLEVÉ: Ulcère gastrique avec anti-inflammatoire. Risque d'hémorragie digestive."
- "SAFE: Aucune interaction majeure détectée. Respectez la posologie."

RÉPONDS MAINTENANT:
PROMPT;
    }

    /**
     * Parse la réponse de l'IA pour extraire les informations
     */
    private function parserReponseIA(string $texte, Produit $produit, string $symptomes, string $antecedentsMedicaux): array
    {
        $this->logger->info('🤖 Parsing réponse IA', ['texte' => substr($texte, 0, 200)]);
        
        $texteLower = $this->normaliserTexte($texte);
        
        // Détection de danger
        $estDangereux = (
            strpos($texteLower, 'dangereux') !== false ||
            strpos($texteLower, 'contre-indication') !== false ||
            strpos($texteLower, 'allergie') !== false ||
            strpos($texteLower, 'allergique') !== false ||
            strpos($texteLower, 'risque') !== false ||
            strpos($texteLower, 'ne pas prendre') !== false ||
            strpos($texteLower, 'interdit') !== false ||
            strpos($texteLower, 'deconseille') !== false
        );

        // Détection niveau de risque avec plus de précision
        $niveauRisque = 'faible';
        if (strpos($texteLower, 'critique') !== false || 
            strpos($texteLower, 'grave') !== false ||
            strpos($texteLower, 'choc anaphylactique') !== false ||
            strpos($texteLower, 'urgence') !== false) {
            $niveauRisque = 'critique';
        } elseif (strpos($texteLower, 'eleve') !== false || 
                  strpos($texteLower, 'important') !== false ||
                  strpos($texteLower, 'serieux') !== false) {
            $niveauRisque = 'élevé';
        } elseif ($estDangereux) {
            $niveauRisque = 'moyen';
        }

        // Extraire les raisons de manière intelligente
        $raisons = [];
        
        // Détection d'allergie
        if (strpos($texteLower, 'allergie') !== false || strpos($texteLower, 'allergique') !== false) {
            if (strpos($texteLower, 'paracetamol') !== false) {
                $raisons[] = 'Allergie au paracétamol détectée dans les antécédents';
            } elseif (strpos($texteLower, 'ibuprofene') !== false) {
                $raisons[] = 'Allergie à l\'ibuprofène détectée';
            } else {
                $raisons[] = 'Allergie médicamenteuse détectée';
            }
            $raisons[] = 'Risque de réaction allergique grave (choc anaphylactique)';
        }
        
        // Détection grossesse
        if (strpos($texteLower, 'grossesse') !== false || strpos($texteLower, 'enceinte') !== false) {
            $raisons[] = 'Grossesse détectée dans les antécédents';
            if (strpos($texteLower, 'ibuprofene') !== false || strpos($texteLower, 'anti-inflammatoire') !== false) {
                $raisons[] = 'Les anti-inflammatoires sont contre-indiqués pendant la grossesse';
                $raisons[] = 'Risque de malformations fœtales et complications';
            }
        }
        
        // Détection ulcère
        if (strpos($texteLower, 'ulcere') !== false || strpos($texteLower, 'estomac') !== false) {
            $raisons[] = 'Problèmes gastriques détectés (ulcère/estomac)';
            if (strpos($texteLower, 'ibuprofene') !== false || strpos($texteLower, 'anti-inflammatoire') !== false) {
                $raisons[] = 'Les anti-inflammatoires peuvent aggraver les ulcères';
                $raisons[] = 'Risque d\'hémorragie digestive';
            }
        }
        
        // Détection diabète
        if (strpos($texteLower, 'diabete') !== false) {
            $raisons[] = 'Diabète détecté dans les antécédents';
            if (strpos($texteLower, 'fievre') !== false) {
                $raisons[] = 'La fièvre peut affecter la glycémie';
            }
        }

        // Si aucune raison spécifique mais dangereux, extraire du texte IA
        if (empty($raisons) && $estDangereux) {
            // Extraire la première phrase après "DANGEREUX"
            $pos = strpos($texte, 'DANGEREUX');
            if ($pos !== false) {
                $extrait = substr($texte, $pos, 300);
                $phrases = preg_split('/[.!?]/', $extrait, 3);
                if (isset($phrases[1])) {
                    $raisons[] = trim($phrases[1]);
                }
            } else {
                $raisons[] = trim(substr($texte, 0, 200));
            }
        }

        $peutContinuer = !($niveauRisque === 'critique');

        $resultat = [
            'dangereux' => $estDangereux,
            'niveau_risque' => $niveauRisque,
            'raisons' => $raisons,
            'recommandation' => $this->genererRecommandation($niveauRisque, $estDangereux),
            'peut_continuer' => $peutContinuer,
            'source' => 'Hugging Face IA (Mistral-7B)'
        ];
        
        $this->logger->info('✅ Parsing terminé', $resultat);
        
        return $resultat;
    }

    /**
     * Génère une recommandation selon le niveau de risque
     */
    private function genererRecommandation(string $niveauRisque, bool $dangereux): string
    {
        if ($niveauRisque === 'critique') {
            return 'NE PAS PRENDRE CE MÉDICAMENT. Consultez immédiatement un médecin ou un pharmacien.';
        }
        
        if ($niveauRisque === 'élevé') {
            return 'Consultez un pharmacien avant de prendre ce médicament. Des alternatives plus sûres existent.';
        }
        
        if ($dangereux || $niveauRisque === 'moyen') {
            return 'Prudence recommandée. Surveillez l\'apparition d\'effets indésirables et consultez si nécessaire.';
        }
        
        return 'Aucune interaction majeure détectée. Respectez la posologie. En cas de doute, consultez un pharmacien.';
    }

    /**
     * Normalise le texte en minuscules et sans accents
     */
    private function normaliserTexte(string $texte): string
    {
        $texte = strtolower($texte);
        // Remplacer les caractères accentués
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
     * Analyse locale sans IA (fallback)
     */
    private function analyseLocale(Produit $produit, string $symptomes, string $antecedentsMedicaux): array
    {
        $this->logger->warning('⚠️ === FALLBACK: ANALYSE LOCALE (SANS IA) ===');
        $this->logger->info('L\'IA Hugging Face n\'a pas pu être utilisée, utilisation des règles programmées');
        
        // Normaliser en enlevant les accents
        $produitNom = $this->normaliserTexte($produit->getNom());
        $antecedentsLower = $this->normaliserTexte($antecedentsMedicaux);
        $symptomesLower = $this->normaliserTexte($symptomes);
        
        $this->logger->info('Données analysées', [
            'produit' => $produitNom,
            'antecedents' => substr($antecedentsLower, 0, 100),
            'symptomes' => substr($symptomesLower, 0, 100)
        ]);
        
        // Détection d'allergies au paracétamol
        $hasParacetamol = (
            strpos($produitNom, 'panadol') !== false || 
            strpos($produitNom, 'paracetamol') !== false ||
            strpos($produitNom, 'doliprane') !== false
        );
        
        $mentionneParacetamol = strpos($antecedentsLower, 'paracetamol') !== false;
        $mentionneAllergie = strpos($antecedentsLower, 'allergie') !== false || strpos($antecedentsLower, 'allergique') !== false;
        
        $hasAllergieParacetamol = $mentionneAllergie && $mentionneParacetamol;
        
        $this->logger->info('Détection allergie', [
            'hasParacetamol' => $hasParacetamol,
            'mentionneParacetamol' => $mentionneParacetamol,
            'mentionneAllergie' => $mentionneAllergie,
            'hasAllergieParacetamol' => $hasAllergieParacetamol
        ]);
        
        if ($hasParacetamol && $hasAllergieParacetamol) {
            $this->logger->warning('⚠️ ALLERGIE DÉTECTÉE - Paracétamol');
            return [
                'dangereux' => true,
                'niveau_risque' => 'critique',
                'raisons' => [
                    'Allergie au paracétamol détectée dans les antécédents',
                    'Le produit sélectionné contient du paracétamol',
                    'Risque de réaction allergique grave (choc anaphylactique)'
                ],
                'recommandation' => 'NE PAS PRENDRE CE MÉDICAMENT. Consultez immédiatement un médecin ou un pharmacien pour une alternative.',
                'peut_continuer' => false,
                'source' => 'Analyse locale (règles programmées)'
            ];
        }
        
        // Détection ibuprofène + ulcère
        $hasIbuprofene = (
            strpos($produitNom, 'ibuprofene') !== false || 
            strpos($produitNom, 'ibuprofen') !== false ||
            strpos($produitNom, 'advil') !== false ||
            strpos($produitNom, 'nurofen') !== false
        );
        
        if (
            $hasIbuprofene &&
            (strpos($antecedentsLower, 'ulcere') !== false || strpos($antecedentsLower, 'estomac') !== false)
        ) {
            $this->logger->warning('⚠️ INTERACTION DÉTECTÉE - Ibuprofène + Ulcère');
            return [
                'dangereux' => true,
                'niveau_risque' => 'élevé',
                'raisons' => [
                    'Problèmes gastriques détectés (ulcère/estomac)',
                    'L\'ibuprofène est un anti-inflammatoire qui peut aggraver les ulcères',
                    'Risque d\'hémorragie digestive'
                ],
                'recommandation' => 'Consultez un pharmacien avant de prendre ce médicament. Une alternative plus sûre existe probablement.',
                'peut_continuer' => true,
                'source' => 'Analyse locale (règles programmées)'
            ];
        }
        
        // Détection grossesse + anti-inflammatoires
        $hasGrossesse = (
            strpos($antecedentsLower, 'enceinte') !== false || 
            strpos($antecedentsLower, 'grossesse') !== false
        );
        
        if ($hasGrossesse && $hasIbuprofene) {
            $this->logger->warning('⚠️ CONTRE-INDICATION CRITIQUE - Grossesse + Ibuprofène');
            return [
                'dangereux' => true,
                'niveau_risque' => 'critique',
                'raisons' => [
                    'Grossesse détectée dans les antécédents',
                    'L\'ibuprofène est contre-indiqué pendant la grossesse',
                    'Risque de malformations fœtales et complications'
                ],
                'recommandation' => 'NE PAS PRENDRE CE MÉDICAMENT. Consultez immédiatement votre médecin ou sage-femme pour un traitement adapté à la grossesse.',
                'peut_continuer' => false,
                'source' => 'Analyse locale (règles programmées)'
            ];
        }
        
        // Détection diabète + certains médicaments
        if (
            (strpos($antecedentsLower, 'diabète') !== false || strpos($antecedentsLower, 'diabete') !== false) &&
            (strpos($symptomesLower, 'fièvre') !== false || strpos($symptomesLower, 'fievre') !== false)
        ) {
            return [
                'dangereux' => false,
                'niveau_risque' => 'moyen',
                'raisons' => [
                    'Diabète détecté dans les antécédents',
                    'La fièvre peut affecter la glycémie'
                ],
                'recommandation' => 'Surveillez votre glycémie régulièrement pendant le traitement. Consultez votre médecin si la fièvre persiste plus de 3 jours.',
                'peut_continuer' => true,
                'source' => 'Analyse locale (règles programmées)'
            ];
        }
        
        // Aucun problème détecté
        return [
            'dangereux' => false,
            'niveau_risque' => 'faible',
            'raisons' => [],
            'recommandation' => 'Aucune interaction majeure détectée. Respectez la posologie indiquée. En cas de doute, consultez un pharmacien.',
            'peut_continuer' => true,
            'source' => 'Analyse locale (règles programmées)'
        ];
    }

    private function construirePrompt(Produit $produit, string $symptomes, string $antecedentsMedicaux): string
    {
        return <<<PROMPT
Analyse si le médicament suivant est approprié pour ce patient :

**MÉDICAMENT:**
- Nom: {$produit->getNom()}
- Description: {$produit->getDescription()}

**PATIENT:**
- Symptômes: {$symptomes}
- Antécédents médicaux: {$antecedentsMedicaux}

**ANALYSE DEMANDÉE:**
Détermine s'il existe des contre-indications, interactions dangereuses ou risques potentiels.

**RÉPONDS UNIQUEMENT EN JSON avec cette structure exacte:**
{
    "dangereux": true/false,
    "niveau_risque": "faible/moyen/élevé/critique",
    "raisons": ["raison 1", "raison 2"],
    "recommandation": "texte de recommandation",
    "peut_continuer": true/false
}

**CRITÈRES:**
- dangereux: true si contre-indication majeure ou interaction dangereuse
- niveau_risque: évalue la gravité (faible/moyen/élevé/critique)
- raisons: liste des problèmes détectés
- recommandation: conseil pour le patient
- peut_continuer: false si le patient doit absolument consulter un médecin avant
PROMPT;
    }

    private function getDefaultSafeResponse(): array
    {
        return [
            'dangereux' => false,
            'niveau_risque' => 'faible',
            'raisons' => [],
            'recommandation' => 'Aucune interaction détectée. Veuillez consulter un pharmacien pour confirmation.',
            'peut_continuer' => true
        ];
    }
}
