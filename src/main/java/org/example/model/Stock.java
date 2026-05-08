package org.example.model;

public class Stock {
    private int id;
    private Produit produit;
    private int quantiteDisponible;
    private int seuilMinimum;
    private Depot depot;
    /** Dernière sortie stock (tracée par la consommation service). */
    private java.time.LocalDateTime derniereSortie;

    public Stock() {}

    public Stock(int id, Produit produit, int quantiteDisponible, int seuilMinimum, Depot depot) {
        this.id = id;
        this.produit = produit;
        this.quantiteDisponible = quantiteDisponible;
        this.seuilMinimum = seuilMinimum;
        this.depot = depot;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public int getQuantiteDisponible() { return quantiteDisponible; }
    public void setQuantiteDisponible(int quantiteDisponible) { this.quantiteDisponible = quantiteDisponible; }

    /** Alias utilisé par la consommation service / écrans dépôt (quantité disponible). */
    public int getQuantite() { return quantiteDisponible; }
    public void setQuantite(int quantite) { this.quantiteDisponible = quantite; }

    public int getSeuilMinimum() { return seuilMinimum; }
    public void setSeuilMinimum(int seuilMinimum) { this.seuilMinimum = seuilMinimum; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public java.time.LocalDateTime getDerniereSortie() { return derniereSortie; }
    public void setDerniereSortie(java.time.LocalDateTime derniereSortie) { this.derniereSortie = derniereSortie; }

    public boolean isStockFaible() {
        return quantiteDisponible <= seuilMinimum;
    }

    @Override
    public String toString() {
        return produit.getNom() + " - Stock: " + quantiteDisponible;
    }
}
