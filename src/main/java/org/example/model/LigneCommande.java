package org.example.model;

public class LigneCommande {
    private int id;
    private int commandeId;
    private int produitId;
    private Produit produit;
    private int quantite;
    private double prixUnitaire;
    private double remise; // En pourcentage
    private double totalLigne;

    public LigneCommande() {}

    public LigneCommande(int commandeId, int produitId, Produit produit, int quantite, double prixUnitaire, double remise) {
        this.commandeId = commandeId;
        this.produitId = produitId;
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.remise = remise;
        this.totalLigne = calculerTotal();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; this.totalLigne = calculerTotal(); }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; this.totalLigne = calculerTotal(); }

    public double getRemise() { return remise; }
    public void setRemise(double remise) { this.remise = remise; this.totalLigne = calculerTotal(); }

    public double getTotalLigne() { return totalLigne; }

    // Calcul du total ligne
    private double calculerTotal() {
        double sousTotal = quantite * prixUnitaire;
        double montantRemise = sousTotal * (remise / 100.0);
        return sousTotal - montantRemise;
    }

    @Override
    public String toString() {
        return produit.getNom() + " x" + quantite + " = " + totalLigne + "€";
    }
}

