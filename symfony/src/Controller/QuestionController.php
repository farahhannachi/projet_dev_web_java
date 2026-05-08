<?php

namespace App\Controller;

use App\Entity\Question;
use App\Entity\ResponseQuestion;
use App\Form\QuestionType;
use App\Form\ResponseQuestionType;
use App\Repository\QuestionRepository;
use App\Repository\ResponseQuestionRepository;
use App\Service\MailerService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\Security\Csrf\CsrfToken;
use Symfony\Component\Security\Csrf\CsrfTokenManagerInterface;

#[Route('/question')]
class QuestionController extends AbstractController
{
    #[Route('/', name: 'app_question_index', methods: ['GET'])]
    public function index(Request $request, QuestionRepository $questionRepository, ResponseQuestionRepository $responseRepository): Response
    {
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à vos tickets.');
            return $this->redirectToRoute('app_login');
        }

        /** @var \App\Entity\Utilisateur $user */
        $user = $this->getUser();
        
        // Récupérer les paramètres de filtre/tri/recherche
        $statut = $request->query->get('statut');
        $priorite = $request->query->get('priorite');
        $type = $request->query->get('type');
        $search = $request->query->get('search');
        $sort = $request->query->get('sort', 'createdAt');
        $order = $request->query->get('order', 'DESC');
        
        if ($this->isGranted('ROLE_ADMIN')) {
            // Un admin voit toutes les questions avec filtres
            $questions = $questionRepository->findWithFilters($statut, $priorite, $type, $search, $sort, $order);
            $template = 'Admin/tickets.html.twig';
            $unreadByTicket = [];
        } else {
            // Un client voit seulement ses questions
            $questions = $questionRepository->findByUtilisateur($user->getId());
            $template = 'front/tickets.html.twig';
            
            // Calculer le nombre de réponses non lues pour chaque ticket
            $unreadByTicket = [];
            foreach ($questions as $question) {
                $unreadCount = 0;
                foreach ($question->getReponses() as $reponse) {
                    if ($reponse->getAuteurType() === 'agent' && !$reponse->isLuParClient()) {
                        $unreadCount++;
                    }
                }
                $unreadByTicket[$question->getId()] = $unreadCount;
            }
        }

        return $this->render($template, [
            'questions' => $questions,
            'unreadByTicket' => $unreadByTicket,
            'currentStatut' => $statut,
            'currentPriorite' => $priorite,
            'currentType' => $type,
            'currentSearch' => $search,
            'currentSort' => $sort,
            'currentOrder' => $order,
        ]);
    }

    #[Route('/new', name: 'app_question_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, SluggerInterface $slugger, MailerService $mailerService): Response
    {
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour créer un ticket.');
            return $this->redirectToRoute('app_login');
        }

        $question = new Question();
        // Définir l'utilisateur et le statut AVANT la création du formulaire
        $question->setUtilisateur($this->getUser());
        $question->setStatut('ouvert');
        
        $form = $this->createForm(QuestionType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // L'utilisateur et le statut sont déjà définis au-dessus
            
            // Gérer le fichier uploadé
            $fichier = $form->get('fichier')->getData();
            if ($fichier) {
                // Récupérer les infos du fichier AVANT le déplacement
                $originalName = $fichier->getClientOriginalName();
                $mimeType = $fichier->getMimeType();
                $fileSize = $fichier->getSize();
                
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
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l\'upload du fichier');
                }
            }

            $entityManager->persist($question);
            $entityManager->flush();

            // Envoyer l'email de confirmation au client
            $mailerService->sendTicketCreatedEmail($question);

            $this->addFlash('success', 'Votre ticket a été créé avec succès ! Un email de confirmation vous a été envoyé.');

            return $this->redirectToRoute('app_question_show', ['id' => $question->getId()]);
        }

        return $this->render('front/ticket_new.html.twig', [
            'question' => $question,
            'form' => $form,
        ]);
    }

    #[Route('/{id}', name: 'app_question_show', methods: ['GET', 'POST'])]
    public function show(
        Question $question, 
        Request $request, 
        EntityManagerInterface $entityManager,
        ResponseQuestionRepository $responseRepository,
        SluggerInterface $slugger,
        MailerService $mailerService
    ): Response {
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour accéder à ce ticket.');
            return $this->redirectToRoute('app_login');
        }

        // Vérifier que l'utilisateur a le droit de voir ce ticket
        if (!$this->isGranted('ROLE_ADMIN') && $question->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à ce ticket');
        }

        // Récupérer les réponses du ticket
        $reponses = $responseRepository->findByQuestion($question->getId());

        // Formulaire pour ajouter une réponse
        $responseQuestion = new ResponseQuestion();
        $responseQuestion->setQuestion($question);
        $responseQuestion->setUtilisateur($this->getUser());
        
        $isAdmin = $this->isGranted('ROLE_ADMIN');
        
        // Définir les valeurs par défaut AVANT la création du formulaire
        // Pour éviter les erreurs de validation NotBlank
        if ($isAdmin) {
            $responseQuestion->setAuteurType('agent');
            $responseQuestion->setReponseRole('info');
            $responseQuestion->setActionType('aucune');
            $responseQuestion->setImpactStatut('aucun');
        } else {
            $responseQuestion->setAuteurType('client');
            $responseQuestion->setReponseRole('question');
            $responseQuestion->setActionType('aucune');
            $responseQuestion->setImpactStatut('aucun');
        }
        
        // Adapter le formulaire selon le rôle
        $formOptions = ['is_admin' => $isAdmin];
        $form = $this->createForm(ResponseQuestionType::class, $responseQuestion, $formOptions);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Les valeurs du formulaire ont déjà été appliquées par handleRequest
            // Pour les admins, elles écrasent les valeurs par défaut
            // Pour les clients, les valeurs par défaut restent car ces champs ne sont pas dans leur formulaire
            
            // S'assurer que auteurType est toujours correctement défini
            if ($isAdmin) {
                $responseQuestion->setAuteurType('agent');
            } else {
                $responseQuestion->setAuteurType('client');
            }

            // Gérer le fichier uploadé
            $fichier = $form->get('fichier')->getData();
            if ($fichier) {
                // Récupérer les infos du fichier AVANT le déplacement
                $originalName = $fichier->getClientOriginalName();
                $mimeType = $fichier->getMimeType();
                $fileSize = $fichier->getSize();
                
                $originalFilename = pathinfo($originalName, PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename.'-'.uniqid().'.'.$fichier->guessExtension();

                try {
                    $uploadDir = $this->getParameter('kernel.project_dir').'/public/uploads/responses';
                    if (!is_dir($uploadDir)) {
                        mkdir($uploadDir, 0777, true);
                    }
                    
                    // Déplacer le fichier
                    $fichier->move($uploadDir, $newFilename);
                    
                    // Utiliser les valeurs sauvegardées AVANT le déplacement
                    $responseQuestion->setFileName($originalName);
                    $responseQuestion->setFilePath('/uploads/responses/'.$newFilename);
                    $responseQuestion->setFileType($mimeType);
                    $responseQuestion->setFileSize($fileSize);
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l\'upload du fichier');
                }
            }

            // Mettre à jour le statut de la question selon l'impact de la réponse
            $impactStatut = $responseQuestion->getImpactStatut();
            if ($impactStatut !== null && $impactStatut !== 'aucun') {
                $question->setStatut($impactStatut);
            }
            
            // Si c'est une réponse d'agent et que le ticket est "ouvert", le passer en "en_cours"
            if ($isAdmin && $question->getStatut() === 'ouvert') {
                $question->setStatut('en_cours');
            }

            // Persister la réponse et mettre à jour la question
            try {
                $entityManager->persist($responseQuestion);
                $entityManager->persist($question);
                $entityManager->flush();

                // Si c'est une réponse d'admin, envoyer un email au client
                if ($isAdmin) {
                    $mailerService->sendTicketResponseEmail($question, $responseQuestion);
                }

                $this->addFlash('success', 'Votre réponse a été ajoutée avec succès !');
                
                return $this->redirectToRoute('app_question_show', ['id' => $question->getId()]);
            } catch (\Exception $e) {
                $this->addFlash('error', 'Erreur lors de l\'enregistrement de la réponse : ' . $e->getMessage());
            }
        } else if ($form->isSubmitted()) {
            // Le formulaire a été soumis mais n'est pas valide
            // Afficher les erreurs de validation
            $errors = [];
            foreach ($form->getErrors(true) as $error) {
                $errors[] = $error->getMessage();
            }
            if (!empty($errors)) {
                $this->addFlash('error', 'Erreurs de validation : ' . implode(', ', $errors));
            }
        }

        // Utiliser le bon template selon le rôle
        if ($this->isGranted('ROLE_ADMIN')) {
            $template = 'Admin/ticket_show.html.twig';
        } else {
            // Pour les clients, marquer toutes les réponses d'agents comme lues
            foreach ($reponses as $reponse) {
                if ($reponse->getAuteurType() === 'agent' && !$reponse->isLuParClient()) {
                    $reponse->setLuParClient(true);
                    $entityManager->persist($reponse);
                }
            }
            $entityManager->flush();
            
            $template = 'front/ticket_show.html.twig';
        }

        return $this->render($template, [
            'question' => $question,
            'reponses' => $reponses,
            'form' => $form,
        ]);
    }

    #[Route('/{id}/statut', name: 'app_question_update_statut', methods: ['POST'])]
    public function updateStatut(Question $question, Request $request, EntityManagerInterface $entityManager): Response
    {
        if (!$this->isGranted('ROLE_ADMIN')) {
            throw $this->createAccessDeniedException();
        }

        $nouveauStatut = $request->request->get('statut');
        $nouvellePriorite = $request->request->get('priorite');
        
        $updated = false;
        
        // Mettre à jour le statut si fourni et valide
        if ($nouveauStatut && in_array($nouveauStatut, ['ouvert', 'en_cours', 'resolu', 'ferme'])) {
            $question->setStatut($nouveauStatut);
            $updated = true;
        }
        
        // Mettre à jour la priorité si fournie et valide
        if ($nouvellePriorite && in_array($nouvellePriorite, ['basse', 'normale', 'haute'])) {
            $question->setPriorite($nouvellePriorite);
            $updated = true;
        }
        
        if ($updated) {
            $entityManager->persist($question);
            $entityManager->flush();
            
            $this->addFlash('success', 'Le ticket a été mis à jour avec succès');
        } else {
            $this->addFlash('warning', 'Aucune modification n\'a été effectuée');
        }

        return $this->redirectToRoute('app_question_show', ['id' => $question->getId()]);
    }

    #[Route('/{id}/update', name: 'app_question_update_field', methods: ['POST'])]
    public function updateField(Question $question, Request $request, EntityManagerInterface $entityManager): Response
    {
        if (!$this->isGranted('ROLE_ADMIN')) {
            throw $this->createAccessDeniedException();
        }

        $field = $request->request->get('field');
        $value = $request->request->get('value');
        
        $validFields = [
            'statut' => ['ouvert', 'en_cours', 'resolu', 'ferme'],
            'priorite' => ['basse', 'normale', 'haute'],
            'typeTicket' => ['support', 'reclamation', 'retour']
        ];
        
        if (isset($validFields[$field]) && in_array($value, $validFields[$field])) {
            // Utiliser la méthode setter appropriée
            $setter = 'set' . ucfirst($field);
            if (method_exists($question, $setter)) {
                $question->$setter($value);
                $entityManager->persist($question);
                $entityManager->flush();
                
                return $this->json([
                    'success' => true,
                    'message' => 'Champ mis à jour avec succès'
                ]);
            }
        }
        
        return $this->json([
            'success' => false,
            'message' => 'Erreur lors de la mise à jour'
        ], 400);
    }

    #[Route('/{id}/edit', name: 'app_question_edit', methods: ['GET', 'POST'])]
    public function edit(Question $question, Request $request, EntityManagerInterface $entityManager, SluggerInterface $slugger): Response
    {
        // LOG : Début de la méthode edit
        error_log("[EDIT] Début de l'édition du ticket #" . $question->getId());
        error_log("[EDIT] Utilisateur connecté : " . ($this->getUser() ? $this->getUser()->getUserIdentifier() : 'aucun'));
        error_log("[EDIT] Statut du ticket : " . $question->getStatut());
        
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            error_log("[EDIT] Erreur : Utilisateur non connecté");
            $this->addFlash('error', 'Vous devez être connecté.');
            return $this->redirectToRoute('app_login');
        }

        // Vérifier que l'utilisateur a le droit de modifier ce ticket
        if (!$this->isGranted('ROLE_ADMIN') && $question->getUtilisateur() !== $this->getUser()) {
            error_log("[EDIT] Erreur : Accès refusé pour l'utilisateur");
            throw $this->createAccessDeniedException('Vous n\'avez pas le droit de modifier ce ticket');
        }
        
        error_log("[EDIT] Vérifications OK, affichage du formulaire");

        $form = $this->createForm(QuestionType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            error_log("[EDIT] Formulaire soumis et valide");
            // Gérer le fichier uploadé
            $fichier = $form->get('fichier')->getData();
            if ($fichier) {
                // Récupérer les infos du fichier AVANT le déplacement
                $originalName = $fichier->getClientOriginalName();
                $mimeType = $fichier->getMimeType();
                $fileSize = $fichier->getSize();
                
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
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l\'upload du fichier');
                }
            }

            $entityManager->flush();

            error_log("[EDIT] Ticket #" . $question->getId() . " modifié avec succès");
            $this->addFlash('success', 'Le ticket a été modifié avec succès !');

            // Rediriger selon le rôle
            if ($this->isGranted('ROLE_ADMIN')) {
                error_log("[EDIT] Redirection vers ticket_show (admin)");
                return $this->redirectToRoute('app_question_show', ['id' => $question->getId()]);
            }
            error_log("[EDIT] Redirection vers app_contact (client)");
            return $this->redirectToRoute('app_contact');
        }

        // Utiliser le bon template selon le rôle
        if ($this->isGranted('ROLE_ADMIN')) {
            $template = 'Admin/ticket_edit.html.twig';
            error_log("[EDIT] Rendu du template admin : " . $template);
        } else {
            $template = 'front/ticket_edit.html.twig';
            error_log("[EDIT] Rendu du template client : " . $template);
        }

        return $this->render($template, [
            'question' => $question,
            'form' => $form,
        ]);
    }

    #[Route('/{id}/delete', name: 'app_question_delete', methods: ['POST'])]
    public function delete(Question $question, Request $request, EntityManagerInterface $entityManager): Response
    {
        error_log("[DELETE] Tentative de suppression du ticket #" . $question->getId());
        
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            error_log("[DELETE] Erreur : Utilisateur non connecté");
            return $this->redirectToRoute('app_login');
        }

        // Autoriser l'admin et l'auteur du ticket à supprimer
        if (!$this->isGranted('ROLE_ADMIN') && $question->getUtilisateur() !== $this->getUser()) {
            error_log("[DELETE] Erreur : Accès refusé");
             throw $this->createAccessDeniedException('Vous n\'avez pas le droit de supprimer ce ticket');
        }

        if ($this->isCsrfTokenValid('delete'.$question->getId(), $request->request->get('_token'))) {
            error_log("[DELETE] Token CSRF valide, suppression en cours");
            $entityManager->remove($question);
            $entityManager->flush();

            error_log("[DELETE] Ticket #" . $question->getId() . " supprimé avec succès");
            $this->addFlash('success', 'Le ticket a été supprimé avec succès');
        } else {
            error_log("[DELETE] Erreur : Token CSRF invalide");
            $this->addFlash('error', 'Erreur de validation du formulaire');
        }

        // Rediriger selon le rôle
        if ($this->isGranted('ROLE_ADMIN')) {
            error_log("[DELETE] Redirection vers liste tickets (admin)");
            return $this->redirectToRoute('app_question_index');
        }
        error_log("[DELETE] Redirection vers page contact (client)");
        return $this->redirectToRoute('app_contact');
    }

    #[Route('/{id}/export-pdf', name: 'app_question_export_pdf', methods: ['GET'])]
    public function exportPdf(Question $question, \App\Service\PdfService $pdfService): Response
    {
        // Vérifier que l'utilisateur est connecté
        if (!$this->getUser()) {
            $this->addFlash('error', 'Vous devez être connecté pour exporter un ticket.');
            return $this->redirectToRoute('app_login');
        }

        // Vérifier que l'utilisateur a le droit de voir ce ticket
        if (!$this->isGranted('ROLE_ADMIN') && $question->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à ce ticket');
        }

        // Générer le PDF
        $pdfContent = $pdfService->generateTicketPdf($question);
        $fileName = $pdfService->generateFileName($question);

        // Retourner la réponse PDF
        return new Response($pdfContent, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $fileName . '"',
        ]);
    }
}
