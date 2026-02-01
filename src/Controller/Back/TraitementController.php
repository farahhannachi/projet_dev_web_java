<?php

namespace App\Controller\Back;

use App\Entity\Traitement;
use App\Repository\TraitementRepository;
use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/traitements')]
class TraitementController extends AbstractController
{
    #[Route('/', name: 'back_traitement_index')]
    public function index(TraitementRepository $traitementRepository): Response
    {
        $traitements = $traitementRepository->findAll();
        $actifs = $traitementRepository->findActiveTraitements();
        $completes = $traitementRepository->findBy(['isCompleted' => true]);
        
        return $this->render('back/traitement/index.html.twig', [
            'traitements' => $traitements,
            'actifs' => $actifs,
            'completes' => $completes,
        ]);
    }

    #[Route('/new', name: 'back_traitement_new')]
    public function new(Request $request, EntityManagerInterface $em, OrdonnanceRepository $ordonnanceRepository): Response
    {
        if ($request->isMethod('POST')) {
            $ordonnance = $ordonnanceRepository->find($request->request->get('ordonnanceId'));
            
            if (!$ordonnance) {
                $this->addFlash('danger', 'Ordonnance introuvable !');
                return $this->redirectToRoute('back_traitement_new');
            }
            
            $traitement = new Traitement();
            $traitement->setOrdonnance($ordonnance);
            $traitement->setClientId((int)$request->request->get('clientId'));
            $traitement->setDosage($request->request->get('dosage'));
            $traitement->setFrequency($request->request->get('frequency'));
            $traitement->setDurationDays((int)$request->request->get('durationDays'));
            $traitement->setStartDate(new \DateTime($request->request->get('startDate')));
            $traitement->setEndDate(new \DateTime($request->request->get('endDate')));
            $traitement->setIsActive($request->request->get('isActive') === '1');
            $traitement->setIsCompleted(false);
            $traitement->setNotes($request->request->get('notes'));
            
            $em->persist($traitement);
            $em->flush();
            
            $this->addFlash('success', 'Traitement créé avec succès !');
            
            return $this->redirectToRoute('back_traitement_show', ['id' => $traitement->getId()]);
        }
        
        $ordonnances = $ordonnanceRepository->findBy(['status' => 'validated']);
        
        return $this->render('back/traitement/new.html.twig', [
            'ordonnances' => $ordonnances,
        ]);
    }

    #[Route('/{id}', name: 'back_traitement_show', requirements: ['id' => '\d+'])]
    public function show(Traitement $traitement): Response
    {
        return $this->render('back/traitement/show.html.twig', [
            'traitement' => $traitement,
        ]);
    }

    #[Route('/{id}/edit', name: 'back_traitement_edit')]
    public function edit(Request $request, Traitement $traitement, EntityManagerInterface $em, OrdonnanceRepository $ordonnanceRepository): Response
    {
        if ($request->isMethod('POST')) {
            $ordonnance = $ordonnanceRepository->find($request->request->get('ordonnanceId'));
            
            if (!$ordonnance) {
                $this->addFlash('danger', 'Ordonnance introuvable !');
                return $this->redirectToRoute('back_traitement_edit', ['id' => $traitement->getId()]);
            }
            
            $traitement->setOrdonnance($ordonnance);
            $traitement->setClientId((int)$request->request->get('clientId'));
            $traitement->setDosage($request->request->get('dosage'));
            $traitement->setFrequency($request->request->get('frequency'));
            $traitement->setDurationDays((int)$request->request->get('durationDays'));
            $traitement->setStartDate(new \DateTime($request->request->get('startDate')));
            $traitement->setEndDate(new \DateTime($request->request->get('endDate')));
            $traitement->setIsActive($request->request->get('isActive') === '1');
            $traitement->setNotes($request->request->get('notes'));
            
            $em->flush();
            
            $this->addFlash('success', 'Traitement modifié avec succès !');
            
            return $this->redirectToRoute('back_traitement_show', ['id' => $traitement->getId()]);
        }
        
        $ordonnances = $ordonnanceRepository->findBy(['status' => 'validated']);
        
        return $this->render('back/traitement/edit.html.twig', [
            'traitement' => $traitement,
            'ordonnances' => $ordonnances,
        ]);
    }

    #[Route('/{id}/delete', name: 'back_traitement_delete', methods: ['POST'])]
    public function delete(Traitement $traitement, EntityManagerInterface $em): Response
    {
        $em->remove($traitement);
        $em->flush();
        
        $this->addFlash('success', 'Traitement supprimé avec succès !');
        
        return $this->redirectToRoute('back_traitement_index');
    }

    #[Route('/{id}/complete', name: 'back_traitement_complete', methods: ['POST'])]
    public function complete(Traitement $traitement, EntityManagerInterface $em): Response
    {
        $traitement->setIsCompleted(true);
        $traitement->setIsActive(false);
        
        $em->flush();
        
        $this->addFlash('success', 'Traitement marqué comme complété !');
        
        return $this->redirectToRoute('back_traitement_show', ['id' => $traitement->getId()]);
    }

    #[Route('/{id}/activate', name: 'back_traitement_activate', methods: ['POST'])]
    public function activate(Traitement $traitement, EntityManagerInterface $em): Response
    {
        $traitement->setIsActive(true);
        $traitement->setIsCompleted(false);
        
        $em->flush();
        
        $this->addFlash('success', 'Traitement réactivé !');
        
        return $this->redirectToRoute('back_traitement_show', ['id' => $traitement->getId()]);
    }
}
