<?php

namespace App\Service;

use App\Entity\Stock;
use App\Repository\StockRepository;
use App\Repository\VenteRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;

class StockService
{
    private StockRepository $stockRepository;
    private VenteRepository $venteRepository;
    private EntityManagerInterface $entityManager;
    private ParameterBagInterface $parameterBag;

    public function __construct(
        StockRepository $stockRepository,
        VenteRepository $venteRepository,
        EntityManagerInterface $entityManager,
        ParameterBagInterface $parameterBag
    ) {
        $this->stockRepository = $stockRepository;
        $this->venteRepository = $venteRepository;
        $this->entityManager = $entityManager;
        $this->parameterBag = $parameterBag;
    }

    /**
     * Gestion des entrées de stock
     */
    public function entrerStock(Stock $stock, int $quantite, string $motif = null, array $donneesSupplementaires = []): bool
    {
        if ($quantite <= 0) {
            return false;
        }

        $stock->entrerStock($quantite, $motif);
        
        // Mettre à jour les données supplémentaires
        if (isset($donneesSupplementaires['prixAchat'])) {
            $stock->setPrixAchatUnitaire($donneesSupplementaires['prixAchat']);
        }
        if (isset($donneesSupplementaires['prixVente'])) {
            $stock->setPrixVenteUnitaire($donneesSupplementaires['prixVente']);
        }
        if (isset($donneesSupplementaires['fournisseur'])) {
            $stock->setFournisseur($donneesSupplementaires['fournisseur']);
        }
        if (isset($donneesSupplementaires['batchNumber'])) {
            $stock->setBatchNumber($donneesSupplementaires['batchNumber']);
        }
        if (isset($donneesSupplementaires['emplacement'])) {
            $stock->setEmplacement($donneesSupplementaires['emplacement']);
        }

        $this->entityManager->flush();
        return true;
    }

    /**
     * Gestion des sorties de stock
     */
    public function sortirStock(Stock $stock, int $quantite, string $motif = null): bool
    {
        if ($quantite <= 0) {
            return false;
        }

        $success = $stock->sortirStock($quantite, $motif);
        
        if ($success) {
            $this->entityManager->flush();
        }

        return $success;
    }

    /**
     * Transfert de stock entre dépôts
     */
    public function transfererStock(Stock $stockSource, Stock $stockDestination, int $quantite, string $motif = null): bool
    {
        if ($quantite <= 0 || $stockSource->getQuantite() < $quantite) {
            return false;
        }

        if ($stockSource->getProduit() !== $stockDestination->getProduit()) {
            return false; // Produits différents
        }

        // Sortie du stock source
        $success = $this->sortirStock($stockSource, $quantite, "Transfert vers {$stockDestination->getDepot()->getNomDepot()}: {$motif}");
        
        if ($success) {
            // Entrée dans le stock destination
            $this->entrerStock($stockDestination, $quantite, "Transfert depuis {$stockSource->getDepot()->getNomDepot()}: {$motif}");
        }

        return $success;
    }

    /**
     * Ajustement de stock (correction)
     */
    public function ajusterStock(Stock $stock, int $nouvelleQuantite, string $motif = null): bool
    {
        $ancienneQuantite = $stock->getQuantite();
        $difference = $nouvelleQuantite - $ancienneQuantite;

        if ($difference > 0) {
            return $this->entrerStock($stock, $difference, "Ajustement positif: {$motif}");
        } elseif ($difference < 0) {
            return $this->sortirStock($stock, abs($difference), "Ajustement négatif: {$motif}");
        }

        return true; // Pas de changement
    }

    /**
     * Obtenir les alertes de stock
     */
    public function getAlertesStock(): array
    {
        $alertes = [
            'rupture' => [],
            'alerte' => [],
            'peremption' => [],
            'proche_peremption' => []
        ];

        $stocks = $this->stockRepository->findAll();

        foreach ($stocks as $stock) {
            // Alertes de rupture
            if ($stock->getQuantite() <= $stock->getSeuilCritique()) {
                $alertes['rupture'][] = [
                    'stock' => $stock,
                    'niveau' => 'critique',
                    'message' => sprintf("Rupture critique de stock pour %s dans %s", 
                        $stock->getProduit()?->getNom(), 
                        $stock->getDepot()?->getNomDepot())
                ];
            } elseif ($stock->getQuantite() <= $stock->getSeuilAlerte()) {
                $alertes['alerte'][] = [
                    'stock' => $stock,
                    'niveau' => 'alerte',
                    'message' => sprintf("Stock faible pour %s dans %s (%d unités)", 
                        $stock->getProduit()?->getNom(), 
                        $stock->getDepot()?->getNomDepot(),
                        $stock->getQuantite())
                ];
            }

            // Alertes de péremption
            if ($stock->estPerime()) {
                $alertes['peremption'][] = [
                    'stock' => $stock,
                    'niveau' => 'critique',
                    'message' => sprintf("Produit périmé: %s dans %s", 
                        $stock->getProduit()?->getNom(), 
                        $stock->getDepot()?->getNomDepot())
                ];
            } elseif ($stock->estProchePeremption()) {
                $jours = $stock->getJoursAvantPeremption();
                $alertes['proche_peremption'][] = [
                    'stock' => $stock,
                    'niveau' => 'warning',
                    'message' => sprintf("Produit proche péremption: %s dans %s (%d jours)", 
                        $stock->getProduit()?->getNom(), 
                        $stock->getDepot()?->getNomDepot(),
                        $jours)
                ];
            }
        }

        return $alertes;
    }

    /**
     * Obtenir les statistiques globales
     */
    public function getStatistiquesGlobales(): array
    {
        $stocks = $this->stockRepository->findAll();
        
        $stats = [
            'total_stocks' => count($stocks),
            'valeur_totale' => 0,
            'total_entrees' => 0,
            'total_sorties' => 0,
            'stocks_par_etat' => [
                'disponible' => 0,
                'alerte' => 0,
                'rupture' => 0,
                'perime' => 0,
                'expire' => 0
            ],
            'stocks_par_depot' => [],
            'produits_plus_vendus' => [],
            'produits_en_roupture' => [],
            'produits_proche_peremption' => []
        ];

        foreach ($stocks as $stock) {
            // Valeur totale
            $valeur = $stock->getValeurStock();
            if ($valeur) {
                $stats['valeur_totale'] += $valeur;
            }

            // Entrées/Sorties
            $stats['total_entrees'] += $stock->getTotalEntrees();
            $stats['total_sorties'] += $stock->getTotalSorties();

            // État des stocks
            $etat = $stock->getEtatStock();
            if (isset($stats['stocks_par_etat'][$etat])) {
                $stats['stocks_par_etat'][$etat]++;
            }

            // Stocks par dépôt
            $depotNom = $stock->getDepot()?->getNomDepot() ?? 'Non assigné';
            if (!isset($stats['stocks_par_depot'][$depotNom])) {
                $stats['stocks_par_depot'][$depotNom] = [
                    'total' => 0,
                    'valeur' => 0,
                    'en_alerte' => 0
                ];
            }
            $stats['stocks_par_depot'][$depotNom]['total']++;
            if ($valeur) {
                $stats['stocks_par_depot'][$depotNom]['valeur'] += $valeur;
            }
            if (in_array($etat, ['alerte', 'rupture'])) {
                $stats['stocks_par_depot'][$depotNom]['en_alerte']++;
            }

            // Produits en rupture
            if ($etat === 'rupture') {
                $produitNom = $stock->getProduit()?->getNom();
                if ($produitNom) {
                    $stats['produits_en_roupture'][] = [
                        'produit' => $produitNom,
                        'depot' => $depotNom,
                        'quantite' => $stock->getQuantite()
                    ];
                }
            }

            // Produits proche péremption
            if ($stock->estProchePeremption() || $stock->estPerime()) {
                $produitNom = $stock->getProduit()?->getNom();
                if ($produitNom) {
                    $stats['produits_proche_peremption'][] = [
                        'produit' => $produitNom,
                        'depot' => $depotNom,
                        'jours_avant_peremption' => $stock->getJoursAvantPeremption(),
                        'date_expiration' => $stock->getDateExpiration()?->format('d/m/Y')
                    ];
                }
            }
        }

        // Trier les produits les plus vendus (basé sur les sorties)
        usort($stats['produits_en_roupture'], fn($a, $b) => $a['quantite'] <=> $b['quantite']);
        usort($stats['produits_proche_peremption'], fn($a, $b) => $a['jours_avant_peremption'] <=> $b['jours_avant_peremption']);

        return $stats;
    }

    /**
     * Optimisation du réapprovisionnement
     */
    public function getSuggestionsReapprovisionnement(): array
    {
        $suggestions = [];
        $stocks = $this->stockRepository->findAll();

        foreach ($stocks as $stock) {
            if ($stock->getQuantite() <= $stock->getSeuilAlerte()) {
                // Calculer la quantité suggérée
                $quantiteSuggeree = max(
                    $stock->getSeuilAlerte() * 2, // Double du seuil d'alerte
                    $stock->getTotalEntrees() > 0 ? intval($stock->getTotalEntrees() / 4) : 50 // 25% des entrées totales
                );

                $suggestions[] = [
                    'stock' => $stock,
                    'quantite_actuelle' => $stock->getQuantite(),
                    'quantite_suggeree' => $quantiteSuggeree,
                    'priorite' => $stock->getQuantite() <= $stock->getSeuilCritique() ? 'critique' : 'haute',
                    'raison' => $stock->getQuantite() <= $stock->getSeuilCritique() ? 
                        'Stock critique - Réapprovisionnement immédiat requis' : 
                        'Stock faible - Réapprovisionnement recommandé',
                    'valeur_estimee' => $stock->getPrixAchatUnitaire() ? 
                        $quantiteSuggeree * $stock->getPrixAchatUnitaire() : null
                ];
            }
        }

        // Trier par priorité
        usort($suggestions, fn($a, $b) => $a['priorite'] === 'critique' ? -1 : ($b['priorite'] === 'critique' ? 1 : 0));

        return $suggestions;
    }

    /**
     * Analyse des tendances de consommation
     */
    public function analyserTendances(Stock $stock, int $jours = 30): array
    {
        $dateDebut = new \DateTime();
        $dateDebut->modify("-{$jours} days");

        // Calculer le taux de consommation moyen
        $totalSorties = $stock->getTotalSorties();
        $tauxJournalier = $totalSorties > 0 ? $totalSorties / max($jours, 1) : 0;

        // Estimer la date de rupture
        $joursRestants = $tauxJournalier > 0 ? intval($stock->getQuantite() / $tauxJournalier) : null;
        $dateRupture = $joursRestants ? (new \DateTime())->modify("+{$joursRestants} days") : null;

        // Analyser la saisonnalité (simplifié)
        $saisonnalite = $this->calculerSaisonnalite($stock);

        return [
            'taux_consommation_journalier' => $tauxJournalier,
            'jours_avant_rupture' => $joursRestants,
            'date_rupture_estimee' => $dateRupture?->format('d/m/Y'),
            'niveau_risque' => $this->calculerNiveauRisque($stock, $joursRestants),
            'saisonnalite' => $saisonnalite,
            'recommendations' => $this->genererRecommendations($stock, $tauxJournalier, $joursRestants)
        ];
    }

    private function calculerSaisonnalite(Stock $stock): array
    {
        // Implémentation simplifiée - pourrait être améliorée avec des données historiques
        $moisActuel = (int)(new \DateTime())->format('m');
        
        $saisonnalite = 'normale';
        if ($moisActuel >= 11 || $moisActuel <= 2) {
            $saisonnalite = 'hivernale';
        } elseif ($moisActuel >= 6 && $moisActuel <= 8) {
            $saisonnalite = 'estivale';
        }

        return [
            'type' => $saisonnalite,
            'impact' => $saisonnalite === 'estivale' ? 'augmentation' : 
                       ($saisonnalite === 'hivernale' ? 'augmentation' : 'stable')
        ];
    }

    private function calculerNiveauRisque(Stock $stock, ?int $joursRestants): string
    {
        if ($stock->estPerime()) {
            return 'critique';
        }

        if ($joursRestants === null || $joursRestants <= 7) {
            return 'critique';
        } elseif ($joursRestants <= 15) {
            return 'eleve';
        } elseif ($joursRestants <= 30) {
            return 'modere';
        }

        return 'faible';
    }

    private function genererRecommendations(Stock $stock, float $tauxJournalier, ?int $joursRestants): array
    {
        $recommendations = [];

        if ($stock->getQuantite() <= $stock->getSeuilCritique()) {
            $recommendations[] = "Réapprovisionnement immédiat requis - Stock critique";
        }

        if ($stock->estProchePeremption()) {
            $recommendations[] = "Promotion suggérée - Produit proche de la péremption";
        }

        if ($joursRestants && $joursRestants <= 15) {
            $recommendations[] = "Commande à planifier - Rupture prévue dans {$joursRestants} jours";
        }

        if ($tauxJournalier > 0 && $stock->getQuantite() / $tauxJournalier > 60) {
            $recommendations[] = "Surstock détecté - Considérer réduction des commandes";
        }

        return $recommendations;
    }

    /**
     * Générer un rapport d'inventaire
     */
    public function genererRapportInventaire(): array
    {
        $stats = $this->getStatistiquesGlobales();
        $alertes = $this->getAlertesStock();
        $suggestions = $this->getSuggestionsReapprovisionnement();

        return [
            'date_generation' => (new \DateTime())->format('d/m/Y H:i'),
            'statistiques' => $stats,
            'alertes' => $alertes,
            'suggestions' => $suggestions,
            'resume' => [
                'total_alertes' => count($alertes['rupture']) + count($alertes['alerte']) + count($alertes['peremption']),
                'total_suggestions' => count($suggestions),
                'valeur_totale_stock' => $stats['valeur_totale'],
                'taux_rotation_global' => $stats['total_entrees'] > 0 ? 
                    round(($stats['total_sorties'] / $stats['total_entrees']) * 100, 2) : 0
            ]
        ];
    }

    /**
     * 🎯 PRÉVISION INTELLIGENTE DE STOCK
     * Calcule automatiquement le nombre de jours restants avant rupture de stock pour chaque produit
     */
    public function calculerPrevisionStock(Stock $stock): array
    {
        // Récupérer la quantité actuelle en stock du produit
        $stockActuel = $stock->getQuantite();
        
        // Calculer la consommation moyenne journalière basée sur les ventes des 30 derniers jours
        $moyenneVenteJournaliere = $this->venteRepository->getConsommationMoyenneJournaliere(
            $stock->getProduit()?->getId() ?? 0
        );

        // ⚠️ Cas particulier : Si la moyenne journalière est égale à 0
        if ($moyenneVenteJournaliere == 0) {
            return [
                'stock_actuel' => $stockActuel,
                'moyenne_vente_journaliere' => 0,
                'jours_restants' => null,
                'statut' => 'STABLE',
                'message' => 'Stock stable - Aucune vente récente détectée',
                'date_rupture_estimee' => null,
                'niveau_risque' => 'faible'
            ];
        }

        // Calculer le nombre de jours restants avant rupture selon la formule
        $joursRestants = intval($stockActuel / $moyenneVenteJournaliere);

        // 🎨 Classification pour affichage dashboard
        $statut = $this->classifierStatutPrevision($joursRestants);
        $niveauRisque = $this->calculerNiveauRisquePrevision($joursRestants);

        // Calculer la date de rupture estimée
        $dateRuptureEstimee = null;
        if ($joursRestants > 0) {
            $dateRuptureEstimee = (new \DateTime())->modify("+{$joursRestants} days");
        }

        return [
            'stock_actuel' => $stockActuel,
            'moyenne_vente_journaliere' => round($moyenneVenteJournaliere, 2),
            'jours_restants' => $joursRestants,
            'statut' => $statut,
            'message' => $this->genererMessagePrevision($joursRestants, $moyenneVenteJournaliere),
            'date_rupture_estimee' => $dateRuptureEstimee?->format('d/m/Y'),
            'niveau_risque' => $niveauRisque,
            'recommendations' => $this->genererRecommendationsPrevision($stock, $joursRestants, $moyenneVenteJournaliere)
        ];
    }

    /**
     * Calcule les prévisions pour tous les stocks
     */
    public function calculerPrevisionsTousStocks(): array
    {
        $stocks = $this->stockRepository->findAll();
        $previsions = [];

        foreach ($stocks as $stock) {
            if ($stock->getProduit()) {
                $previsions[] = [
                    'stock' => $stock,
                    'prevision' => $this->calculerPrevisionStock($stock)
                ];
            }
        }

        // Trier par niveau de risque (critique en premier)
        usort($previsions, function($a, $b) {
            $ordreRisque = ['critique' => 0, 'eleve' => 1, 'modere' => 2, 'faible' => 3];
            $niveauA = $a['prevision']['niveau_risque'];
            $niveauB = $b['prevision']['niveau_risque'];
            
            return $ordreRisque[$niveauA] ?? 4 <=> ($ordreRisque[$niveauB] ?? 4);
        });

        return $previsions;
    }

    /**
     * 🎨 Classification pour affichage dashboard
     */
    private function classifierStatutPrevision(int $joursRestants): string
    {
        if ($joursRestants > 30) {
            return 'STABLE'; // vert
        } elseif ($joursRestants >= 7 && $joursRestants <= 30) {
            return 'A_SURVEILLER'; // orange
        } else {
            return 'RUPTURE_IMMINENTE'; // rouge
        }
    }

    /**
     * Calcule le niveau de risque pour prévision
     */
    private function calculerNiveauRisquePrevision(int $joursRestants): string
    {
        if ($joursRestants <= 3) {
            return 'critique';
        } elseif ($joursRestants <= 7) {
            return 'eleve';
        } elseif ($joursRestants <= 15) {
            return 'modere';
        } else {
            return 'faible';
        }
    }

    /**
     * Génère un message explicatif pour la prévision
     */
    private function genererMessagePrevision(int $joursRestants, float $moyenneVenteJournaliere): string
    {
        if ($joursRestants <= 0) {
            return 'Rupture de stock imminente - Réapprovisionnement urgent requis';
        } elseif ($joursRestants <= 7) {
            return "Rupture prévue dans {$joursRestants} jours - Commande à planifier rapidement";
        } elseif ($joursRestants <= 30) {
            return "Stock suffisant pour {$joursRestants} jours - Surveillance recommandée";
        } else {
            return "Stock confortable - Prochaine commande à planifier dans {$joursRestants} jours";
        }
    }

    /**
     * Génère des recommandations basées sur la prévision
     */
    private function genererRecommendationsPrevision(Stock $stock, int $joursRestants, float $moyenneVenteJournaliere): array
    {
        $recommendations = [];

        if ($joursRestants <= 7) {
            $recommendations[] = [
                'type' => 'urgent',
                'message' => 'Réapprovisionnement immédiat requis',
                'action' => 'Commander maintenant'
            ];
        }

        if ($stock->estProchePeremption()) {
            $recommendations[] = [
                'type' => 'peremption',
                'message' => 'Produit proche de la péremption',
                'action' => 'Promotion ou destruction'
            ];
        }

        if ($moyenneVenteJournaliere > 0 && $joursRestants > 60) {
            $recommendations[] = [
                'type' => 'surstock',
                'message' => 'Risque de surstock détecté',
                'action' => 'Réduire les commandes'
            ];
        }

        if ($stock->getQuantite() <= $stock->getSeuilAlerte()) {
            $recommendations[] = [
                'type' => 'seuil',
                'message' => 'Stock en dessous du seuil d\'alerte',
                'action' => 'Déclencher alerte'
            ];
        }

        return $recommendations;
    }

    /**
     * Obtient les statistiques de prévision pour le dashboard
     */
    public function getStatistiquesPrevisionDashboard(): array
    {
        $previsions = $this->calculerPrevisionsTousStocks();
        
        $stats = [
            'total_stocks' => count($previsions),
            'stocks_critiques' => 0,
            'stocks_a_surveiller' => 0,
            'stocks_stables' => 0,
            'stocks_stable_zero_vente' => 0,
            'ruptures_imminentes' => [],
            'top_risques' => array_slice($previsions, 0, 5) // Top 5 des plus à risque
        ];

        foreach ($previsions as $item) {
            $prevision = $item['prevision'];
            $statut = $prevision['statut'];

            switch ($statut) {
                case 'RUPTURE_IMMINENTE':
                    $stats['stocks_critiques']++;
                    if ($prevision['jours_restants'] <= 7) {
                        $stats['ruptures_imminentes'][] = $item;
                    }
                    break;
                case 'A_SURVEILLER':
                    $stats['stocks_a_surveiller']++;
                    break;
                case 'STABLE':
                    if ($prevision['moyenne_vente_journaliere'] == 0) {
                        $stats['stocks_stable_zero_vente']++;
                    } else {
                        $stats['stocks_stables']++;
                    }
                    break;
            }
        }

        return $stats;
    }
}
