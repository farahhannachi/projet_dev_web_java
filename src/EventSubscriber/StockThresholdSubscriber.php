<?php

namespace App\EventSubscriber;

use App\Entity\Stock;
use App\Service\SmsStockService;
use Doctrine\Common\EventSubscriber;
use Doctrine\ORM\Event\OnFlushEventArgs;
use Doctrine\ORM\Event\PostFlushEventArgs;
use Doctrine\ORM\Events;

class StockThresholdSubscriber implements EventSubscriber
{
    /** @var array<int, Stock> */
    private array $pendingAlerts = [];

    public function __construct(private readonly SmsStockService $smsStockService)
    {
    }

    public function getSubscribedEvents(): array
    {
        return [
            Events::onFlush,
            Events::postFlush,
        ];
    }

    public function onFlush(OnFlushEventArgs $args): void
    {
        if ($this->pendingAlerts !== []) {
            $this->pendingAlerts = [];
        }

        $uow = $args->getObjectManager()->getUnitOfWork();

        foreach ($uow->getScheduledEntityInsertions() as $entity) {
            if (!$entity instanceof Stock) {
                continue;
            }

            if ($this->isCritical($entity->getQuantite(), $entity->getSeuilCritique())) {
                $this->queueAlert($entity);
            }
        }

        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            if (!$entity instanceof Stock) {
                continue;
            }

            $changeSet = $uow->getEntityChangeSet($entity);

            $quantiteChanged = array_key_exists('quantite', $changeSet);
            $seuilChanged = array_key_exists('seuilCritique', $changeSet);

            if (!$quantiteChanged && !$seuilChanged) {
                continue;
            }

            $oldQuantite = $quantiteChanged ? $changeSet['quantite'][0] : $entity->getQuantite();
            $newQuantite = $quantiteChanged ? $changeSet['quantite'][1] : $entity->getQuantite();
            $oldSeuil = $seuilChanged ? $changeSet['seuilCritique'][0] : $entity->getSeuilCritique();
            $newSeuil = $seuilChanged ? $changeSet['seuilCritique'][1] : $entity->getSeuilCritique();

            $wasCritical = $this->isCritical($oldQuantite, $oldSeuil);
            $isCritical = $this->isCritical($newQuantite, $newSeuil);

            if (!$wasCritical && $isCritical) {
                $this->queueAlert($entity);
            }
        }
    }

    public function postFlush(PostFlushEventArgs $args): void
    {
        if ($this->pendingAlerts === []) {
            return;
        }

        $pending = $this->pendingAlerts;
        $this->pendingAlerts = [];

        foreach ($pending as $stock) {
            $this->smsStockService->sendCriticalAlert($stock);
        }
    }

    private function isCritical(?int $quantite, ?int $seuil): bool
    {
        return (int) ($quantite ?? 0) <= (int) ($seuil ?? 0);
    }

    private function queueAlert(Stock $stock): void
    {
        $this->pendingAlerts[spl_object_id($stock)] = $stock;
    }
}
