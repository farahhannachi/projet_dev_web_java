<?php

namespace App\EventSubscriber;

use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Service\AuditService;
use Doctrine\Bundle\DoctrineBundle\EventSubscriber\EventSubscriberInterface;
use Doctrine\ORM\Events;
use Doctrine\Persistence\Event\LifecycleEventArgs;
use Doctrine\ORM\Event\PreUpdateEventArgs;

class AuditSubscriber implements EventSubscriberInterface
{
    private AuditService $auditService;
    private array $oldValues = [];

    public function __construct(AuditService $auditService)
    {
        $this->auditService = $auditService;
    }

    public function getSubscribedEvents(): array
    {
        return [
            Events::postPersist,
            Events::preUpdate,
            Events::postUpdate,
            Events::preRemove,
        ];
    }

    /**
     * Après création d'une entité
     */
    public function postPersist(LifecycleEventArgs $args): void
    {
        $entity = $args->getObject();

        if (!$this->shouldAudit($entity)) {
            return;
        }

        $entityType = $this->getEntityType($entity);
        $newValues = $this->auditService->extractEntityValues($entity);

        $this->auditService->logChange(
            $entityType,
            $entity->getId(),
            'create',
            null,
            $newValues
        );
    }

    /**
     * Avant mise à jour - sauvegarder les anciennes valeurs
     */
    public function preUpdate(PreUpdateEventArgs $args): void
    {
        $entity = $args->getObject();

        if (!$this->shouldAudit($entity)) {
            return;
        }

        // Sauvegarder les anciennes valeurs AVANT la modification
        $oldValues = [];
        $changeSet = $args->getEntityChangeSet();
        
        // Log pour debug
        error_log('PreUpdate - Entity: ' . get_class($entity) . ' ID: ' . $entity->getId());
        error_log('ChangeSet: ' . json_encode(array_keys($changeSet)));
        
        foreach ($changeSet as $field => $change) {
            $oldValue = $change[0]; // Ancienne valeur
            $newValue = $change[1]; // Nouvelle valeur
            
            // Convertir les objets pour le stockage
            if ($oldValue instanceof \DateTime) {
                $oldValue = $oldValue->format('Y-m-d H:i:s');
            } elseif (is_object($oldValue) && method_exists($oldValue, 'getId')) {
                $oldValue = $oldValue->getId();
            }
            
            if ($newValue instanceof \DateTime) {
                $newValue = $newValue->format('Y-m-d H:i:s');
            } elseif (is_object($newValue) && method_exists($newValue, 'getId')) {
                $newValue = $newValue->getId();
            }
            
            $oldValues[$field] = $oldValue;
            
            error_log("Field '$field': " . json_encode($oldValue) . " -> " . json_encode($newValue));
        }
        
        // Sauvegarder pour postUpdate
        $this->oldValues[spl_object_id($entity)] = [
            'oldValues' => $oldValues,
            'changeSet' => $changeSet
        ];
    }

    /**
     * Après mise à jour
     */
    public function postUpdate(LifecycleEventArgs $args): void
    {
        $entity = $args->getObject();

        if (!$this->shouldAudit($entity)) {
            return;
        }

        $objectId = spl_object_id($entity);
        $savedData = $this->oldValues[$objectId] ?? null;
        
        if (!$savedData) {
            error_log('PostUpdate - No saved data for entity ' . get_class($entity) . ' ID: ' . $entity->getId());
            return;
        }
        
        $oldValues = $savedData['oldValues'] ?? [];
        $changeSet = $savedData['changeSet'] ?? [];
        
        // Extraire les nouvelles valeurs
        $newValues = [];
        foreach ($changeSet as $field => $change) {
            $newValue = $change[1]; // Nouvelle valeur
            
            // Convertir les objets pour le stockage
            if ($newValue instanceof \DateTime) {
                $newValue = $newValue->format('Y-m-d H:i:s');
            } elseif (is_object($newValue) && method_exists($newValue, 'getId')) {
                $newValue = $newValue->getId();
            }
            
            $newValues[$field] = $newValue;
        }

        $entityType = $this->getEntityType($entity);

        // Enregistrer seulement si des changements ont été détectés
        if (!empty($oldValues) || !empty($newValues)) {
            error_log('Logging change for ' . $entityType . ' ID: ' . $entity->getId());
            error_log('Old values: ' . json_encode($oldValues));
            error_log('New values: ' . json_encode($newValues));
            
            $this->auditService->logChange(
                $entityType,
                $entity->getId(),
                'update',
                $oldValues,
                $newValues
            );
        } else {
            error_log('No changes detected for ' . $entityType . ' ID: ' . $entity->getId());
        }

        // Nettoyer
        unset($this->oldValues[$objectId]);
    }

    /**
     * Avant suppression
     */
    public function preRemove(LifecycleEventArgs $args): void
    {
        $entity = $args->getObject();

        if (!$this->shouldAudit($entity)) {
            return;
        }

        $entityType = $this->getEntityType($entity);
        $oldValues = $this->auditService->extractEntityValues($entity);

        $this->auditService->logChange(
            $entityType,
            $entity->getId(),
            'delete',
            $oldValues,
            null
        );
    }

    /**
     * Vérifie si l'entité doit être auditée
     */
    private function shouldAudit(object $entity): bool
    {
        return $entity instanceof Ordonnance || $entity instanceof Traitement;
    }

    /**
     * Récupère le type d'entité
     */
    private function getEntityType(object $entity): string
    {
        if ($entity instanceof Ordonnance) {
            return 'Ordonnance';
        }
        if ($entity instanceof Traitement) {
            return 'Traitement';
        }

        return (new \ReflectionClass($entity))->getShortName();
    }
}
