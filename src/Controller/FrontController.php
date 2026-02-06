<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Entity\Stock;
use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Form\OrdonnanceFrontType;
use App\Form\TraitementFrontType;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Dompdf\Dompdf;
use Dompdf\Options;

class FrontController extends AbstractController
{
    #[Route('/', name: 'app_home', methods: ['GET'])]
    public function home(): Response
    {
        return $this->render('front/home.html.twig');
    }

    #[Route('/products', name: 'app_products', methods: ['GET'])]
    public function products(): Response
    {
        return $this->render('front/products.html.twig');
    }

    #[Route('/about', name: 'app_about', methods: ['GET'])]
    public function about(): Response
    {
        return $this->render('front/about.html.twig');
    }

    #[Route('/contact', name: 'app_contact', methods: ['GET'])]
    public function contact(): Response
    {
        return $this->render('front/contact.html.twig');
    }

    #[Route('/ordonnance', name: 'app_ordonnance', methods: ['GET', 'POST'])]
    public function ordonnance(
        Request $request, 
        EntityManagerInterface $entityManager
    ): Response
    {
        // Vérifier si l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à cette page');
            return $this->redirectToRoute('app_login');
        }

        // Formulaire Ordonnance
        $ordonnance = new Ordonnance();
        $ordonnance->setStatut('en attente');
        $ordonnance->setUtilisateur($this->getUser());
        
        // Générer un numéro d'ordonnance automatique
        $numeroOrdonnance = 'ORD-' . date('Y') . '-' . str_pad(rand(1, 9999), 4, '0', STR_PAD_LEFT);
        $ordonnance->setNumeroOrdonnance($numeroOrdonnance);
        
        // Définir la date d'expiration par défaut (3 mois après la date d'ordonnance)
        $dateExpiration = new \DateTime();
        $dateExpiration->modify('+3 months');
        $ordonnance->setDateExpiration($dateExpiration);
        
        $formOrdonnance = $this->createForm(OrdonnanceFrontType::class, $ordonnance);
        $formOrdonnance->handleRequest($request);
        
        if ($formOrdonnance->isSubmitted() && $formOrdonnance->isValid()) {
            // Validation supplémentaire côté serveur
            $dateOrdonnance = $ordonnance->getDateOrdonnance();
            $dateExpiration = $ordonnance->getDateExpiration();
            
            if ($dateExpiration <= $dateOrdonnance) {
                $this->addFlash('error', 'La date d\'expiration doit être postérieure à la date de l\'ordonnance');
            } else {
                $entityManager->persist($ordonnance);
                $entityManager->flush();
                
                $this->addFlash('success', 'Votre ordonnance a été envoyée avec succès ! Un pharmacien va la vérifier.');
                return $this->redirectToRoute('app_ordonnance');
            }
        }

        return $this->render('front/ordonnance.html.twig', [
            'form' => $formOrdonnance->createView()
        ]);
    }

    #[Route('/demande_de_traitement', name: 'app_demande_traitement', methods: ['GET', 'POST'])]
    public function demandeTraitement(
        Request $request, 
        EntityManagerInterface $entityManager
    ): Response
    {
        // Vérifier si l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à cette page');
            return $this->redirectToRoute('app_login');
        }

        // Créer une nouvelle demande de traitement
        $traitement = new Traitement();
        $traitement->setStatus('en attente');
        $traitement->setUtilisateur($this->getUser());
        
        $form = $this->createForm(TraitementFrontType::class, $traitement, [
            'user' => $this->getUser()
        ]);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            // Vérifier que l'ordonnance est validée
            $ordonnance = $traitement->getOrdonnance();
            if ($ordonnance->getStatut() !== 'validé') {
                $this->addFlash('error', 'Vous ne pouvez créer une demande de traitement que pour une ordonnance validée');
            } else {
                // Validation supplémentaire côté serveur
                $dateDebut = $traitement->getDateDebut();
                $dateFin = $traitement->getDateFin();
                $dureeJours = $traitement->getDureeJours();
                
                if ($dateFin <= $dateDebut) {
                    $this->addFlash('error', 'La date de fin doit être postérieure à la date de début');
                } else {
                    // Vérifier la cohérence entre la durée et les dates
                    $diff = $dateDebut->diff($dateFin);
                    $joursCalcules = $diff->days;
                    
                    if (abs($joursCalcules - $dureeJours) > 1) {
                        $this->addFlash('error', 'La durée en jours ne correspond pas à la période entre la date de début et la date de fin');
                    } else {
                        // Forcer le statut à "en attente"
                        $traitement->setStatus('en attente');
                        $traitement->setUtilisateur($this->getUser());
                        
                        $entityManager->persist($traitement);
                        $entityManager->flush();
                        
                        $this->addFlash('success', 'Votre demande de traitement a été envoyée avec succès ! Un administrateur va la vérifier.');
                        return $this->redirectToRoute('app_mes_traitements');
                    }
                }
            }
        }

        return $this->render('front/demande_traitement.html.twig', [
            'form' => $form->createView()
        ]);
    }

    #[Route('/mes-traitements', name: 'app_mes_traitements', methods: ['GET'])]
    public function mesTraitements(
        Request $request,
        TraitementRepository $traitementRepository,
        OrdonnanceRepository $ordonnanceRepository
    ): Response
    {
        // Vérifier si l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à cette page');
            return $this->redirectToRoute('app_login');
        }

        // Récupérer le terme de recherche
        $searchTerm = $request->query->get('search', '');

        // Récupérer les traitements du client
        if ($searchTerm) {
            // Recherche dans les traitements
            $traitements = $traitementRepository->createQueryBuilder('t')
                ->leftJoin('t.ordonnance', 'o')
                ->where('t.utilisateur = :user')
                ->andWhere('o.numeroOrdonnance LIKE :search OR t.dosage LIKE :search OR t.frequence LIKE :search OR t.notes LIKE :search OR t.status LIKE :search')
                ->setParameter('user', $this->getUser())
                ->setParameter('search', '%' . $searchTerm . '%')
                ->orderBy('t.id', 'DESC')
                ->getQuery()
                ->getResult();
        } else {
            $traitements = $traitementRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['id' => 'DESC']
            );
        }

        // Récupérer les ordonnances validées du client
        if ($searchTerm) {
            // Recherche dans les ordonnances
            $ordonnancesValidees = $ordonnanceRepository->createQueryBuilder('o')
                ->where('o.utilisateur = :user')
                ->andWhere('o.statut = :statut')
                ->andWhere('o.numeroOrdonnance LIKE :search OR o.noteMedical LIKE :search')
                ->setParameter('user', $this->getUser())
                ->setParameter('statut', 'validé')
                ->setParameter('search', '%' . $searchTerm . '%')
                ->orderBy('o.dateOrdonnance', 'DESC')
                ->getQuery()
                ->getResult();
        } else {
            $ordonnancesValidees = $ordonnanceRepository->findBy(
                ['utilisateur' => $this->getUser(), 'statut' => 'validé'],
                ['dateOrdonnance' => 'DESC']
            );
        }

        return $this->render('front/mes_traitements.html.twig', [
            'traitements' => $traitements,
            'ordonnancesValidees' => $ordonnancesValidees,
            'searchTerm' => $searchTerm
        ]);
    }

    #[Route('/ordonnance/{id}/pdf', name: 'app_ordonnance_pdf', methods: ['GET'])]
    public function ordonnancePdf(Ordonnance $ordonnance): Response
    {
        // Vérifier que l'ordonnance appartient à l'utilisateur connecté
        if (!$this->getUser() || $ordonnance->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à cette ordonnance');
        }

        // Générer le HTML pour le PDF
        $html = $this->renderView('front/pdf/ordonnance_pdf.html.twig', [
            'ordonnance' => $ordonnance
        ]);

        // Configurer Dompdf
        $options = new Options();
        $options->set('defaultFont', 'Arial');
        $options->set('isRemoteEnabled', true);
        $options->set('isHtml5ParserEnabled', true);
        
        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        // Générer le nom du fichier
        $filename = 'Ordonnance_' . $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf';

        // Retourner le PDF en téléchargement
        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"'
        ]);
    }

    #[Route('/traitement/{id}/pdf', name: 'app_traitement_pdf', methods: ['GET'])]
    public function traitementPdf(Traitement $traitement): Response
    {
        // Vérifier que le traitement appartient à l'utilisateur connecté
        if (!$this->getUser() || $traitement->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à ce traitement');
        }

        // Générer le HTML pour le PDF
        $html = $this->renderView('front/pdf/traitement_pdf.html.twig', [
            'traitement' => $traitement
        ]);

        // Configurer Dompdf
        $options = new Options();
        $options->set('defaultFont', 'Arial');
        $options->set('isRemoteEnabled', true);
        $options->set('isHtml5ParserEnabled', true);
        
        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        // Générer le nom du fichier
        $filename = 'Traitement_' . $traitement->getOrdonnance()->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf';

        // Retourner le PDF en téléchargement
        return new Response($dompdf->output(), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"'
        ]);
    }

    #[Route('/guide-sante', name: 'app_guide_sante', methods: ['GET'])]
    public function guideSante(): Response
    {
        return $this->render('front/guide_sante.html.twig');
    }

    #[Route('/commande', name: 'app_commande', methods: ['GET'])]
    public function commande(): Response
    {
        return $this->render('front/commande.html.twig');
    }

    #[Route('/depots', name: 'front_depots')]
    public function depots(DepotRepository $depotRepository): Response
    {
        $depots = $depotRepository->findAll();
        
        return $this->render('front/depots.html.twig', [
            'depots' => $depots
        ]);
    }

    #[Route('/stocks', name: 'front_stocks')]
    public function stocks(StockRepository $stockRepository): Response
    {
        $stocks = $stockRepository->findAll();
        
        return $this->render('front/stocks.html.twig', [
            'stocks' => $stocks
        ]);
    }
}
