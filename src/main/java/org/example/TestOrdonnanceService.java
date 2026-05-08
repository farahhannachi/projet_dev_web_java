package org.example;

import org.example.model.Ordonnance;
import org.example.service.OrdonnanceService;

import java.time.LocalDateTime;

public class TestOrdonnanceService {
    public static void main(String[] args) {
        OrdonnanceService service = OrdonnanceService.getInstance();

        // Ajouter des ordonnances de test
        Ordonnance ord1 = new Ordonnance(0, "ORD-2026-001", LocalDateTime.now(),
                LocalDateTime.now().plusMonths(3), "en_attente", "Traitement grippe", 1);
        Ordonnance ord2 = new Ordonnance(0, "ORD-2026-002", LocalDateTime.now(),
                LocalDateTime.now().plusMonths(6), "validee", "Traitement chronique", 1);

        try {
            service.add(ord1);
            service.add(ord2);
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        // Vérifier les données
        System.out.println("=== Test OrdonnanceService ===");
        System.out.println("Total ordonnances: " + service.getAll().size());
        for (Ordonnance o : service.getAll()) {
            System.out.println("- " + o.getNumeroOrdonnance() + " (" + o.getStatut() + ")");
        }

        // Vérification singleton
        OrdonnanceService service2 = OrdonnanceService.getInstance();
        if (service == service2) {
            System.out.println("\n✅ Singleton fonctionne correctement!");
        } else {
            System.out.println("\n❌ Singleton FAILED!");
        }
    }
}
