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
        $filterDate = $request->query->get('date', '');
        $filterClient = $request->query->get('client', '');
        $filterStatut = $request->query->get('statut', '');
        
        // Construire la requête avec filtres
        $queryBuilder = $ordonnanceRepository->createQueryBuilder('o')
            ->leftJoin('o.utilisateur', 'u')
            ->orderBy('o.dateOrdonnance', 'DESC');
        
        // Filtre par date
        if ($filterDate) {
            $queryBuilder->andWhere('o.dateOrdonnance = :date')
                ->setParameter('date', new \DateTime($filterDate));
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
            'filterDate' => $filterDate,
            'filterClient' => $filterClient,
            'filterStatut' => $filterStatut
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
