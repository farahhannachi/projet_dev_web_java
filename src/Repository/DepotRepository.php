<?php

namespace App\Repository;

use App\Entity\Depot;
use App\Entity\Produit;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Depot>
 *
 * @method Depot|null find($id, $lockMode = null, $lockVersion = null)
 * @method Depot|null findOneBy(array $criteria, array $orderBy = null)
 * @method Depot[]    findAll()
 * @method Depot[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class DepotRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Depot::class);
    }

    public function save(Depot $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Depot $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function calculateDistanceKm(Depot $from, Depot $to): ?float
    {
        $lat1 = $from->getLatitude();
        $lon1 = $from->getLongitude();
        $lat2 = $to->getLatitude();
        $lon2 = $to->getLongitude();

        if ($lat1 === null || $lon1 === null || $lat2 === null || $lon2 === null) {
            return null;
        }

        $earthRadiusKm = 6371.0;
        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);

        $a = sin($dLat / 2) * sin($dLat / 2)
            + cos(deg2rad($lat1)) * cos(deg2rad($lat2))
            * sin($dLon / 2) * sin($dLon / 2);
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

        return round($earthRadiusKm * $c, 2);
    }

    /**
     * @return array{depot: Depot, distance_km: float}|null
     */
    public function findNearestDepot(Depot $origin, ?int $excludeDepotId = null): ?array
    {
        $candidates = $this->createQueryBuilder('d')
            ->andWhere('d.latitude IS NOT NULL')
            ->andWhere('d.longitude IS NOT NULL')
            ->getQuery()
            ->getResult();

        $nearest = null;
        foreach ($candidates as $candidate) {
            if (!$candidate instanceof Depot) {
                continue;
            }
            if ($candidate->getId() === $origin->getId()) {
                continue;
            }
            if ($excludeDepotId !== null && $candidate->getId() === $excludeDepotId) {
                continue;
            }

            $distance = $this->calculateDistanceKm($origin, $candidate);
            if ($distance === null) {
                continue;
            }

            if ($nearest === null || $distance < $nearest['distance_km']) {
                $nearest = ['depot' => $candidate, 'distance_km' => $distance];
            }
        }

        return $nearest;
    }

    /**
     * @return array{depot: Depot, distance_km: float}|null
     */
    public function findNearestDepotWithAvailableStock(Depot $origin, Produit $produit, int $quantity = 1): ?array
    {
        $quantity = max(1, $quantity);
        $now = new \DateTimeImmutable('now');

        $candidates = $this->createQueryBuilder('d')
            ->innerJoin('d.stocks', 's')
            ->andWhere('d.latitude IS NOT NULL')
            ->andWhere('d.longitude IS NOT NULL')
            ->andWhere('d.id != :originId')
            ->andWhere('s.produit = :produit')
            ->andWhere('s.quantite >= :qty')
            ->andWhere('s.isActif = 1')
            ->andWhere('(s.dateExpiration IS NULL OR s.dateExpiration > :now)')
            ->setParameter('originId', $origin->getId())
            ->setParameter('produit', $produit)
            ->setParameter('qty', $quantity)
            ->setParameter('now', $now)
            ->groupBy('d.id')
            ->getQuery()
            ->getResult();

        $nearest = null;
        foreach ($candidates as $candidate) {
            if (!$candidate instanceof Depot) {
                continue;
            }

            $distance = $this->calculateDistanceKm($origin, $candidate);
            if ($distance === null) {
                continue;
            }

            if ($nearest === null || $distance < $nearest['distance_km']) {
                $nearest = ['depot' => $candidate, 'distance_km' => $distance];
            }
        }

        return $nearest;
    }
}
