<?php

namespace App\Service;

use App\Entity\Stock;
use App\Entity\StockMovement;
use App\Exception\InvalidQrPayloadException;
use App\Exception\StockExpiredException;
use App\Exception\StockInactiveException;
use App\Exception\StockInsufficientQuantityException;
use App\Exception\StockNotFoundFromQrException;
use App\Repository\StockMovementRepository;
use App\Repository\StockRepository;
use Doctrine\ORM\EntityManagerInterface;

class StockQrService
{
    public function __construct(
        private readonly StockRepository $stockRepository,
        private readonly StockMovementRepository $movementRepository,
        private readonly EntityManagerInterface $entityManager
    ) {
    }

    /**
     * Generate QR metadata for a stock lot.
     *
     * @throws InvalidQrPayloadException
     */
    public function initializeQrForStock(Stock $stock): void
    {
        $lot = trim((string) $stock->getBatchNumber());
        $depotId = $stock->getDepot()?->getId();
        $produitId = $stock->getProduit()?->getId();
        $expiration = $stock->getDateExpiration();

        if ($lot === '' || $depotId === null || $produitId === null || $expiration === null) {
            throw new InvalidQrPayloadException('Impossible de générer le QR: lot, dépôt, produit et date expiration sont obligatoires.');
        }

        if ($this->stockRepository->existsDuplicateLotInDepot($lot, $depotId, $stock->getId())) {
            throw new InvalidQrPayloadException('Un stock avec le même numéro de lot existe déjà dans ce dépôt.');
        }

        if ($stock->getQuantiteInitiale() <= 0) {
            $stock->setQuantiteInitiale(max(0, (int) $stock->getQuantite()));
        }

        $payload = [
            'numeroLot' => $lot,
            'idDepot' => $depotId,
            'idProduit' => $produitId,
            'dateExpiration' => $expiration->format('Y-m-d'),
        ];

        $tokenSource = implode('|', [
            $payload['numeroLot'],
            (string) $payload['idDepot'],
            (string) $payload['idProduit'],
            $payload['dateExpiration'],
        ]);

        $token = hash('sha256', $tokenSource);
        $stock->setQrCodeToken($token);
        $stock->setQrCodePayload(json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES));
    }

    public function getQrRenderableContent(Stock $stock): string
    {
        return (string) $stock->getQrCodePayload();
    }

    public function getQrImageUrl(Stock $stock, int $size = 220): string
    {
        $data = rawurlencode($this->getQrRenderableContent($stock));
        return sprintf('https://api.qrserver.com/v1/create-qr-code/?size=%dx%d&data=%s', $size, $size, $data);
    }

    /**
     * @throws InvalidQrPayloadException
     * @throws StockNotFoundFromQrException
     */
    public function resolveStockFromScan(string $qrInput): Stock
    {
        $qrInput = trim($qrInput);
        if ($qrInput === '') {
            throw new InvalidQrPayloadException('QR vide.');
        }

        // Path 1: direct token scan
        $byToken = $this->stockRepository->findOneByQrToken($qrInput);
        if ($byToken instanceof Stock) {
            return $byToken;
        }

        // Path 2: JSON payload scan
        $decoded = json_decode($qrInput, true);
        if (!is_array($decoded)) {
            throw new InvalidQrPayloadException('QR invalide: format non reconnu.');
        }

        $lot = (string) ($decoded['numeroLot'] ?? '');
        $depotId = (int) ($decoded['idDepot'] ?? 0);
        $produitId = (int) ($decoded['idProduit'] ?? 0);
        $dateExpiration = (string) ($decoded['dateExpiration'] ?? '');

        if ($lot === '' || $depotId <= 0 || $produitId <= 0 || $dateExpiration === '') {
            throw new InvalidQrPayloadException('QR invalide: données métier incomplètes.');
        }

        $expiration = new \DateTimeImmutable($dateExpiration);
        $stock = $this->stockRepository->findOneByLotDepotProduitExpiration($lot, $depotId, $produitId, $expiration);
        if (!$stock instanceof Stock) {
            throw new StockNotFoundFromQrException('Aucun stock correspondant au QR scanné.');
        }

        return $stock;
    }

    /**
     * @throws StockExpiredException
     * @throws StockInactiveException
     * @throws StockInsufficientQuantityException
     */
    public function assertStockEligibleForScan(Stock $stock, int $requestedQuantity = 1): void
    {
        if ($stock->estPerime()) {
            throw new StockExpiredException('Sortie bloquée: produit expiré.');
        }

        if (!$stock->isActif()) {
            throw new StockInactiveException('Sortie bloquée: stock inactif.');
        }

        if ($stock->getQuantiteInitiale() <= 0) {
            throw new StockInsufficientQuantityException('Sortie bloquée: quantité initiale invalide.');
        }

        if ((int) $stock->getQuantite() < $requestedQuantity) {
            throw new StockInsufficientQuantityException('Sortie bloquée: quantité insuffisante.');
        }
    }

    /**
     * @return array{
     *   depot:string,
     *   produit:string,
     *   numeroLot:string,
     *   dateExpiration:?string,
     *   quantiteDisponible:int,
     *   seuilAlerte:int,
     *   seuilCritique:int,
     *   emplacement:?string,
     *   etatStock:?string
     * }
     */
    public function getScanDisplayData(Stock $stock): array
    {
        return [
            'depot' => (string) ($stock->getDepot()?->getNomDepot() ?? 'N/A'),
            'produit' => (string) ($stock->getProduit()?->getNom() ?? 'N/A'),
            'numeroLot' => (string) ($stock->getBatchNumber() ?? ''),
            'dateExpiration' => $stock->getDateExpiration()?->format('Y-m-d'),
            'quantiteDisponible' => (int) ($stock->getQuantite() ?? 0),
            'seuilAlerte' => (int) ($stock->getSeuilAlerte() ?? 0),
            'seuilCritique' => (int) ($stock->getSeuilCritique() ?? 0),
            'emplacement' => $stock->getEmplacement(),
            'etatStock' => $stock->getEtatStock(),
        ];
    }

    /**
     * Process a stock output after QR scan.
     *
     * @return array{stock:array<string,mixed>, mouvementId:int}
     *
     * @throws InvalidQrPayloadException
     * @throws StockNotFoundFromQrException
     * @throws StockExpiredException
     * @throws StockInactiveException
     * @throws StockInsufficientQuantityException
     */
    public function processSortieFromScan(string $qrInput, int $quantity, ?string $motif = null): array
    {
        if ($quantity <= 0) {
            throw new StockInsufficientQuantityException('Quantité de sortie invalide.');
        }

        $stock = $this->resolveStockFromScan($qrInput);
        $before = (int) ($stock->getQuantite() ?? 0);
        $this->assertStockEligibleForScan($stock, $quantity);

        $after = max(0, $before - $quantity);
        $stock->setQuantite($after);
        $stock->setTotalSorties($stock->getTotalSorties() + $quantity);
        $stock->setDerniereSortie(new \DateTime());
        $stock->setDateDerniereMiseAJour(new \DateTime());
        $stock->updateEtatStock();

        $movement = (new StockMovement())
            ->setStock($stock)
            ->setType(StockMovement::TYPE_SORTIE)
            ->setQuantite($quantity)
            ->setQuantiteBefore($before)
            ->setQuantiteAfter($after)
            ->setStatus(StockMovement::STATUS_DONE)
            ->setMotif($motif ?: 'Sortie via scan QR');

        $this->movementRepository->save($movement);
        $this->entityManager->flush();

        return [
            'stock' => $this->getScanDisplayData($stock),
            'mouvementId' => (int) $movement->getId(),
        ];
    }
}
