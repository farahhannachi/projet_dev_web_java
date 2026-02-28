<?php

namespace App\Service;

use App\Entity\Produit;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class IADosageService
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
     * Suggère le dosage, la fréquence et la durée en fonction de la note médicale
     */
    public function suggererDosage(
        Produit $produit,
        string $noteMedicale
    ): array {
        // Essayer OpenAI en priorité
        if (!empty($this->apiKey) && strpos($this->apiKey, 'sk-') === 0) {
            $this->logger->info('🤖 Utilisation de OpenAI pour suggestion dosage');
            $resultat = $this->suggererAvecOpenAI($produit, $noteMedicale);
            if ($resultat !== null) {
                return $resultat;
            }
        }

        // Fallback : Suggestions par défaut
        $this->logger->warning('⚠️ IA non disponible, utilisation des suggestions par défaut');
        return $this->suggestionsParDefaut($produit, $noteMedicale);
    }

    /**
     * Suggestion avec OpenAI
     */
    private function suggererAvecOpenAI(Produit $produit, string $noteMedicale): ?array
    {
        try {
            $this->logger->info('🤖 === SUGGESTION DOSAGE AVEC OPENAI ===');

            $prompt = $this->construirePrompt($produit, $noteMedicale);

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
                            'content' => 'Tu es un pharmacien expert. Tu dois suggérer un dosage, une fréquence et une durée de traitement appropriés. Réponds UNIQUEMENT en JSON.'
                        ],
                        [
                            'role' => 'user',
                            'content' => $prompt
                        ]
                    ],
                    'temperature' => 0.3,
                    'max_tokens' => 500
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
                    $this->logger->info('✅ Suggestion dosage générée', $result);
                    return [
                        'dosage' => $result['dosage'],
                        'frequence' => $result['frequence'],
                        'duree_jours' => $result['duree_jours'],
                        'explication' => $result['explication'] ?? '',
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
    private function construirePrompt(Produit $produit, string $noteMedicale): string
    {
        return <<<PROMPT
Tu es un pharmacien expert. Analyse cette situation et suggère un dosage approprié.

**MÉDICAMENT:**
- Nom: {$produit->getNom()}
- Description: {$produit->getDescription()}

**NOTE MÉDICALE DU PATIENT:**
{$noteMedicale}

**INSTRUCTIONS:**
Suggère un dosage, une fréquence et une durée de traitement appropriés en fonction:
- Du médicament prescrit
- Des symptômes et antécédents du patient
- Des recommandations médicales standards

**RÉPONDS UNIQUEMENT EN JSON avec cette structure exacte:**
{
    "dosage": "exemple: 500mg par prise",
    "frequence": "exemple: 3 fois par jour (matin, midi, soir)",
    "duree_jours": 7,
    "explication": "Courte explication de la posologie recommandée"
}

**RÈGLES:**
- Le dosage doit être précis (mg, ml, comprimés, etc.)
- La fréquence doit indiquer quand prendre le médicament
- La durée doit être en jours (nombre entier)
- L'explication doit être courte et claire
PROMPT;
    }

    /**
     * Suggestions par défaut (fallback)
     */
    private function suggestionsParDefaut(Produit $produit, string $noteMedicale): array
    {
        $this->logger->info('📋 Génération de suggestions par défaut');

        $nomProduit = strtolower($produit->getNom());
        $description = strtolower($produit->getDescription() ?? '');
        $noteL = strtolower($noteMedicale);

        // Paracétamol / Doliprane / Panadol
        if (strpos($nomProduit, 'paracetamol') !== false || 
            strpos($nomProduit, 'doliprane') !== false || 
            strpos($nomProduit, 'panadol') !== false ||
            strpos($description, 'paracetamol') !== false) {
            return [
                'dosage' => '500mg par prise',
                'frequence' => '3 fois par jour (toutes les 6-8 heures)',
                'duree_jours' => 5,
                'explication' => 'Posologie standard pour le paracétamol chez l\'adulte. Maximum 3g/jour.',
                'source' => 'Suggestions par défaut'
            ];
        }

        // Ibuprofène / Advil
        if (strpos($nomProduit, 'ibuprofene') !== false || 
            strpos($nomProduit, 'ibuprofen') !== false ||
            strpos($nomProduit, 'advil') !== false ||
            strpos($description, 'ibuprofene') !== false) {
            return [
                'dosage' => '400mg par prise',
                'frequence' => '2 à 3 fois par jour (pendant les repas)',
                'duree_jours' => 5,
                'explication' => 'Posologie standard pour l\'ibuprofène, à prendre pendant les repas pour éviter les troubles gastriques.',
                'source' => 'Suggestions par défaut'
            ];
        }

        // Aspirine
        if (strpos($nomProduit, 'aspirine') !== false || 
            strpos($nomProduit, 'aspirin') !== false ||
            strpos($description, 'aspirine') !== false) {
            return [
                'dosage' => '500mg par prise',
                'frequence' => '2 à 3 fois par jour (pendant les repas)',
                'duree_jours' => 5,
                'explication' => 'Posologie standard pour l\'aspirine, à prendre pendant les repas.',
                'source' => 'Suggestions par défaut'
            ];
        }

        // Antibiotiques (Amoxicilline)
        if (strpos($nomProduit, 'amoxicilline') !== false || 
            strpos($nomProduit, 'amoxicillin') !== false ||
            strpos($description, 'amoxicilline') !== false) {
            return [
                'dosage' => '500mg par prise',
                'frequence' => '3 fois par jour (toutes les 8 heures)',
                'duree_jours' => 7,
                'explication' => 'Traitement antibiotique standard. Important de terminer le traitement complet.',
                'source' => 'Suggestions par défaut'
            ];
        }

        // Antihistaminiques
        if (strpos($nomProduit, 'cetirizine') !== false || 
            strpos($nomProduit, 'loratadine') !== false ||
            strpos($description, 'allergie') !== false ||
            strpos($description, 'antihistaminique') !== false) {
            return [
                'dosage' => '10mg par prise',
                'frequence' => '1 fois par jour (le soir)',
                'duree_jours' => 7,
                'explication' => 'Traitement antiallergique standard.',
                'source' => 'Suggestions par défaut'
            ];
        }

        // Suggestion générique basée sur l'analyse de la note médicale
        $dosageGenerique = '500mg par prise';
        $frequenceGenerique = '2 à 3 fois par jour';
        $dureeGenerique = 7;
        
        // Ajuster selon les symptômes mentionnés
        if (strpos($noteL, 'douleur') !== false || strpos($noteL, 'mal') !== false) {
            $dosageGenerique = '400mg par prise';
            $frequenceGenerique = '3 fois par jour (toutes les 8 heures)';
            $dureeGenerique = 5;
        }
        
        if (strpos($noteL, 'fievre') !== false || strpos($noteL, 'fièvre') !== false) {
            $dosageGenerique = '500mg par prise';
            $frequenceGenerique = '3 fois par jour si nécessaire';
            $dureeGenerique = 3;
        }

        return [
            'dosage' => $dosageGenerique,
            'frequence' => $frequenceGenerique,
            'duree_jours' => $dureeGenerique,
            'explication' => 'Posologie suggérée basée sur les symptômes. À adapter selon les recommandations du médecin.',
            'source' => 'Suggestions par défaut'
        ];
    }
}
