<?php

namespace App\EventSubscriber;

use App\Entity\Stock;
use App\Service\MailStockService;
use Doctrine\Common\EventSubscriber;
use Doctrine\ORM\Event\OnFlushEventArgs;
use Doctrine\ORM\Event\PostFlushEventArgs;
use Doctrine\ORM\Events;

class StockAlertMailSubscriber implements EventSubscriber
{
    /** @var array<int, Stock> */
    private array $pendingAlerts = [];

    private bool $processing = false;

    public function __construct(private readonly MailStockService $mailStockService)
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
        if ($this->processing) {
            return;
        }

        $uow = $args->getObjectManager()->getUnitOfWork();

        foreach ($uow->getScheduledEntityInsertions() as $entity) {
            if (!$entity instanceof Stock) {
                continue;
            }

            $level = $this->computeLevel(
                $entity->getQuantite(),
                $entity->getSeuilCritique(),
                $entity->getSeuilAlerte()
            );

            if ($level !== null) {
                $this->queueAlert($entity);
            }
        }

        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            if (!$entity instanceof Stock) {
                continue;
            }

            $changeSet = $uow->getEntityChangeSet($entity);
            $quantiteChanged = array_key_exists('quantite', $changeSet);
            $seuilCritiqueChanged = array_key_exists('seuilCritique', $changeSet);
            $seuilAlerteChanged = array_key_exists('seuilAlerte', $changeSet);

            if (!$quantiteChanged && !$seuilCritiqueChanged && !$seuilAlerteChanged) {
                continue;
            }

            $oldQuantite = $quantiteChanged ? $changeSet['quantite'][0] : $entity->getQuantite();
            $newQuantite = $quantiteChanged ? $changeSet['quantite'][1] : $entity->getQuantite();
            $oldSeuilCritique = $seuilCritiqueChanged ? $changeSet['seuilCritique'][0] : $entity->getSeuilCritique();
            $newSeuilCritique = $seuilCritiqueChanged ? $changeSet['seuilCritique'][1] : $entity->getSeuilCritique();
            $oldSeuilAlerte = $seuilAlerteChanged ? $changeSet['seuilAlerte'][0] : $entity->getSeuilAlerte();
            $newSeuilAlerte = $seuilAlerteChanged ? $changeSet['seuilAlerte'][1] : $entity->getSeuilAlerte();

            $oldLevel = $this->computeLevel($oldQuantite, $oldSeuilCritique, $oldSeuilAlerte);
            $newLevel = $this->computeLevel($newQuantite, $newSeuilCritique, $newSeuilAlerte);

            if ($newLevel !== null && $newLevel !== $oldLevel) {
                $this->queueAlert($entity);
            }
        }
    }

    public function postFlush(PostFlushEventArgs $args): void
    {
        if ($this->processing || $this->pendingAlerts === []) {
            return;
        }

        $this->processing = true;
        $pending = $this->pendingAlerts;
        $this->pendingAlerts = [];

        foreach ($pending as $stock) {
            $this->mailStockService->envoyerAlerteStock($stock);
        }

        $this->processing = false;
    }

    private function computeLevel(?int $quantite, ?int $seuilCritique, ?int $seuilAlerte): ?string
    {
        $quantite = (int) ($quantite ?? 0);
        $seuilCritique = (int) ($seuilCritique ?? 0);
        $seuilAlerte = (int) ($seuilAlerte ?? 0);

        if ($quantite <= $seuilCritique) {
            return MailStockService::LEVEL_CRITIQUE;
        }

        if ($quantite <= $seuilAlerte) {
            return MailStockService::LEVEL_ALERTE;
        }

        return null;
    }

    private function queueAlert(Stock $stock): void
    {
        $this->pendingAlerts[spl_object_id($stock)] = $stock;
    }
}
