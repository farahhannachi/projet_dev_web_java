<?php

namespace App\Controller\Back;

use App\Entity\Ordonnance;
use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/ordonnances')]
class OrdonnanceController extends AbstractController
{
    #[Route('/', name: 'back_ordonnance_index')]
    public function index(OrdonnanceRepository $ordonnanceRepository): Response
    {
        $ordonnances = $ordonnanceRepository->findAll();
        $pending = $ordonnanceRepository->findPendingValidation();
        
        return $this->render('back/ordonnance/index.html.twig', [
            'ordonnances' => $ordonnances,
            'pending' => $pending,
        ]);
    }

    #[Route('/new', name: 'back_ordonnance_new')]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        if ($request->isMethod('POST')) {
            $ordonnance = new Ordonnance();
            $ordonnance->setFileName($request->request->get('fileName'));
            $ordonnance->setFilePath($request->request->get('filePath'));
            $ordonnance->setStatus($request->request->get('status', Ordonnance::STATUS_PENDING_VALIDATION));
            $ordonnance->setClientId((int)$request->request->get('clientId'));
            $ordonnance->setUploadedAt(new \DateTime());
            
            $em->persist($ordonnance);
            $em->flush();
            
            $this->addFlash('success', 'Ordonnance créée avec succès !');
            
            return $this->redirectToRoute('back_ordonnance_show', ['id' => $ordonnance->getId()]);
        }
        
        return $this->render('back/ordonnance/new.html.twig');
    }

    #[Route('/{id}', name: 'back_ordonnance_show', requirements: ['id' => '\d+'])]
    public function show(Ordonnance $ordonnance): Response
    {
        return $this->render('back/ordonnance/show.html.twig', [
            'ordonnance' => $ordonnance,
        ]);
    }

    #[Route('/{id}/edit', name: 'back_ordonnance_edit')]
    public function edit(Request $request, Ordonnance $ordonnance, EntityManagerInterface $em): Response
    {
        if ($request->isMethod('POST')) {
            $ordonnance->setFileName($request->request->get('fileName'));
            $ordonnance->setFilePath($request->request->get('filePath'));
            $ordonnance->setStatus($request->request->get('status'));
            $ordonnance->setClientId((int)$request->request->get('clientId'));
            
            if ($request->request->get('rejectionReason')) {
                $ordonnance->setRejectionReason($request->request->get('rejectionReason'));
            }
            
            $em->flush();
            
            $this->addFlash('success', 'Ordonnance modifiée avec succès !');
            
            return $this->redirectToRoute('back_ordonnance_show', ['id' => $ordonnance->getId()]);
        }
        
        return $this->render('back/ordonnance/edit.html.twig', [
            'ordonnance' => $ordonnance,
        ]);
    }

    #[Route('/{id}/delete', name: 'back_ordonnance_delete', methods: ['POST'])]
    public function delete(Ordonnance $ordonnance, EntityManagerInterface $em): Response
    {
        $em->remove($ordonnance);
        $em->flush();
        
        $this->addFlash('success', 'Ordonnance supprimée avec succès !');
        
        return $this->redirectToRoute('back_ordonnance_index');
    }

    #[Route('/{id}/valider', name: 'back_ordonnance_validate', methods: ['POST'])]
    public function validate(Ordonnance $ordonnance, EntityManagerInterface $em): Response
    {
        $ordonnance->setStatus(Ordonnance::STATUS_VALIDATED);
        $ordonnance->setValidatedAt(new \DateTime());
        $ordonnance->setValidatedById(2); // Simuler un pharmacien connecté
        
        $em->flush();
        
        $this->addFlash('success', 'Ordonnance validée avec succès !');
        
        return $this->redirectToRoute('back_ordonnance_show', ['id' => $ordonnance->getId()]);
    }

    #[Route('/{id}/rejeter', name: 'back_ordonnance_reject', methods: ['POST'])]
    public function reject(Request $request, Ordonnance $ordonnance, EntityManagerInterface $em): Response
    {
        $reason = $request->request->get('reason', 'Non spécifié');
        
        $ordonnance->setStatus(Ordonnance::STATUS_REJECTED);
        $ordonnance->setRejectionReason($reason);
        $ordonnance->setValidatedAt(new \DateTime());
        $ordonnance->setValidatedById(2); // Simuler un pharmacien connecté
        
        $em->flush();
        
        $this->addFlash('warning', 'Ordonnance rejetée.');
        
        return $this->redirectToRoute('back_ordonnance_index');
    }
}
