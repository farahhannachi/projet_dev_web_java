<?php

namespace App\Service;

use Symfony\Component\HttpClient\HttpClient;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class OllamaService
{
    private HttpClientInterface $httpClient;
    private string $baseUrl;
    private string $model;
    private LoggerInterface $logger;

    public function __construct(
        string $ollamaUrl = 'http://localhost:11434',
        string $model = 'mistral',
        LoggerInterface $logger = null
    ) {
        $this->httpClient = HttpClient::create();
        $this->baseUrl = $ollamaUrl;
        $this->model = $model;
        $this->logger = $logger;
    }

    /**
     * Check if Ollama is available
     */
    public function isAvailable(): bool
    {
        try {
            $response = $this->httpClient->request('GET', $this->baseUrl . '/api/tags', [
                'timeout' => 5,
            ]);
            return $response->getStatusCode() === 200;
        } catch (\Exception $e) {
            if ($this->logger) {
                $this->logger->warning('Ollama not available: ' . $e->getMessage());
            }
            return false;
        }
    }

    /**
     * Get available models
     */
    public function getAvailableModels(): array
    {
        try {
            $response = $this->httpClient->request('GET', $this->baseUrl . '/api/tags', [
                'timeout' => 5,
            ]);
            $data = $response->toArray();
            
            if (isset($data['models'])) {
                return array_map(fn($m) => $m['name'], $data['models']);
            }
            
            return [];
        } catch (\Exception $e) {
            if ($this->logger) {
                $this->logger->error('Error getting models: ' . $e->getMessage());
            }
            return [];
        }
    }

    /**
     * Generate a response using Ollama
     */
    public function generateResponse(string $message, array $conversationHistory = []): string
    {
        try {
            // Build context from conversation history
            $context = $this->buildContext($conversationHistory);
            
            $prompt = $context . "\nUser: " . $message . "\nAssistant:";

            $response = $this->httpClient->request(
                'POST',
                $this->baseUrl . '/api/generate',
                [
                    'json' => [
                        'model' => $this->model,
                        'prompt' => $prompt,
                        'stream' => false,
                        'temperature' => 0.7,
                        'top_p' => 0.9,
                        'top_k' => 40,
                    ],
                    'timeout' => 120, // Longer timeout for generation
                ]
            );

            $data = $response->toArray();
            return trim($data['response'] ?? 'Je n\'ai pas pu générer une réponse.');
        } catch (\Exception $e) {
            if ($this->logger) {
                $this->logger->error('Error generating response: ' . $e->getMessage());
            }
            return 'Désolé, une erreur s\'est produite. Veuillez réessayer.';
        }
    }

    /**
     * Build context from conversation history
     */
    private function buildContext(array $history): string
    {
        $context = 'You are a friendly, energetic pharmacy assistant AI named "CURAVITA Bot" for CURAVITA pharmacy. ';
        $context .= 'Your personality: You\'re helpful, witty, conversational, and genuinely interested in helping customers. ';
        $context .= 'You use casual French (tu/vous mix), occasional emojis, and natural language. ';
        $context .= 'You\'re NOT a generic robot - you have personality and warmth! ';
        $context .= "\n\n";
        $context .= 'IMPORTANT RULES: ';
        $context .= '1. ONLY answer about what the user asks - don\'t go off-topic or volunteer extra information ';
        $context .= '2. If asked about something not related to CURAVITA, politely redirect to pharmacy topics ';
        $context .= '3. Be concise and natural - short responses are better than long ones ';
        $context .= '4. Use French for all responses ';
        $context .= '5. You can help with: products, orders, prescriptions, treatments, loyalty program, account help, tracking ';
        $context .= '6. If you don\'t know something specific, say "Je sais pas trop sur ce point!" instead of making it up ';
        $context .= "\n\n";
        $context .= 'Tone: Friendly, casual, helpful. Like chatting with a knowledgeable friend at the pharmacy. ';
        $context .= 'Respond naturally and conversationally!';

        // Add recent conversation context (last 3 exchanges)
        $recentHistory = array_slice($history, -6);
        
        if (!empty($recentHistory)) {
            $context .= "\n\nConversation so far:\n";
            foreach ($recentHistory as $msg) {
                if ($msg['role'] === 'user') {
                    $context .= "Client: " . $msg['content'] . "\n";
                } else {
                    $context .= "Bot: " . $msg['content'] . "\n";
                }
            }
        }

        return $context;
    }

    /**
     * Get introduction message for new users
     */
    public function getIntroductionMessage(): string
    {
        $intro = "Bienvenue sur CURAVITA! 👋\n\n";
        $intro .= "Je vais te présenter rapidement ce qu'on peut faire ensemble:\n\n";
        $intro .= "📦 **Produits** - Trouve les médicaments, suppléments et produits de soin que tu cherches\n";
        $intro .= "🛒 **Commandes** - Passe une commande, suivis ta livraison, gère tes prescriptions\n";
        $intro .= "💊 **Conseils** - Je peux t'expliquer les traitements, les dosages, les interactions\n";
        $intro .= "⭐ **Loyauté** - Gagne des points à chaque achat et accède à des avantages exclusifs\n";
        $intro .= "📞 **Aide** - Je suis là pour répondre à toutes tes questions!\n\n";
        $intro .= "Pose-moi n'importe quelle question sur comment utiliser le site ou sur un produit. Je suis là pour t'aider! 😊";
        
        return $intro;
    }

    /**
     * Set the model to use
     */
    public function setModel(string $model): self
    {
        $this->model = $model;
        return $this;
    }

    /**
     * Get current model
     */
    public function getModel(): string
    {
        return $this->model;
    }
}
