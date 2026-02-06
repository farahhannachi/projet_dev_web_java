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
        $filterDate = $request->query->get('date', '');
        $filterClient = $request->query->get('client', '');
        $filterStatut = $request->query->get('statut', '');
        
        // Construire la requête avec filtres
        $queryBuilder = $traitementRepository->createQueryBuilder('t')
            ->leftJoin('t.utilisateur', 'u')
            ->leftJoin('t.ordonnance', 'o')
            ->orderBy('t.id', 'DESC');
        
        // Filtre par date (date de début du traitement)
        if ($filterDate) {
            $queryBuilder->andWhere('t.dateDebut = :date')
                ->setParameter('date', new \DateTime($filterDate));
        }
        
        // Filtre par client (nom, prénom ou email)
        if ($filterClient) {
            $queryBuilder->andWhere('u.nom LIKE :client OR u.prenom LIKE :client OR u.email LIKE :client')
                ->setParameter('client', '%' . $filterClient . '%');
        }
        
        // Filtre par statut
        if ($filterStatut) {
            $queryBuilder->andWhere('t.status = :statut')
                ->setParameter('statut', $filterStatut);
        }
        
        $traitements = $queryBuilder->getQuery()->getResult();
        
        return $this->render('Admin/traitements/index.html.twig', [
            'traitements' => $traitements,
            'filterDate' => $filterDate,
            'filterClient' => $filterClient,
            'filterStatut' => $filterStatut
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
