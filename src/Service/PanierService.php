<?php

namespace App\Service;

use Symfony\Component\HttpFoundation\RequestStack;

/**
 * Service pour gérer le panier d'achats
 */
class PanierService
{
    private RequestStack $requestStack;

    public function __construct(RequestStack $requestStack)
    {
        $this->requestStack = $requestStack;
    }

    /**
     * Ajoute une commande au panier
     */
    public function ajouterCommande(int $commandeId): void
    {
        $panier = $this->getPanier();
        
        // Ajouter la commande au panier
        if (!in_array($commandeId, $panier)) {
            $panier[] = $commandeId;
            $this->getSession()->set('panier_commandes', $panier);
        }
    }

    /**
     * Récupère le panier actuel
     */
    public function getPanier(): array
    {
        return $this->getSession()->get('panier_commandes', []);
    }

    /**
     * Compte le nombre d'articles dans le panier
     */
    public function getNombreArticles(): int
    {
        return count($this->getPanier());
    }

    /**
     * Vide le panier
     */
    public function viderPanier(): void
    {
        $this->getSession()->remove('panier_commandes');
    }

    /**
     * Retire une commande du panier
     */
    public function retirerCommande(int $commandeId): void
    {
        $panier = $this->getPanier();
        $key = array_search($commandeId, $panier);
        
        if ($key !== false) {
            unset($panier[$key]);
            $this->getSession()->set('panier_commandes', array_values($panier));
        }
    }

    /**
     * Vérifie si une commande est dans le panier
     */
    public function estDansPanier(int $commandeId): bool
    {
        return in_array($commandeId, $this->getPanier());
    }

    /**
     * Récupère la session actuelle
     */
    private function getSession()
    {
        return $this->requestStack->getSession();
    }
}
