<?php

namespace App\Repository;

use App\Entity\Depot;
use App\Entity\Produit;
use App\Entity\Stock;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Stock>
 *
 * @method Stock|null find($id, $lockMode = null, $lockVersion = null)
 * @method Stock|null findOneBy(array $criteria, array $orderBy = null)
 * @method Stock[]    findAll()
 * @method Stock[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class StockRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Stock::class);
    }

    public function save(Stock $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Stock $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    // src/Repository/StockRepository.php

    public function getStatsByProduitAndEtat(): array
    {
        $qb = $this->createQueryBuilder('s')
            ->select('IDENTITY(s.produit) AS produitId, s.etatStock, COUNT(s.id) AS total')
            ->groupBy('s.produit, s.etatStock');

        return $qb->getQuery()->getResult();
    }

    public function findOneByQrToken(string $qrToken): ?Stock
    {
        return $this->createQueryBuilder('s')
            ->leftJoin('s.depot', 'd')->addSelect('d')
            ->leftJoin('s.produit', 'p')->addSelect('p')
            ->andWhere('s.qrCodeToken = :token')
            ->setParameter('token', $qrToken)
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function findOneByLotDepotProduitExpiration(
        string $numeroLot,
        int $depotId,
        int $produitId,
        ?\DateTimeInterface $dateExpiration
    ): ?Stock {
        $qb = $this->createQueryBuilder('s')
            ->leftJoin('s.depot', 'd')->addSelect('d')
            ->leftJoin('s.produit', 'p')->addSelect('p')
            ->andWhere('s.batchNumber = :lot')
            ->andWhere('d.id = :depotId')
            ->andWhere('p.id = :produitId')
            ->setParameter('lot', $numeroLot)
            ->setParameter('depotId', $depotId)
            ->setParameter('produitId', $produitId)
            ->setMaxResults(1);

        if ($dateExpiration !== null) {
            $start = (new \DateTimeImmutable($dateExpiration->format('Y-m-d')))->setTime(0, 0, 0);
            $end = $start->setTime(23, 59, 59);
            $qb
                ->andWhere('s.dateExpiration BETWEEN :start AND :end')
                ->setParameter('start', $start)
                ->setParameter('end', $end);
        }

        return $qb->getQuery()->getOneOrNullResult();
    }

    public function existsDuplicateLotInDepot(string $numeroLot, int $depotId, ?int $excludeStockId = null): bool
    {
        $qb = $this->createQueryBuilder('s')
            ->select('COUNT(s.id)')
            ->leftJoin('s.depot', 'd')
            ->andWhere('s.batchNumber = :lot')
            ->andWhere('d.id = :depotId')
            ->setParameter('lot', $numeroLot)
            ->setParameter('depotId', $depotId);

        if ($excludeStockId !== null) {
            $qb->andWhere('s.id != :excludeId')->setParameter('excludeId', $excludeStockId);
        }

        return (int) $qb->getQuery()->getSingleScalarResult() > 0;
    }

    /**
     * @return Produit[]
     */
    public function findDistinctProduitsByDepot(Depot $depot): array
    {
        return $this->getEntityManager()->createQueryBuilder()
            ->select('DISTINCT p')
            ->from(Produit::class, 'p')
            ->innerJoin(Stock::class, 's', 'WITH', 's.produit = p')
            ->andWhere('s.depot = :depot')
            ->setParameter('depot', $depot)
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Stock[]
     */
    public function findActiveLotsByDepotAndProduitFefo(Depot $depot, Produit $produit): array
    {
        $now = new \DateTimeImmutable('now');

        return $this->createQueryBuilder('s')
            ->andWhere('s.depot = :depot')
            ->andWhere('s.produit = :produit')
            ->andWhere('s.isActif = 1')
            ->andWhere('(s.dateExpiration IS NULL OR s.dateExpiration > :now)')
            ->setParameter('depot', $depot)
            ->setParameter('produit', $produit)
            ->setParameter('now', $now)
            ->orderBy('s.dateExpiration', 'ASC') // FEFO
            ->addOrderBy('s.id', 'ASC')
            ->getQuery()
            ->getResult();
    }

    public function getUsedCapacityForDepot(Depot $depot): int
    {
        $result = $this->createQueryBuilder('s')
            ->select('COALESCE(SUM(s.quantite), 0) AS usedCapacity')
            ->andWhere('s.depot = :depot')
            ->setParameter('depot', $depot)
            ->getQuery()
            ->getSingleResult();

        return (int) ($result['usedCapacity'] ?? 0);
    }

}
