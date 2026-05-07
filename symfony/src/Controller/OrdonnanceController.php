<?php

namespace App\Controller;

use App\Entity\Ordonnance;
use App\Form\OrdonnanceType;
use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class OrdonnanceController extends AbstractController
{
    #[Route('/admin/ordonnances', name: 'admin_ordonnances')]
    public function index(Request $request, OrdonnanceRepository $ordonnanceRepository, EntityManagerInterface $entityManager): Response
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
                ->setParameter('dateOrdonnance', new \DateTime($filterDateOrdonnance));
        }
        
        // Filtre par date d'expiration
        if ($filterDateExpiration) {
            $queryBuilder->andWhere('o.dateExpiration = :dateExpiration')
                ->setParameter('dateExpiration', new \DateTime($filterDateExpiration));
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
        
        $ordonnances = $queryBuilder->getQuery()->getResult();
        
        // Récupérer les traitements pour chaque ordonnance
        $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
        $traitementsByOrdonnance = [];
        foreach ($ordonnances as $ordonnance) {
            $traitementsByOrdonnance[$ordonnance->getId()] = $traitementRepository->findBy(
                ['ordonnance' => $ordonnance],
                ['id' => 'ASC']
            );
        }
        
        return $this->render('Admin/ordonnances/index.html.twig', [
            'ordonnances' => $ordonnances,
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

    #[Route('/admin/ordonnances/search', name: 'admin_ordonnances_search', methods: ['GET'])]
    public function search(Request $request, OrdonnanceRepository $ordonnanceRepository, EntityManagerInterface $entityManager): Response
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
                ->setParameter('dateOrdonnance', new \DateTime($filterDateOrdonnance));
        }
        
        // Filtre par date d'expiration
        if ($filterDateExpiration) {
            $queryBuilder->andWhere('o.dateExpiration = :dateExpiration')
                ->setParameter('dateExpiration', new \DateTime($filterDateExpiration));
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
        
        $ordonnances = $queryBuilder->getQuery()->getResult();
        
        // Récupérer les traitements pour chaque ordonnance
        $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
        $traitementsByOrdonnance = [];
        foreach ($ordonnances as $ordonnance) {
            $traitementsByOrdonnance[$ordonnance->getId()] = $traitementRepository->findBy(
                ['ordonnance' => $ordonnance],
                ['id' => 'ASC']
            );
        }
        
        // Rendre le HTML partiel
        $html = $this->renderView('Admin/ordonnances/_table.html.twig', [
            'ordonnances' => $ordonnances,
            'traitementsByOrdonnance' => $traitementsByOrdonnance
        ]);
        
        return $this->json([
            'html' => $html,
            'count' => count($ordonnances)
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
    public function edit(Ordonnance $ordonnance, Request $request, EntityManagerInterface $entityManager): Response
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
                
                $this->addFlash('success', 'Ordonnance validée avec succès. ' . count($traitements) . ' traitement(s) associé(s) ont été validés automatiquement.');
            } elseif ($ancienStatut !== 'rejeté' && $nouveauStatut === 'rejeté') {
                // Si l'ordonnance est rejetée, rejeter automatiquement tous les traitements associés
                $traitementRepository = $entityManager->getRepository(\App\Entity\Traitement::class);
                $traitements = $traitementRepository->findBy(['ordonnance' => $ordonnance]);
                
                foreach ($traitements as $traitement) {
                    $traitement->setStatus('rejeté');
                    $entityManager->persist($traitement);
                }
                
                $this->addFlash('success', 'Ordonnance rejetée. ' . count($traitements) . ' traitement(s) associé(s) ont été rejetés automatiquement.');
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
}
