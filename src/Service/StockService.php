<?php

namespace App\Service;

use App\Entity\Produit;
use App\Entity\Stock;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Service pour gérer la liaison entre produits et stocks
 */
class StockService
{
    private EntityManagerInterface $entityManager;

    public function __construct(EntityManagerInterface $entityManager)
    {
        $this->entityManager = $entityManager;
    }

    /**
     * Met à jour le statut d'un produit en fonction de tous ses stocks
     */
    public function updateProduitStatutFromAllStocks(Produit $produit): void
    {
        $stockTotal = $produit->getStockTotal();
        $produit->updateStatutFromStock($stockTotal);
        
        $this->entityManager->flush();
    }

    /**
     * Crée un nouveau stock pour un produit et met à jour le statut
     */
    public function createStockForProduit(Produit $produit, int $quantite, int $seuilAlerte = 5, ?\DateTime $dateExpiration = null): Stock
    {
        $stock = new Stock();
        $stock->setProduit($produit);
        $stock->setQuantite($quantite);
        $stock->setSeuilAlerte($seuilAlerte);
        $stock->setDateExpiration($dateExpiration);
        
        // Mettre à jour l'état du stock
        if ($quantite <= 0) {
            $stock->setEtatStock('rupture');
        } elseif ($quantite <= $seuilAlerte) {
            $stock->setEtatStock('alerte');
        } else {
            $stock->setEtatStock('disponible');
        }
        
        $this->entityManager->persist($stock);
        
        // Mettre à jour le statut du produit
        $this->updateProduitStatutFromAllStocks($produit);
        
        return $stock;
    }

    /**
     * Met à jour la quantité d'un stock et le statut du produit associé
     */
    public function updateStockQuantite(Stock $stock, int $nouvelleQuantite): void
    {
        $stock->setQuantite($nouvelleQuantite);
        
        // Mettre à jour l'état du stock
        $seuilAlerte = $stock->getSeuilAlerte() ?: 5;
        if ($nouvelleQuantite <= 0) {
            $stock->setEtatStock('rupture');
        } elseif ($nouvelleQuantite <= $seuilAlerte) {
            $stock->setEtatStock('alerte');
        } else {
            $stock->setEtatStock('disponible');
        }
        
        // Mettre à jour le statut du produit
        if ($stock->getProduit()) {
            $this->updateProduitStatutFromAllStocks($stock->getProduit());
        }
    }

    /**
     * Récupère les produits en alerte de stock
     */
    public function getProduitsEnAlerte(): array
    {
        $produitRepo = $this->entityManager->getRepository(Produit::class);
        
        return $produitRepo->createQueryBuilder('p')
            ->join('p.stocks', 's')
            ->where('s.etatStock = :alerte OR s.etatStock = :rupture')
            ->setParameter('alerte', 'alerte')
            ->setParameter('rupture', 'rupture')
            ->groupBy('p.id')
            ->getQuery()
            ->getResult();
    }

    /**
     * Récupère les stocks bientôt expirés
     */
    public function getStocksExpirants(int $jours = 30): array
    {
        $stockRepo = $this->entityManager->getRepository(Stock::class);
        $dateLimite = new \DateTime();
        $dateLimite->add(new \DateInterval("P{$jours}D"));
        
        return $stockRepo->createQueryBuilder('s')
            ->where('s.dateExpiration IS NOT NULL')
            ->andWhere('s.dateExpiration <= :dateLimite')
            ->andWhere('s.dateExpiration >= :aujourd_hui')
            ->setParameter('dateLimite', $dateLimite)
            ->setParameter('aujourd_hui', new \DateTime())
            ->orderBy('s.dateExpiration', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Synchronise tous les statuts de produits avec leurs stocks
     */
    public function synchroniserTousLesStatuts(): void
    {
        $produitRepo = $this->entityManager->getRepository(Produit::class);
        $produits = $produitRepo->findAll();
        
        foreach ($produits as $produit) {
            $this->updateProduitStatutFromAllStocks($produit);
        }
    }
}
