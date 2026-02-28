<?php

namespace App\Controller;

use App\Entity\Ordonnance;
use App\Form\OrdonnanceType;
use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class OrdonnanceController extends AbstractController
{
    #[Route('/admin/ordonnances', name: 'admin_ordonnances')]
    public function index(Request $request, OrdonnanceRepository $ordonnanceRepository, EntityManagerInterface $entityManager, PaginatorInterface $paginator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer les paramètres de filtrage
        $filterDateOrdonnance = $request->query->get('date_ordonnance', '');
        $filterDateExpiration = $request->query->get('date_expiration', '');
        $filterClient = $request->query->get('client', '');
        $filterStatut = $request->query->get('statut', '');
        $sortBy = $request->query->get('sort', 'date_desc');
        
        // Construire la requête avec filtres
        $queryBuilder = $ordonnanceRepository->createQueryBuilder('o')
            ->leftJoin('o.utilisateur', 'u');
        
        // Filtre par date d'ordonnance
        if ($filterDateOrdonnance) {
            $queryBuilder->andWhere('o.dateOrdonnance = :dateOrdonnance')
                ->setParameter('dateOrdonnance', new \DateTime((string) $filterDateOrdonnance));
        }
        
        // Filtre par date d'expiration
        if ($filterDateExpiration) {
            $queryBuilder->andWhere('o.dateExpiration = :dateExpiration')
                ->setParameter('dateExpiration', new \DateTime((string) $filterDateExpiration));
        }
        
        // Filtre par client (nom, prénom ou email)
        if ($filterClient) {
            $queryBuilder->andWhere('u.nom LIKE :client OR u.prenom LIKE :client OR u.email LIKE :client')
                ->setParameter('client', '%' . $filterClient . '%');
        }
        
        // Filtre par statut
        if ($filterStatut) {
            $queryBuilder->andWhere('o.statut = :statut')
                ->setParameter('statut', $filterStatut);
        }
        
        // Tri
        switch ($sortBy) {
            case 'date_asc':
                $queryBuilder->orderBy('o.dateOrdonnance', 'ASC');
                break;
            case 'date_desc':
                $queryBuilder->orderBy('o.dateOrdonnance', 'DESC');
                break;
            case 'expiration_asc':
                $queryBuilder->orderBy('o.dateExpiration', 'ASC');
                break;
            case 'expiration_desc':
                $queryBuilder->orderBy('o.dateExpiration', 'DESC');
                break;
            case 'client_asc':
                $queryBuilder->orderBy('u.nom', 'ASC');
                break;
            case 'client_desc':
                $queryBuilder->orderBy('u.nom', 'DESC');
                break;
            case 'numero_asc':
                $queryBuilder->orderBy('o.numeroOrdonnance', 'ASC');
                break;
            case 'numero_desc':
                $queryBuilder->orderBy('o.numeroOrdonnance', 'DESC');
                break;
            default:
                $queryBuilder->orderBy('o.dateOrdonnance', 'DESC');
        }
        
        // Paginer les résultats - 5 par page
        $pagination = $paginator->paginate(
            $queryBuilder,
            $request->query->getInt('page', 1),
            5
        );
        
        // Récupérer les traitements pour chaque ordonnance de la page courante
        $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
        $traitementsByOrdonnance = [];
        foreach ($pagination->getItems() as $ordonnance) {
            $traitementsByOrdonnance[$ordonnance->getId()] = $traitementRepository->findBy(
                ['ordonnance' => $ordonnance],
                ['id' => 'ASC']
            );
        }
        
        return $this->render('Admin/ordonnances/index.html.twig', [
            'ordonnances' => $pagination,
            'traitementsByOrdonnance' => $traitementsByOrdonnance,
            'filterDateOrdonnance' => $filterDateOrdonnance,
            'filterDateExpiration' => $filterDateExpiration,
            'filterClient' => $filterClient,
            'filterStatut' => $filterStatut,
            'sortBy' => $sortBy
        ]);
    }

    #[Route('/admin/ordonnances/stats', name: 'admin_ordonnances_stats', methods: ['GET'])]
    public function stats(OrdonnanceRepository $ordonnanceRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer toutes les ordonnances
        $ordonnances = $ordonnanceRepository->findAll();
        $total = count($ordonnances);
        
        // Compter par statut
        $enAttente = 0;
        $valide = 0;
        $rejete = 0;
        
        foreach ($ordonnances as $ordonnance) {
            switch ($ordonnance->getStatut()) {
                case 'en attente':
                    $enAttente++;
                    break;
                case 'validé':
                    $valide++;
                    break;
                case 'rejeté':
                    $rejete++;
                    break;
            }
        }
        
        // Calculer les ordonnances expirées
        $now = new \DateTime();
        $expirees = 0;
        foreach ($ordonnances as $ordonnance) {
            if ($ordonnance->getDateExpiration() < $now) {
                $expirees++;
            }
        }
        
        return $this->json([
            'total' => $total,
            'enAttente' => $enAttente,
            'valide' => $valide,
            'rejete' => $rejete,
            'expirees' => $expirees,
            'pourcentageValide' => $total > 0 ? round(($valide / $total) * 100, 1) : 0,
            'pourcentageRejete' => $total > 0 ? round(($rejete / $total) * 100, 1) : 0
        ]);
    }
    
    #[Route('/admin/ordonnances/check-new', name: 'admin_ordonnances_check_new', methods: ['GET'])]
    public function checkNew(Request $request, OrdonnanceRepository $ordonnanceRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $lastId = $request->query->get('lastId', 0);
        
        // Chercher les ordonnances avec un ID supérieur au dernier ID connu
        $newOrdonnances = $ordonnanceRepository->createQueryBuilder('o')
            ->where('o.id > :lastId')
            ->setParameter('lastId', $lastId)
            ->orderBy('o.id', 'DESC')
            ->setMaxResults(1)
            ->getQuery()
            ->getResult();
        
        if (empty($newOrdonnances)) {
            return $this->json(['hasNew' => false]);
        }
        
        $ordonnance = $newOrdonnances[0];
        $utilisateur = $ordonnance->getUtilisateur();
        
        return $this->json([
            'hasNew' => true,
            'ordonnance' => [
                'id' => $ordonnance->getId(),
                'numero' => $ordonnance->getNumeroOrdonnance(),
                'patient' => $utilisateur ? $utilisateur->getNom() . ' ' . $utilisateur->getPrenom() : 'Inconnu',
                'date' => $ordonnance->getDateOrdonnance()->format('d/m/Y H:i')
            ]
        ]);
    }

    #[Route('/admin/ordonnances/search', name: 'admin_ordonnances_search', methods: ['GET'])]
    public function search(Request $request, OrdonnanceRepository $ordonnanceRepository, EntityManagerInterface $entityManager, PaginatorInterface $paginator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer les paramètres de filtrage
        $filterDateOrdonnance = $request->query->get('date_ordonnance', '');
        $filterDateExpiration = $request->query->get('date_expiration', '');
        $filterClient = $request->query->get('client', '');
        $filterStatut = $request->query->get('statut', '');
        $sortBy = $request->query->get('order', 'date_desc');
        
        // Construire la requête avec filtres
        $queryBuilder = $ordonnanceRepository->createQueryBuilder('o')
            ->leftJoin('o.utilisateur', 'u');
        
        // Filtre par date d'ordonnance
        if ($filterDateOrdonnance) {
            $queryBuilder->andWhere('o.dateOrdonnance = :dateOrdonnance')
                ->setParameter('dateOrdonnance', new \DateTime((string) $filterDateOrdonnance));
        }
        
        // Filtre par date d'expiration
        if ($filterDateExpiration) {
            $queryBuilder->andWhere('o.dateExpiration = :dateExpiration')
                ->setParameter('dateExpiration', new \DateTime((string) $filterDateExpiration));
        }
        
        // Filtre par client (nom, prénom ou email)
        if ($filterClient) {
            $queryBuilder->andWhere('u.nom LIKE :client OR u.prenom LIKE :client OR u.email LIKE :client')
                ->setParameter('client', '%' . $filterClient . '%');
        }
        
        // Filtre par statut
        if ($filterStatut) {
            $queryBuilder->andWhere('o.statut = :statut')
                ->setParameter('statut', $filterStatut);
        }
        
        // Tri
        switch ($sortBy) {
            case 'date_asc':
                $queryBuilder->orderBy('o.dateOrdonnance', 'ASC');
                break;
            case 'date_desc':
                $queryBuilder->orderBy('o.dateOrdonnance', 'DESC');
                break;
            case 'expiration_asc':
                $queryBuilder->orderBy('o.dateExpiration', 'ASC');
                break;
            case 'expiration_desc':
                $queryBuilder->orderBy('o.dateExpiration', 'DESC');
                break;
            case 'client_asc':
                $queryBuilder->orderBy('u.nom', 'ASC');
                break;
            case 'client_desc':
                $queryBuilder->orderBy('u.nom', 'DESC');
                break;
            case 'numero_asc':
                $queryBuilder->orderBy('o.numeroOrdonnance', 'ASC');
                break;
            case 'numero_desc':
                $queryBuilder->orderBy('o.numeroOrdonnance', 'DESC');
                break;
            default:
                $queryBuilder->orderBy('o.dateOrdonnance', 'DESC');
        }
        
        // Paginer les résultats - 5 par page
        $request->query->remove('sort');
        $pagination = $paginator->paginate(
    $queryBuilder,
    $request->query->getInt('page', 1),
    5,
    [
        // Disable KnpPaginator's automatic sortable handling to prevent invalid field errors
        'sortFieldWhitelist' => [],
        'sortDirectionWhitelist' => [],
    ]
);
        
        // Récupérer les traitements pour chaque ordonnance de la page courante
        $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
        $traitementsByOrdonnance = [];
        foreach ($pagination->getItems() as $ordonnance) {
            $traitementsByOrdonnance[$ordonnance->getId()] = $traitementRepository->findBy(
                ['ordonnance' => $ordonnance],
                ['id' => 'ASC']
            );
        }
        
        // Rendre le HTML complet avec pagination
        $html = $this->renderView('Admin/ordonnances/_search_results.html.twig', [
            'ordonnances' => $pagination,
            'traitementsByOrdonnance' => $traitementsByOrdonnance
        ]);
        
        return $this->json([
            'html' => $html,
            'count' => $pagination->getTotalItemCount()
        ]);
    }

    #[Route('/admin/ordonnance/new', name: 'admin_ordonnance_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $ordonnance = new Ordonnance();
        $ordonnance->setStatut('en attente');
        
        // Définir des valeurs par défaut pour les dates
        $ordonnance->setDateOrdonnance(new \DateTime());
        $dateExpiration = new \DateTime();
        $dateExpiration->modify('+3 months');
        $ordonnance->setDateExpiration($dateExpiration);
        
        $form = $this->createForm(OrdonnanceType::class, $ordonnance, [
            'is_edit' => false
        ]);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Forcer le statut à "en attente" lors de la création
            $ordonnance->setStatut('en attente');
            
            // Validation supplémentaire côté serveur
            $dateOrdonnance = $ordonnance->getDateOrdonnance();
            $dateExpiration = $ordonnance->getDateExpiration();
            
            if ($dateExpiration <= $dateOrdonnance) {
                $this->addFlash('error', 'La date d\'expiration doit être postérieure à la date de l\'ordonnance');
                return $this->render('Admin/ordonnances/form.html.twig', [
                    'form' => $form->createView(),
                    'ordonnance' => null
                ]);
            }
            
            $entityManager->persist($ordonnance);
            $entityManager->flush();
            
            // Créer automatiquement un traitement vide associé à cette ordonnance
            $traitement = new \App\Entity\Traitement();
            $traitement->setOrdonnance($ordonnance);
            $traitement->setUtilisateur($ordonnance->getUtilisateur());
            $traitement->setStatus('en attente');
            $traitement->setNotes('Traitement créé automatiquement - À compléter par l\'administrateur');
            
            $entityManager->persist($traitement);
            $entityManager->flush();
            
            $this->addFlash('success', 'Ordonnance créée avec succès (statut: en attente). Un traitement a été créé automatiquement.');
            return $this->redirectToRoute('admin_ordonnances');
        }

        return $this->render('Admin/ordonnances/form.html.twig', [
            'form' => $form->createView(),
            'ordonnance' => null
        ]);
    }

    #[Route('/admin/ordonnance/{id}/edit', name: 'admin_ordonnance_edit', methods: ['GET', 'POST'])]
    public function edit(Ordonnance $ordonnance, Request $request, EntityManagerInterface $entityManager, \App\Service\TwilioSmsService $twilioSmsService): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Sauvegarder l'ancien statut pour détecter les changements
        $ancienStatut = $ordonnance->getStatut();
        
        $form = $this->createForm(OrdonnanceType::class, $ordonnance, [
            'is_edit' => true
        ]);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Validation supplémentaire côté serveur
            $dateOrdonnance = $ordonnance->getDateOrdonnance();
            $dateExpiration = $ordonnance->getDateExpiration();
            
            if ($dateExpiration <= $dateOrdonnance) {
                $this->addFlash('error', 'La date d\'expiration doit être postérieure à la date de l\'ordonnance');
                return $this->render('Admin/ordonnances/form.html.twig', [
                    'form' => $form->createView(),
                    'ordonnance' => $ordonnance
                ]);
            }
            
            // Si l'ordonnance passe à "validé", valider automatiquement tous les traitements associés
            $nouveauStatut = $ordonnance->getStatut();
            if ($ancienStatut !== 'validé' && $nouveauStatut === 'validé') {
                $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
                $traitements = $traitementRepository->findBy(['ordonnance' => $ordonnance]);
                
                foreach ($traitements as $traitement) {
                    $traitement->setStatus('validé');
                    $entityManager->persist($traitement);
                }
                
                // 📱 ENVOI SMS AUTOMATIQUE - Ordonnance validée
                $utilisateur = $ordonnance->getUtilisateur();
                $nomComplet = $utilisateur ? $utilisateur->getNom() . ' ' . $utilisateur->getPrenom() : 'Client';
                
                // Envoyer SMS au numéro fixe +21626581955
                $numeroOrdonnance = $ordonnance->getNumeroOrdonnance() ?? 'N/A';
                $smsEnvoye = $twilioSmsService->sendOrdonnancePrete(
                    '+21626581955',
                    $numeroOrdonnance,
                    $nomComplet
                );
                
                if ($smsEnvoye) {
                    $this->addFlash('success', 'Ordonnance validée avec succès. ' . count($traitements) . ' traitement(s) associé(s) ont été validés automatiquement. 📱 SMS envoyé au +21626581955.');
                } else {
                    $this->addFlash('success', 'Ordonnance validée avec succès. ' . count($traitements) . ' traitement(s) associé(s) ont été validés automatiquement. ⚠️ SMS non envoyé (erreur Twilio).');
                }
            } elseif ($ancienStatut !== 'rejeté' && $nouveauStatut === 'rejeté') {
                // Si l'ordonnance est rejetée, rejeter automatiquement tous les traitements associés
                $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
                $traitements = $traitementRepository->findBy(['ordonnance' => $ordonnance]);
                
                foreach ($traitements as $traitement) {
                    $traitement->setStatus('rejeté');
                    $entityManager->persist($traitement);
                }
                
                // 📱 ENVOI SMS AUTOMATIQUE - Ordonnance rejetée
                $utilisateur = $ordonnance->getUtilisateur();
                $nomComplet = $utilisateur ? $utilisateur->getNom() . ' ' . $utilisateur->getPrenom() : 'Client';
                
                // Envoyer SMS au numéro fixe +21626581955
                $numeroOrdonnance = $ordonnance->getNumeroOrdonnance() ?? 'N/A';
                $smsEnvoye = $twilioSmsService->sendOrdonnanceRejetee(
                    '+21626581955',
                    $numeroOrdonnance,
                    $nomComplet
                );
                
                if ($smsEnvoye) {
                    $this->addFlash('success', 'Ordonnance rejetée. ' . count($traitements) . ' traitement(s) associé(s) ont été rejetés automatiquement. 📱 SMS envoyé au +21626581955.');
                } else {
                    $this->addFlash('success', 'Ordonnance rejetée. ' . count($traitements) . ' traitement(s) associé(s) ont été rejetés automatiquement. ⚠️ SMS non envoyé (erreur Twilio).');
                }
            } else {
                $this->addFlash('success', 'Ordonnance modifiée avec succès');
            }
            
            $entityManager->flush();

            return $this->redirectToRoute('admin_ordonnances');
        }

        return $this->render('Admin/ordonnances/form.html.twig', [
            'form' => $form->createView(),
            'ordonnance' => $ordonnance
        ]);
    }

    #[Route('/admin/ordonnance/{id}/analyser-note', name: 'admin_ordonnance_analyser_note', methods: ['POST'])]
    public function analyserNoteMedicale(
        Ordonnance $ordonnance,
        Request $request,
        \App\Service\IAAnalyseNoteMedicaleService $iaAnalyseService,
        EntityManagerInterface $entityManager
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer l'ID du produit si fourni
        $data = json_decode($request->getContent(), true);
        $produitId = $data['produit_id'] ?? null;
        
        $produit = null;
        if ($produitId) {
            $produit = $entityManager->getRepository(\App\Entity\Produit::class)->find($produitId);
        }
        
        // Analyser la note médicale avec l'IA
        $resultat = $iaAnalyseService->analyserNoteMedicale($ordonnance, $produit);
        
        return $this->json($resultat);
    }

    #[Route('/admin/ordonnance/{id}/delete', name: 'admin_ordonnance_delete', methods: ['POST'])]
    public function delete(Ordonnance $ordonnance, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Supprimer d'abord tous les traitements associés à cette ordonnance
        $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
        $traitements = $traitementRepository->findBy(['ordonnance' => $ordonnance]);
        
        foreach ($traitements as $traitement) {
            $entityManager->remove($traitement);
        }
        
        // Ensuite supprimer l'ordonnance
        $entityManager->remove($ordonnance);
        $entityManager->flush();
        
        $this->addFlash('success', 'Ordonnance et ' . count($traitements) . ' traitement(s) associé(s) supprimés avec succès');

        return $this->redirectToRoute('admin_ordonnances');
    }

    #[Route('/admin/ordonnance/{id}/signer', name: 'admin_ordonnance_signer', methods: ['POST'])]
    public function signerElectroniquement(
        Ordonnance $ordonnance,
        Request $request,
        \App\Service\DocuSignService $docuSignService,
        EntityManagerInterface $entityManager
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        try {
            $data = json_decode($request->getContent(), true);
            $signerEmail = $data['signer_email'] ?? '';
            $signerName = $data['signer_name'] ?? '';

            if (empty($signerEmail) || empty($signerName)) {
                return $this->json([
                    'success' => false,
                    'message' => 'Email et nom du signataire requis'
                ], 400);
            }

            // Créer l'enveloppe de signature
            $result = $docuSignService->createSignatureEnvelope($ordonnance, $signerEmail, $signerName);

            if ($result && $result['success']) {
                // Mettre à jour l'ordonnance
                $ordonnance->setSignatureElectronique(true);
                $ordonnance->setDocusignEnvelopeId($result['envelope_id']);
                $ordonnance->setDocusignStatus($result['status']);
                $ordonnance->setSignatureMedecin($signerName);
                
                // Si la signature est simulée (DocuSign non configuré), marquer comme signée immédiatement
                if (isset($result['simulated']) && $result['simulated']) {
                    $ordonnance->setSignatureDate(new \DateTime());
                    $ordonnance->setDocusignStatus('completed');
                }
                
                $entityManager->flush();

                return $this->json([
                    'success' => true,
                    'message' => isset($result['simulated']) 
                        ? 'Signature simulée avec succès (DocuSign non configuré)' 
                        : 'Demande de signature envoyée avec succès',
                    'envelope_id' => $result['envelope_id'],
                    'status' => $result['status'],
                    'simulated' => $result['simulated'] ?? false
                ]);
            }

            return $this->json([
                'success' => false,
                'message' => 'Erreur lors de la création de la demande de signature'
            ], 500);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'message' => 'Erreur: ' . $e->getMessage()
            ], 500);
        }
    }

    #[Route('/admin/ordonnance/{id}/signature-status', name: 'admin_ordonnance_signature_status', methods: ['GET'])]
    public function checkSignatureStatus(
        Ordonnance $ordonnance,
        \App\Service\DocuSignService $docuSignService,
        EntityManagerInterface $entityManager
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if (!$ordonnance->getDocusignEnvelopeId()) {
            return $this->json([
                'success' => false,
                'message' => 'Aucune signature en cours'
            ]);
        }

        $status = $docuSignService->checkEnvelopeStatus($ordonnance->getDocusignEnvelopeId());

        if ($status) {
            // Mettre à jour le statut
            $ordonnance->setDocusignStatus($status['status']);
            
            if ($status['status'] === 'completed' && !$ordonnance->getSignatureDate()) {
                $ordonnance->setSignatureDate($status['completed_date'] ?? new \DateTime());
            }
            
            $entityManager->flush();

            return $this->json([
                'success' => true,
                'status' => $status['status'],
                'signed' => $status['status'] === 'completed',
                'signature_date' => $ordonnance->getSignatureDate() ? $ordonnance->getSignatureDate()->format('d/m/Y H:i') : null
            ]);
        }

        return $this->json([
            'success' => false,
            'message' => 'Impossible de vérifier le statut'
        ]);
    }

    #[Route('/admin/ordonnance/{id}/signer-admin', name: 'admin_ordonnance_signer_admin', methods: ['POST'])]
    public function signerParAdmin(
        Ordonnance $ordonnance,
        Request $request,
        \App\Service\SignatureService $signatureService
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        try {
            $data = json_decode($request->getContent(), true);
            $nomMedecin = $data['nom_medecin'] ?? '';

            if (empty($nomMedecin)) {
                return $this->json([
                    'success' => false,
                    'message' => 'Le nom du médecin est requis'
                ], 400);
            }

            // Vérifier si déjà signé
            if ($ordonnance->getSignatureMedecin()) {
                return $this->json([
                    'success' => false,
                    'message' => 'Cette ordonnance a déjà été signée par un médecin'
                ], 400);
            }

            $result = $signatureService->signerParAdmin($ordonnance, $nomMedecin);
            
            return $this->json($result);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'message' => 'Erreur: ' . $e->getMessage()
            ], 500);
        }
    }
}
