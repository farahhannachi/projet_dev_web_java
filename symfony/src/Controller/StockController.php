<?php

namespace App\Controller;

use App\Entity\Stock;
use App\Repository\StockRepository;
use App\Form\StockType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class StockController extends AbstractController
{
    #[Route('/admin/stocks', name: 'admin_stocks')]
    public function index(StockRepository $stockRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $stocks = $stockRepository->findAll();
        
        return $this->render('Admin/stocks/index.html.twig', [
            'stocks' => $stocks
        ]);
    }

    #[Route('/admin/stock/new', name: 'admin_stock_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $stock = new Stock();
        // Initialiser la date de dernière mise à jour avec la date actuelle
        $stock->setDateDerniereMiseAJour(new \DateTime());
        
        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($stock);
            $entityManager->flush();
            
            $this->addFlash('success', 'Stock créé avec succès');
            return $this->redirectToRoute('admin_stocks');
        }

        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => null
        ]);
    }

    #[Route('/admin/stock/{id}/edit', name: 'admin_stock_edit', methods: ['GET', 'POST'])]
    public function edit(Stock $stock, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);
        
        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Stock modifié avec succès');

            return $this->redirectToRoute('admin_stocks');
        }

        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => $stock
        ]);
    }

    #[Route('/admin/stock/{id}/delete', name: 'admin_stock_delete', methods: ['POST'])]
    public function delete(Stock $stock, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $entityManager->remove($stock);
        $entityManager->flush();
        $this->addFlash('success', 'Stock supprimé avec succès');

        return $this->redirectToRoute('admin_stocks');
    }

    #[Route('/admin/stocks/stats', name: 'admin_stocks_stats')]
public function stats(StockRepository $stockRepository): Response
{
    $this->denyAccessUnlessGranted('ROLE_ADMIN');

    $rawStats = $stockRepository->getStatsByProduitAndEtat();

    $etatCounts = [];

    foreach ($rawStats as $row) {
        $etat = $row['etatStock'];
        $etatCounts[$etat] = ($etatCounts[$etat] ?? 0) + (int) $row['total'];
    }

    return $this->render('Admin/stocks/stats.html.twig', [
        'labels' => array_keys($etatCounts),
        'data' => array_values($etatCounts),
    ]);
}




}
