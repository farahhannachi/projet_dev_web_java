package org.example.model;

import java.time.LocalDateTime;

public class Stock {
    private int id;
    private Produit produit;
    private int quantiteDisponible;
    private int seuilMinimum;
    private Depot depot;
    private LocalDateTime derniereSortie;
    private LocalDateTime dateDerniereMiseAJour;
    private int quantiteInitiale;

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

    public int getSeuilMinimum() { return seuilMinimum; }
    public void setSeuilMinimum(int seuilMinimum) { this.seuilMinimum = seuilMinimum; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public LocalDateTime getDerniereSortie() {
        return derniereSortie;
    }

    public void setDerniereSortie(LocalDateTime derniereSortie) {
        this.derniereSortie = derniereSortie;
    }

    public LocalDateTime getDateDerniereMiseAJour() {
        return dateDerniereMiseAJour;
    }

    public void setDateDerniereMiseAJour(LocalDateTime dateDerniereMiseAJour) {
        this.dateDerniereMiseAJour = dateDerniereMiseAJour;
    }

    public int getQuantiteInitiale() {
        return quantiteInitiale;
    }

    public void setQuantiteInitiale(int quantiteInitiale) {
        this.quantiteInitiale = quantiteInitiale;
    }

    // Alias pour la compatibilité (getQuantite/setQuantite)
    public int getQuantite() { return quantiteDisponible; }
    public void setQuantite(int quantite) { this.quantiteDisponible = quantite; }

    public boolean isStockFaible() {
        return quantiteDisponible <= seuilMinimum;
    }

    @Override
    public String toString() {
        return produit.getNom() + " - Stock: " + quantiteDisponible;
    }
}
