<?php

namespace App\Controller;

use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Form\DemandeTraitementType;
use App\Repository\OrdonnanceRepository;
use App\Repository\ProduitRepository;
use App\Service\PusherNotificationService;
use App\Service\IAInteractionMedicamenteuse;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;

class DemandeTraitementController extends AbstractController
{
    #[Route('/demande_de_traitement', name: 'app_demande_traitement', methods: ['GET', 'POST'])]
    public function nouvelle(
        Request $request,
        EntityManagerInterface $entityManager,
        ProduitRepository $produitRepository,
        PusherNotificationService $pusherService
    ): Response
    {
        // VÃƒÂ©rifier si l'utilisateur est connectÃƒÂ©
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez ÃƒÂªtre connectÃƒÂ© pour accÃƒÂ©der ÃƒÂ  cette page');
            return $this->redirectToRoute('app_login');
        }

        /** @var \App\Entity\Utilisateur $user */
        $user = $this->getUser();

        // CrÃƒÂ©er le formulaire
        $form = $this->createForm(DemandeTraitementType::class);
        
        // PrÃƒÂ©-remplir les champs avec les donnÃƒÂ©es de l'utilisateur
        $form->get('nom')->setData($user->getNom() . ' ' . $user->getPrenom());
        $form->get('email')->setData($user->getEmail());
        
        if ($user->getDateNaissance()) {
            $form->get('dateNaissance')->setData($user->getDateNaissance());
        }

        // Pr�-s�lectionner le produit si fourni via GET
        $produitId = $request->query->get('produit_id');
        $produitPreselectionne = null;
        if ($produitId) {
            $produitPreselectionne = $produitRepository->find($produitId);
            if ($produitPreselectionne) {
                $form->get('produit')->setData($produitPreselectionne);
            }
        }

        $form->handleRequest($request);

        // Debug: Log form state
        if ($form->isSubmitted()) {
            $this->addFlash('debug', 'Form submitted. Is valid: ' . ($form->isValid() ? 'yes' : 'no'));
            if (!$form->isValid()) {
                foreach ($form->getErrors(true) as $error) {
                    if ($error instanceof \Symfony\Component\Form\FormError) {
                        $this->addFlash('error', 'Validation error: ' . $error->getMessage());
                    }
                }
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            $data = $form->getData();
            
            // Mettre ÃƒÂ  jour la date de naissance de l'utilisateur si elle n'existe pas
            if ($data['dateNaissance'] && !$user->getDateNaissance()) {
                $user->setDateNaissance($data['dateNaissance']);
                $entityManager->persist($user);
            }

            // Créer une ordonnance temporaire pour cette demande (sans traitement)
            $ordonnance = new Ordonnance();
            $ordonnance->setUtilisateur($user);
            $ordonnance->setNumeroOrdonnance('ORD-' . date('YmdHis') . '-' . $user->getId());
            $ordonnance->setDateOrdonnance(new \DateTime());
            $ordonnance->setDateExpiration((new \DateTime())->modify('+1 year'));
            $ordonnance->setStatut('en_attente');
            
            // Ensure UTF-8 encoding for the note
            /** @var string $antecedents */
            $antecedents = mb_convert_encoding($data['antecedentsMedicaux'] ?? '', 'UTF-8', 'UTF-8') ?: '';
            /** @var string $symptomes */
            $symptomes = mb_convert_encoding(is_string($data['symptomes'] ?? null) ? $data['symptomes'] : '', 'UTF-8', 'UTF-8') ?: '';
            /** @var string $produitNom */
            $produitNom = mb_convert_encoding($data['produit']->getNom() ?? '', 'UTF-8', 'UTF-8') ?: '';
            
            $ordonnance->setNoteMedical('Antécédents: ' . $antecedents . "\n\nSymptômes: " . $symptomes . "\n\nProduit demandé: " . $produitNom);
            $entityManager->persist($ordonnance);
            $entityManager->flush();

            // Envoyer notification Pusher pour nouvelle ordonnance
            $pusherService->notifyNewOrdonnance([
                'id' => $ordonnance->getId(),
                'numero' => $ordonnance->getNumeroOrdonnance(),
                'patient' => $user->getNom() . ' ' . $user->getPrenom(),
                'date' => $ordonnance->getDateOrdonnance()?->format('d/m/Y H:i') ?? ''
            ]);

            // Stocker l'ID de l'ordonnance et du produit en session pour la page ordonnance
            $request->getSession()->set('ordonnance_id', $ordonnance->getId());
            $request->getSession()->set('produit_demande_id', $data['produit']->getId());

            $this->addFlash('success', 'Votre demande de traitement a ete enregistree. Veuillez maintenant creer votre ordonnance.');
            return $this->redirectToRoute('app_formulaire_ordonnance');
        }

        return $this->render('front/demande_traitement_new.html.twig', [
            'form' => $form->createView(),
            'user' => $user
        ]);
    }

    #[Route('/conditions-generales', name: 'app_conditions_generales', methods: ['GET'])]
    public function conditionsGenerales(): Response
    {
        return $this->render('front/conditions_generales.html.twig');
    }

    #[Route('/api/verifier-interactions', name: 'app_verifier_interactions', methods: ['POST'])]
    public function verifierInteractions(
        Request $request,
        ProduitRepository $produitRepository,
        IAInteractionMedicamenteuse $iaInteraction
    ): JsonResponse
    {
        try {
            $data = json_decode($request->getContent(), true);
            
            $produitId = $data['produit_id'] ?? null;
            $symptomes = $data['symptomes'] ?? '';
            $antecedents = $data['antecedents'] ?? '';

            if (!$produitId || !$symptomes || !$antecedents) {
                return new JsonResponse([
                    'success' => false,
                    'message' => 'Données manquantes'
                ], 400);
            }

            $produit = $produitRepository->find($produitId);
            if (!$produit) {
                return new JsonResponse([
                    'success' => false,
                    'message' => 'Produit introuvable'
                ], 404);
            }

            // Analyser les interactions avec l'IA
            $analyse = $iaInteraction->analyserInteractions($produit, $symptomes, $antecedents);

            return new JsonResponse([
                'success' => true,
                'analyse' => $analyse
            ]);

        } catch (\Exception $e) {
            return new JsonResponse([
                'success' => false,
                'message' => 'Erreur lors de l\'analyse: ' . $e->getMessage()
            ], 500);
        }
    }
}





