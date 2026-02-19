<?php

namespace App\Repository;

use App\Entity\Depot;
use App\Entity\Produit;
use App\Entity\StockMovement;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<StockMovement>
 */
class StockMovementRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, StockMovement::class);
    }

    public function save(StockMovement $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * @return array<int, array{date:string, qty:int}>
     */
    public function getDailySortieSeries(Depot $depot, Produit $produit, int $days = 30): array
    {
        $days = max(1, $days);
        $from = (new \DateTimeImmutable('now'))->modify(sprintf('-%d days', $days));

        $sql = <<<SQL
SELECT DATE(sm.created_at) AS day, COALESCE(SUM(sm.quantite), 0) AS qty
FROM stock_movement sm
INNER JOIN stock s ON s.id_stock = sm.id_stock
WHERE sm.type = :type
  AND sm.status = :status
  AND s.depot_id = :depotId
  AND s.produit_id = :produitId
  AND sm.created_at >= :fromDate
GROUP BY DATE(sm.created_at)
ORDER BY DATE(sm.created_at) ASC
SQL;

        $rows = $this->getEntityManager()->getConnection()->fetchAllAssociative($sql, [
            'type' => StockMovement::TYPE_SORTIE,
            'status' => StockMovement::STATUS_DONE,
            'depotId' => $depot->getId(),
            'produitId' => $produit->getId(),
            'fromDate' => $from->format('Y-m-d H:i:s'),
        ]);

        $series = [];
        foreach ($rows as $row) {
            $series[] = [
                'date' => (string) ($row['day'] ?? ''),
                'qty' => (int) ($row['qty'] ?? 0),
            ];
        }

        return $series;
    }

    public function getAverageDailySortie(Depot $depot, Produit $produit, int $days = 30): float
    {
        $days = max(1, $days);
        $series = $this->getDailySortieSeries($depot, $produit, $days);

        $total = 0;
        foreach ($series as $point) {
            $total += (int) $point['qty'];
        }

        // Divide by the full window (including days with zero sorties).
        return $total / $days;
    }
}
