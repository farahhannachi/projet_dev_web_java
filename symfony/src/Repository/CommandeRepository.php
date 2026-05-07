<?php

namespace App\Repository;

use App\Entity\Commande;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Commande>
 *
 * @method Commande|null find($id, $lockMode = null, $lockVersion = null)
 * @method Commande|null findOneBy(array $criteria, array $orderBy = null)
 * @method Commande[]    findAll()
 * @method Commande[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class CommandeRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Commande::class);
    }

    public function save(Commande $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Commande $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * @return array{count:int,total:float,lastOrderAt:?\\DateTimeInterface}
     */
    public function getUserOrderStatsByEmail(string $email): array
    {
        $qb = $this->createQueryBuilder('c')
            ->select('COUNT(c.id) AS ordersCount')
            ->addSelect('COALESCE(SUM(c.total), 0) AS ordersTotal')
            ->addSelect('MAX(c.dateCommande) AS lastOrderAt')
            ->where('c.email = :email')
            ->setParameter('email', $email);

        $row = $qb->getQuery()->getOneOrNullResult();
        return [
            'count' => (int) ($row['ordersCount'] ?? 0),
            'total' => (float) ($row['ordersTotal'] ?? 0.0),
            'lastOrderAt' => $row['lastOrderAt'] ?? null,
        ];
    }

    public function countRecentOrdersByEmail(string $email, int $minutes): int
    {
        $from = new \DateTimeImmutable(sprintf('-%d minutes', max(1, $minutes)));
        return (int) $this->createQueryBuilder('c')
            ->select('COUNT(c.id)')
            ->where('c.email = :email')
            ->andWhere('c.dateCommande >= :from')
            ->setParameter('email', $email)
            ->setParameter('from', $from)
            ->getQuery()
            ->getSingleScalarResult();
    }
}
