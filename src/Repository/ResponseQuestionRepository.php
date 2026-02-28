<?php

namespace App\Repository;

use App\Entity\ResponseQuestion;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<ResponseQuestion>
 */
class ResponseQuestionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, ResponseQuestion::class);
    }

    /**
     * Trouve les réponses par question
     */
    public function findByQuestion(int $questionId): array
    {
        return $this->createQueryBuilder('r')
            ->andWhere('r.question = :questionId')
            ->setParameter('questionId', $questionId)
            ->orderBy('r.createdAt', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Trouve les réponses par type d'auteur
     */
    public function findByAuteurType(string $auteurType): array
    {
        return $this->createQueryBuilder('r')
            ->andWhere('r.auteurType = :auteurType')
            ->setParameter('auteurType', $auteurType)
            ->orderBy('r.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Trouve la dernière réponse d'une question
     */
    public function findLastResponseByQuestion(int $questionId): ?ResponseQuestion
    {
        return $this->createQueryBuilder('r')
            ->andWhere('r.question = :questionId')
            ->setParameter('questionId', $questionId)
            ->orderBy('r.createdAt', 'DESC')
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();
    }

    /**
     * Compte les réponses d'agents non lues par un client
     */
    public function countUnreadResponsesForUser(int $userId): int
    {
        return $this->createQueryBuilder('r')
            ->select('COUNT(r.id)')
            ->join('r.question', 'q')
            ->andWhere('q.utilisateur = :userId')
            ->andWhere('r.auteurType = :auteurType')
            ->andWhere('r.luParClient = :luParClient')
            ->setParameter('userId', $userId)
            ->setParameter('auteurType', 'agent')
            ->setParameter('luParClient', false)
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Trouve les réponses non lues pour un utilisateur
     */
    public function findUnreadResponsesForUser(int $userId, int $limit = 0): array
    {
        $qb = $this->createQueryBuilder('r')
            ->join('r.question', 'q')
            ->addSelect('q')
            ->andWhere('q.utilisateur = :userId')
            ->andWhere('r.auteurType = :auteurType')
            ->andWhere('r.luParClient = :luParClient')
            ->setParameter('userId', $userId)
            ->setParameter('auteurType', 'agent')
            ->setParameter('luParClient', false)
            ->orderBy('r.createdAt', 'DESC');
        
        if ($limit > 0) {
            $qb->setMaxResults($limit);
        }
        
        return $qb->getQuery()->getResult();
    }

    /**
     * Trouve TOUTES les réponses récentes d'agents pour un utilisateur (lues + non lues)
     * Utilisé pour le dropdown de notifications afin que les réponses restent visibles
     */
    public function findRecentAgentResponsesForUser(int $userId, int $limit = 15): array
    {
        return $this->createQueryBuilder('r')
            ->join('r.question', 'q')
            ->addSelect('q')
            ->leftJoin('r.utilisateur', 'u')
            ->addSelect('u')
            ->andWhere('q.utilisateur = :userId')
            ->andWhere('r.auteurType = :auteurType')
            ->setParameter('userId', $userId)
            ->setParameter('auteurType', 'agent')
            ->orderBy('r.createdAt', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }
}
