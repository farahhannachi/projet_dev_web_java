package org.example;

import org.example.model.Stock;
import org.example.service.StockService;

import java.util.List;

public class TestStockJointure {
    public static void main(String[] args) {
        // Test de la jointure Stock + Produit + Depot
        StockService service = StockService.getInstance();

        System.out.println("=== Test Jointure Stock + Produit + Depot ===");

        // Récupérer tous les stocks avec jointure
        List<Stock> stocks = service.getAll();

        System.out.println("Nombre de stocks trouvés: " + stocks.size());

        for (Stock stock : stocks) {
            System.out.println("Stock ID: " + stock.getId());

            // Afficher les informations du produit
            if (stock.getProduit() != null) {
                System.out.println("  Produit: " + stock.getProduit().getNom() +
                                 " (ID: " + stock.getProduit().getId() + ")");
            } else {
                System.out.println("  Produit: NULL");
            }

            // Afficher la quantité
            System.out.println("  Quantité: " + stock.getQuantiteDisponible());

            // Afficher les informations du dépôt
            if (stock.getDepot() != null) {
                System.out.println("  Dépôt: " + stock.getDepot().getNom() +
                                 " (Ville: " + stock.getDepot().getVille() + ")");
            } else {
                System.out.println("  Dépôt: NULL");
            }

            System.out.println("  ---");
        }

        // Test de récupération par ID
        if (!stocks.isEmpty()) {
            Stock firstStock = stocks.get(0);
            System.out.println("\n=== Test getById(" + firstStock.getId() + ") ===");
            Stock stockById = service.getById(firstStock.getId());

            if (stockById != null) {
                System.out.println("Stock trouvé:");
                System.out.println("  ID: " + stockById.getId());
                System.out.println("  Produit: " + (stockById.getProduit() != null ? stockById.getProduit().getNom() : "NULL"));
                System.out.println("  Quantité: " + stockById.getQuantiteDisponible());
                System.out.println("  Dépôt: " + (stockById.getDepot() != null ? stockById.getDepot().getNom() : "NULL"));
            } else {
                System.out.println("Stock non trouvé!");
            }
        }

        System.out.println("\n=== Test terminé ===");
    }
}
