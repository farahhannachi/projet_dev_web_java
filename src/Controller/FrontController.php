<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Entity\Stock;
use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Entity\Produit;
use App\Entity\Commande;
use App\Form\OrdonnanceFrontType;
use App\Form\TraitementFrontType;
use App\Form\CommandeType;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use App\Service\MailerService;
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

    #[Route('/contact', name: 'app_contact', methods: ['GET', 'POST'])]
    public function contact(Request $request, EntityManagerInterface $entityManager, \App\Repository\QuestionRepository $questionRepository, MailerService $mailerService): Response
    {
        $editMode = false;
        $editQuestionId = $request->query->get('edit');
        
        // Mode édition : charger la question existante
        if ($editQuestionId && $this->getUser()) {
            $question = $questionRepository->find($editQuestionId);
            
            // Vérifier que la question existe et appartient à l'utilisateur
            if ($question && $question->getUtilisateur() === $this->getUser()) {
                $editMode = true;
            } else {
                $this->addFlash('error', 'Question introuvable ou accès non autorisé.');
                return $this->redirectToRoute('app_contact');
            }
        } else {
            // Mode création : nouvelle question
            $question = new \App\Entity\Question();
            $question->setStatut('ouvert');
            
            if ($this->getUser()) {
                $question->setUtilisateur($this->getUser());
            }
        }
        
        $form = $this->createForm(\App\Form\QuestionType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted()) {
            // Si l'utilisateur n'est pas connecté, bloquer la soumission
            if (!$this->getUser()) {
                $this->addFlash('error', 'Vous devez être connecté pour envoyer un ticket.');
                return $this->redirectToRoute('app_login');
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            // L'utilisateur et le statut sont déjà définis au-dessus
            
            // Gérer le fichier uploadé
            $fichier = $form->get('fichier')->getData();
            if ($fichier) {
                // Récupérer les infos du fichier AVANT le déplacement
                $originalName = $fichier->getClientOriginalName();
                $mimeType = $fichier->getMimeType();
                $fileSize = $fichier->getSize();
                
                $slugger = new \Symfony\Component\String\Slugger\AsciiSlugger();
                $originalFilename = pathinfo($originalName, PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename.'-'.uniqid().'.'.$fichier->guessExtension();

                try {
                    $uploadDir = $this->getParameter('kernel.project_dir').'/public/uploads/questions';
                    if (!is_dir($uploadDir)) {
                        mkdir($uploadDir, 0777, true);
                    }
                    
                    // Déplacer le fichier
                    $fichier->move($uploadDir, $newFilename);
                    
                    // Utiliser les valeurs sauvegardées AVANT le déplacement
                    $question->setFileName($originalName);
                    $question->setFilePath('/uploads/questions/'.$newFilename);
                    $question->setFileType($mimeType);
                    $question->setFileSize($fileSize);
                } catch (\Symfony\Component\HttpFoundation\File\Exception\FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l\'upload du fichier');
                }
            }

            $entityManager->persist($question);
            $entityManager->flush();

            if ($editMode) {
                $this->addFlash('success', 'Votre ticket a été modifié avec succès !');
            } else {
                // Envoyer l'email de confirmation au client
                $mailerService->sendTicketCreatedEmail($question);
                $this->addFlash('success', 'Votre message a été envoyé avec succès ! Un email de confirmation vous a été envoyé.');
            }

            // Rediriger vers la page contact pour voir le ticket dans "Mes Tickets"
            return $this->redirectToRoute('app_contact');
        }

        // Récupérer les questions de l'utilisateur connecté
        $mesQuestions = [];
        if ($this->getUser()) {
            $mesQuestions = $questionRepository->findByUtilisateur($this->getUser()->getId());
        }

        return $this->render('front/contact.html.twig', [
            'form' => $form,
            'mesQuestions' => $mesQuestions,
            'editMode' => $editMode,
            'editQuestion' => $editMode ? $question : null,
        ]);
    }

    #[Route('/ordonnance', name: 'app_ordonnance', methods: ['GET', 'POST'])]
    public function ordonnance(
        Request $request, 
        EntityManagerInterface $entityManager,
        TraitementRepository $traitementRepository
    ): Response
    {
        // Vérifier si l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à cette page');
            return $this->redirectToRoute('app_login');
        }

        // Récupérer les produits sélectionnés depuis l'URL
        $produitsIds = $request->query->get('produits');
        $produitsSelectionnes = [];
        
        if ($produitsIds) {
            $idsArray = explode(',', $produitsIds);
            foreach ($idsArray as $id) {
                $produit = $entityManager->getRepository(Produit::class)->find($id);
                if ($produit) {
                    $produitsSelectionnes[] = $produit;
                }
            }
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
                
                // Créer un traitement pour chaque produit sélectionné
                if (!empty($produitsSelectionnes)) {
                    foreach ($produitsSelectionnes as $produit) {
                        $traitement = new Traitement();
                        $traitement->setOrdonnance($ordonnance);
                        $traitement->setUtilisateur($this->getUser());
                        $traitement->setProduit($produit);
                        $traitement->setStatus('en attente');
                        $traitement->setNotes('Demande de traitement pour: ' . $produit->getNom());
                        
                        $entityManager->persist($traitement);
                    }
                    $entityManager->flush();
                }
                
                $this->addFlash('success', 'Votre ordonnance a été envoyée avec succès ! Un pharmacien va la vérifier.');
                return $this->redirectToRoute('app_mes_traitements');
            }
        }

        return $this->render('front/ordonnance.html.twig', [
            'form' => $formOrdonnance->createView(),
            'produitsSelectionnes' => $produitsSelectionnes
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

        // Récupérer tous les produits disponibles
        $produits = $entityManager->getRepository(Produit::class)->findBy(
            ['statut' => 'disponible'],
            ['nom' => 'ASC']
        );

        // Traiter la soumission du formulaire
        if ($request->isMethod('POST')) {
            $produitsSelectionnes = $request->request->all('produits');
            
            if (empty($produitsSelectionnes)) {
                $this->addFlash('error', 'Veuillez sélectionner au moins un produit');
            } else {
                // Rediriger vers la page ordonnance avec les produits sélectionnés
                return $this->redirectToRoute('app_ordonnance', [
                    'produits' => implode(',', $produitsSelectionnes)
                ]);
            }
        }

        return $this->render('front/demande_traitement.html.twig', [
            'produits' => $produits
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

        // Récupérer TOUTES les ordonnances du client (validées, en attente, rejetées)
        if ($searchTerm) {
            // Recherche dans les ordonnances
            $ordonnances = $ordonnanceRepository->createQueryBuilder('o')
                ->where('o.utilisateur = :user')
                ->andWhere('o.numeroOrdonnance LIKE :search OR o.noteMedical LIKE :search OR o.statut LIKE :search')
                ->setParameter('user', $this->getUser())
                ->setParameter('search', '%' . $searchTerm . '%')
                ->orderBy('o.dateOrdonnance', 'DESC')
                ->getQuery()
                ->getResult();
        } else {
            $ordonnances = $ordonnanceRepository->findBy(
                ['utilisateur' => $this->getUser()],
                ['dateOrdonnance' => 'DESC']
            );
        }

        return $this->render('front/mes_traitements.html.twig', [
            'traitements' => $traitements,
            'ordonnances' => $ordonnances,
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

    #[Route('/ordonnance/{id}/complete-pdf', name: 'app_ordonnance_complete_pdf', methods: ['GET'])]
    public function ordonnanceCompletePdf(Ordonnance $ordonnance): Response
    {
        // Vérifier que l'ordonnance appartient à l'utilisateur connecté
        if (!$this->getUser() || $ordonnance->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à cette ordonnance');
        }

        // Générer le HTML pour le PDF avec ordonnance et traitements
        $html = $this->renderView('front/pdf/ordonnance_complete_pdf.html.twig', [
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
        $filename = 'Ordonnance_Complete_' . $ordonnance->getNumeroOrdonnance() . '_' . date('Y-m-d') . '.pdf';

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

    #[Route('/commande', name: 'app_commande', methods: ['GET', 'POST'])]
    public function commande(Request $request, EntityManagerInterface $entityManager): Response
    {
        $commande = new Commande();
        $commande->setDateCommande(new \DateTime());
        $commande->setStatut('en attente');
        $commande->setTotal(0); // À calculer selon les produits
        
        $form = $this->createForm(CommandeType::class, $commande);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($commande);
            $entityManager->flush();
            
            $this->addFlash('success', 'Votre commande a été enregistrée avec succès ! Nous vous contacterons bientôt.');
            return $this->redirectToRoute('app_commande');
        }
        
        return $this->render('front/commande.html.twig', [
            'form' => $form->createView()
        ]);
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
