<?php

namespace App\Controller\Front;

use App\Repository\TraitementRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/traitements')]
class TraitementController extends AbstractController
{
    #[Route('/', name: 'front_traitement_index')]
    public function index(TraitementRepository $traitementRepository): Response
    {
        // Simuler un client connecté (ID 1)
        $clientId = 1;
        
        $traitementsActifs = $traitementRepository->findActiveByClientId($clientId);
        $tousTraitements = $traitementRepository->findByClientId($clientId);
        
        return $this->render('front/traitement/index.html.twig', [
            'traitementsActifs' => $traitementsActifs,
            'tousTraitements' => $tousTraitements,
            'clientId' => $clientId,
        ]);
    }
}
