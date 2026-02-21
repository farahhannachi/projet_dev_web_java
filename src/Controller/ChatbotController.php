<?php

namespace App\Controller;

use App\Service\OllamaService;
use App\Entity\Utilisateur;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;
use Psr\Log\LoggerInterface;

#[Route('/api/chatbot')]
class ChatbotController extends AbstractController
{
    #[Route('/status', name: 'api_chatbot_status', methods: ['GET'])]
    public function status(OllamaService $ollamaService): JsonResponse
    {
        $isAvailable = $ollamaService->isAvailable();
        
        return new JsonResponse([
            'available' => $isAvailable,
            'model' => $isAvailable ? $ollamaService->getModel() : null,
        ]);
    }

    #[Route('/is-new', name: 'api_chatbot_is_new', methods: ['GET'])]
    public function isNewUser(): JsonResponse
    {
        // Check authentication
        if (!$this->isGranted('ROLE_USER') && !$this->isGranted('ROLE_ADMIN')) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        try {
            $user = $this->getUser();
            if (!$user) {
                return new JsonResponse(['error' => 'User not found'], 401);
            }

            // Fetch latest user data from database
            $entityManager = $this->getDoctrine()->getManager();
            $repository = $entityManager->getRepository(Utilisateur::class);
            $freshUser = $repository->find($user->getId());
            
            if (!$freshUser) {
                return new JsonResponse(['error' => 'User not found'], 401);
            }
            
            // Check if user has seen the introduction
            $isNew = !$freshUser->hasSeenIntroduction();
            
            return new JsonResponse([
                'isNew' => $isNew,
            ]);
        } catch (\Exception $e) {
            return new JsonResponse(['error' => 'Error checking user status', 'details' => $e->getMessage()], 500);
        }
    }

    #[Route('/mark-seen', name: 'api_chatbot_mark_seen', methods: ['POST'])]
    public function markIntroductionAsSeen(): JsonResponse
    {
        // Check authentication
        if (!$this->isGranted('ROLE_USER') && !$this->isGranted('ROLE_ADMIN')) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        try {
            $user = $this->getUser();
            if (!$user) {
                return new JsonResponse(['error' => 'User not found'], 401);
            }

            $user->setHasSeenIntroduction(true);
            
            // Explicitly persist and flush to database
            $entityManager = $this->getDoctrine()->getManager();
            $entityManager->persist($user);
            $entityManager->flush();
            
            return new JsonResponse(['success' => true]);
        } catch (\Exception $e) {
            return new JsonResponse(['error' => 'Failed to save introduction status'], 500);
        }
    }

    #[Route('/chat', name: 'api_chatbot_chat', methods: ['POST'])]
    public function chat(
        Request $request,
        OllamaService $ollamaService,
        LoggerInterface $logger
    ): JsonResponse {
        // Check authentication - allow ROLE_USER and ROLE_ADMIN
        if (!$this->isGranted('ROLE_USER') && !$this->isGranted('ROLE_ADMIN')) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        try {
            $data = json_decode($request->getContent(), true);

            if (!isset($data['message']) || empty($data['message'])) {
                return new JsonResponse(['error' => 'Message is required'], 400);
            }

            $message = trim($data['message']);
            $conversation = $data['conversation'] ?? [];

            // Check if Ollama is available
            if (!$ollamaService->isAvailable()) {
                return new JsonResponse([
                    'error' => 'AI assistant is temporarily unavailable. Please try again later.',
                ], 503);
            }

            // Limit message length
            if (strlen($message) > 1000) {
                return new JsonResponse(['error' => 'Message is too long (max 1000 characters)'], 400);
            }

            // Sanitize message
            $message = htmlspecialchars($message, ENT_QUOTES, 'UTF-8');

            // Generate response
            $response = $ollamaService->generateResponse($message, $conversation);

            return new JsonResponse([
                'success' => true,
                'response' => $response,
            ]);
        } catch (\Exception $e) {
            $logger->error('Chatbot error: ' . $e->getMessage());
            
            return new JsonResponse([
                'error' => 'An error occurred while processing your message. Please try again.',
            ], 500);
        }
    }

    #[Route('/models', name: 'api_chatbot_models', methods: ['GET'])]
    public function models(OllamaService $ollamaService): JsonResponse
    {
        try {
            $models = $ollamaService->getAvailableModels();
            
            return new JsonResponse([
                'models' => $models,
                'current' => $ollamaService->getModel(),
            ]);
        } catch (\Exception $e) {
            return new JsonResponse(['error' => 'Failed to fetch models'], 500);
        }
    }
}
