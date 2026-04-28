package org.example.model;

import java.time.LocalDate;

public class Coupon {
    public static final String TYPE_PERCENTAGE = "percentage";
    public static final String TYPE_FIXED = "fixed";

    private int id;
    private String code;
    private String type;
    private double valeur;
    private LocalDate dateExpiration;
    private int usageMax;
    private int usageCount;
    private boolean actif;
    private double montantMinimumPanier;

    public Coupon() {}

    public Coupon(int id, String code, String type, double valeur, LocalDate dateExpiration,
                  int usageMax, int usageCount, boolean actif, double montantMinimumPanier) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.valeur = valeur;
        this.dateExpiration = dateExpiration;
        this.usageMax = usageMax;
        this.usageCount = usageCount;
        this.actif = actif;
        this.montantMinimumPanier = montantMinimumPanier;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim().toUpperCase() : null; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = Math.max(0, valeur); }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public int getUsageMax() { return usageMax; }
    public void setUsageMax(int usageMax) { this.usageMax = Math.max(1, usageMax); }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = Math.max(0, usageCount); }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public double getMontantMinimumPanier() { return montantMinimumPanier; }
    public void setMontantMinimumPanier(double montantMinimumPanier) { this.montantMinimumPanier = Math.max(0, montantMinimumPanier); }

    // Compat legacy accessors
    public double getReduction() { return valeur; }
    public void setReduction(double reduction) { this.valeur = Math.max(0, reduction); }

    public boolean isPercentage() { return TYPE_PERCENTAGE.equalsIgnoreCase(type); }
    public void setPercentage(boolean percentage) { this.type = percentage ? TYPE_PERCENTAGE : TYPE_FIXED; }

    public void incrementUsage() { this.usageCount++; }

    public boolean isValide() {
        boolean notExpired = dateExpiration == null || !LocalDate.now().isAfter(dateExpiration);
        boolean hasCapacity = usageCount < usageMax;
        return actif && notExpired && hasCapacity;
    }

    @Override
    public String toString() {
        return code + " - " + valeur + (isPercentage() ? "%" : " DT");
    }
}
