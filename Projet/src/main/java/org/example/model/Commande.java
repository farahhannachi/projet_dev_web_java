package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente une commande de produits
 * Contient plusieurs lignes de commande (LigneCommande) avec quantités et prix
 */
public class Commande {
    private int id;
    private Client client;
    private List<LigneCommande> lignes; // Remplace produits : contient quantité et prix par article
    private LocalDate dateCommande;
    private double total;
    private String statut; // "En attente", "Confirmée", "Livrée", "Annulée"
    private Depot depot; // Dépôt responsable de la commande (optionnel)

    public Commande() {
        this.lignes = new ArrayList<>();
    }

    public Commande(int id, Client client, List<LigneCommande> lignes, LocalDate dateCommande, double total, String statut) {
        this.id = id;
        this.client = client;
        this.lignes = lignes != null ? lignes : new ArrayList<>();
        this.dateCommande = dateCommande;
        this.total = total;
        this.statut = statut;
    }

    public Commande(int id, Client client, List<LigneCommande> lignes, LocalDate dateCommande, String statut, Depot depot) {
        this.id = id;
        this.client = client;
        this.lignes = lignes != null ? lignes : new ArrayList<>();
        this.dateCommande = dateCommande;
        this.statut = statut;
        this.depot = depot;
        this.total = calculerTotal();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes != null ? lignes : new ArrayList<>(); }

    // Backward compatibility: getProduits retourne la liste des produits extraits des lignes
    public List<Produit> getProduits() {
        List<Produit> produits = new ArrayList<>();
        if (lignes != null) {
            for (LigneCommande ligne : lignes) {
                if (ligne.getProduit() != null) {
                    produits.add(ligne.getProduit());
                }
            }
        }
        return produits;
    }

    // Backward compatibility: setProduits ajoute les produits comme lignes avec quantité 1
    public void setProduits(List<Produit> produits) {
        this.lignes = new ArrayList<>();
        if (produits != null) {
            for (Produit produit : produits) {
                if (produit != null) {
                    this.lignes.add(new LigneCommande(produit, 1, produit.getPrix()));
                }
            }
        }
    }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    /**
     * Calcule le total à partir des lignes
     */
    public double calculerTotal() {
        double montant = 0;
        if (lignes != null) {
            for (LigneCommande ligne : lignes) {
                montant += ligne.getMontantTotal();
            }
        }
        this.total = montant;
        return montant;
    }

    /**
     * Ajoute une ligne à la commande
     */
    public void addLigne(LigneCommande ligne) {
        if (lignes == null) {
            lignes = new ArrayList<>();
        }
        lignes.add(ligne);
        calculerTotal();
    }

    /**
     * Supprime une ligne de la commande
     */
    public void removeLigne(int index) {
        if (lignes != null && index >= 0 && index < lignes.size()) {
            lignes.remove(index);
            calculerTotal();
        }
    }

    /**
     * Nombre d'articles (somme des quantités)
     */
    public int getNombreArticles() {
        int total = 0;
        if (lignes != null) {
            for (LigneCommande ligne : lignes) {
                total += ligne.getQuantite();
            }
        }
        return total;
    }

    /**
     * Vérifie que la commande a des données valides
     */
    public boolean isValide() {
        return client != null && lignes != null && !lignes.isEmpty() && 
               dateCommande != null && statut != null && !statut.isBlank();
    }

    @Override
    public String toString() {
        if (client == null) {
            return "Commande " + id + " - Client inconnu";
        }
        return "Commande " + id + " - " + client.getNom() + " " + client.getPrenom() + 
               " (" + getNombreArticles() + " articles - " + total + " €)";
    }
}
