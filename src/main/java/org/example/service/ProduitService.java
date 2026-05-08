package org.example.service;

import org.example.model.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProduitService {
    private static ProduitService instance;

    private final List<Produit> produits = new ArrayList<>();
    private int nextId = 1;

    private ProduitService() {}

    public static synchronized ProduitService getInstance() {
        if (instance == null) {
            instance = new ProduitService();
        }
        return instance;
    }

    public void add(Produit produit) {
        produit.setId(nextId++);
        produits.add(produit);
    }

    public void update(Produit produit) {
        for (int i = 0; i < produits.size(); i++) {
            if (produits.get(i).getId() == produit.getId()) {
                produits.set(i, produit);
                break;
            }
        }
    }

    public void delete(int id) {
        produits.removeIf(p -> p.getId() == id);
    }

    public List<Produit> getAll() {
        return new ArrayList<>(produits);
    }

    public List<Produit> search(String query) {
        return produits.stream()
                .filter(p -> p.getNom().toLowerCase().contains(query.toLowerCase())
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase()))
                        || (p.getCategorie() != null && p.getCategorie().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Produit getById(int id) {
        return produits.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }
}
