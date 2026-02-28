<?php

namespace App\Controller;

use App\Service\GroqLlmService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

class AssistantChatController extends AbstractController
{
    private const SYSTEM_PROMPT = <<<PROMPT
You are the CURAVITA Expert Pharmaceutical Assistant. Your goal is to help users with BOTH the CURAVITA platform AND general pharmaceutical/health inquiries.

CORE RULES:
1. RESPONSE DOMAIN: 
   - CURAVITA PLATFORM: Help with pharmacy management, stocks, prescriptions, and support tickets.
   - MEDICAL/HEALTH: Provide helpful pharmaceutical advice regarding common symptoms (pain/douleur, fever, minor ailments) and medication information.
2. MEDICAL DISCLAIMER: ALWAYS include a variation of "Important : Je suis un assistant IA. Consultez toujours un médecin ou demandez conseil à votre pharmacien en personne pour un diagnostic précis." especially when giving health advice.
3. TONE: Empathetic, professional, and precise.
4. LANGUAGE: Respond in the language used by the user.
5. HEALTH ADVICE: If a user mentions "douleur" (pain), ask for details (location, intensity) and suggest potential over-the-counter pharmaceutical approaches while strongly recommending professional consultation if the pain persists or is severe.
6. OFF-TOPIC: If the user asks about anything completely unrelated to health or CURAVITA, politely refocus on how you can assist with their health or the platform.

KNOWLEDGE BASE:
- CURAVITA: Advanced pharmacy management suite.
- SYMPTOM SUPPORT: Knowledgeable about general pharmaceutical advice for pain, allergies, first aid, and preventive care.
- STOCK & TICKETS: Real-time monitoring and intelligent support categorization.
PROMPT;

    public function __construct(private GroqLlmService $llm) {}

    #[Route('/api/assistant/chat', name: 'api_assistant_chat', methods: ['POST'])]
    public function handleChat(Request $request): JsonResponse
    {
        // Prevent any warning/notice from leaking into JSON output
        error_reporting(0);
        ini_set('display_errors', 0);
        
        try {
            $data = json_decode($request->getContent(), true);
            $userMessage = $data['message'] ?? '';

            if (empty(trim($userMessage))) {
                return new JsonResponse(['success' => false, 'response' => 'Message vide.'], 200);
            }

            $messages = [
                ['role' => 'system', 'content' => self::SYSTEM_PROMPT],
                ['role' => 'user', 'content' => $userMessage]
            ];

            $result = $this->llm->chat($messages, temperature: 0.5, maxTokens: 400);
            
            // Check for API-level errors returned as JSON
            if (isset($result['error'])) {
                error_log("Groq API Error: " . json_encode($result['error']));
                return new JsonResponse([
                    'success' => false, 
                    'response' => "Désolé, je rencontre une erreur avec mon moteur d'intelligence artificielle. Veuillez réessayer dans quelques instants."
                ], 200);
            }

            $aiResponse = $result['choices'][0]['message']['content'] ?? null;

            if (!$aiResponse) {
                return new JsonResponse([
                    'success' => false, 
                    'response' => "Je n'ai pas pu générer de réponse. Essayez de reformuler votre question."
                ], 200);
            }

            return new JsonResponse([
                'success' => true,
                'response' => $aiResponse
            ]);

        } catch (\Exception $e) {
            error_log("AssistantChatController Exception: " . $e->getMessage());
            return new JsonResponse([
                'success' => false,
                'response' => "Erreur interne du serveur lors de la communication avec l'assistant."
            ], 200);
        }
    }
}
