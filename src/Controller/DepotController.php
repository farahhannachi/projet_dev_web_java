<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Repository\DepotRepository;
use App\Form\DepotType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class DepotController extends AbstractController
{
    #[Route('/admin/depots', name: 'admin_depots')]
    public function index(DepotRepository $depotRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $depots = $depotRepository->findAll();
        
        return $this->render('Admin/depots/index.html.twig', [
            'depots' => $depots
        ]);
    }

    #[Route('/admin/depot/new', name: 'admin_depot_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $depot = new Depot();
        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($depot);
            $entityManager->flush();
            
            $this->addFlash('success', 'Dépôt créé avec succès');
            return $this->redirectToRoute('admin_depots');
        }

        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => null
        ]);
    }

    #[Route('/admin/depot/{id}/edit', name: 'admin_depot_edit', methods: ['GET', 'POST'])]
    public function edit(Depot $depot, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Dépôt modifié avec succès');

            return $this->redirectToRoute('admin_depots');
        }

        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => $depot
        ]);
    }

    #[Route('/admin/depot/{id}/delete', name: 'admin_depot_delete', methods: ['POST'])]
    public function delete(Depot $depot, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($depot);
        $entityManager->flush();
        $this->addFlash('success', 'Dépôt supprimé avec succès');

        return $this->redirectToRoute('admin_depots');
    }
}
