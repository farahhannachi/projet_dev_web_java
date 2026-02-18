<?php

namespace App\Repository;

use App\Entity\Commande;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Vente>
 */
class VenteRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Commande::class);
    }

    /**
     * Calcule la quantité totale vendue pour un produit sur les 30 derniers jours
     */
    public function getQuantiteVendue30DerniersJours(int $idProduit): int
    {
        $dateLimite = new \DateTime('-30 days');
        
        // Comme nous n'avons pas d'entité Vente, nous utilisons les commandes
        // pour estimer les ventes. Cette méthode devra être adaptée selon votre structure réelle
        $qb = $this->createQueryBuilder('c')
            ->select('COUNT(c.id) as totalVentes')
            ->where('c.dateCommande >= :dateLimite')
            ->setParameter('dateLimite', $dateLimite);
            
        // Note: Cette requête devra être adaptée selon votre structure de données réelle
        // pour lier les commandes aux produits vendus
        
        $result = $qb->getQuery()->getSingleResult();
        
        return (int) $result['totalVentes'];
    }

    /**
     * Calcule la quantité totale vendue pour tous les produits sur les 30 derniers jours
     */
    public function getVentesParProduit30DerniersJours(): array
    {
        $dateLimite = new \DateTime('-30 days');
        
        $qb = $this->createQueryBuilder('c')
            ->select('COUNT(c.id) as totalVentes')
            ->addSelect('c.dateCommande')
            ->where('c.dateCommande >= :dateLimite')
            ->setParameter('dateLimite', $dateLimite)
            ->groupBy('c.dateCommande')
            ->orderBy('c.dateCommande', 'DESC');
            
        return $qb->getQuery()->getResult();
    }

    /**
     * Version alternative utilisant les stocks pour estimer les ventes
     * Cette méthode est plus réaliste si vous n'avez pas de table de ventes
     */
    public function getConsommationMoyenneJournaliere(int $idProduit): float
    {
        $dateLimite = new \DateTime('-30 days');
        
        // Simulation : nous utilisons les variations de stock comme proxy des ventes
        // Cette approche est une approximation et devra être adaptée
        $qb = $this->getEntityManager()->createQueryBuilder()
            ->select('AVG(s.totalSorties) as moyenneSorties')
            ->from('App\Entity\Stock', 's')
            ->where('s.produit = :idProduit')
            ->andWhere('s.dateDerniereMiseAJour >= :dateLimite')
            ->setParameter('idProduit', $idProduit)
            ->setParameter('dateLimite', $dateLimite);
            
        $result = $qb->getQuery()->getSingleResult();
        
        return $result['moyenneSorties'] ?? 0;
    }
}
