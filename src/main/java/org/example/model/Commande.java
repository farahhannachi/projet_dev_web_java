package org.example.model;

import java.time.LocalDate;
import java.util.List;

public class Commande {
    private int id;
    private Client client;
    private LocalDate dateCommande;

    public Commande() {}

        this.id = id;
        this.client = client;
        this.dateCommande = dateCommande;
        this.statut = statut;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }


    public LocalDate getDateCommande() { return dateCommande; }


    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
    }
}
