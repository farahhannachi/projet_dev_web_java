<?php

namespace App\Controller\Back;

use App\Repository\OrdonnanceRepository;
use App\Repository\TraitementRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class DashboardController extends AbstractController
{
    #[Route('/admin', name: 'back_dashboard')]
    public function index(
        OrdonnanceRepository $ordonnanceRepository,
        TraitementRepository $traitementRepository
    ): Response {
        $stats = [
            'ordonnances_total' => count($ordonnanceRepository->findAll()),
            'ordonnances_pending' => $ordonnanceRepository->countPendingValidation(),
            'ordonnances_validated' => count($ordonnanceRepository->findByStatus('validated')),
            'ordonnances_rejected' => count($ordonnanceRepository->findByStatus('rejected')),
            'traitements_total' => count($traitementRepository->findAll()),
        ];
        
        $recentOrdonnances = $ordonnanceRepository->findRecentPrescriptions(7);
        
        return $this->render('back/dashboard/index.html.twig', [
            'stats' => $stats,
            'recentOrdonnances' => $recentOrdonnances,
        ]);
    }
}
