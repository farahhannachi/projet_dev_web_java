package org.example.model;

import java.time.LocalDate;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private int quantiteStock;
    private String categorie;
    private boolean disponible;
    private String image;
    private LocalDate dateExpiration;
    /** Statut métier : disponible, indisponible, rupture (écrans back-office). */
    private String statut;

    public Produit() {}

    public Produit(int id, String nom, String description, double prix, int quantiteStock, String categorie, boolean disponible) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantiteStock = quantiteStock;
        this.categorie = categorie;
        this.disponible = disponible;
        this.statut = disponible ? "disponible" : "indisponible";
    }

    /** Constructeur utilisé par le back-office ProduitsController (Symfony-like). */
    public Produit(int id, String nom, String description, double prix, int quantiteStock, LocalDate dateExpiration,
                   String categorie, String image, String statut) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantiteStock = quantiteStock;
        this.dateExpiration = dateExpiration;
        this.categorie = categorie;
        this.image = image;
        this.statut = statut != null && !statut.isBlank() ? statut : "disponible";
        this.disponible = "disponible".equalsIgnoreCase(this.statut);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(int quantiteStock) { this.quantiteStock = quantiteStock; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isDisponible() { return disponible; }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
        if (this.statut == null || this.statut.isBlank()) {
            this.statut = disponible ? "disponible" : "indisponible";
        }
    }

    public String getImage() { return image != null ? image : ""; }
    public void setImage(String image) { this.image = image; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getStatut() {
        return statut != null && !statut.isBlank() ? statut : (disponible ? "disponible" : "indisponible");
    }

    public void setStatut(String statut) {
        this.statut = statut != null && !statut.isBlank() ? statut : "disponible";
        this.disponible = "disponible".equalsIgnoreCase(this.statut);
    }

    @Override
    public String toString() {
        return nom;
    }
}
