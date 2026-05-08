package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Commande {
    private int id;
    private Client client;
    private List<Produit> produits;
    private LocalDate dateCommande;
    /** Legacy TTC field; kept for older code paths */
    private double total;
    private String statut;

    private Integer utilisateurId;
    private String email;
    private String nom;
    private String telephone;
    private String modePaiement;
    private String adresseLivraison;
    private String message;
    private String produitsIds;
    private String couponCode;
    private double couponDiscount;
    private int fraudScore;
    private double baseShippingCost;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime dateCommandeDateTime;
    private double totalHt;
    private double totalTtc;

    public Commande() {}

    public Commande(int id, Client client, List<Produit> produits, LocalDate dateCommande, double total, String statut) {
        this.id = id;
        this.client = client;
        this.produits = produits;
        this.dateCommande = dateCommande;
        this.total = total;
        this.statut = statut;
        this.totalTtc = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<Produit> getProduits() { return produits; }
    public void setProduits(List<Produit> produits) { this.produits = produits; }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }

    /**
     * Montant TTC affiché aux écrans legacy (tableaux, mails, scoring).
     * Préfère {@link #totalTtc} lorsqu'il a été renseigné par le flux e-commerce.
     */
    public double getTotal() {
        if (totalTtc > 0) {
            return totalTtc;
        }
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
        if (totalTtc <= 0) {
            this.totalTtc = total;
        }
    }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return client != null ? client.getEmail() : null;
    }

    public void setEmail(String email) { this.email = email; }

    public String getNom() {
        if (nom != null && !nom.isBlank()) {
            return nom;
        }
        if (client != null) {
            String n = client.getNom() == null ? "" : client.getNom();
            String p = client.getPrenom() == null ? "" : client.getPrenom();
            return (n + " " + p).trim();
        }
        return "";
    }

    public void setNom(String nom) { this.nom = nom; }

    /** Nom affiché dans les tableaux admin (PropertyValueFactory "clientNom") */
    public String getClientNom() {
        return getNom();
    }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProduitsIds() { return produitsIds; }
    public void setProduitsIds(String produitsIds) { this.produitsIds = produitsIds; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public double getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(double couponDiscount) { this.couponDiscount = couponDiscount; }

    public int getFraudScore() { return fraudScore; }
    public void setFraudScore(int fraudScore) { this.fraudScore = fraudScore; }

    public double getBaseShippingCost() { return baseShippingCost; }
    public void setBaseShippingCost(double baseShippingCost) { this.baseShippingCost = baseShippingCost; }

    public LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public LocalDateTime getDateCommandeDateTime() {
        if (dateCommandeDateTime != null) {
            return dateCommandeDateTime;
        }
        if (dateCommande != null) {
            return dateCommande.atStartOfDay();
        }
        return null;
    }

    public void setDateCommandeDateTime(LocalDateTime dateCommandeDateTime) {
        this.dateCommandeDateTime = dateCommandeDateTime;
    }

    public double getTotalHt() { return totalHt; }

    public void setTotalHt(double totalHt) { this.totalHt = totalHt; }

    public double getTotalTtc() {
        if (totalTtc > 0) {
            return totalTtc;
        }
        return total;
    }

    public void setTotalTtc(double totalTtc) {
        this.totalTtc = totalTtc;
        this.total = totalTtc;
    }

    @Override
    public String toString() {
        if (client != null) {
            return "Commande " + id + " - " + client.getNom() + " " + client.getPrenom();
        }
        return "Commande " + id + " - " + getNom();
    }
}
