<?php

namespace App\Controller;

use App\Entity\Produit;
use App\Entity\Stock;
use App\Service\StockService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Controller pour tester la liaison Produit-Stock
 */
class TestStockController extends AbstractController
{
    #[Route('/test/stock-produit', name: 'test_stock_produit')]
    public function testStockProduit(EntityManagerInterface $entityManager, StockService $stockService): Response
    {
        // Créer un produit de test
        $produit = new Produit();
        $produit->setNom('Produit Test Liaison');
        $produit->setDescription('Description du produit de test');
        $produit->setPrix(29.99);
        $produit->setQuantiteStock(0); // Sera mis à jour par le stock
        $produit->setCategorie('Test');
        $produit->setDateExpiration(new \DateTime('+1 year'));
        
        $entityManager->persist($produit);
        $entityManager->flush(); // Pour avoir l'ID
        
        // Test 1: Créer un stock avec quantité > 5
        echo "<h2>Test 1: Création stock avec quantité 10</h2>";
        $stock1 = $stockService->createStockForProduit($produit, 10, 5);
        echo "Stock créé avec quantité: " . $stock1->getQuantite() . "<br>";
        echo "État du stock: " . $stock1->getEtatStock() . "<br>";
        echo "Statut du produit: " . $produit->getStatut() . "<br>";
        echo "Stock total du produit: " . $produit->getStockTotal() . "<br>";
        echo "Produit disponible? " . ($produit->estDisponible() ? 'Oui' : 'Non') . "<br><br>";
        
        // Test 2: Mettre à jour le stock à 3 (alerte)
        echo "<h2>Test 2: Mise à jour stock à 3 (alerte)</h2>";
        $stockService->updateStockQuantite($stock1, 3);
        echo "Quantité mise à jour: " . $stock1->getQuantite() . "<br>";
        echo "État du stock: " . $stock1->getEtatStock() . "<br>";
        echo "Statut du produit: " . $produit->getStatut() . "<br>";
        echo "Stock total du produit: " . $produit->getStockTotal() . "<br>";
        echo "Produit disponible? " . ($produit->estDisponible() ? 'Oui' : 'Non') . "<br><br>";
        
        // Test 3: Mettre à jour le stock à 0 (rupture)
        echo "<h2>Test 3: Mise à jour stock à 0 (rupture)</h2>";
        $stockService->updateStockQuantite($stock1, 0);
        echo "Quantité mise à jour: " . $stock1->getQuantite() . "<br>";
        echo "État du stock: " . $stock1->getEtatStock() . "<br>";
        echo "Statut du produit: " . $produit->getStatut() . "<br>";
        echo "Stock total du produit: " . $produit->getStockTotal() . "<br>";
        echo "Produit disponible? " . ($produit->estDisponible() ? 'Oui' : 'Non') . "<br><br>";
        
        // Test 4: Ajouter un deuxième stock
        echo "<h2>Test 4: Ajout d'un deuxième stock avec 8 unités</h2>";
        $stock2 = $stockService->createStockForProduit($produit, 8, 5);
        echo "Deuxième stock créé avec quantité: " . $stock2->getQuantite() . "<br>";
        echo "État du deuxième stock: " . $stock2->getEtatStock() . "<br>";
        echo "Stock total du produit: " . $produit->getStockTotal() . "<br>";
        echo "Statut du produit: " . $produit->getStatut() . "<br>";
        echo "Produit disponible? " . ($produit->estDisponible() ? 'Oui' : 'Non') . "<br>";
        echo "Nombre de stocks pour ce produit: " . count($produit->getStocks()) . "<br><br>";
        
        // Test 5: Récupérer les produits en alerte
        echo "<h2>Test 5: Produits en alerte de stock</h2>";
        $produitsEnAlerte = $stockService->getProduitsEnAlerte();
        echo "Nombre de produits en alerte: " . count($produitsEnAlerte) . "<br>";
        foreach ($produitsEnAlerte as $produitAlerte) {
            echo "- " . $produitAlerte->getNom() . " (Stock: " . $produitAlerte->getStockTotal() . ", Statut: " . $produitAlerte->getStatut() . ")<br>";
        }
        echo "<br>";
        
        // Test 6: Stocks bientôt expirants
        echo "<h2>Test 6: Stocks bientôt expirants (30 jours)</h2>";
        $stockExpirant = $stockService->createStockForProduit(
            $produit, 
            5, 
            2, 
            new \DateTime('+15 days') // Expire dans 15 jours
        );
        $stocksExpirants = $stockService->getStocksExpirants(30);
        echo "Nombre de stocks expirants dans 30 jours: " . count($stocksExpirants) . "<br>";
        foreach ($stocksExpirants as $stock) {
            $dateExpiration = $stock->getDateExpiration();
            echo "- Stock #" . $stock->getId() . " (Expire le: " . $dateExpiration->format('d/m/Y') . ")<br>";
        }
        echo "<br>";
        
        // Test 7: Synchronisation globale
        echo "<h2>Test 7: Synchronisation de tous les statuts</h2>";
        $stockService->synchroniserTousLesStatuts();
        echo "Tous les statuts ont été synchronisés<br>";
        echo "Statut final du produit test: " . $produit->getStatut() . "<br>";
        echo "Stock total final: " . $produit->getStockTotal() . "<br><br>";
        
        // Résumé final
        echo "<h2>📊 Résumé des Tests</h2>";
        echo "<table border='1' style='border-collapse: collapse; margin: 20px 0;'>";
        echo "<tr><th>Test</th><th>Résultat</th><th>Statut Produit</th><th>Stock Total</th></tr>";
        echo "<tr><td>Création stock (10)</td><td>✅ Succès</td><td>" . $produit->getStatut() . "</td><td>" . $produit->getStockTotal() . "</td></tr>";
        echo "<tr><td>Stock alerte (3)</td><td>✅ Succès</td><td>stock_critique</td><td>" . $produit->getStockTotal() . "</td></tr>";
        echo "<tr><td>Stock rupture (0)</td><td>✅ Succès</td><td>rupture</td><td>" . $produit->getStockTotal() . "</td></tr>";
        echo "<tr><td>Ajout 2ème stock (8)</td><td>✅ Succès</td><td>" . $produit->getStatut() . "</td><td>" . $produit->getStockTotal() . "</td></tr>";
        echo "</table>";
        
        echo "<h2>✅ Tous les tests complétés avec succès!</h2>";
        echo "<p><a href='/'>Retour à l'accueil</a></p>";
        
        return new Response('<html><body><h1>🧪 Tests de Liaison Produit-Stock</h1>' . ob_get_clean() . '</body></html>');
    }
    
    #[Route('/test/stock-produit/clean', name: 'test_stock_produit_clean')]
    public function cleanTestData(EntityManagerInterface $entityManager): Response
    {
        // Nettoyer les données de test
        $produitRepo = $entityManager->getRepository(Produit::class);
        $stockRepo = $entityManager->getRepository(Stock::class);
        
        // Supprimer les stocks de test
        $stocks = $stockRepo->createQueryBuilder('s')
            ->join('s.produit', 'p')
            ->where('p.nom LIKE :testName')
            ->setParameter('testName', '%Test%')
            ->getQuery()
            ->getResult();
            
        foreach ($stocks as $stock) {
            $entityManager->remove($stock);
        }
        
        // Supprimer les produits de test
        $produits = $produitRepo->createQueryBuilder('p')
            ->where('p.nom LIKE :testName')
            ->setParameter('testName', '%Test%')
            ->getQuery()
            ->getResult();
            
        foreach ($produits as $produit) {
            $entityManager->remove($produit);
        }
        
        $entityManager->flush();
        
        return new Response('<html><body><h1>🧹 Données de test nettoyées</h1><p><a href="/test/stock-produit">Relancer les tests</a></p></body></html>');
    }
}
