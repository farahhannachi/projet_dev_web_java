package org.example.model;

import java.time.LocalDateTime;

public class Ordonnance {
    private int idOrdonnance;
    private String numeroOrdonnance;
    private LocalDateTime dateOrdonnance;
    private LocalDateTime dateExpiration;
    private String statut;
    private String noteMedical;
    private int idUtilisateurId;

    public Ordonnance() {}

    public Ordonnance(int idOrdonnance, String numeroOrdonnance, LocalDateTime dateOrdonnance,
                      LocalDateTime dateExpiration, String statut, String noteMedical, int idUtilisateurId) {
        this.idOrdonnance = idOrdonnance;
        this.numeroOrdonnance = numeroOrdonnance;
        this.dateOrdonnance = dateOrdonnance;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
        this.noteMedical = noteMedical;
        this.idUtilisateurId = idUtilisateurId;
    }

    // Getters and Setters
    public int getIdOrdonnance() { return idOrdonnance; }
    public void setIdOrdonnance(int idOrdonnance) { this.idOrdonnance = idOrdonnance; }

    public String getNumeroOrdonnance() { return numeroOrdonnance; }
    public void setNumeroOrdonnance(String numeroOrdonnance) { this.numeroOrdonnance = numeroOrdonnance; }

    public LocalDateTime getDateOrdonnance() { return dateOrdonnance; }
    public void setDateOrdonnance(LocalDateTime dateOrdonnance) { this.dateOrdonnance = dateOrdonnance; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNoteMedical() { return noteMedical; }
    public void setNoteMedical(String noteMedical) { this.noteMedical = noteMedical; }

    public int getIdUtilisateurId() { return idUtilisateurId; }
    public void setIdUtilisateurId(int idUtilisateurId) { this.idUtilisateurId = idUtilisateurId; }

    @Override
    public String toString() {
        return "Ordonnance " + idOrdonnance + " - " + numeroOrdonnance + " (" + statut + ")";
    }
}
