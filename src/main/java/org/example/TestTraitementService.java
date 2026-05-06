package org.example;

import org.example.model.Traitement;
import org.example.service.TraitementService;

import java.time.LocalDateTime;

public class TestTraitementService {
    public static void main(String[] args) {
        TraitementService service = TraitementService.getInstance();

        // Ajouter des traitements de test
        Traitement t1 = new Traitement(0, 1, "500mg", "2 fois/jour", 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                "en_attente", "Prendre avec eau", 1, 1, "avant");
        Traitement t2 = new Traitement(0, 1, "250mg", "3 fois/jour", 14,
                LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                "actif", "Après repas", 1, 2, "apres");

        try {
            service.add(t1);
            service.add(t2);
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        // Vérifier les données
        System.out.println("=== Test TraitementService ===");
        System.out.println("Total traitements: " + service.getAll().size());
        for (Traitement t : service.getAll()) {
            System.out.println("- " + t.getDosage() + " " + t.getFrequence() + " (" + t.getStatus() + ")");
        }

        // Vérification singleton
        TraitementService service2 = TraitementService.getInstance();
        if (service == service2) {
            System.out.println("\n✅ Singleton fonctionne correctement!");
        } else {
            System.out.println("\n❌ Singleton FAILED!");
        }
    }
}
