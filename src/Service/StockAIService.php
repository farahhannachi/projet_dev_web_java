<?php

namespace App\Service;

use App\Entity\Depot;
use App\Entity\Produit;
use App\Entity\Stock;
use App\Exception\StockAIException;
use App\Repository\StockMovementRepository;
use App\Repository\StockRepository;

class StockAIService
{
    public function __construct(
        private readonly StockRepository $stockRepository,
        private readonly StockMovementRepository $stockMovementRepository
    ) {
    }

    /**
     * Regule le stock d'un depot et retourne les recommandations par produit.
     *
     * @return array<int, array{
     *   produit_id:int,
     *   produit_nom:string,
     *   consommation_moyenne_journaliere:float,
     *   prevision_7_jours:int,
     *   prevision_15_jours:int,
     *   prevision_30_jours:int,
     *   stock_actuel:int,
     *   stock_previsionnel_30:int,
     *   quantite_recommandee:int,
     *   statut:string,
     *   delai_livraison_jours:int,
     *   seuil_alerte:int,
     *   seuil_critique:int,
     *   capacite_disponible:int,
     *   fefo:array<int, array{lot:?string, expiration:?string, quantite:int}>,
     *   alertes:array<int, string>
     * }>
     *
     * @throws StockAIException
     */
    public function regulerStock(Depot $depot): array
    {
        if ($depot->getId() === null) {
            throw new StockAIException('Depot invalide: ID manquant.');
        }

        $produits = $this->stockRepository->findDistinctProduitsByDepot($depot);
        if (empty($produits)) {
            throw new StockAIException('Aucun produit trouve dans ce depot.');
        }

        $usedCapacity = $this->stockRepository->getUsedCapacityForDepot($depot);
        $maxCapacity = max(0, (int) ($depot->getCapaciteDepot() ?? 0));
        $availableCapacity = max(0, $maxCapacity - $usedCapacity);

        $results = [];
        foreach ($produits as $produit) {
            if (!$produit instanceof Produit || $produit->getId() === null) {
                continue;
            }

            $lots = $this->stockRepository->findActiveLotsByDepotAndProduitFefo($depot, $produit);
            if (empty($lots)) {
                continue;
            }

            $stockActuel = 0;
            $seuilAlerte = 0;
            $seuilCritique = 0;
            $fefo = [];
            foreach ($lots as $lot) {
                if (!$lot instanceof Stock) {
                    continue;
                }
                $stockActuel += (int) ($lot->getQuantite() ?? 0);
                $seuilAlerte = max($seuilAlerte, (int) ($lot->getSeuilAlerte() ?? 0));
                $seuilCritique = max($seuilCritique, (int) ($lot->getSeuilCritique() ?? 0));
                $fefo[] = [
                    'lot' => $lot->getBatchNumber(),
                    'expiration' => $lot->getDateExpiration()?->format('Y-m-d'),
                    'quantite' => (int) ($lot->getQuantite() ?? 0),
                ];
            }

            $avgDaily = $this->stockMovementRepository->getAverageDailySortie($depot, $produit, 30);
            $prevision7 = (int) ceil($avgDaily * 7);
            $prevision15 = (int) ceil($avgDaily * 15);
            $prevision30 = (int) ceil($avgDaily * 30);
            $stockPrevisionnel30 = $stockActuel - $prevision30;

            $leadTimeDays = $this->estimateLeadTimeDays($lots);
            $safetyDays = 7;
            $targetStock = (int) ceil($avgDaily * ($leadTimeDays + $safetyDays));
            $baseRecommended = max(0, $targetStock - $stockActuel);
            $quantiteRecommandee = min($baseRecommended, $availableCapacity);

            $statut = 'STABLE';
            $alertes = [];

            if ($stockActuel <= $seuilCritique) {
                $statut = 'RUPTURE_IMMINENTE';
                $alertes[] = 'Alerte urgente: seuil critique atteint.';
            } elseif ($stockActuel <= $seuilAlerte) {
                $statut = 'A_SURVEILLER';
                $alertes[] = 'Alerte preventive: seuil alerte atteint.';
            }

            if ($stockActuel < $prevision30) {
                if ($statut !== 'RUPTURE_IMMINENTE') {
                    $statut = 'A_SURVEILLER';
                }
                $alertes[] = 'Stock actuel inferieur a la demande prevue 30 jours.';
            }

            if ($availableCapacity <= 0 && $baseRecommended > 0) {
                $alertes[] = 'Capacite depot insuffisante pour reapprovisionnement.';
            }

            if ($this->hasNearExpiryRisk($fefo, 30, $stockActuel, $prevision30)) {
                $alertes[] = 'Risque FEFO: part de stock expire/proche expiration significative.';
            }

            $results[] = [
                'produit_id' => (int) $produit->getId(),
                'produit_nom' => (string) $produit->getNom(),
                'consommation_moyenne_journaliere' => round($avgDaily, 2),
                'prevision_7_jours' => $prevision7,
                'prevision_15_jours' => $prevision15,
                'prevision_30_jours' => $prevision30,
                'stock_actuel' => $stockActuel,
                'stock_previsionnel_30' => $stockPrevisionnel30,
                'quantite_recommandee' => $quantiteRecommandee,
                'statut' => $statut,
                'delai_livraison_jours' => $leadTimeDays,
                'seuil_alerte' => $seuilAlerte,
                'seuil_critique' => $seuilCritique,
                'capacite_disponible' => $availableCapacity,
                'fefo' => $fefo,
                'alertes' => $alertes,
            ];
        }

        usort($results, function (array $a, array $b): int {
            $order = ['RUPTURE_IMMINENTE' => 0, 'A_SURVEILLER' => 1, 'STABLE' => 2];
            return ($order[$a['statut']] ?? 9) <=> ($order[$b['statut']] ?? 9);
        });

        return $results;
    }

    /**
     * @param array<int, Stock> $lots
     */
    private function estimateLeadTimeDays(array $lots): int
    {
        $maxDays = 3; // baseline local delivery
        foreach ($lots as $lot) {
            $fournisseur = mb_strtolower(trim((string) $lot->getFournisseur()));
            if ($fournisseur === '') {
                $maxDays = max($maxDays, 5);
                continue;
            }
            if (str_contains($fournisseur, 'import') || str_contains($fournisseur, 'international')) {
                $maxDays = max($maxDays, 12);
            } elseif (str_contains($fournisseur, 'central')) {
                $maxDays = max($maxDays, 7);
            } else {
                $maxDays = max($maxDays, 4);
            }
        }

        return $maxDays;
    }

    /**
     * @param array<int, array{lot:?string, expiration:?string, quantite:int}> $fefo
     */
    private function hasNearExpiryRisk(array $fefo, int $horizonDays, int $stockActuel, int $prevision): bool
    {
        if ($stockActuel <= 0) {
            return false;
        }

        $limit = (new \DateTimeImmutable('now'))->modify(sprintf('+%d days', $horizonDays));
        $qtyNearExpiry = 0;

        foreach ($fefo as $lot) {
            $exp = $lot['expiration'] ?? null;
            if (!$exp) {
                continue;
            }
            try {
                $date = new \DateTimeImmutable($exp);
            } catch (\Throwable) {
                continue;
            }
            if ($date <= $limit) {
                $qtyNearExpiry += (int) ($lot['quantite'] ?? 0);
            }
        }

        // Risk if >= 30% of current stock expires soon while 30-day demand exists.
        return $prevision > 0 && $qtyNearExpiry >= (int) floor($stockActuel * 0.3);
    }
}

