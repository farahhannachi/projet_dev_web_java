<?php

namespace App\Controller;

use App\Entity\Depot;
use App\Entity\Stock;
use App\Repository\DepotRepository;
use App\Repository\StockRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class FrontController extends AbstractController
{
    #[Route('/', name: 'app_home', methods: ['GET'])]
    public function home(): Response
    {
        return $this->render('front/home.html.twig');
    }

    #[Route('/products', name: 'app_products', methods: ['GET'])]
    public function products(): Response
    {
        return $this->render('front/products.html.twig');
    }

    #[Route('/about', name: 'app_about', methods: ['GET'])]
    public function about(): Response
    {
        return $this->render('front/about.html.twig');
    }

    #[Route('/contact', name: 'app_contact', methods: ['GET'])]
    public function contact(): Response
    {
        return $this->render('front/contact.html.twig');
    }

    #[Route('/ordonnance', name: 'app_ordonnance', methods: ['GET'])]
    public function ordonnance(): Response
    {
        return $this->render('front/ordonnance.html.twig');
    }

    #[Route('/guide-sante', name: 'app_guide_sante', methods: ['GET'])]
    public function guideSante(): Response
    {
        return $this->render('front/guide_sante.html.twig');
    }

    #[Route('/commande', name: 'app_commande', methods: ['GET'])]
    public function commande(): Response
    {
        return $this->render('front/commande.html.twig');
    }

    #[Route('/depots', name: 'front_depots')]
    public function depots(DepotRepository $depotRepository): Response
    {
        $depots = $depotRepository->findAll();
        
        return $this->render('front/depots.html.twig', [
            'depots' => $depots
        ]);
    }

    #[Route('/stocks', name: 'front_stocks')]
    public function stocks(StockRepository $stockRepository): Response
    {
        $stocks = $stockRepository->findAll();
        
        return $this->render('front/stocks.html.twig', [
            'stocks' => $stocks
        ]);
    }
}
