package org.example.model;

import java.time.LocalDateTime;

public class Service {
    private int id;
    private String nom;
    private String type; // "Médecin" ou "Infirmier"
    private String specialite;
    private String telephone;
    private String email;
    private String adresse;
    private LocalDateTime dateCreation;
    private double consommation;

    public Service() {}

    public Service(int id, String nom, String type, String specialite, String telephone, String email, String adresse, LocalDateTime dateCreation, double consommation) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.specialite = specialite;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
        this.dateCreation = dateCreation;
        this.consommation = consommation;
    }

    public Service(int id, String nom, String type, String specialite, String telephone, String email, String adresse, LocalDateTime dateCreation) {
        this(id, nom, type, specialite, telephone, email, adresse, dateCreation, 0.0);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public double getConsommation() {
        return consommation;
    }

    public void setConsommation(double consommation) {
        this.consommation = consommation;
    }

    @Override
    public String toString() {
        return nom + " (" + type + ")";
    }
}
