<?php

namespace App\Service;

use App\Entity\AuditLog;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Security\Core\Security;
use Symfony\Component\HttpFoundation\RequestStack;
use Psr\Log\LoggerInterface;

class AuditService
{
    private EntityManagerInterface $entityManager;
    private Security $security;
    private RequestStack $requestStack;
    private LoggerInterface $logger;

    public function __construct(
        EntityManagerInterface $entityManager,
        Security $security,
        RequestStack $requestStack,
        LoggerInterface $logger
    ) {
        $this->entityManager = $entityManager;
        $this->security = $security;
        $this->requestStack = $requestStack;
        $this->logger = $logger;
    }

    /**
     * Enregistre une modification dans l'audit log
     */
    public function logChange(
        string $entityType,
        int $entityId,
        string $action,
        ?array $oldValues = null,
        ?array $newValues = null
    ): void {
        try {
            $auditLog = new AuditLog();
            $auditLog->setEntityType($entityType);
            $auditLog->setEntityId($entityId);
            $auditLog->setAction($action);

            // Informations utilisateur
            $user = $this->security->getUser();
            if ($user) {
                $auditLog->setUserId($user->getId());
                $auditLog->setUserName($user->getUserIdentifier());
            }

            // Informations requête
            $request = $this->requestStack->getCurrentRequest();
            if ($request) {
                $auditLog->setIpAddress($request->getClientIp());
                $auditLog->setUserAgent($request->headers->get('User-Agent'));
            }

            // Valeurs
            $auditLog->setOldValues($oldValues);
            $auditLog->setNewValues($newValues);

            // Calculer les champs modifiés
            if ($oldValues && $newValues) {
                $changedFields = $this->getChangedFields($oldValues, $newValues);
                $auditLog->setChangedFields($changedFields);
            }

            $this->entityManager->persist($auditLog);
            $this->entityManager->flush();

            $this->logger->info('Audit log créé', [
                'entity' => $entityType,
                'id' => $entityId,
                'action' => $action
            ]);

        } catch (\Exception $e) {
            $this->logger->error('Erreur lors de la création de l\'audit log', [
                'error' => $e->getMessage()
            ]);
        }
    }

    /**
     * Récupère l'historique d'une entité
     */
    public function getEntityHistory(string $entityType, int $entityId): array
    {
        return $this->entityManager
            ->getRepository(AuditLog::class)
            ->findBy(
                ['entityType' => $entityType, 'entityId' => $entityId],
                ['createdAt' => 'DESC']
            );
    }

    /**
     * Détermine les champs qui ont changé
     */
    private function getChangedFields(array $oldValues, array $newValues): array
    {
        $changed = [];

        foreach ($newValues as $field => $newValue) {
            $oldValue = $oldValues[$field] ?? null;

            // Comparer les valeurs
            if ($this->valuesAreDifferent($oldValue, $newValue)) {
                $changed[] = [
                    'field' => $field,
                    'old' => $oldValue,
                    'new' => $newValue
                ];
            }
        }

        return $changed;
    }

    /**
     * Compare deux valeurs
     */
    private function valuesAreDifferent($oldValue, $newValue): bool
    {
        // Gérer les dates
        if ($oldValue instanceof \DateTime && $newValue instanceof \DateTime) {
            return $oldValue->format('Y-m-d H:i:s') !== $newValue->format('Y-m-d H:i:s');
        }

        // Gérer les objets
        if (is_object($oldValue) && is_object($newValue)) {
            if (method_exists($oldValue, 'getId') && method_exists($newValue, 'getId')) {
                return $oldValue->getId() !== $newValue->getId();
            }
        }

        // Comparaison standard
        return $oldValue !== $newValue;
    }

    /**
     * Extrait les valeurs d'une entité pour l'audit
     */
    public function extractEntityValues(object $entity): array
    {
        $values = [];
        $reflection = new \ReflectionClass($entity);

        foreach ($reflection->getProperties() as $property) {
            $property->setAccessible(true);
            $value = $property->getValue($entity);

            // Convertir les objets en identifiants
            if (is_object($value)) {
                if ($value instanceof \DateTime) {
                    $value = $value->format('Y-m-d H:i:s');
                } elseif (method_exists($value, 'getId')) {
                    $value = $value->getId();
                } elseif (method_exists($value, '__toString')) {
                    $value = (string) $value;
                } else {
                    $value = get_class($value);
                }
            }

            $values[$property->getName()] = $value;
        }

        return $values;
    }
}
