<?php

namespace App\Controller;

use App\Service\IARecommandationService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class RecommandationTraitementController extends AbstractController
{
    #[Route('/recommandation-traitement', name: 'app_recommandation_traitement', methods: ['GET', 'POST'])]
    public function index(
        Request $request,
        IARecommandationService $iaService
    ): Response
    {
        $recommandations = [];
        $description = '';
        $antecedents = '';
        $erreur = null;

        if ($request->isMethod('POST')) {
            $description = trim($request->request->get('description', ''));
            $antecedents = trim($request->request->get('antecedents', ''));

            if (empty($description)) {
                $erreur = 'Veuillez décrire vos symptômes pour obtenir des recommandations.';
            } elseif (strlen($description) < 10) {
                $erreur = 'Veuillez fournir une description plus détaillée de vos symptômes (au moins 10 caractères).';
            } else {
                try {
                    $recommandations = $iaService->analyserEtRecommander(
                        $description,
                        $antecedents,
                        8 // Limite de recommandations
                    );

                    if (empty($recommandations)) {
                        $erreur = 'Aucun produit correspondant trouvé. Veuillez reformuler votre description ou consulter un pharmacien.';
                    }
                } catch (\Exception $e) {
                    $erreur = 'Une erreur est survenue lors de la recherche de recommandations. Veuillez réessayer.';
                }
            }
        }

        return $this->render('front/recommandation_traitement.html.twig', [
            'recommandations' => $recommandations,
            'description' => $description,
            'antecedents' => $antecedents,
            'erreur' => $erreur
        ]);
    }

    #[Route('/recommandation-traitement/api', name: 'app_recommandation_traitement_api', methods: ['POST'])]
    public function api(
        Request $request,
        IARecommandationService $iaService
    ): Response
    {
        $description = trim($request->request->get('description', ''));
        $antecedents = trim($request->request->get('antecedents', ''));

        if (empty($description)) {
            return $this->json([
                'success' => false,
                'message' => 'Veuillez décrire vos symptômes.'
            ], 400);
        }

        try {
            $recommandations = $iaService->analyserEtRecommander(
                $description,
                $antecedents,
                8
            );

            $resultat = [];
            foreach ($recommandations as $item) {
                $produit = $item['produit'];
                $resultat[] = [
                    'id' => $produit->getId(),
                    'nom' => $produit->getNom(),
                    'description' => $produit->getDescription(),
                    'prix' => $produit->getPrix(),
                    'categorie' => $produit->getCategorie(),
                    'image' => $produit->getImage(),
                    'score' => $item['score'],
                    'raison' => $item['raison']
                ];
            }

            return $this->json([
                'success' => true,
                'recommandations' => $resultat
            ]);
        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'message' => 'Une erreur est survenue: ' . $e->getMessage()
            ], 500);
        }
    }
}
