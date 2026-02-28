<?php

namespace App\Controller;

use App\Service\IARecommandationService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

class AIController extends AbstractController
{
    #[Route('/api/ai/recommend', name: 'api_ai_recommend', methods: ['POST'])]
    public function recommend(Request $request, IARecommandationService $iaService): JsonResponse
    {
        try {
            $data = json_decode($request->getContent(), true);
            $symptomes = trim((string)($data['message'] ?? ''));

            if (empty($symptomes)) {
                return new JsonResponse(['success' => false, 'message' => 'Veuillez décrire vos symptômes.'], 400);
            }

            // Detect greetings
            $lowerSymptomes = mb_strtolower($symptomes);
            $greetings = ['bonjour', 'hello', 'salut', 'hi', 'hey', 'bonsoir'];
            foreach ($greetings as $greet) {
                if ($lowerSymptomes === $greet || str_starts_with($lowerSymptomes, $greet . ' ')) {
                    return new JsonResponse([
                        'success' => true,
                        'response' => "Bonjour ! Je suis votre assistant CURAVITA. Comment puis-je vous aider aujourd'hui ? Vous pouvez me décrire vos symptômes ou me demander conseil sur un type de produit."
                    ]);
                }
            }

            // Optional: antecedents could be passed from user session or profile
            $recommandations = $iaService->analyserEtRecommander($symptomes, null, 3);

            if (empty($recommandations)) {
                return new JsonResponse([
                    'success' => true,
                    'response' => "Désolé, je n'ai pas trouvé de produits spécifiques dans notre catalogue correspondant à vos symptômes. Veuillez consulter un pharmacien ou un médecin."
                ]);
            }

            $responseStr = "D'après vos symptômes, voici quelques produits qui pourraient vous aider :\n\n";
            foreach ($recommandations as $rec) {
                $produit = $rec['produit'];
                $responseStr .= sprintf("- **%s** (%s) : %s\n", 
                    $produit->getNom(), 
                    $produit->getCategorie(), 
                    $rec['raison']
                );
            }
            
            $responseStr .= "\nImportant : Je suis un assistant IA. Consultez toujours un médecin ou demandez conseil à votre pharmacien en personne pour un diagnostic précis.";

            return new JsonResponse([
                'success' => true,
                'response' => $responseStr,
                'recommandations' => array_map(function($rec) {
                    return [
                        'id' => $rec['produit']->getId(),
                        'nom' => $rec['produit']->getNom(),
                        'prix' => $rec['produit']->getPrix(),
                        'image' => $rec['produit']->getImage(),
                    ];
                }, $recommandations)
            ]);

        } catch (\Exception $e) {
            return new JsonResponse([
                'success' => false,
                'message' => 'Une erreur est survenue lors de l\'analyse : ' . $e->getMessage()
            ], 500);
        }
    }
}
