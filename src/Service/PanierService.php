<?php

namespace App\Service;

use Symfony\Component\HttpFoundation\RequestStack;

class PanierService
{
    private RequestStack $requestStack;
    private const PRODUITS_KEY = 'panier_produits';
    private const COMMANDES_KEY = 'panier_commandes';

    public function __construct(RequestStack $requestStack)
    {
        $this->requestStack = $requestStack;
    }

    public function ajouterCommande(int $commandeId): void
    {
        $panier = $this->getPanier();

        if (!in_array($commandeId, $panier, true)) {
            $panier[] = $commandeId;
            $this->getSession()->set(self::COMMANDES_KEY, $panier);
        }
    }

    /**
     * @return array<int>
     */
    public function getPanier(): array
    {
        return $this->getSession()->get(self::COMMANDES_KEY, []);
    }

    public function getNombreArticles(): int
    {
        $produits = $this->getProduitsPanier();

        if (!empty($produits)) {
            return array_sum($produits);
        }

        return count($this->getPanier());
    }

    public function viderPanier(): void
    {
        $this->getSession()->remove(self::COMMANDES_KEY);
        $this->getSession()->remove(self::PRODUITS_KEY);
    }

    public function retirerCommande(int $commandeId): void
    {
        $panier = $this->getPanier();
        $key = array_search($commandeId, $panier, true);

        if ($key !== false) {
            unset($panier[$key]);
            $this->getSession()->set(self::COMMANDES_KEY, array_values($panier));
        }
    }

    public function estDansPanier(int $commandeId): bool
    {
        return in_array($commandeId, $this->getPanier(), true);
    }

    /**
     * @return array<int, int>
     */
    public function getProduitsPanier(): array
    {
        $panier = $this->getSession()->get(self::PRODUITS_KEY, []);

        if (!is_array($panier)) {
            return [];
        }

        $clean = [];
        foreach ($panier as $productId => $quantity) {
            $id = (int) $productId;
            $qty = (int) $quantity;
            if ($id > 0 && $qty > 0) {
                $clean[$id] = $qty;
            }
        }

        return $clean;
    }

    public function ajouterProduit(int $productId, int $quantity = 1): void
    {
        if ($productId <= 0 || $quantity <= 0) {
            return;
        }

        $panier = $this->getProduitsPanier();
        $panier[$productId] = ($panier[$productId] ?? 0) + $quantity;
        $this->getSession()->set(self::PRODUITS_KEY, $panier);
    }

    public function setQuantiteProduit(int $productId, int $quantity): void
    {
        $panier = $this->getProduitsPanier();

        if ($quantity <= 0) {
            unset($panier[$productId]);
        } elseif ($productId > 0) {
            $panier[$productId] = $quantity;
        }

        $this->getSession()->set(self::PRODUITS_KEY, $panier);
    }

    public function retirerProduit(int $productId): void
    {
        $panier = $this->getProduitsPanier();
        unset($panier[$productId]);
        $this->getSession()->set(self::PRODUITS_KEY, $panier);
    }

    public function viderPanierProduits(): void
    {
        $this->getSession()->remove(self::PRODUITS_KEY);
    }

    /**
     * @return array<string, mixed>
     */
    public function getPanierDetails(): array
    {
        $produitsPanier = $this->getProduitsPanier();
        
        // Build items array for the template
        $items = [];
        $total = 0;
        
        foreach ($produitsPanier as $productId => $quantity) {
            $items[] = [
                'id' => $productId,
                'quantity' => $quantity,
                'price' => 0, // Will be filled by the caller if needed
                'lineTotal' => 0,
            ];
        }
        
        return [
            'items' => $items,
            'total' => $total,
            'count' => $this->getNombreArticles(),
            'produits' => $produitsPanier,
            'commandes' => $this->getPanier()
        ];
    }

    private function getSession(): \Symfony\Component\HttpFoundation\Session\SessionInterface
    {
        return $this->requestStack->getSession();
    }
}
