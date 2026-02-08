<?php

namespace App\Repository;

use App\Entity\Question;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Question>
 */
class QuestionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Question::class);
    }

    /**
     * Trouve les questions par utilisateur
     */
    public function findByUtilisateur(int $utilisateurId): array
    {
        return $this->createQueryBuilder('q')
            ->andWhere('q.utilisateur = :utilisateurId')
            ->setParameter('utilisateurId', $utilisateurId)
            ->orderBy('q.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Trouve les questions par statut
     */
    public function findByStatut(string $statut): array
    {
        return $this->createQueryBuilder('q')
            ->andWhere('q.statut = :statut')
            ->setParameter('statut', $statut)
            ->orderBy('q.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Trouve les questions par priorité
     */
    public function findByPriorite(string $priorite): array
    {
        return $this->createQueryBuilder('q')
            ->andWhere('q.priorite = :priorite')
            ->setParameter('priorite', $priorite)
            ->orderBy('q.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Trouve les questions par type de ticket
     */
    public function findByTypeTicket(string $typeTicket): array
    {
        return $this->createQueryBuilder('q')
            ->andWhere('q.typeTicket = :typeTicket')
            ->setParameter('typeTicket', $typeTicket)
            ->orderBy('q.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Compte les questions ouvertes
     */
    public function countQuestionsOuvertes(): int
    {
        return $this->createQueryBuilder('q')
            ->select('COUNT(q.id)')
            ->andWhere('q.statut IN (:statuts)')
            ->setParameter('statuts', ['ouvert', 'en_cours'])
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Recherche avec filtres, tri et recherche
     */
    public function findWithFilters(?string $statut, ?string $priorite, ?string $type, ?string $search, string $sort = 'createdAt', string $order = 'DESC'): array
    {
        $qb = $this->createQueryBuilder('q')
            ->leftJoin('q.utilisateur', 'u');

        // Filtre par statut
        if ($statut && $statut !== '') {
            $qb->andWhere('q.statut = :statut')
               ->setParameter('statut', $statut);
        }

        // Filtre par priorité
        if ($priorite && $priorite !== '') {
            $qb->andWhere('q.priorite = :priorite')
               ->setParameter('priorite', $priorite);
        }

        // Filtre par type
        if ($type && $type !== '') {
            $qb->andWhere('q.typeTicket = :type')
               ->setParameter('type', $type);
        }

        // Recherche textuelle
        if ($search && $search !== '') {
            $qb->andWhere('q.objet LIKE :search OR q.description LIKE :search OR u.nom LIKE :search OR u.prenom LIKE :search OR u.email LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        // Tri
        $allowedSorts = ['id', 'createdAt', 'objet', 'statut', 'priorite', 'typeTicket'];
        if (!in_array($sort, $allowedSorts)) {
            $sort = 'createdAt';
        }
        $order = strtoupper($order) === 'ASC' ? 'ASC' : 'DESC';
        
        $qb->orderBy('q.' . $sort, $order);

        return $qb->getQuery()->getResult();
    }
}
