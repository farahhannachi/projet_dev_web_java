package org.example.model;

import java.time.LocalDateTime;

public class Depot {
    private int id;
    private String nom;
    private String adresse;
    private String telephone;
    private String ville;
    private String locationName;
    private int capaciteDepot;
    private String responsableDepot;
    private String responsableTelephone;
    private double latitude;
    private double longitude;
    private LocalDateTime dateCreation;

    public Depot() {}

    public Depot(int id, String nom, String adresse, String telephone) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.telephone = telephone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public int getCapaciteDepot() { return capaciteDepot; }
    public void setCapaciteDepot(int capaciteDepot) { this.capaciteDepot = capaciteDepot; }

    public String getResponsableDepot() { return responsableDepot; }
    public void setResponsableDepot(String responsableDepot) { this.responsableDepot = responsableDepot; }

    public String getResponsableTelephone() { return responsableTelephone; }
    public void setResponsableTelephone(String responsableTelephone) { this.responsableTelephone = responsableTelephone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return nom;
    }
}
