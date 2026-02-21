<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class TestController extends AbstractController
{
    #[Route('/test-admin', name: 'test_admin')]
    public function testAdmin(): Response
    {
        return new Response('<h1>Test Admin Route - OK!</h1><p><a href="/admin">Go to Admin</a></p>');
    }
    
    #[Route('/dashboard-test', name: 'dashboard_test')]
    public function dashboardTest(): Response
    {
        return $this->render('Admin/dashboard.html.twig', [
            'totalClients' => 0,
            'totalDepots' => 0,
            'totalStocks' => 0,
            'totalProduits' => 0,
            'stocks' => [],
            'commandes' => [],
            'depots' => []
        ]);
    }
}
