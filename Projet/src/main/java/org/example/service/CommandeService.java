package org.example.service;

import org.example.model.Commande;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandeService {
    private List<Commande> commandes = new ArrayList<>();
    private int nextId = 1;

    public void add(Commande commande) {
        commande.setId(nextId++);
        commandes.add(commande);
    }

    public void update(Commande commande) {
        for (int i = 0; i < commandes.size(); i++) {
            if (commandes.get(i).getId() == commande.getId()) {
                commandes.set(i, commande);
                break;
            }
        }
    }

    public void delete(int id) {
        commandes.removeIf(c -> c.getId() == id);
    }

    public List<Commande> getAll() {
        return new ArrayList<>(commandes);
    }

    public List<Commande> search(String query) {
        return commandes.stream()
                .filter(c -> String.valueOf(c.getId()).contains(query) ||
                             c.getClient().getNom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getStatut().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Commande getById(int id) {
        return commandes.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }
}
