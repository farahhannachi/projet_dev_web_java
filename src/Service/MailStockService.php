<?php

namespace App\Service;

use App\Entity\Stock;
use Psr\Log\LoggerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;

class MailStockService
{
    public const LEVEL_CRITIQUE = 'critique';
    public const LEVEL_ALERTE = 'alerte';

    public function __construct(
        private readonly MailerInterface $mailer,
        private readonly LoggerInterface $logger
    ) {
    }

    public function envoyerAlerteStock(Stock $stock): bool
    {
        $level = $this->determineAlertLevel($stock);
        if ($level === null) {
            return false;
        }

        $recipient = $this->getRecipient();
        if ($recipient === '') {
            $this->logger->error('Stock mail alert skipped: empty recipient.', [
                'stock_id' => $stock->getId(),
            ]);
            return false;
        }

        $produit = $stock->getProduit()?->getNom() ?? 'Produit inconnu';
        $depot = $stock->getDepot()?->getNomDepot() ?? 'Depot inconnu';
        $subject = $level === self::LEVEL_CRITIQUE
            ? sprintf('[URGENT] Stock critique - %s - %s', $produit, $depot)
            : sprintf('[ALERTE] Stock faible - %s - %s', $produit, $depot);

        $email = (new TemplatedEmail())
            ->from(new Address($this->getFromEmail(), $this->getFromName()))
            ->to($recipient)
            ->subject($subject)
            ->htmlTemplate('emails/stock_alert.html.twig')
            ->context($this->buildContext($stock, $level));
        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');

        try {
            $this->mailer->send($email);
            $this->logger->info('Stock mail alert sent.', [
                'stock_id' => $stock->getId(),
                'recipient' => $recipient,
                'level' => $level,
            ]);
            return true;
        } catch (\Throwable $e) {
            $this->logger->error('Stock mail alert failed.', [
                'stock_id' => $stock->getId(),
                'recipient' => $recipient,
                'level' => $level,
                'error' => $e->getMessage(),
            ]);
            return false;
        }
    }

    private function buildContext(Stock $stock, string $level): array
    {
        $expiration = $stock->getDateExpiration();

        return [
            'level' => $level,
            'produit' => $stock->getProduit()?->getNom() ?? 'Produit inconnu',
            'lot' => $stock->getBatchNumber() ?: 'N/A',
            'depot' => $stock->getDepot()?->getNomDepot() ?? 'Depot inconnu',
            'quantite' => (int) ($stock->getQuantite() ?? 0),
            'seuil_critique' => (int) ($stock->getSeuilCritique() ?? 0),
            'seuil_alerte' => (int) ($stock->getSeuilAlerte() ?? 0),
            'expiration' => $expiration ? $expiration->format('d/m/Y') : 'N/A',
            'recommandation' => $this->computeRecommendedQty($stock),
        ];
    }

    private function computeRecommendedQty(Stock $stock): int
    {
        $seuilAlerte = (int) ($stock->getSeuilAlerte() ?? 0);
        $totalEntrees = $stock->getTotalEntrees();

        $historique = $totalEntrees > 0 ? intdiv($totalEntrees, 4) : 0;
        $base = $seuilAlerte > 0 ? $seuilAlerte * 2 : 0;

        $recommended = max($base, $historique);
        if ($recommended <= 0) {
            $recommended = 50;
        }

        return $recommended;
    }

    private function determineAlertLevel(Stock $stock): ?string
    {
        $quantite = (int) ($stock->getQuantite() ?? 0);
        $seuilCritique = (int) ($stock->getSeuilCritique() ?? 0);
        $seuilAlerte = (int) ($stock->getSeuilAlerte() ?? 0);

        if ($quantite <= $seuilCritique) {
            return self::LEVEL_CRITIQUE;
        }

        if ($quantite <= $seuilAlerte) {
            return self::LEVEL_ALERTE;
        }

        return null;
    }

    private function getRecipient(): string
    {
        $value = $this->getEnvValue('STOCK_ALERT_EMAIL_TO');
        if ($value === '') {
            $value = 'farah.hannachi@esprit.tn';
        }

        return trim($value);
    }

    private function getFromEmail(): string
    {
        $value = $this->getEnvValue('STOCK_ALERT_EMAIL_FROM');
        if ($value === '') {
            $value = 'curavita123@gmail.com';
        }

        return trim($value);
    }

    private function getFromName(): string
    {
        $value = $this->getEnvValue('STOCK_ALERT_EMAIL_FROM_NAME');
        if ($value === '') {
            $value = 'CURAVITA Stock';
        }

        return trim($value);
    }

    private function getEnvValue(string $name): string
    {
        $value = $_ENV[$name] ?? $_SERVER[$name] ?? getenv($name);
        return is_string($value) ? trim($value) : '';
    }
}
