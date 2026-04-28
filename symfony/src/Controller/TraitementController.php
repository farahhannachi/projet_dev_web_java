<?php

namespace App\Controller;

use App\Entity\Traitement;
use App\Form\TraitementType;
use App\Repository\TraitementRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class TraitementController extends AbstractController
{
    #[Route('/admin/traitements', name: 'admin_traitements')]
    public function index(Request $request, TraitementRepository $traitementRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer les paramètres de filtrage
        $filterDateDebut = $request->query->get('date_debut', '');
        $filterDateFin = $request->query->get('date_fin', '');
        $filterClient = $request->query->get('client', '');
        $filterProduit = $request->query->get('produit', '');
        $filterStatut = $request->query->get('statut', '');
        $sortBy = $request->query->get('sort', 'id_desc');
        
        // Construire la requête avec filtres
        $queryBuilder = $traitementRepository->createQueryBuilder('t')
            ->leftJoin('t.utilisateur', 'u')
            ->leftJoin('t.ordonnance', 'o')
            ->leftJoin('t.produit', 'p');
        
        // Filtre par date de début
        if ($filterDateDebut) {
            $queryBuilder->andWhere('t.dateDebut = :dateDebut')
                ->setParameter('dateDebut', new \DateTime($filterDateDebut));
        }
        
        // Filtre par date de fin
        if ($filterDateFin) {
            $queryBuilder->andWhere('t.dateFin = :dateFin')
                ->setParameter('dateFin', new \DateTime($filterDateFin));
        }
        
        // Filtre par client (nom, prénom ou email)
        if ($filterClient) {
            $queryBuilder->andWhere('u.nom LIKE :client OR u.prenom LIKE :client OR u.email LIKE :client')
                ->setParameter('client', '%' . $filterClient . '%');
        }
        
        // Filtre par produit (nom du produit)
        if ($filterProduit) {
            $queryBuilder->andWhere('p.nom LIKE :produit')
                ->setParameter('produit', '%' . $filterProduit . '%');
        }
        
        // Filtre par statut
        if ($filterStatut) {
            $queryBuilder->andWhere('t.status = :statut')
                ->setParameter('statut', $filterStatut);
        }
        
        // Tri
        switch ($sortBy) {
            case 'date_debut_asc':
                $queryBuilder->orderBy('t.dateDebut', 'ASC');
                break;
            case 'date_debut_desc':
                $queryBuilder->orderBy('t.dateDebut', 'DESC');
                break;
            case 'date_fin_asc':
                $queryBuilder->orderBy('t.dateFin', 'ASC');
                break;
            case 'date_fin_desc':
                $queryBuilder->orderBy('t.dateFin', 'DESC');
                break;
            case 'client_asc':
                $queryBuilder->orderBy('u.nom', 'ASC');
                break;
            case 'client_desc':
                $queryBuilder->orderBy('u.nom', 'DESC');
                break;
            case 'produit_asc':
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
            case 'produit_desc':
                $queryBuilder->orderBy('p.nom', 'DESC');
                break;
            case 'id_asc':
                $queryBuilder->orderBy('t.id', 'ASC');
                break;
            case 'id_desc':
                $queryBuilder->orderBy('t.id', 'DESC');
                break;
            default:
                $queryBuilder->orderBy('t.id', 'DESC');
        }
        
        $traitements = $queryBuilder->getQuery()->getResult();
        
        return $this->render('Admin/traitements/index.html.twig', [
            'traitements' => $traitements,
            'filterDateDebut' => $filterDateDebut,
            'filterDateFin' => $filterDateFin,
            'filterClient' => $filterClient,
            'filterProduit' => $filterProduit,
            'filterStatut' => $filterStatut,
            'sortBy' => $sortBy
        ]);
    }

    #[Route('/admin/traitements/stats', name: 'admin_traitements_stats', methods: ['GET'])]
    public function stats(TraitementRepository $traitementRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer tous les traitements
        $traitements = $traitementRepository->findAll();
        $total = count($traitements);
        
        // Compter par statut
        $enAttente = 0;
        $valide = 0;
        $rejete = 0;
        $actif = 0;
        $termine = 0;
        $suspendu = 0;
        $annule = 0;
        
        foreach ($traitements as $traitement) {
            switch ($traitement->getStatus()) {
                case 'en attente':
                    $enAttente++;
                    break;
                case 'validé':
                    $valide++;
                    break;
                case 'rejeté':
                    $rejete++;
                    break;
                case 'actif':
                    $actif++;
                    break;
                case 'terminé':
                    $termine++;
                    break;
                case 'suspendu':
                    $suspendu++;
                    break;
                case 'annulé':
                    $annule++;
                    break;
            }
        }
        
        // Calculer la durée moyenne
        $dureeTotale = 0;
        $count = 0;
        foreach ($traitements as $traitement) {
            if ($traitement->getDureeJours()) {
                $dureeTotale += $traitement->getDureeJours();
                $count++;
            }
        }
        $dureeMoyenne = $count > 0 ? round($dureeTotale / $count, 1) : 0;
        
        return $this->json([
            'total' => $total,
            'enAttente' => $enAttente,
            'valide' => $valide,
            'rejete' => $rejete,
            'actif' => $actif,
            'termine' => $termine,
            'suspendu' => $suspendu,
            'annule' => $annule,
            'dureeMoyenne' => $dureeMoyenne,
            'pourcentageActif' => $total > 0 ? round(($actif / $total) * 100, 1) : 0,
            'pourcentageTermine' => $total > 0 ? round(($termine / $total) * 100, 1) : 0
        ]);
    }

    #[Route('/admin/traitements/search', name: 'admin_traitements_search', methods: ['GET'])]
    public function search(Request $request, TraitementRepository $traitementRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Récupérer les paramètres de filtrage
        $filterDateDebut = $request->query->get('date_debut', '');
        $filterDateFin = $request->query->get('date_fin', '');
        $filterClient = $request->query->get('client', '');
        $filterProduit = $request->query->get('produit', '');
        $filterStatut = $request->query->get('statut', '');
        $sortBy = $request->query->get('sort', 'id_desc');
        
        // Construire la requête avec filtres
        $queryBuilder = $traitementRepository->createQueryBuilder('t')
            ->leftJoin('t.utilisateur', 'u')
            ->leftJoin('t.ordonnance', 'o')
            ->leftJoin('t.produit', 'p');
        
        // Filtre par date de début
        if ($filterDateDebut) {
            $queryBuilder->andWhere('t.dateDebut = :dateDebut')
                ->setParameter('dateDebut', new \DateTime($filterDateDebut));
        }
        
        // Filtre par date de fin
        if ($filterDateFin) {
            $queryBuilder->andWhere('t.dateFin = :dateFin')
                ->setParameter('dateFin', new \DateTime($filterDateFin));
        }
        
        // Filtre par client (nom, prénom ou email)
        if ($filterClient) {
            $queryBuilder->andWhere('u.nom LIKE :client OR u.prenom LIKE :client OR u.email LIKE :client')
                ->setParameter('client', '%' . $filterClient . '%');
        }
        
        // Filtre par produit (nom du produit)
        if ($filterProduit) {
            $queryBuilder->andWhere('p.nom LIKE :produit')
                ->setParameter('produit', '%' . $filterProduit . '%');
        }
        
        // Filtre par statut
        if ($filterStatut) {
            $queryBuilder->andWhere('t.status = :statut')
                ->setParameter('statut', $filterStatut);
        }
        
        // Tri
        switch ($sortBy) {
            case 'date_debut_asc':
                $queryBuilder->orderBy('t.dateDebut', 'ASC');
                break;
            case 'date_debut_desc':
                $queryBuilder->orderBy('t.dateDebut', 'DESC');
                break;
            case 'date_fin_asc':
                $queryBuilder->orderBy('t.dateFin', 'ASC');
                break;
            case 'date_fin_desc':
                $queryBuilder->orderBy('t.dateFin', 'DESC');
                break;
            case 'client_asc':
                $queryBuilder->orderBy('u.nom', 'ASC');
                break;
            case 'client_desc':
                $queryBuilder->orderBy('u.nom', 'DESC');
                break;
            case 'produit_asc':
                $queryBuilder->orderBy('p.nom', 'ASC');
                break;
            case 'produit_desc':
                $queryBuilder->orderBy('p.nom', 'DESC');
                break;
            case 'id_asc':
                $queryBuilder->orderBy('t.id', 'ASC');
                break;
            case 'id_desc':
                $queryBuilder->orderBy('t.id', 'DESC');
                break;
            default:
                $queryBuilder->orderBy('t.id', 'DESC');
        }
        
        $traitements = $queryBuilder->getQuery()->getResult();
        
        // Rendre le HTML partiel
        $html = $this->renderView('Admin/traitements/_table.html.twig', [
            'traitements' => $traitements
        ]);
        
        return $this->json([
            'html' => $html,
            'count' => count($traitements)
        ]);
    }

    #[Route('/admin/traitement/new', name: 'admin_traitement_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $traitement = new Traitement();
        $traitement->setStatus('en attente');
        
        // Définir des valeurs par défaut pour les dates
        $traitement->setDateDebut(new \DateTime());
        $dateFin = new \DateTime();
        $dateFin->modify('+7 days');
        $traitement->setDateFin($dateFin);
        $traitement->setDureeJours(7);
        
        $form = $this->createForm(TraitementType::class, $traitement, [
            'is_edit' => false
        ]);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Forcer le statut à "en attente" lors de la création
            $traitement->setStatus('en attente');
            
            // Validation supplémentaire côté serveur
            $dateDebut = $traitement->getDateDebut();
            $dateFin = $traitement->getDateFin();
            $dureeJours = $traitement->getDureeJours();
            
            if ($dateFin <= $dateDebut) {
                $this->addFlash('error', 'La date de fin doit être postérieure à la date de début');
                return $this->render('Admin/traitements/form.html.twig', [
                    'form' => $form->createView(),
                    'traitement' => null
                ]);
            }
            
            // Vérifier que la durée correspond aux dates
            $interval = $dateDebut->diff($dateFin);
            $joursCalcules = $interval->days;
            
            if (abs($joursCalcules - $dureeJours) > 1) {
                $this->addFlash('error', 'La durée en jours ne correspond pas aux dates sélectionnées');
                return $this->render('Admin/traitements/form.html.twig', [
                    'form' => $form->createView(),
                    'traitement' => null
                ]);
            }
            
            $entityManager->persist($traitement);
            $entityManager->flush();
            
            $this->addFlash('success', 'Traitement créé avec succès');
            return $this->redirectToRoute('admin_traitements');
        }

        return $this->render('Admin/traitements/form.html.twig', [
            'form' => $form->createView(),
            'traitement' => null
        ]);
    }

    #[Route('/admin/traitement/{id}/edit', name: 'admin_traitement_edit', methods: ['GET', 'POST'])]
    public function edit(Traitement $traitement, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(TraitementType::class, $traitement, [
            'is_edit' => true
        ]);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Validation supplémentaire côté serveur
            $dateDebut = $traitement->getDateDebut();
            $dateFin = $traitement->getDateFin();
            $dureeJours = $traitement->getDureeJours();
            
            if ($dateFin <= $dateDebut) {
                $this->addFlash('error', 'La date de fin doit être postérieure à la date de début');
                return $this->render('Admin/traitements/form.html.twig', [
                    'form' => $form->createView(),
                    'traitement' => $traitement
                ]);
            }
            
            // Vérifier que la durée correspond aux dates
            $interval = $dateDebut->diff($dateFin);
            $joursCalcules = $interval->days;
            
            if (abs($joursCalcules - $dureeJours) > 1) {
                $this->addFlash('error', 'La durée en jours ne correspond pas aux dates sélectionnées');
                return $this->render('Admin/traitements/form.html.twig', [
                    'form' => $form->createView(),
                    'traitement' => $traitement
                ]);
            }
            
            $entityManager->flush();
            $this->addFlash('success', 'Traitement modifié avec succès');

            return $this->redirectToRoute('admin_traitements');
        }

        return $this->render('Admin/traitements/form.html.twig', [
            'form' => $form->createView(),
            'traitement' => $traitement
        ]);
    }

    #[Route('/admin/traitement/{id}/delete', name: 'admin_traitement_delete', methods: ['POST'])]
    public function delete(Traitement $traitement, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($traitement);
        $entityManager->flush();
        $this->addFlash('success', 'Traitement supprimé avec succès');

        return $this->redirectToRoute('admin_traitements');
    }
}
