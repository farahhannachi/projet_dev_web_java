<?php

namespace App\Service;

use App\Entity\Stock;
use Psr\Log\LoggerInterface;

class SmsStockService
{
    public function __construct(
        private readonly SmsService $smsService,
        private readonly LoggerInterface $logger
    ) {
    }

    public function sendCriticalAlert(Stock $stock): bool
    {
        if (!$this->isEnabled()) {
            $this->logger->info('SMS stock alert disabled.');
            return false;
        }

        if (!$this->smsService->canSend()) {
            $this->logger->warning('SMS stock alert skipped: no SMS provider configured.');
            return false;
        }

        $recipient = $this->resolveRecipient($stock);
        if ($recipient === '') {
            $this->logger->error('SMS stock alert skipped: empty recipient.');
            return false;
        }

        $message = $this->buildMessage($stock);

        try {
            $sent = $this->smsService->sendSms($recipient, $message);
        } catch (\Throwable $e) {
            $this->logger->error('SMS stock alert failed with exception.', [
                'error' => $e->getMessage(),
                'stock_id' => $stock->getId(),
            ]);
            return false;
        }

        if (!$sent) {
            $this->logger->error('SMS stock alert failed.', [
                'error' => $this->smsService->getLastError(),
                'stock_id' => $stock->getId(),
                'recipient' => $recipient,
            ]);
            return false;
        }

        $this->logger->info('SMS stock alert sent.', [
            'stock_id' => $stock->getId(),
            'recipient' => $recipient,
        ]);

        return true;
    }

    private function buildMessage(Stock $stock): string
    {
        $produit = $stock->getProduit()?->getNom() ?? 'Produit inconnu';
        $lot = $stock->getBatchNumber() ?: 'N/A';
        $quantite = (int) ($stock->getQuantite() ?? 0);
        $depot = $stock->getDepot()?->getNomDepot() ?? 'Depot inconnu';

        return sprintf(
            'ALERTE STOCK CRITIQUE: %s | Lot %s | Qte %d | Depot %s',
            $produit,
            $lot,
            $quantite,
            $depot
        );
    }

    private function resolveRecipient(Stock $stock): string
    {
        $depotPhone = $stock->getDepot()?->getResponsableTelephone();
        if (is_string($depotPhone) && trim($depotPhone) !== '') {
            return trim($depotPhone);
        }

        $value = $this->getEnvValue('SMS_STOCK_ALERT_TO');
        if ($value === '') {
            $value = '+21628579499';
        }

        return trim($value);
    }

    private function isEnabled(): bool
    {
        $value = $this->getEnvValue('SMS_STOCK_ALERT_ENABLED');
        if ($value === '') {
            return true;
        }

        $parsed = filter_var($value, FILTER_VALIDATE_BOOL, FILTER_NULL_ON_FAILURE);
        return $parsed ?? true;
    }

    private function getEnvValue(string $name): string
    {
        $value = $_ENV[$name] ?? $_SERVER[$name] ?? getenv($name);
        return is_string($value) ? trim($value) : '';
    }
}
