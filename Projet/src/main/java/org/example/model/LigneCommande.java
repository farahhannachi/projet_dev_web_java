package org.example.model;

/**
 * Représente une ligne d'une commande (article avec quantité et prix unitaire)
 * Établit le lien entre Commande et Produit avec quantité commandée
 */
public class LigneCommande {
    private int id;
    private Produit produit;
    private int quantite;
    private double prixUnitaire;
    private Depot depot; // Le dépôt d'où provient cet article (optionnel)
    private Stock stock; // Référence au stock spécifique (optionnel)

    public LigneCommande() {}

    public LigneCommande(Produit produit, int quantite, double prixUnitaire) {
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public LigneCommande(Produit produit, int quantite, double prixUnitaire, Depot depot) {
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.depot = depot;
    }

    public LigneCommande(Produit produit, int quantite, double prixUnitaire, Stock stock) {
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.stock = stock;
        if (stock != null && stock.getDepot() != null) {
            this.depot = stock.getDepot();
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { 
        this.stock = stock;
        if (stock != null && stock.getDepot() != null) {
            this.depot = stock.getDepot();
        }
    }

    /**
     * Calcule le montant total pour cette ligne (quantité × prix unitaire)
     */
    public double getMontantTotal() {
        return quantite * prixUnitaire;
    }

    @Override
    public String toString() {
        return (produit != null ? produit.getNom() : "Produit inconnu") + 
               " - Quantité: " + quantite + 
               " - Prix unitaire: " + prixUnitaire +
               " - Total: " + getMontantTotal();
    }
}

