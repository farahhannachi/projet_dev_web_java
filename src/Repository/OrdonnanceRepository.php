<?php

namespace App\Repository;

use App\Entity\Ordonnance;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Ordonnance>
 */
class OrdonnanceRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Ordonnance::class);
    }

    public function save(Ordonnance $ordonnance, bool $flush = false): void
    {
        $this->getEntityManager()->persist($ordonnance);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Ordonnance $ordonnance, bool $flush = false): void
    {
        $this->getEntityManager()->remove($ordonnance);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * Find all prescriptions pending validation
     */
    public function findPendingValidation(): array
    {
        return $this->createQueryBuilder('o')
            ->where('o.status = :status')
            ->setParameter('status', Ordonnance::STATUS_PENDING_VALIDATION)
            ->orderBy('o.uploadedAt', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find prescriptions by client ID
     */
    public function findByClientId(int $clientId): array
    {
        return $this->createQueryBuilder('o')
            ->where('o.clientId = :clientId')
            ->setParameter('clientId', $clientId)
            ->orderBy('o.uploadedAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find prescriptions by status
     */
    public function findByStatus(string $status): array
    {
        return $this->createQueryBuilder('o')
            ->where('o.status = :status')
            ->setParameter('status', $status)
            ->orderBy('o.uploadedAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Count pending prescriptions
     */
    public function countPendingValidation(): int
    {
        return (int) $this->createQueryBuilder('o')
            ->select('COUNT(o.id)')
            ->where('o.status = :status')
            ->setParameter('status', Ordonnance::STATUS_PENDING_VALIDATION)
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Find prescriptions by client ID and status
     */
    public function findByClientIdAndStatus(int $clientId, string $status): array
    {
        return $this->createQueryBuilder('o')
            ->where('o.clientId = :clientId')
            ->andWhere('o.status = :status')
            ->setParameter('clientId', $clientId)
            ->setParameter('status', $status)
            ->orderBy('o.uploadedAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find recent prescriptions (last N days)
     */
    public function findRecentPrescriptions(int $days = 30): array
    {
        $date = new \DateTime();
        $date->modify("-{$days} days");

        return $this->createQueryBuilder('o')
            ->where('o.uploadedAt >= :date')
            ->setParameter('date', $date)
            ->orderBy('o.uploadedAt', 'DESC')
            ->getQuery()
            ->getResult();
    }
}
