<?php

namespace App\Controller\Front;

use App\Entity\Ordonnance;
use App\Repository\OrdonnanceRepository;
use App\Repository\TraitementRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/ordonnances')]
class OrdonnanceController extends AbstractController
{
    #[Route('/', name: 'front_ordonnance_index')]
    public function index(OrdonnanceRepository $ordonnanceRepository): Response
    {
        // Simuler un client connecté (ID 1)
        $clientId = 1;
        
        $ordonnances = $ordonnanceRepository->findByClientId($clientId);
        
        return $this->render('front/ordonnance/index.html.twig', [
            'ordonnances' => $ordonnances,
            'clientId' => $clientId,
        ]);
    }

    #[Route('/{id}', name: 'front_ordonnance_show')]
    public function show(Ordonnance $ordonnance): Response
    {
        return $this->render('front/ordonnance/show.html.twig', [
            'ordonnance' => $ordonnance,
        ]);
    }

    #[Route('/{id}/traitements', name: 'front_ordonnance_traitements')]
    public function traitements(Ordonnance $ordonnance): Response
    {
        return $this->render('front/ordonnance/traitements.html.twig', [
            'ordonnance' => $ordonnance,
            'traitements' => $ordonnance->getTraitements(),
        ]);
    }
}
