<?php

namespace App\Service;

use App\Entity\Depot;
use App\Repository\StockRepository;
use Psr\Log\LoggerInterface;

class DepotHealthScoreService
{
    private const DEFAULT_WEIGHTS = [
        'rupture' => 0.40,
        'expiration' => 0.25,
        'rotation' => 0.20,
        'seuils' => 0.15,
    ];

    public function __construct(
        private readonly StockRepository $stockRepository,
        private readonly LoggerInterface $logger,
        private readonly array $weights = []
    ) {
    }

    /**
     * @return array<string, mixed>
     */
    public function calculerScoreDepot(Depot $depot): array
    {
        try {
            $weights = $this->normalizeWeights($this->weights ?: self::DEFAULT_WEIGHTS);
            $stocks = $this->stockRepository->findBy(['depot' => $depot]);
            $total = count($stocks);

            if ($total === 0) {
                return $this->buildEmptyScore($weights);
            }

            $ruptureCount = 0;
            $expirationCount = 0;
            $seuilBreachCount = 0;
            $totalSorties = 0;
            $totalEntrees = 0;

            foreach ($stocks as $stock) {
                $quantite = (int) ($stock->getQuantite() ?? 0);
                $seuilCritique = (int) ($stock->getSeuilCritique() ?? 0);
                $seuilAlerte = (int) ($stock->getSeuilAlerte() ?? 0);

                if ($quantite <= $seuilCritique) {
                    $ruptureCount++;
                }

                if ($quantite <= $seuilAlerte) {
                    $seuilBreachCount++;
                }

                if ($stock->estPerime() || $stock->estProchePeremption()) {
                    $expirationCount++;
                }

                $totalSorties += $stock->getTotalSorties();
                $totalEntrees += $stock->getTotalEntrees();
            }

            $ruptureRate = $ruptureCount / $total;
            $expirationRate = $expirationCount / $total;
            $seuilRate = $seuilBreachCount / $total;
            $rotationRatio = $totalEntrees > 0 ? ($totalSorties / $totalEntrees) : 0.0;
            $rotationRatio = max(0.0, min(1.0, $rotationRatio));

            $scoreRupture = $this->scoreFromPenaltyRate($ruptureRate);
            $scoreExpiration = $this->scoreFromPenaltyRate($expirationRate);
            $scoreSeuils = $this->scoreFromPenaltyRate($seuilRate);
            $scoreRotation = $this->scoreFromBonusRate($rotationRatio);

            $score = (
                $scoreRupture * $weights['rupture'] +
                $scoreExpiration * $weights['expiration'] +
                $scoreRotation * $weights['rotation'] +
                $scoreSeuils * $weights['seuils']
            );

            $score = round($score, 2);

            return [
                'score' => $score,
                'status' => $this->statusFromScore($score),
                'color' => $this->colorFromScore($score),
                'criteria' => [
                    'rupture' => [
                        'count' => $ruptureCount,
                        'rate' => round($ruptureRate * 100, 2),
                        'score' => $scoreRupture,
                    ],
                    'expiration' => [
                        'count' => $expirationCount,
                        'rate' => round($expirationRate * 100, 2),
                        'score' => $scoreExpiration,
                    ],
                    'rotation' => [
                        'ratio' => round($rotationRatio * 100, 2),
                        'score' => $scoreRotation,
                    ],
                    'seuils' => [
                        'count' => $seuilBreachCount,
                        'rate' => round($seuilRate * 100, 2),
                        'score' => $scoreSeuils,
                    ],
                ],
                'weights' => $weights,
                'meta' => [
                    'total_stocks' => $total,
                ],
            ];
        } catch (\Throwable $e) {
            $this->logger->error('Depot health score failed.', [
                'depot_id' => $depot->getId(),
                'error' => $e->getMessage(),
            ]);

            return [
                'score' => 0,
                'status' => 'indisponible',
                'color' => '#6b7280',
                'criteria' => [],
                'weights' => $this->normalizeWeights($this->weights ?: self::DEFAULT_WEIGHTS),
                'meta' => ['total_stocks' => 0],
            ];
        }
    }

    /**
     * @param array<string, float|int> $weights
     * @return array<string, float>
     */
    private function normalizeWeights(array $weights): array
    {
        $sum = 0.0;
        foreach (self::DEFAULT_WEIGHTS as $key => $value) {
            $weights[$key] = isset($weights[$key]) ? (float) $weights[$key] : (float) $value;
            $sum += $weights[$key];
        }

        if ($sum <= 0) {
            return self::DEFAULT_WEIGHTS;
        }

        foreach ($weights as $key => $value) {
            $weights[$key] = round(((float) $value) / $sum, 4);
        }

        return $weights;
    }

    /**
     * @param array<string, float> $weights
     * @return array<string, mixed>
     */
    private function buildEmptyScore(array $weights): array
    {
        $score = 50.0;
        return [
            'score' => $score,
            'status' => $this->statusFromScore($score),
            'color' => $this->colorFromScore($score),
            'criteria' => [
                'rupture' => ['count' => 0, 'rate' => 0.0, 'score' => 100],
                'expiration' => ['count' => 0, 'rate' => 0.0, 'score' => 100],
                'rotation' => ['ratio' => 0.0, 'score' => 0],
                'seuils' => ['count' => 0, 'rate' => 0.0, 'score' => 100],
            ],
            'weights' => $weights,
            'meta' => [
                'total_stocks' => 0,
            ],
        ];
    }

    private function scoreFromPenaltyRate(float $rate): float
    {
        $score = 100 - max(0.0, min(1.0, $rate)) * 100;
        return round($score, 2);
    }

    private function scoreFromBonusRate(float $rate): float
    {
        $score = max(0.0, min(1.0, $rate)) * 100;
        return round($score, 2);
    }

    private function statusFromScore(float $score): string
    {
        if ($score >= 80) {
            return 'sain';
        }
        if ($score >= 50) {
            return 'surveiller';
        }
        return 'risque';
    }

    private function colorFromScore(float $score): string
    {
        if ($score >= 80) {
            return '#16a34a';
        }
        if ($score >= 50) {
            return '#f59e0b';
        }
        return '#dc2626';
    }
}
