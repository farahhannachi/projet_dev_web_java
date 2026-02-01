<?php

namespace App\Repository;

use App\Entity\Traitement;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Traitement>
 */
class TraitementRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Traitement::class);
    }

    public function save(Traitement $traitement, bool $flush = false): void
    {
        $this->getEntityManager()->persist($traitement);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Traitement $traitement, bool $flush = false): void
    {
        $this->getEntityManager()->remove($traitement);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * Find active treatments for a client by ID
     */
    public function findActiveByClientId(int $clientId): array
    {
        return $this->createQueryBuilder('t')
            ->where('t.clientId = :clientId')
            ->andWhere('t.isActive = :active')
            ->setParameter('clientId', $clientId)
            ->setParameter('active', true)
            ->orderBy('t.startDate', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find all treatments for a client by ID
     */
    public function findByClientId(int $clientId): array
    {
        return $this->createQueryBuilder('t')
            ->where('t.clientId = :clientId')
            ->orderBy('t.startDate', 'DESC')
            ->setParameter('clientId', $clientId)
            ->getQuery()
            ->getResult();
    }

    /**
     * Find all active treatments
     */
    public function findActiveTraitements(): array
    {
        return $this->createQueryBuilder('t')
            ->where('t.isActive = :active')
            ->setParameter('active', true)
            ->orderBy('t.startDate', 'DESC')
            ->getQuery()
            ->getResult();
    }
}
