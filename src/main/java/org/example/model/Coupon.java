package org.example.model;

import java.time.LocalDate;

public class Coupon {
    private int id;
    private String code;
    private double reduction; // percentage or fixed amount
    private boolean isPercentage;
    private LocalDate dateExpiration;
    private boolean actif;

    public Coupon() {}

    public Coupon(int id, String code, double reduction, boolean isPercentage, LocalDate dateExpiration, boolean actif) {
        this.id = id;
        this.code = code;
        this.reduction = reduction;
        this.isPercentage = isPercentage;
        this.dateExpiration = dateExpiration;
        this.actif = actif;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getReduction() { return reduction; }
    public void setReduction(double reduction) { this.reduction = reduction; }

    public boolean isPercentage() { return isPercentage; }
    public void setPercentage(boolean percentage) { isPercentage = percentage; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public boolean isValide() {
        return actif && LocalDate.now().isBefore(dateExpiration);
    }

    @Override
    public String toString() {
        return code + " - " + reduction + (isPercentage ? "%" : "€");
    }
}
