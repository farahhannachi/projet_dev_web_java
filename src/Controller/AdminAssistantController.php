<?php

namespace App\Controller;

use App\Repository\QuestionRepository;
use App\Repository\ResponseQuestionRepository;
use App\Service\GroqLlmService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/admin/api-assistant')]
#[IsGranted('ROLE_ADMIN')]
class AdminAssistantController extends AbstractController
{
    private const SYSTEM_PROMPT = <<<PROMPT
You are the CURAVITA Admin Strategic Analyst. Your role is to analyze support tickets and provide a high-level summary for the pharmacy administrator.

YOUR TASKS:
1. ANALYSIS: Look at the provided list of tickets (id, subject, description, priority).
2. TRENDS: Identify recurring problems (e.g., login issues, stock errors, specific medication questions).
3. SENTIMENT: Note the general tone of users (angry, confused, satisfied).
4. PRIORITIZATION: Highlight the top 3 tickets or themes that need immediate human intervention.
5. FORMAT: Use a professional, concise tone. Use bullet points and bold text for readability.

STRUCTURE YOUR RESPONSE:
- 📊 **Synthèse Générale** (Short paragraph)
- ⚠️ **Points Critiques / Urgences** (Top 3 issues)
- 💡 **Recommandations** (What the admin should do now)

Limit your response to approximately 300 words. Always respond in French.
PROMPT;

    public function __construct(
        private GroqLlmService $llm,
        private QuestionRepository $questionRepository,
        private ResponseQuestionRepository $responseRepository,
        private \App\Service\OcrService $ocrService
    ) {}

    #[Route('/ocr/{id}', name: 'admin_api_ocr_ticket', methods: ['POST'])]
    public function extractTextFromTicket(int $id): JsonResponse
    {
        try {
            $question = $this->questionRepository->find($id);
            if (!$question) {
                return new JsonResponse(['success' => false, 'response' => "Ticket non trouvé"], 404);
            }

            $filePath = $question->getFilePath(); // e.g., /uploads/questions/xyz.jpg
            if (!$filePath) {
                return new JsonResponse(['success' => false, 'response' => "Aucune pièce jointe sur ce ticket"], 400);
            }

            // Check if it's an image
            $fileType = $question->getFileType(); // e.g., image/jpeg
            if ($fileType === null || !str_contains($fileType, 'image/')) {
                return new JsonResponse(['success' => false, 'response' => "La pièce jointe n'est pas une image supportée"], 400);
            }

            $projectDir = $this->getParameter('kernel.project_dir');
            $absolutePath = (is_string($projectDir) ? $projectDir : '') . '/public' . $filePath;
            $text = $this->ocrService->extractText($absolutePath);

            if (str_starts_with($text, 'Erreur')) {
                return new JsonResponse(['success' => false, 'response' => $text], 500);
            }

            return new JsonResponse([
                'success' => true,
                'text' => $text
            ]);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'response' => $e->getMessage()], 500);
        }
    }

    private const REPLY_SYSTEM_PROMPT = <<<PROMPT
You are the CURAVITA Support Intelligence. Your mission is to help the admin respond to a customer ticket.
Based on the ticket description and the conversation history, you must suggest a professional and helpful response.

YOUR OUTPUT MUST BE A JSON OBJECT WITH THESE FIELDS:
1. "reply": The proposed response text (in French).
2. "role": One of ["info", "demande_preuve", "solution", "decision"].
3. "action": One of ["aucune", "remboursement", "remplacement", "retour_accepte", "retour_refuse", "escalade"].
4. "status_impact": One of ["aucun", "en_cours", "resolu", "ferme"].

RULES:
- Be empathetic and professional.
- ALWAYS respond in French.
- Use your pharmaceutical knowledge if applicable.
- STATUS PREDICTION: 
    * If you are asking for info/files: suggest "en_cours".
    * If you provides a final answer/fix: suggest "resolu".
    * If the ticket is handled: suggest "ferme".
- ACTION PREDICTION: Be specific. If they want a refund, suggest "remboursement". If a product is bad, suggest "remplacement".

ONLY RETURN THE JSON OBJECT. NO OTHER TEXT.
PROMPT;

    #[Route('/summarize', name: 'admin_api_summarize_tickets', methods: ['POST'])]
    public function summarizeTickets(Request $request): JsonResponse
    {
        try {
            $data = json_decode($request->getContent(), true);
            $limit = $data['limit'] ?? 20;
            
            // Fetch tickets (ordered by newest first)
            $questions = $this->questionRepository->findBy([], ['createdAt' => 'DESC'], $limit);
            
            if (empty($questions)) {
                return new JsonResponse([
                    'success' => true,
                    'response' => "Aucun ticket n'est disponible pour l'analyse pour le moment."
                ]);
            }

            // Format tickets for the LLM
            $ticketContext = "";
            foreach ($questions as $q) {
                $ticketContext .= sprintf(
                    "ID: %d | Objet: %s | Priorité: %s | Description: %s\n---\n",
                    $q->getId(),
                    $q->getObjet(),
                    $q->getPriorite(),
                    substr($q->getDescription() ?? '', 0, 150) // Limit description length for token efficiency
                );
            }

            $messages = [
                ['role' => 'system', 'content' => self::SYSTEM_PROMPT],
                ['role' => 'user', 'content' => "Voici les derniers tickets à analyser :\n\n" . $ticketContext]
            ];

            // Increased max tokens for a detailed summary
            $result = $this->llm->chat($messages, temperature: 0.3, maxTokens: 800);
            
            if (isset($result['error'])) {
                return new JsonResponse([
                    'success' => false, 
                    'response' => "Erreur API Groq : " . ($result['error']['message'] ?? 'Erreur inconnue')
                ], 500);
            }

            $summary = $result['choices'][0]['message']['content'] ?? "Impossible de générer le résumé.";

            return new JsonResponse([
                'success' => true,
                'response' => $summary
            ]);

        } catch (\Exception $e) {
            return new JsonResponse([
                'success' => false,
                'response' => "Erreur lors de l'analyse : " . $e->getMessage()
            ], 500);
        }
    }

    #[Route('/{id}/generate-reply', name: 'admin_api_generate_reply', methods: ['POST'])]
    public function generateReply(int $id): JsonResponse
    {
        try {
            $question = $this->questionRepository->find($id);
            if (!$question) {
                return new JsonResponse(['success' => false, 'response' => "Ticket non trouvé"], 404);
            }

            // Fetch conversation history
            $reponses = $this->responseRepository->findBy(['question' => $question], ['createdAt' => 'ASC']);
            
            $history = "TICKET INITIAL :\nObjet: " . $question->getObjet() . "\nDescription: " . $question->getDescription() . "\n\n";
            $history .= "HISTORIQUE DES ÉCHANGES :\n";
            
            foreach ($reponses as $rep) {
                $history .= sprintf("[%s] %s: %s\n", 
                    $rep->getCreatedAt()?->format('d/m/Y H:i') ?? '',
                    $rep->getAuteurType() === 'agent' ? 'AGENT' : 'CLIENT',
                    $rep->getReponseText()
                );
            }

            $messages = [
                ['role' => 'system', 'content' => self::REPLY_SYSTEM_PROMPT],
                ['role' => 'user', 'content' => "Génère une proposition de réponse pour ce ticket :\n\n" . $history]
            ];

            // Use low temperature for consistent JSON output
            $result = $this->llm->chat($messages, temperature: 0.1, maxTokens: 1000);
            
            if (isset($result['error'])) {
                return new JsonResponse(['success' => false, 'response' => "Erreur API Groq"], 500);
            }

            $rawContent = $result['choices'][0]['message']['content'] ?? "{}";
            
            // Extract JSON if there is markdown wrapping
            if (preg_match('/```json\s*(.*?)\s*```/s', $rawContent, $matches)) {
                $rawContent = $matches[1];
            }
            
            $suggestion = json_decode($rawContent, true);

            if (!$suggestion || !isset($suggestion['reply'])) {
                return new JsonResponse([
                    'success' => false, 
                    'response' => "L'IA a généré une réponse invalide.",
                    'raw' => $rawContent
                ], 500);
            }

            return new JsonResponse([
                'success' => true,
                'suggestion' => $suggestion
            ]);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'response' => $e->getMessage()], 500);
        }
    }
}
