<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Form\DepotType;
use App\Exception\StockAIException;
use App\Repository\DepotRepository;
use App\Repository\ProduitRepository;
use App\Service\DepotGeocodingService;
use App\Service\StockAIService;
use Doctrine\DBAL\Exception\ForeignKeyConstraintViolationException;
use Doctrine\ORM\EntityManagerInterface;
use Knp\Snappy\Pdf;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class DepotController extends AbstractController
{
    #[Route('/admin/depots', name: 'admin_depots')]
    public function index(DepotRepository $depotRepository, ProduitRepository $produitRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $depots = $depotRepository->findAll();
        $mapDepots = [];

        foreach ($depots as $depot) {
            if (!$depot instanceof Depot || $depot->getLatitude() === null || $depot->getLongitude() === null) {
                continue;
            }

            $mapDepots[] = [
                'id' => $depot->getId(),
                'nom' => $depot->getNomDepot(),
                'responsable' => $depot->getResponsableDepot(),
                'capacite' => $depot->getCapaciteDepot(),
                'adresse' => trim(sprintf('%s, %s', (string) $depot->getAdresseDepot(), (string) $depot->getVille())),
                'latitude' => $depot->getLatitude(),
                'longitude' => $depot->getLongitude(),
            ];
        }

        $products = $produitRepository->findBy([], ['nom' => 'ASC']);

        return $this->render('Admin/depots/index.html.twig', [
            'depots' => $depots,
            'map_depots' => $mapDepots,
            'produits' => $products,
        ]);
    }

    #[Route('/admin/depot/new', name: 'admin_depot_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $entityManager,
        DepotGeocodingService $geocodingService
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $depot = new Depot();
        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$depot->getDateCreation()) {
                $depot->setDateCreation(new \DateTimeImmutable('now'));
            }

            $geocoded = $geocodingService->geocodeDepot($depot);
            if (!$geocoded) {
                $this->addFlash('warning', 'Coordonnees GPS non trouvees automatiquement. Verifiez adresse/ville ou saisissez latitude/longitude manuellement.');
            }

            $entityManager->persist($depot);
            $entityManager->flush();

            $this->addFlash('success', 'Depot cree avec succes');
            return $this->redirectToRoute('admin_depots');
        }

        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => null,
        ]);
    }

    #[Route('/admin/depot/{id}/edit', name: 'admin_depot_edit', methods: ['GET', 'POST'])]
    public function edit(
        Depot $depot,
        Request $request,
        EntityManagerInterface $entityManager,
        DepotGeocodingService $geocodingService
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $form = $this->createForm(DepotType::class, $depot);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $geocoded = $geocodingService->geocodeDepot($depot);
            if (!$geocoded) {
                $this->addFlash('warning', 'Coordonnees GPS non mises a jour automatiquement. Verifiez adresse/ville ou saisissez latitude/longitude manuellement.');
            }

            $entityManager->flush();
            $this->addFlash('success', 'Depot modifie avec succes');

            return $this->redirectToRoute('admin_depots');
        }

        return $this->render('Admin/depot_form.html.twig', [
            'form' => $form->createView(),
            'depot' => $depot,
        ]);
    }

    #[Route('/admin/depot/{id}/delete', name: 'admin_depot_delete', methods: ['POST'])]
    public function delete(Depot $depot, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if ($depot->getStocks()->count() > 0) {
            $this->addFlash('warning', 'Suppression impossible: ce depot contient encore des stocks.');
            return $this->redirectToRoute('admin_depots');
        }

        try {
            $entityManager->remove($depot);
            $entityManager->flush();
            $this->addFlash('success', 'Depot supprime avec succes');
        } catch (ForeignKeyConstraintViolationException) {
            $this->addFlash('warning', 'Suppression impossible: depot lie a des enregistrements existants.');
        }

        return $this->redirectToRoute('admin_depots');
    }

    #[Route('/admin/depot/{id}/pdf', name: 'admin_depot_pdf', methods: ['GET'])]
    public function generatePdf(Depot $depot, Pdf $snappy): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $html = $this->renderView('Admin/depots/depot_pdf.html.twig', [
            'depot' => $depot,
        ]);

        $snappy->setOption('margin-top', '20mm');
        $snappy->setOption('margin-right', '15mm');
        $snappy->setOption('margin-bottom', '20mm');
        $snappy->setOption('margin-left', '15mm');
        $snappy->setOption('page-size', 'A4');
        $snappy->setOption('encoding', 'UTF-8');
        $snappy->setOption('enable-local-file-access', true);
        $snappy->setOption('no-outline', true);

        $pdfData = $snappy->getOutputFromHtml($html);

        return new Response($pdfData, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="depot_'.$depot->getId().'.pdf"',
        ]);
    }

    #[Route('/admin/depots/nearest', name: 'admin_depots_nearest', methods: ['GET'])]
    public function nearestDepotForRupture(
        Request $request,
        DepotRepository $depotRepository,
        ProduitRepository $produitRepository
    ): JsonResponse {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $originId = (int) $request->query->get('origin_depot_id', 0);
        $produitId = (int) $request->query->get('produit_id', 0);
        $quantity = max(1, (int) $request->query->get('quantity', 1));

        if ($originId <= 0 || $produitId <= 0) {
            return $this->json(['ok' => false, 'message' => 'Parametres invalides'], 422);
        }

        $origin = $depotRepository->find($originId);
        $produit = $produitRepository->find($produitId);
        if (!$origin instanceof Depot || !$produit) {
            return $this->json(['ok' => false, 'message' => 'Depot ou produit introuvable'], 404);
        }

        $nearest = $depotRepository->findNearestDepotWithAvailableStock($origin, $produit, $quantity);
        if ($nearest === null) {
            return $this->json(['ok' => false, 'message' => 'Aucun depot disponible pour ce produit'], 404);
        }

        /** @var Depot $depot */
        $depot = $nearest['depot'];

        return $this->json([
            'ok' => true,
            'depot' => [
                'id' => $depot->getId(),
                'nom' => $depot->getNomDepot(),
                'adresse' => trim(sprintf('%s, %s', (string) $depot->getAdresseDepot(), (string) $depot->getVille())),
                'responsable' => $depot->getResponsableDepot(),
                'capacite' => $depot->getCapaciteDepot(),
                'distance_km' => $nearest['distance_km'],
            ],
        ]);
    }

    #[Route('/admin/depot/{id}/ai-regulation', name: 'admin_depot_ai_regulation', methods: ['GET'])]
    public function aiRegulation(Depot $depot, StockAIService $stockAIService): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        try {
            $result = $stockAIService->regulerStock($depot);
            return $this->json([
                'ok' => true,
                'depot' => [
                    'id' => $depot->getId(),
                    'nom' => $depot->getNomDepot(),
                ],
                'results' => $result,
            ]);
        } catch (StockAIException $e) {
            return $this->json([
                'ok' => false,
                'message' => $e->getMessage(),
            ], 422);
        }
    }
}
