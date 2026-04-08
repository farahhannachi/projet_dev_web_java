package org.example.entities;

public class Traitement {

    private int idTraitement;
    private int idOrdonnance;
    private int idUtilisateur;
    private String dosage;
    private String frequence;
    private int dureeJours;
    private String dateDebut;
    private String dateFin;
    private String status;
    private String notes;
    private int idProduit;

    public Traitement() {}

    public Traitement(int idOrdonnance, int idUtilisateur, String dosage, String frequence,
                      int dureeJours, String dateDebut, String dateFin, String status,
                      String notes, int idProduit) {
        this.idOrdonnance = idOrdonnance;
        this.idUtilisateur = idUtilisateur;
        this.dosage = dosage;
        this.frequence = frequence;
        this.dureeJours = dureeJours;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.status = status;
        this.notes = notes;
        this.idProduit = idProduit;
    }

    public int getIdTraitement() { return idTraitement; }
    public void setIdTraitement(int idTraitement) { this.idTraitement = idTraitement; }
    public int getIdOrdonnance() { return idOrdonnance; }
    public void setIdOrdonnance(int idOrdonnance) { this.idOrdonnance = idOrdonnance; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequence() { return frequence; }
    public void setFrequence(String frequence) { this.frequence = frequence; }
    public int getDureeJours() { return dureeJours; }
    public void setDureeJours(int dureeJours) { this.dureeJours = dureeJours; }
    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    @Override
    public String toString() {
        return "Traitement #" + idTraitement + " - " + dosage + " (" + status + ")";
    }
}
