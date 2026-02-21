<?php

namespace App\Controller;

use App\Entity\Stock;
use App\Exception\StockQrException;
use App\Form\StockType;
use App\Repository\StockRepository;
use App\Service\StockQrService;
use App\Service\StockService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class StockController extends AbstractController
{
    #[Route('/admin/stocks', name: 'admin_stocks')]
    public function index(StockRepository $stockRepository, StockService $stockService, StockQrService $stockQrService): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $stocks = $stockRepository->findAll();
        $previsionsStock = $stockService->getStatistiquesPrevisionDashboard();

        return $this->render('Admin/stocks/index.html.twig', [
            'stocks' => $stocks,
            'previsions_stock' => $previsionsStock,
            'qr_service' => $stockQrService,
        ]);
    }

    #[Route('/admin/stock/new', name: 'admin_stock_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $entityManager,
        StockQrService $stockQrService
    ): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $stock = new Stock();
        $stock->setDateDerniereMiseAJour(new \DateTime());

        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            try {
                $stock->setQuantiteInitiale((int) $stock->getQuantite());
                $stock->setIsActif(true);
                $stockQrService->initializeQrForStock($stock);

                $entityManager->persist($stock);
                $entityManager->flush();

                $this->addFlash('success', 'Stock créé avec succès. QR Code généré automatiquement.');
                return $this->redirectToRoute('admin_stocks');
            } catch (StockQrException $e) {
                $this->addFlash('error', $e->getMessage());
            }
        }

        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => null,
        ]);
    }

    #[Route('/admin/stock/{id}/edit', name: 'admin_stock_edit', methods: ['GET', 'POST'])]
    public function edit(
        Stock $stock,
        Request $request,
        EntityManagerInterface $entityManager,
        StockQrService $stockQrService
    ): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $form = $this->createForm(StockType::class, $stock);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            try {
                $stockQrService->initializeQrForStock($stock);
                $entityManager->flush();
                $this->addFlash('success', 'Stock modifié avec succès. QR Code régénéré.');

                return $this->redirectToRoute('admin_stocks');
            } catch (StockQrException $e) {
                $this->addFlash('error', $e->getMessage());
            }
        }

        return $this->render('Admin/stock_form.html.twig', [
            'form' => $form->createView(),
            'stock' => $stock,
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

    #[Route('/admin/stocks/scan', name: 'admin_stocks_scan', methods: ['GET'])]
    public function scanPage(): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        return $this->render('Admin/stocks/scan.html.twig');
    }

    #[Route('/admin/stocks/scan/resolve', name: 'admin_stocks_scan_resolve', methods: ['POST'])]
    public function resolveScan(Request $request, StockQrService $stockQrService): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        $qr = (string) $request->request->get('qr', '');

        try {
            $stock = $stockQrService->resolveStockFromScan($qr);
            $stockQrService->assertStockEligibleForScan($stock, 1);

            return $this->json([
                'ok' => true,
                'stock' => $stockQrService->getScanDisplayData($stock),
            ]);
        } catch (StockQrException $e) {
            return $this->json([
                'ok' => false,
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    #[Route('/admin/stocks/scan/sortie', name: 'admin_stocks_scan_sortie', methods: ['POST'])]
    public function sortieFromScan(Request $request, StockQrService $stockQrService): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $qr = (string) $request->request->get('qr', '');
        $quantity = max(0, (int) $request->request->get('quantity', 1));
        $motif = trim((string) $request->request->get('motif', 'Sortie via scan QR'));

        try {
            $result = $stockQrService->processSortieFromScan($qr, $quantity, $motif);
            return $this->json([
                'ok' => true,
                'message' => 'Sortie validée avec succès.',
                'result' => $result,
            ]);
        } catch (StockQrException $e) {
            return $this->json([
                'ok' => false,
                'message' => $e->getMessage(),
            ], 422);
        }
    }
}
