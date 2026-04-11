package org.example.model;

import java.time.LocalDate;
import java.util.List;

public class Commande {
    private int id;
    private Client client;
    private List<Produit> produits;
    private LocalDate dateCommande;
    private double total;
    private String statut; // "En attente", "Confirmée", "Livrée"

    public Commande() {}

    public Commande(int id, Client client, List<Produit> produits, LocalDate dateCommande, double total, String statut) {
        this.id = id;
        this.client = client;
        this.produits = produits;
        this.dateCommande = dateCommande;
        this.total = total;
        this.statut = statut;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<Produit> getProduits() { return produits; }
    public void setProduits(List<Produit> produits) { this.produits = produits; }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Commande " + id + " - " + client.getNom() + " " + client.getPrenom();
    }
}
