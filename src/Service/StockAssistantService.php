<?php

namespace App\Service;

use App\Entity\Stock;
use App\Repository\StockRepository;
use App\Repository\VenteRepository;
use Psr\Log\LoggerInterface;

class StockAssistantService
{
    public function __construct(
        private readonly StockRepository $stockRepository,
        private readonly VenteRepository $venteRepository,
        private readonly LoggerInterface $logger
    ) {
    }

    /**
     * @return array<string, mixed>
     */
    public function analyserQuestion(string $question): array
    {
        $question = trim($question);
        if ($question === '') {
            return [
                'intent' => 'empty',
                'analysis' => 'Question vide.',
                'prevision' => ['items' => [], 'count' => 0],
                'recommendation' => 'Veuillez poser une question.',
            ];
        }

        try {
            $intent = $this->detectIntent($question);
            $stocks = $this->stockRepository->createQueryBuilder('s')
                ->leftJoin('s.produit', 'p')->addSelect('p')
                ->leftJoin('s.depot', 'd')->addSelect('d')
                ->getQuery()
                ->getResult();

            return match ($intent) {
                'rupture_semaine' => $this->analyseRuptureSoon($question, $stocks, 7),
                'performance_depot' => $this->analyseDepotPerformance($question, $stocks),
                'recommandation_commande' => $this->analyseReorderNeeds($question, $stocks),
                default => $this->analyseGeneral($question, $stocks),
            };
        } catch (\Throwable $e) {
            $this->logger->error('Stock assistant failed.', [
                'question' => $question,
                'error' => $e->getMessage(),
            ]);

            return [
                'intent' => 'error',
                'analysis' => 'Erreur lors de l analyse.',
                'prevision' => ['items' => [], 'count' => 0],
                'recommendation' => 'Veuillez reessayer plus tard.',
            ];
        }
    }

    private function detectIntent(string $question): string
    {
        $q = $this->normalize($question);
        if (str_contains($q, 'rupture') && (str_contains($q, 'semaine') || str_contains($q, '7 jour'))) {
            return 'rupture_semaine';
        }

        if (str_contains($q, 'depot') && (str_contains($q, 'performant') || str_contains($q, 'performance'))) {
            return 'performance_depot';
        }

        if (str_contains($q, 'commander') || str_contains($q, 'commande') || str_contains($q, 'quantite')) {
            return 'recommandation_commande';
        }

        return 'general';
    }

    /**
     * @param Stock[] $stocks
     * @return array<string, mixed>
     */
    private function analyseRuptureSoon(string $question, array $stocks, int $daysWindow): array
    {
        $avgByProduct = $this->buildAverageDailyConsumption($stocks);
        $limitDate = (new \DateTimeImmutable('now'))->modify('+' . $daysWindow . ' days');

        $items = [];
        foreach ($stocks as $stock) {
            $produitId = $stock->getProduit()?->getId() ?? 0;
            if ($produitId <= 0) {
                continue;
            }
            $avgDaily = $avgByProduct[$produitId] ?? 0.0;
            if ($avgDaily <= 0) {
                continue;
            }

            $quantite = (int) ($stock->getQuantite() ?? 0);
            $daysLeft = (int) floor($quantite / $avgDaily);
            $ruptureDate = (new \DateTimeImmutable('now'))->modify('+' . $daysLeft . ' days');

            if ($ruptureDate > $limitDate) {
                continue;
            }

            $items[] = [
                'produit' => $stock->getProduit()?->getNom() ?? 'Produit inconnu',
                'depot' => $stock->getDepot()?->getNomDepot() ?? 'Depot inconnu',
                'quantite' => $quantite,
                'seuil_critique' => (int) ($stock->getSeuilCritique() ?? 0),
                'consommation_journaliere' => round($avgDaily, 2),
                'jours_restants' => $daysLeft,
                'date_rupture' => $ruptureDate->format('d/m/Y'),
            ];
        }

        usort($items, static fn(array $a, array $b) => $a['jours_restants'] <=> $b['jours_restants']);

        $analysis = count($items) > 0
            ? sprintf('%d produits risquent une rupture dans les %d prochains jours.', count($items), $daysWindow)
            : sprintf('Aucun stock ne montre de rupture probable dans les %d prochains jours.', $daysWindow);

        $recommendation = count($items) > 0
            ? 'Verifier les quantites ci-dessous et preparer un reapprovisionnement ou un transfert.'
            : 'Surveillance reguliere recommande.';

        return [
            'intent' => 'rupture_semaine',
            'analysis' => $analysis,
            'prevision' => ['items' => array_slice($items, 0, 10), 'count' => count($items)],
            'recommendation' => $recommendation,
        ];
    }

    /**
     * @param Stock[] $stocks
     * @return array<string, mixed>
     */
    private function analyseDepotPerformance(string $question, array $stocks): array
    {
        $stats = [];
        foreach ($stocks as $stock) {
            $depot = $stock->getDepot();
            if (!$depot) {
                continue;
            }
            $id = $depot->getId() ?? 0;
            if ($id <= 0) {
                continue;
            }

            $stats[$id] ??= [
                'depot' => $depot->getNomDepot() ?? 'Depot inconnu',
                'total_stocks' => 0,
                'total_quantite' => 0,
                'total_sorties' => 0,
                'total_entrees' => 0,
                'rupture_count' => 0,
            ];

            $stats[$id]['total_stocks']++;
            $stats[$id]['total_quantite'] += (int) ($stock->getQuantite() ?? 0);
            $stats[$id]['total_sorties'] += $stock->getTotalSorties();
            $stats[$id]['total_entrees'] += $stock->getTotalEntrees();

            if ((int) ($stock->getQuantite() ?? 0) <= (int) ($stock->getSeuilCritique() ?? 0)) {
                $stats[$id]['rupture_count']++;
            }
        }

        $items = [];
        foreach ($stats as $row) {
            $rotation = $row['total_entrees'] > 0 ? $row['total_sorties'] / $row['total_entrees'] : 0.0;
            $ruptureRate = $row['total_stocks'] > 0 ? ($row['rupture_count'] / $row['total_stocks']) * 100 : 0.0;
            $score = ($rotation * 100) - ($ruptureRate * 2);

            $items[] = [
                'depot' => $row['depot'],
                'rotation' => round($rotation * 100, 2),
                'taux_rupture' => round($ruptureRate, 2),
                'volume_stock' => $row['total_quantite'],
                'score' => round($score, 2),
            ];
        }

        usort($items, static fn(array $a, array $b) => $b['score'] <=> $a['score']);

        $top = $items[0]['depot'] ?? 'N/A';
        $analysis = count($items) > 0
            ? sprintf('Le depot le plus performant est %s.', $top)
            : 'Aucune donnee de depot disponible.';

        $recommendation = count($items) > 0
            ? 'Renforcer les depots avec taux de rupture eleve et equilibrer les transferts.'
            : 'Verifier la qualite des donnees de stock.';

        return [
            'intent' => 'performance_depot',
            'analysis' => $analysis,
            'prevision' => ['items' => array_slice($items, 0, 10), 'count' => count($items)],
            'recommendation' => $recommendation,
        ];
    }

    /**
     * @param Stock[] $stocks
     * @return array<string, mixed>
     */
    private function analyseReorderNeeds(string $question, array $stocks): array
    {
        $avgByProduct = $this->buildAverageDailyConsumption($stocks);
        $items = [];

        foreach ($stocks as $stock) {
            $quantite = (int) ($stock->getQuantite() ?? 0);
            $seuilAlerte = (int) ($stock->getSeuilAlerte() ?? 0);
            $seuilCritique = (int) ($stock->getSeuilCritique() ?? 0);

            if ($quantite > $seuilAlerte) {
                continue;
            }

            $produitId = $stock->getProduit()?->getId() ?? 0;
            $avgDaily = $produitId > 0 ? ($avgByProduct[$produitId] ?? 0.0) : 0.0;
            $recommended = $this->computeRecommendedQty($stock, $avgDaily);

            $items[] = [
                'produit' => $stock->getProduit()?->getNom() ?? 'Produit inconnu',
                'depot' => $stock->getDepot()?->getNomDepot() ?? 'Depot inconnu',
                'quantite' => $quantite,
                'seuil_alerte' => $seuilAlerte,
                'seuil_critique' => $seuilCritique,
                'consommation_journaliere' => round($avgDaily, 2),
                'recommandation_commande' => $recommended,
            ];
        }

        usort($items, static fn(array $a, array $b) => $a['quantite'] <=> $b['quantite']);

        $analysis = count($items) > 0
            ? sprintf('%d stocks sont a reapprovisionner.', count($items))
            : 'Aucun stock n est en dessous du seuil d alerte.';

        $recommendation = count($items) > 0
            ? 'Planifier une commande prioritaire pour les stocks critiques.'
            : 'Surveillance reguliere recommande.';

        return [
            'intent' => 'recommandation_commande',
            'analysis' => $analysis,
            'prevision' => ['items' => array_slice($items, 0, 10), 'count' => count($items)],
            'recommendation' => $recommendation,
        ];
    }

    /**
     * @param Stock[] $stocks
     * @return array<string, mixed>
     */
    private function analyseGeneral(string $question, array $stocks): array
    {
        $total = count($stocks);
        $critique = 0;
        $alerte = 0;
        foreach ($stocks as $stock) {
            $quantite = (int) ($stock->getQuantite() ?? 0);
            if ($quantite <= (int) ($stock->getSeuilCritique() ?? 0)) {
                $critique++;
            } elseif ($quantite <= (int) ($stock->getSeuilAlerte() ?? 0)) {
                $alerte++;
            }
        }

        return [
            'intent' => 'general',
            'analysis' => sprintf('Stocks total: %d. Critiques: %d. En alerte: %d.', $total, $critique, $alerte),
            'prevision' => ['items' => [], 'count' => 0],
            'recommendation' => 'Essayez: "rupture cette semaine", "depot le plus performant", "combien commander".',
        ];
    }

    /**
     * @param Stock[] $stocks
     * @return array<int, float>
     */
    private function buildAverageDailyConsumption(array $stocks): array
    {
        $avgByProduct = [];
        foreach ($stocks as $stock) {
            $produitId = $stock->getProduit()?->getId() ?? 0;
            if ($produitId <= 0 || isset($avgByProduct[$produitId])) {
                continue;
            }

            $avgByProduct[$produitId] = (float) $this->venteRepository->getConsommationMoyenneJournaliere($produitId);
        }

        return $avgByProduct;
    }

    private function computeRecommendedQty(Stock $stock, float $avgDaily): int
    {
        $quantite = (int) ($stock->getQuantite() ?? 0);
        if ($avgDaily > 0) {
            $targetDays = 30;
            $targetQty = (int) ceil($avgDaily * $targetDays);
            return max(0, $targetQty - $quantite);
        }

        $seuilAlerte = (int) ($stock->getSeuilAlerte() ?? 0);
        $base = max($seuilAlerte * 2, 50);
        return max(0, $base - $quantite);
    }

    private function normalize(string $text): string
    {
        $text = strtolower(trim($text));
        $text = iconv('UTF-8', 'ASCII//TRANSLIT', $text) ?: $text;
        return preg_replace('/\s+/', ' ', $text) ?? $text;
    }
}
