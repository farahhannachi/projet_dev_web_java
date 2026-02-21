<?php

namespace App\Service;

use App\Repository\ProduitRepository;
use Doctrine\ORM\EntityManagerInterface;

class RecommandationTraitementService
{
    private ProduitRepository $produitRepository;
    private EntityManagerInterface $entityManager;

    // Mots-clés pour la correspondance symptômes -> catégories
    private array $motsClésSymptomes = [
        // Douleurs générales
        'douleur' => ['catégories' => ['Antidouleurs', 'Anti-inflammatoires', 'Analgésiques'], 'poids' => 4],
        'mal' => ['catégories' => ['Antidouleurs', 'Anti-inflammatoires', 'Analgésiques'], 'poids' => 4],
        'souffre' => ['catégories' => ['Antidouleurs', 'Anti-inflammatoires'], 'poids' => 3],
        
        // Mal de tête spécifique
        'tete' => ['catégories' => ['Antidouleurs', 'Analgésiques', 'Antipyrétiques', 'Céphalées'], 'poids' => 5],
        'crane' => ['catégories' => ['Antidouleurs', 'Analgésiques'], 'poids' => 5],
        'migraine' => ['catégories' => ['Antidouleurs', 'Anti-inflammatoires', 'Analgésiques'], 'poids' => 6],
        'cephalee' => ['catégories' => ['Antidouleurs', 'Analgésiques'], 'poids' => 5],
        
        // Fièvre
        'fievre' => ['catégories' => ['Antipyrétiques', 'Antidouleurs', 'Analgésiques'], 'poids' => 5],
        'temperature' => ['catégories' => ['Antipyrétiques', 'Antidouleurs'], 'poids' => 4],
        'chaud' => ['catégories' => ['Antipyrétiques'], 'poids' => 3],
        
        // Rhume et toux
        'toux' => ['catégories' => ['Antitussifs', 'Sirops', 'Expectorants'], 'poids' => 5],
        'rhume' => ['catégories' => ['Décongestionnants', 'Antitussifs', 'Rhume'], 'poids' => 5],
        'nez' => ['catégories' => ['Décongestionnants', 'Rhume'], 'poids' => 4],
        'gorge' => ['catégories' => ['Pastilles', 'Anti-inflammatoires', 'Maux de gorge'], 'poids' => 5],
        'grippe' => ['catégories' => ['Antipyrétiques', 'Antidouleurs', 'Rhume'], 'poids' => 5],
        
        // Problèmes digestifs
        'estomac' => ['catégories' => ['Antiacides', 'Digestifs', 'Gastro'], 'poids' => 5],
        'ventre' => ['catégories' => ['Antispasmodiques', 'Digestifs', 'Gastro'], 'poids' => 4],
        'digestion' => ['catégories' => ['Digestifs', 'Probiotiques'], 'poids' => 4],
        'nausee' => ['catégories' => ['Antiémétiques', 'Digestifs'], 'poids' => 4],
        'vomissement' => ['catégories' => ['Antiémétiques', 'Digestifs'], 'poids' => 5],
        'diarrhee' => ['catégories' => ['Antidiarrhéiques', 'Digestifs'], 'poids' => 5],
        
        // Allergies
        'allergie' => ['catégories' => ['Antihistaminiques', 'Allergies'], 'poids' => 6],
        'demangeaison' => ['catégories' => ['Antihistaminiques', 'Crèmes', 'Allergies'], 'poids' => 4],
        'eczema' => ['catégories' => ['Dermatologie', 'Crèmes', 'Allergies'], 'poids' => 5],
        
        // Peau
        'peau' => ['catégories' => ['Dermatologie', 'Crèmes', 'Soins'], 'poids' => 3],
        'plaie' => ['catégories' => ['Antiseptiques', 'Pansements', 'Cicatrisants'], 'poids' => 5],
        'brulure' => ['catégories' => ['Crèmes', 'Antiseptiques', 'Cicatrisants'], 'poids' => 5],
        
        // Stress et sommeil
        'stress' => ['catégories' => ['Anxiolytiques', 'Compléments', 'Relaxants'], 'poids' => 4],
        'sommeil' => ['catégories' => ['Somnifères', 'Compléments', 'Relaxants'], 'poids' => 5],
        'insomnie' => ['catégories' => ['Somnifères', 'Compléments'], 'poids' => 5],
        'anxiete' => ['catégories' => ['Anxiolytiques', 'Compléments'], 'poids' => 4],
        
        // Fatigue et vitamines
        'fatigue' => ['catégories' => ['Vitamines', 'Compléments', 'Toniques'], 'poids' => 4],
        'vitamine' => ['catégories' => ['Vitamines', 'Compléments'], 'poids' => 4],
        'energie' => ['catégories' => ['Vitamines', 'Compléments', 'Toniques'], 'poids' => 3],
        
        // Douleurs musculaires et articulaires
        'articulation' => ['catégories' => ['Anti-inflammatoires', 'Antidouleurs', 'Rhumatologie'], 'poids' => 5],
        'muscle' => ['catégories' => ['Anti-inflammatoires', 'Antidouleurs', 'Myorelaxants'], 'poids' => 4],
        'dos' => ['catégories' => ['Anti-inflammatoires', 'Antidouleurs', 'Analgésiques'], 'poids' => 4],
        'courbature' => ['catégories' => ['Anti-inflammatoires', 'Antidouleurs'], 'poids' => 4],
        
        // Infections
        'infection' => ['catégories' => ['Antiseptiques', 'Antibiotiques', 'Anti-infectieux'], 'poids' => 5],
    ];

    public function __construct(
        ProduitRepository $produitRepository,
        EntityManagerInterface $entityManager
    ) {
        $this->produitRepository = $produitRepository;
        $this->entityManager = $entityManager;
    }

    /**
     * Recommande des produits basés sur la description des symptômes
     */
    public function recommanderProduits(string $description, ?string $antecedents = null, int $limite = 5): array
    {
        // Normaliser la description
        $descriptionNormalisee = $this->normaliserTexte($description);
        $antecedentsNormalises = $antecedents ? $this->normaliserTexte($antecedents) : '';

        // Analyser les symptômes et calculer les scores par catégorie
        $scoresCategories = $this->analyserSymptomes($descriptionNormalisee);

        // Si aucune catégorie n'est identifiée, retourner les produits populaires
        if (empty($scoresCategories)) {
            return $this->obtenirProduitsPopulaires($limite);
        }

        // Récupérer tous les produits disponibles
        $produits = $this->produitRepository->createQueryBuilder('p')
            ->where('p.statut IN (:statuts)')
            ->setParameter('statuts', ['disponible', 'stock_critique'])
            ->getQuery()
            ->getResult();

        // Calculer le score de pertinence pour chaque produit
        $produitsAvecScores = [];
        foreach ($produits as $produit) {
            $score = $this->calculerScoreProduit($produit, $scoresCategories, $descriptionNormalisee, $antecedentsNormalises);
            
            if ($score > 0) {
                $produitsAvecScores[] = [
                    'produit' => $produit,
                    'score' => $score,
                    'raison' => $this->genererRaisonRecommandation($produit, $scoresCategories, $descriptionNormalisee)
                ];
            }
        }

        // Trier par score décroissant
        usort($produitsAvecScores, function($a, $b) {
            return $b['score'] <=> $a['score'];
        });

        // Retourner les meilleurs résultats
        return array_slice($produitsAvecScores, 0, $limite);
    }

    /**
     * Normalise le texte pour l'analyse
     */
    private function normaliserTexte(string $texte): string
    {
        $texte = mb_strtolower($texte, 'UTF-8');
        $texte = $this->supprimerAccents($texte);
        return $texte;
    }

    /**
     * Supprime les accents d'une chaîne
     */
    private function supprimerAccents(string $texte): string
    {
        $accents = [
            'à' => 'a', 'á' => 'a', 'â' => 'a', 'ã' => 'a', 'ä' => 'a',
            'è' => 'e', 'é' => 'e', 'ê' => 'e', 'ë' => 'e',
            'ì' => 'i', 'í' => 'i', 'î' => 'i', 'ï' => 'i',
            'ò' => 'o', 'ó' => 'o', 'ô' => 'o', 'õ' => 'o', 'ö' => 'o',
            'ù' => 'u', 'ú' => 'u', 'û' => 'u', 'ü' => 'u',
            'ç' => 'c', 'ñ' => 'n'
        ];
        return strtr($texte, $accents);
    }

    /**
     * Analyse les symptômes et retourne les scores par catégorie
     */
    private function analyserSymptomes(string $description): array
    {
        $scores = [];

        foreach ($this->motsClésSymptomes as $motCle => $config) {
            if (strpos($description, $motCle) !== false) {
                foreach ($config['catégories'] as $categorie) {
                    if (!isset($scores[$categorie])) {
                        $scores[$categorie] = 0;
                    }
                    $scores[$categorie] += $config['poids'];
                }
            }
        }

        arsort($scores);
        return $scores;
    }

    /**
     * Calcule le score de pertinence d'un produit
     */
    private function calculerScoreProduit($produit, array $scoresCategories, string $description, string $antecedents): float
    {
        $score = 0;

        // Score basé sur la catégorie
        $categorieProduit = $this->normaliserTexte($produit->getCategorie() ?? '');
        foreach ($scoresCategories as $categorie => $scoreCategorie) {
            $categorieNormalisee = $this->normaliserTexte($categorie);
            
            // Match exact ou partiel de la catégorie
            if (strpos($categorieProduit, $categorieNormalisee) !== false || 
                strpos($categorieNormalisee, $categorieProduit) !== false) {
                $score += $scoreCategorie * 3; // Augmenté de 2 à 3
            }
        }

        // Score basé sur le nom et la description du produit
        $nomProduit = $this->normaliserTexte($produit->getNom());
        $descriptionProduit = $this->normaliserTexte($produit->getDescription() ?? '');

        // Vérifier si des mots-clés symptômes apparaissent dans le produit
        foreach ($this->motsClésSymptomes as $motCle => $config) {
            if (strpos($description, $motCle) !== false) {
                // Si le mot-clé apparaît dans le nom du produit
                if (strpos($nomProduit, $motCle) !== false) {
                    $score += $config['poids'] * 2;
                }
                // Si le mot-clé apparaît dans la description du produit
                if (strpos($descriptionProduit, $motCle) !== false) {
                    $score += $config['poids'];
                }
                // Si le mot-clé apparaît dans la catégorie du produit
                if (strpos($categorieProduit, $motCle) !== false) {
                    $score += $config['poids'] * 1.5;
                }
            }
        }

        // Vérifier si des mots de la description apparaissent dans le produit
        $motsDescription = explode(' ', $description);
        foreach ($motsDescription as $mot) {
            if (strlen($mot) > 3) { // Ignorer les mots trop courts
                if (strpos($nomProduit, $mot) !== false) {
                    $score += 2;
                }
                if (strpos($descriptionProduit, $mot) !== false) {
                    $score += 0.5;
                }
                if (strpos($categorieProduit, $mot) !== false) {
                    $score += 1;
                }
            }
        }

        // Bonus pour les produits avec un bon statut
        if ($produit->getStatut() === 'disponible') {
            $score += 3; // Augmenté de 2 à 3
        }

        return $score;
    }

    /**
     * Génère une raison pour la recommandation
     */
    private function genererRaisonRecommandation($produit, array $scoresCategories, string $description): string
    {
        $raisons = [];

        // Raison basée sur la catégorie
        $categorieProduit = $this->normaliserTexte($produit->getCategorie() ?? '');
        foreach ($scoresCategories as $categorie => $score) {
            $categorieNormalisee = $this->normaliserTexte($categorie);
            if (strpos($categorieProduit, $categorieNormalisee) !== false || 
                strpos($categorieNormalisee, $categorieProduit) !== false) {
                $raisons[] = "Adapté pour " . strtolower($categorie);
                break;
            }
        }

        // Raison basée sur les symptômes détectés
        $symptomesDetectes = [];
        foreach ($this->motsClésSymptomes as $motCle => $config) {
            if (strpos($description, $motCle) !== false) {
                $symptomesDetectes[] = $motCle;
            }
        }

        if (!empty($symptomesDetectes)) {
            $raisons[] = "Correspond à vos symptômes: " . implode(', ', array_slice($symptomesDetectes, 0, 3));
        }

        return !empty($raisons) ? implode('. ', $raisons) : "Recommandé pour votre situation";
    }

    /**
     * Retourne les produits populaires par défaut
     */
    private function obtenirProduitsPopulaires(int $limite): array
    {
        $produits = $this->produitRepository->createQueryBuilder('p')
            ->where('p.statut = :statut')
            ->setParameter('statut', 'disponible')
            ->orderBy('p.nom', 'ASC')
            ->setMaxResults($limite)
            ->getQuery()
            ->getResult();

        $resultat = [];
        foreach ($produits as $produit) {
            $resultat[] = [
                'produit' => $produit,
                'score' => 1,
                'raison' => 'Produit populaire et disponible'
            ];
        }

        return $resultat;
    }
}
