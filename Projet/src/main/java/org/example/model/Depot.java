package org.example.model;

import java.time.LocalDateTime;

public class Depot {
    private int id;
    private String nom;
    private String adresse;
    private String ville;
    private String locationName;
    private int capaciteDepot;
    private String responsableDepot;
    private String responsableTelephone;
    private LocalDateTime dateCreation;
    private double latitude;
    private double longitude;

    public Depot() {
    }

    public Depot(int id, String nom, String adresse, String ville, int capaciteDepot,
                 String responsableDepot, String responsableTelephone, LocalDateTime dateCreation,
                 double latitude, double longitude) {
        this(id, nom, adresse, ville, ville, capaciteDepot, responsableDepot, responsableTelephone,
                dateCreation, latitude, longitude);
    }

    public Depot(int id, String nom, String adresse, String ville, String locationName, int capaciteDepot,
                 String responsableDepot, String responsableTelephone, LocalDateTime dateCreation,
                 double latitude, double longitude) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.ville = ville;
        this.locationName = locationName;
        this.capaciteDepot = capaciteDepot;
        this.responsableDepot = responsableDepot;
        this.responsableTelephone = responsableTelephone;
        this.dateCreation = dateCreation;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public int getCapaciteDepot() {
        return capaciteDepot;
    }

    public void setCapaciteDepot(int capaciteDepot) {
        this.capaciteDepot = capaciteDepot;
    }

    public String getResponsableDepot() {
        return responsableDepot;
    }

    public void setResponsableDepot(String responsableDepot) {
        this.responsableDepot = responsableDepot;
    }

    public String getResponsableTelephone() {
        return responsableTelephone;
    }

    public void setResponsableTelephone(String responsableTelephone) {
        this.responsableTelephone = responsableTelephone;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return Double.compare(latitude, 0d) != 0
                || Double.compare(longitude, 0d) != 0
                || (locationName != null && !locationName.isBlank());
    }

    @Override
    public String toString() {
        return nom;
    }
}
