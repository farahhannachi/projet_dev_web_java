package org.example;

import org.example.model.Depot;
import org.example.service.DepotService;
import org.example.util.ValidationException;

public class TestDepotService {
    public static void main(String[] args) {
        // Get the singleton instance
        DepotService service = DepotService.getInstance();

        // Add test depots
        Depot depot1 = new Depot(0, "Dépôt Central", "10 Rue du Stock, Paris", "Paris", 1000, "Jean Dupont", "0145678901", null, 48.8566, 2.3522);
        Depot depot2 = new Depot(0, "Dépôt Régional", "20 Boulevard Commercial, Lyon", "Lyon", 800, "Marie Martin", "0276543210", null, 45.7640, 4.8357);

        try {
            service.add(depot1);
            service.add(depot2);
        } catch (ValidationException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
        }

        // Verify data
        System.out.println("=== Test DepotService ===");
        System.out.println("Total dépôts: " + service.getAll().size());

        for (Depot d : service.getAll()) {
            System.out.println("- " + d.getNom() + " (" + d.getVille() + ")");
        }

        // Get same instance again
        DepotService service2 = DepotService.getInstance();
        System.out.println("\nVérification singleton: " + service.getAll().size() + " dépôts");

        if (service == service2) {
            System.out.println("✅ Singleton fonctionne correctement!");
        } else {
            System.out.println("❌ Singleton FAILED!");
        }
    }
}


