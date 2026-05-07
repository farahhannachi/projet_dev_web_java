package org.example.model;

import java.time.LocalDateTime;

public class Promotion {
    private int id;
    private Integer produitId;
    private String titre;
    private String description;
    private double valeurReduction;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String statut;
    private int idAdmin;

    public Promotion() {}

    public Promotion(int id, Integer produitId, String titre, String description, double valeurReduction,
                     LocalDateTime dateDebut, LocalDateTime dateFin, String statut, int idAdmin) {
        this.id = id;
        this.produitId = produitId;
        this.titre = titre;
        this.description = description;
        this.valeurReduction = valeurReduction;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.idAdmin = idAdmin;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getProduitId() { return produitId; }
    public void setProduitId(Integer produitId) { this.produitId = produitId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getValeurReduction() { return valeurReduction; }
    public void setValeurReduction(double valeurReduction) { this.valeurReduction = valeurReduction; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getIdAdmin() { return idAdmin; }
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return "active".equalsIgnoreCase(statut)
                && (dateDebut == null || !dateDebut.isAfter(now))
                && (dateFin == null || !dateFin.isBefore(now));
    }

    public double applyDiscount(double originalPrice) {
        if (!isActive()) {
            return originalPrice;
        }
        return Math.max(0.0, originalPrice * (1.0 - (valeurReduction / 100.0)));
    }
}
