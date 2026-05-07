package org.example.service;

import org.example.model.Commande;
import org.example.model.LigneCommande;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les Commandes
 * Gère l'ajout, mise à jour, suppression et recherche de commandes
 * Intègre vérification du stock disponible via StockService
 */
public class CommandeService {
    private List<Commande> commandes = new ArrayList<>();
    private int nextId = 1;
    private StockService stockService = StockService.getInstance();

    public void add(Commande commande) {
        if (commande == null || !commande.isValide()) {
            throw new IllegalArgumentException("Commande invalide (client, lignes, date et statut requis)");
        }

        // Vérifier la disponibilité du stock pour chaque ligne
        for (LigneCommande ligne : commande.getLignes()) {
            if (ligne.getProduit() == null || ligne.getProduit().getId() == 0) {
                throw new IllegalArgumentException("Produit invalide dans la ligne de commande");
            }
            
            // Chercher un stock disponible pour ce produit
            List<Integer> depotsDisponibles = new ArrayList<>();
            var stocks = stockService.getStocksByProduit(ligne.getProduit().getId());
            
            if (stocks == null || stocks.isEmpty()) {
                throw new RuntimeException("Aucun stock trouvé pour le produit: " + ligne.getProduit().getNom());
            }

            // Vérifier qu'au moins un dépôt a la quantité requise
            boolean trouvee = false;
            for (var stock : stocks) {
                if (stock.getQuantiteDisponible() >= ligne.getQuantite()) {
                    trouvee = true;
                    if (ligne.getStock() == null) {
                        ligne.setStock(stock); // Affecter le stock trouvé
                    }
                    break;
                }
            }

            if (!trouvee) {
                throw new RuntimeException("Stock insuffisant pour " + ligne.getProduit().getNom() + 
                                         ". Demandé: " + ligne.getQuantite() + 
                                         ", Disponible: " + stocks.stream().mapToInt(s -> s.getQuantiteDisponible()).sum());
            }
        }

        // Si toutes les lignes sont validées, affecter l'ID et ajouter
        commande.setId(nextId++);
        commandes.add(commande);
    }

    public void update(Commande commande) {
        if (commande == null) {
            throw new IllegalArgumentException("Commande ne peut pas être null");
        }
        
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
        if (query == null || query.isBlank()) {
            return getAll();
        }
        
        String q = query.toLowerCase();
        return commandes.stream()
                .filter(c -> {
                    // Vérifier ID
                    if (String.valueOf(c.getId()).contains(q)) {
                        return true;
                    }
                    // Vérifier client (null-safe)
                    if (c.getClient() != null) {
                        String nom = c.getClient().getNom();
                        String prenom = c.getClient().getPrenom();
                        if ((nom != null && nom.toLowerCase().contains(q)) ||
                            (prenom != null && prenom.toLowerCase().contains(q))) {
                            return true;
                        }
                    }
                    // Vérifier statut (null-safe)
                    String statut = c.getStatut();
                    return statut != null && statut.toLowerCase().contains(q);
                })
                .collect(Collectors.toList());
    }

    public Commande getById(int id) {
        return commandes.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    /**
     * Cherche les commandes d'un client spécifique
     */
    public List<Commande> getCommandesByClient(int clientId) {
        return commandes.stream()
                .filter(c -> c.getClient() != null && c.getClient().getId() == clientId)
                .collect(Collectors.toList());
    }

    /**
     * Cherche les commandes par statut
     */
    public List<Commande> getCommandesByStatut(String statut) {
        if (statut == null) return new ArrayList<>();
        return commandes.stream()
                .filter(c -> statut.equals(c.getStatut()))
                .collect(Collectors.toList());
    }
}
