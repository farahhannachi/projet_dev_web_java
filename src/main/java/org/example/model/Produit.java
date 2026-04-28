package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prixUnitaire;
    private int quantiteStock;
    private LocalDate dateExpiration;
    private String categorie;
    private String image;
    private String statut;
    private int seuilMinimum; // Pour alerter si stock < seuil (fallback JavaFX)
    private String codeSku;
    private LocalDateTime dateCreation;
    private boolean actif;
    private boolean archive; // Soft delete

    public Produit() {}

    public Produit(int id, String nom, String description, double prixUnitaire, int quantiteStock,
                   String codeSku, String categorie, int seuilMinimum, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
        this.quantiteStock = quantiteStock;
        this.dateExpiration = null;
        this.image = null;
        this.statut = actif ? "disponible" : "indisponible";
        this.codeSku = codeSku;
        this.categorie = categorie;
        this.seuilMinimum = seuilMinimum;
        this.dateCreation = LocalDateTime.now();
        this.actif = actif;
        this.archive = false;
    }

    public Produit(int id, String nom, String description, double prix, int quantiteStock,
                   LocalDate dateExpiration, String categorie, String image, String statut) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prixUnitaire = prix;
        this.quantiteStock = quantiteStock;
        this.dateExpiration = dateExpiration;
        this.categorie = categorie;
        this.image = image;
        this.statut = statut != null ? statut : "disponible";
        this.codeSku = "";
        this.seuilMinimum = 5;
        this.dateCreation = LocalDateTime.now();
        this.actif = !"indisponible".equalsIgnoreCase(this.statut) && !"rupture".equalsIgnoreCase(this.statut);
        this.archive = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    
    // Rétro-compatibilité
    public double getPrix() { return prixUnitaire; }
    public void setPrix(double prix) { this.prixUnitaire = prix; }

    public int getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(int quantiteStock) { this.quantiteStock = quantiteStock; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public int getSeuilMinimum() { return seuilMinimum; }
    public void setSeuilMinimum(int seuilMinimum) { this.seuilMinimum = seuilMinimum; }

    public String getCodeSku() { return codeSku; }
    public void setCodeSku(String codeSku) { this.codeSku = codeSku; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) {
        this.statut = statut;
        this.actif = !"indisponible".equalsIgnoreCase(statut) && !"rupture".equalsIgnoreCase(statut);
    }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) {
        this.actif = actif;
        this.statut = actif ? "disponible" : "indisponible";
    }
    
    // Rétro-compatibilité
    public boolean isDisponible() { return actif; }
    public void setDisponible(boolean disponible) {
        this.actif = disponible;
        this.statut = disponible ? "disponible" : "indisponible";
    }

    public boolean isArchive() { return archive; }
    public void setArchive(boolean archive) { this.archive = archive; }

    // Méthodes métier
    public boolean estDisponible() {
        return actif && !archive && quantiteStock > 0 && !"rupture".equalsIgnoreCase(statut);
    }

    public boolean estStockFaible() {
        return quantiteStock < seuilMinimum && quantiteStock > 0;
    }

    public boolean peutEtreCommandé(int quantiteRequise) {
        return estDisponible() && quantiteStock >= quantiteRequise;
    }

    @Override
    public String toString() {
        return nom + " (" + codeSku + ")";
    }
}
