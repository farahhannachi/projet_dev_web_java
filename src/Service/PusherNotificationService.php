<?php

namespace App\Service;

use Pusher\Pusher;
use Psr\Log\LoggerInterface;

class PusherNotificationService
{
    private ?Pusher $pusher = null;
    private LoggerInterface $logger;
    private bool $enabled = false;

    public function __construct(
        LoggerInterface $logger,
        ?string $appId = null,
        ?string $appKey = null,
        ?string $appSecret = null,
        string $appCluster = 'eu'
    ) {
        $this->logger = $logger;

        // Vérifier si Pusher est configuré
        if (!empty($appId) && !empty($appKey) && !empty($appSecret)) {
            try {
                $this->pusher = new Pusher(
                    $appKey,
                    $appSecret,
                    $appId,
                    [
                        'cluster' => $appCluster ?: 'eu',
                        'useTLS' => true
                    ]
                );
                $this->enabled = true;
                $this->logger->info('Pusher initialisé avec succès');
            } catch (\Exception $e) {
                $this->logger->error('Erreur lors de l\'initialisation de Pusher: ' . $e->getMessage());
            }
        } else {
            $this->logger->warning('Pusher non configuré - Les notifications en temps réel sont désactivées');
        }
    }

    /**
     * Envoie une notification de nouvelle ordonnance
     */
    public function notifyNewOrdonnance(array $ordonnanceData): bool
    {
        if (!$this->enabled) {
            $this->logger->info('Notification ignorée - Pusher non configuré');
            return false;
        }

        try {
            $this->pusher->trigger(
                'admin-channel',
                'new-ordonnance',
                [
                    'id' => $ordonnanceData['id'],
                    'numero' => $ordonnanceData['numero'],
                    'patient' => $ordonnanceData['patient'],
                    'date' => $ordonnanceData['date'],
                    'message' => 'Nouvelle ordonnance reçue de ' . $ordonnanceData['patient']
                ]
            );

            $this->logger->info('Notification Pusher envoyée pour l\'ordonnance #' . $ordonnanceData['numero']);
            return true;
        } catch (\Exception $e) {
            $this->logger->error('Erreur lors de l\'envoi de la notification Pusher: ' . $e->getMessage());
            return false;
        }
    }

    /**
     * Envoie une notification de nouveau traitement
     */
    public function notifyNewTraitement(array $traitementData): bool
    {
        if (!$this->enabled) {
            return false;
        }

        try {
            $this->pusher->trigger(
                'admin-channel',
                'new-traitement',
                [
                    'id' => $traitementData['id'],
                    'patient' => $traitementData['patient'],
                    'produit' => $traitementData['produit'],
                    'message' => 'Nouveau traitement pour ' . $traitementData['patient']
                ]
            );

            $this->logger->info('Notification Pusher envoyée pour le traitement #' . $traitementData['id']);
            return true;
        } catch (\Exception $e) {
            $this->logger->error('Erreur lors de l\'envoi de la notification: ' . $e->getMessage());
            return false;
        }
    }

    /**
     * Vérifie si Pusher est activé
     */
    public function isEnabled(): bool
    {
        return $this->enabled;
    }
}
