package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Commande {
    private int id;
    private Integer utilisateurId;
    private Client client;
    private List<LigneCommande> lignes;
    private LocalDate dateCommande;
    private LocalDateTime dateCommandeDateTime;
    private double totalHt;
    private double totalTtc;
    private String modePaiement;
    private String adresseLivraison;
    private String telephone;
    private String nom;
    private String email;
    private String message;
    private String produitsIds;
    private String couponCode;
    private double couponDiscount;
    private LocalDateTime estimatedDeliveryDate;
    private int fraudScore;
    private double baseShippingCost;
    private String statut; // "brouillon", "confirmée", "expédiée", "annulée"

    // Énumération des statuts
    public enum Statut {
        BROUILLON("brouillon"),
        CONFIRMEE("confirmée"),
        EXPEDIEE("expédiée"),
        ANNULEE("annulée");

        private final String valeur;
        Statut(String valeur) { this.valeur = valeur; }
        public String getValeur() { return valeur; }
    }

    public Commande() {}

    public Commande(int id, Client client, List<LigneCommande> lignes, LocalDate dateCommande, String adresseLivraison, String statut) {
        this.id = id;
        this.client = client;
        this.lignes = lignes;
        this.dateCommande = dateCommande;
        this.dateCommandeDateTime = dateCommande != null ? dateCommande.atStartOfDay() : LocalDateTime.now();
        this.adresseLivraison = adresseLivraison;
        this.statut = statut;
        this.totalHt = 0;
        this.totalTtc = 0;
        this.modePaiement = "en_ligne";
        this.telephone = "";
        this.nom = client != null ? client.getNom() : "";
        this.email = client != null ? client.getEmail() : "";
        this.message = null;
        this.produitsIds = null;
        this.couponCode = null;
        this.couponDiscount = 0;
        this.estimatedDeliveryDate = null;
        this.fraudScore = 0;
        this.baseShippingCost = 0;
    }

    public Commande(int id, Integer utilisateurId, LocalDateTime dateCommandeDateTime, String statut, double total,
                    String modePaiement, String adresseLivraison, String telephone, String nom, String email,
                    String message, String produitsIds, String couponCode, double couponDiscount,
                    LocalDateTime estimatedDeliveryDate, int fraudScore, double baseShippingCost) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.client = null;
        this.lignes = java.util.Collections.emptyList();
        this.dateCommandeDateTime = dateCommandeDateTime;
        this.dateCommande = dateCommandeDateTime != null ? dateCommandeDateTime.toLocalDate() : LocalDate.now();
        this.statut = statut;
        this.totalHt = total;
        this.totalTtc = total;
        this.modePaiement = modePaiement;
        this.adresseLivraison = adresseLivraison;
        this.telephone = telephone;
        this.nom = nom;
        this.email = email;
        this.message = message;
        this.produitsIds = produitsIds;
        this.couponCode = couponCode;
        this.couponDiscount = couponDiscount;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.fraudScore = fraudScore;
        this.baseShippingCost = baseShippingCost;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) {
        this.dateCommande = dateCommande;
        this.dateCommandeDateTime = dateCommande != null ? dateCommande.atStartOfDay() : null;
    }

    public LocalDateTime getDateCommandeDateTime() { return dateCommandeDateTime; }
    public void setDateCommandeDateTime(LocalDateTime dateCommandeDateTime) {
        this.dateCommandeDateTime = dateCommandeDateTime;
        this.dateCommande = dateCommandeDateTime != null ? dateCommandeDateTime.toLocalDate() : null;
    }

    public double getTotalHt() { return totalHt; }
    public void setTotalHt(double totalHt) { this.totalHt = totalHt; }

    public double getTotalTtc() { return totalTtc; }
    public void setTotalTtc(double totalTtc) { this.totalTtc = totalTtc; }

    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProduitsIds() { return produitsIds; }
    public void setProduitsIds(String produitsIds) { this.produitsIds = produitsIds; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public double getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(double couponDiscount) { this.couponDiscount = couponDiscount; }

    public LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }

    public int getFraudScore() { return fraudScore; }
    public void setFraudScore(int fraudScore) { this.fraudScore = Math.max(0, Math.min(100, fraudScore)); }

    public double getBaseShippingCost() { return baseShippingCost; }
    public void setBaseShippingCost(double baseShippingCost) { this.baseShippingCost = Math.max(0, baseShippingCost); }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    // Rétro-compatibilité
    public double getTotal() { return totalTtc; }
    public void setTotal(double total) { this.totalTtc = total; }

    public String getClientNom() {
        if (nom != null && !nom.isBlank()) {
            return nom;
        }
        return client != null ? client.getNom() : "";
    }

    // Méthodes métier
    public boolean peutEtreModifiee() {
        if (statut == null) {
            return true;
        }
        String s = statut.toLowerCase();
        return !s.equals(Statut.EXPEDIEE.getValeur()) &&
                !s.equals("livree") &&
                !s.equals(Statut.ANNULEE.getValeur()) &&
                !s.equals("annulee") &&
                !s.equals("bloquee");
    }

    public boolean estConfirmee() {
        if (statut == null) {
            return false;
        }
        String s = statut.toLowerCase();
        return s.equals(Statut.CONFIRMEE.getValeur()) || s.equals("confirmee");
    }

    public boolean estExpediee() {
        if (statut == null) {
            return false;
        }
        String s = statut.toLowerCase();
        return s.equals(Statut.EXPEDIEE.getValeur()) || s.equals("livree");
    }

    public int getNbArticles() {
        return lignes != null ? lignes.stream().mapToInt(LigneCommande::getQuantite).sum() : 0;
    }

    @Override
    public String toString() {
        return "Commande " + id + " - " + (client != null ? client.getNom() : "N/A") + " (" + statut + ")";
    }
}
