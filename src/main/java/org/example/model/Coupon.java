package org.example.model;

import java.time.LocalDate;

public class Coupon {
    public static final String TYPE_PERCENTAGE = "percentage";
    public static final String TYPE_FIXED = "fixed";

    private int id;
    private String code;
    private double reduction;
    private boolean isPercentage;
    /** Type texte pour l'admin (voir constantes ci-dessus). */
    private String type = TYPE_PERCENTAGE;
    private LocalDate dateExpiration;
    private boolean actif;
    private int usageCount;
    private int usageMax = 1;
    private double montantMinimumPanier;

    public Coupon() {}

    public Coupon(int id, String code, double reduction, boolean isPercentage, LocalDate dateExpiration, boolean actif) {
        this.id = id;
        this.code = code;
        this.reduction = reduction;
        this.isPercentage = isPercentage;
        this.type = isPercentage ? TYPE_PERCENTAGE : TYPE_FIXED;
        this.dateExpiration = dateExpiration;
        this.actif = actif;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getReduction() { return reduction; }
    public void setReduction(double reduction) { this.reduction = reduction; }

    /** Alias UI : même valeur que {@link #reduction}. */
    public double getValeur() { return reduction; }

    public void setValeur(double valeur) {
        this.reduction = valeur;
    }

    public boolean isPercentage() { return isPercentage; }

    public void setPercentage(boolean percentage) {
        this.isPercentage = percentage;
        if (type == null || type.isBlank()) {
            this.type = percentage ? TYPE_PERCENTAGE : TYPE_FIXED;
        }
    }

    public String getType() {
        return type != null ? type : (isPercentage ? TYPE_PERCENTAGE : TYPE_FIXED);
    }

    public void setType(String type) {
        this.type = type;
        this.isPercentage = TYPE_PERCENTAGE.equalsIgnoreCase(type);
    }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    /** Pour les colonnes JavaFX PropertyValueFactory("actif"). */
    public boolean getActif() { return actif; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public int getUsageMax() { return usageMax; }
    public void setUsageMax(int usageMax) { this.usageMax = usageMax; }

    public double getMontantMinimumPanier() { return montantMinimumPanier; }
    public void setMontantMinimumPanier(double montantMinimumPanier) { this.montantMinimumPanier = montantMinimumPanier; }

    public boolean isValide() {
        if (!actif) {
            return false;
        }
        if (usageMax > 0 && usageCount >= usageMax) {
            return false;
        }
        if (dateExpiration == null) {
            return true;
        }
        return !LocalDate.now().isAfter(dateExpiration);
    }

    @Override
    public String toString() {
        return code + " - " + reduction + (isPercentage ? "%" : " DT");
    }
}
