<?php

namespace App\Controller;

use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Entity\Utilisateur;
use App\Form\OrdonnanceFrontType;
use App\Repository\OrdonnanceRepository;
use App\Repository\ProduitRepository;
use App\Service\PusherNotificationService;
use App\Service\SignatureService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class OrdonnanceFrontController extends AbstractController
{
    #[Route('/ordonnance/{id}/signer-patient', name: 'app_ordonnance_signer_patient', methods: ['POST'])]
    public function signerPatient(
        int $id,
        OrdonnanceRepository $ordonnanceRepository,
        SignatureService $signatureService
    ): Response
    {
        if (!$this->getUser()) {
            return $this->json(['success' => false, 'message' => 'Non authentifié'], 401);
        }

        $ordonnance = $ordonnanceRepository->find($id);
        
        if (!$ordonnance) {
            return $this->json(['success' => false, 'message' => 'Ordonnance non trouvée'], 404);
        }

        // Vérifier que c'est bien l'ordonnance de l'utilisateur connecté
        /** @var Utilisateur|null $currentUser */
        $currentUser = $this->getUser();
        $ordonnanceUser = $ordonnance->getUtilisateur();
        if ($ordonnanceUser === null || $currentUser === null || $ordonnanceUser->getId() !== $currentUser->getId()) {
            return $this->json(['success' => false, 'message' => 'Accès non autorisé'], 403);
        }

        // Vérifier si déjà signé
        if ($ordonnance->getSignaturePatient()) {
            return $this->json([
                'success' => false, 
                'message' => 'Cette ordonnance a déjà été signée par le patient'
            ], 400);
        }

        $result = $signatureService->signerParPatient($ordonnance);
        
        return $this->json($result);
    }

    #[Route('/formulaire-ordonnance', name: 'app_formulaire_ordonnance', methods: ['GET', 'POST'])]
    public function index(
        Request $request, 
        EntityManagerInterface $entityManager,
        OrdonnanceRepository $ordonnanceRepository,
        ProduitRepository $produitRepository,
        PusherNotificationService $pusherService,
        SignatureService $signatureService
    ): Response
    {
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à cette page');
            return $this->redirectToRoute('app_login');
        }

        $ordonnanceId = $request->getSession()->get('ordonnance_id');
        $produitDemandeId = $request->getSession()->get('produit_demande_id');
        
        $ordonnance = null;
        $produitDemande = null;
        
        if ($ordonnanceId) {
            $ordonnance = $ordonnanceRepository->find($ordonnanceId);
        }
        
        if ($produitDemandeId) {
            $produitDemande = $produitRepository->find($produitDemandeId);
        }

        /** @var Utilisateur|null $user */
        $user = $this->getUser();
        if (!$user instanceof Utilisateur) {
            throw $this->createAccessDeniedException('User not authenticated');
        }

        if (!$ordonnance) {
            $ordonnance = new Ordonnance();
            $ordonnance->setStatut('en_attente');
            $ordonnance->setUtilisateur($user);
            $numeroOrdonnance = 'ORD-' . date('YmdHis') . '-' . $user->getId();
            $ordonnance->setNumeroOrdonnance($numeroOrdonnance);
            $ordonnance->setDateOrdonnance(new \DateTime());
            $dateExpiration = new \DateTime();
            $dateExpiration->modify('+1 year');
            $ordonnance->setDateExpiration($dateExpiration);
        }
        
        $formOrdonnance = $this->createForm(OrdonnanceFrontType::class, $ordonnance);
        $formOrdonnance->handleRequest($request);
        
        if ($formOrdonnance->isSubmitted() && $formOrdonnance->isValid()) {
            $dateOrdonnance = $ordonnance->getDateOrdonnance();
            $dateExpiration = $ordonnance->getDateExpiration();
            $today = new \DateTime('today');
            
            // Validation des dates
            $hasError = false;
            
            if ($dateOrdonnance > $today) {
                $this->addFlash('error', 'La date de l\'ordonnance ne peut pas être dans le futur');
                $hasError = true;
            }
            
            if ($dateExpiration < $today) {
                $this->addFlash('error', 'La date d\'expiration doit être aujourd\'hui ou dans le futur');
                $hasError = true;
            }
            
            if ($dateExpiration <= $dateOrdonnance) {
                $this->addFlash('error', 'La date d\'expiration doit être postérieure à la date de l\'ordonnance');
                $hasError = true;
            }
            
            if (!$hasError) {
                $isNew = !$ordonnance->getId();
                
                if ($isNew) {
                    $entityManager->persist($ordonnance);
                }
                
                $entityManager->flush();
                
                // Vérifier si le patient veut signer
                $wantsToSign = $request->request->get('wants_to_sign');
                if ($wantsToSign === '1' && $isNew && !$ordonnance->getSignaturePatient()) {
                    // Signer automatiquement l'ordonnance
                    $signatureService->signerParPatient($ordonnance);
                }
                
                if ($isNew) {
                    // Envoyer notification Pusher pour nouvelle ordonnance
                    $utilisateur = $ordonnance->getUtilisateur();
                    $dateOrdonnance = $ordonnance->getDateOrdonnance();
                    $pusherService->notifyNewOrdonnance([
                        'id' => $ordonnance->getId(),
                        'numero' => $ordonnance->getNumeroOrdonnance(),
                        'patient' => $utilisateur ? $utilisateur->getNom() . ' ' . $utilisateur->getPrenom() : 'Unknown',
                        'date' => $dateOrdonnance ? $dateOrdonnance->format('d/m/Y H:i') : ''
                    ]);
                }
                
                if ($produitDemande) {
                    $traitement = new Traitement();
                    $traitement->setOrdonnance($ordonnance);
                    $traitement->setUtilisateur($user);
                    $traitement->setProduit($produitDemande);
                    $traitement->setDateDebut(new \DateTime());
                    $traitement->setStatus('en_attente');
                    $traitement->setNotes($ordonnance->getNoteMedical());
                    
                    $entityManager->persist($traitement);
                    $entityManager->flush();
                }
                
                $request->getSession()->remove('ordonnance_id');
                $request->getSession()->remove('produit_demande_id');
                
                $this->addFlash('success', 'Votre ordonnance a ete envoyee avec succes ! Un pharmacien va la verifier.');
                
                // Pattern POST/Redirect/GET pour éviter la double soumission
                return $this->redirectToRoute('app_ordonnances');
            }
        } elseif ($formOrdonnance->isSubmitted()) {
            // Formulaire soumis mais invalide - afficher les erreurs détaillées
            $errors = [];
            foreach ($formOrdonnance->getErrors(true) as $error) {
                if ($error instanceof \Symfony\Component\Form\FormError) {
                    $errors[] = $error->getMessage();
                }
            }
            if (!empty($errors)) {
                $this->addFlash('error', 'Erreurs: ' . implode(', ', $errors));
            } else {
                $this->addFlash('error', 'Veuillez corriger les erreurs dans le formulaire');
            }
        }

        return $this->render('front/formulaire_ordonnance.html.twig', [
            'form' => $formOrdonnance->createView(),
            'produitsSelectionnes' => [],
            'ordonnance' => $ordonnance
        ]);
    }
}
