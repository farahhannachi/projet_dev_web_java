package org.example.model;

import java.time.LocalDateTime;

public class Traitement {
    private int idTraitement;
    private int idUtilisateurId;
    private String dosage;
    private String frequence;
    private int dureeJours;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String status;
    private String notes;
    private int idOrdonnanceId;
    private int idProduitId;
    private String repas;

    public Traitement() {}

    public Traitement(int idTraitement, int idUtilisateurId, String dosage, String frequence,
                      int dureeJours, LocalDateTime dateDebut, LocalDateTime dateFin,
                      String status, String notes, int idOrdonnanceId, int idProduitId, String repas) {
        this.idTraitement = idTraitement;
        this.idUtilisateurId = idUtilisateurId;
        this.dosage = dosage;
        this.frequence = frequence;
        this.dureeJours = dureeJours;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.status = status;
        this.notes = notes;
        this.idOrdonnanceId = idOrdonnanceId;
        this.idProduitId = idProduitId;
        this.repas = repas;
    }

    // Getters and Setters
    public int getIdTraitement() { return idTraitement; }
    public void setIdTraitement(int idTraitement) { this.idTraitement = idTraitement; }

    public int getIdUtilisateurId() { return idUtilisateurId; }
    public void setIdUtilisateurId(int idUtilisateurId) { this.idUtilisateurId = idUtilisateurId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequence() { return frequence; }
    public void setFrequence(String frequence) { this.frequence = frequence; }

    public int getDureeJours() { return dureeJours; }
    public void setDureeJours(int dureeJours) { this.dureeJours = dureeJours; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getIdOrdonnanceId() { return idOrdonnanceId; }
    public void setIdOrdonnanceId(int idOrdonnanceId) { this.idOrdonnanceId = idOrdonnanceId; }

    public int getIdProduitId() { return idProduitId; }
    public void setIdProduitId(int idProduitId) { this.idProduitId = idProduitId; }

    public String getRepas() { return repas; }
    public void setRepas(String repas) { this.repas = repas; }

    @Override
    public String toString() {
        return "Traitement " + idTraitement + " - " + dosage + " (" + status + ")";
    }
}
