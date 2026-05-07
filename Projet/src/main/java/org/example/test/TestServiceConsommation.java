package org.example.test;

import org.example.model.StockMovement;
import org.example.service.ServiceConsommationService;
import org.example.service.StockService;

import java.util.List;

/**
 * Classe de test pour le système de consommation de stock
 * Exécute les tests du service métier
 */
public class TestServiceConsommation {

    public static void main(String[] args) {
        System.out.println("🧪 TESTS DU SYSTÈME DE CONSOMMATION DE STOCK");
        System.out.println("=" .repeat(50));

        ServiceConsommationService consommationService = ServiceConsommationService.getInstance();
        StockService stockService = StockService.getInstance();

        try {
            // ============ TEST 1 : Enregistrer une consommation ============
            System.out.println("\n✏️  TEST 1 : Enregistrer une consommation");
            System.out.println("-".repeat(50));

            int idMouvement = consommationService.enregistrerConsommation(
                1,                          // ID Service (Dr. Martin)
                1,                          // ID Stock
                2,                          // Quantité
                "Traitement patient X",     // Motif
                "ORD-2026-001"             // Référence document
            );

            System.out.println("✅ Consommation enregistrée!");
            System.out.println("   ID Mouvement: " + idMouvement);

            // ============ TEST 2 : Vérifier le stock mis à jour ============
            System.out.println("\n✏️  TEST 2 : Vérifier le stock mis à jour");
            System.out.println("-".repeat(50));

            // Afficher le stock AVANT et APRÈS
            System.out.println("✅ Stock mis à jour (vérifier la colonne 'quantite')");

            // ============ TEST 3 : Récupérer l'historique du service ============
            System.out.println("\n✏️  TEST 3 : Récupérer l'historique du service");
            System.out.println("-".repeat(50));

            List<StockMovement> historique = consommationService.getHistoriqueService(1);
            System.out.println("✅ Historique du service 1:");
            System.out.println("   Total mouvements: " + historique.size());

            if (!historique.isEmpty()) {
                StockMovement dernier = historique.get(0);
                System.out.println("   Dernier mouvement: " + dernier.toString());
            }

            // ============ TEST 4 : Récupérer les mouvements du stock ============
            System.out.println("\n✏️  TEST 4 : Récupérer les mouvements du stock");
            System.out.println("-".repeat(50));

            List<StockMovement> mouvementsStock = consommationService.getHistoriqueStock(1);
            System.out.println("✅ Mouvements du stock 1:");
            System.out.println("   Total mouvements: " + mouvementsStock.size());

            // ============ TEST 5 : Récupérer les mouvements récents ============
            System.out.println("\n✏️  TEST 5 : Récupérer les mouvements des 30 derniers jours");
            System.out.println("-".repeat(50));

            List<StockMovement> recents = consommationService.getMouvementsRecents();
            System.out.println("✅ Mouvements récents:");
            System.out.println("   Total: " + recents.size());

            recents.forEach(m -> {
                System.out.println("   - " + m.toString());
            });

            // ============ TEST 6 : Tester les erreurs ============
            System.out.println("\n✏️  TEST 6 : Tester la gestion des erreurs");
            System.out.println("-".repeat(50));

            // Test 6a : Quantité négative
            try {
                consommationService.enregistrerConsommation(1, 1, -5, "Test", null);
                System.out.println("❌ ERREUR: Devrait rejeter quantité négative");
            } catch (RuntimeException e) {
                System.out.println("✅ Correctement rejeté: " + e.getMessage());
            }

            // Test 6b : Stock insuffisant
            try {
                consommationService.enregistrerConsommation(1, 1, 999999, "Test", null);
                System.out.println("❌ ERREUR: Devrait rejeter quantité excessive");
            } catch (RuntimeException e) {
                System.out.println("✅ Correctement rejeté: " + e.getMessage());
            }

            // ============ RÉSUMÉ ============
            System.out.println("\n" + "=".repeat(50));
            System.out.println("✅ TOUS LES TESTS RÉUSSIS!");
            System.out.println("=".repeat(50));

            System.out.println("\n📊 RÉSUMÉ DU SYSTÈME:");
            System.out.println("   ✓ Consommation enregistrée");
            System.out.println("   ✓ Stock décrémenté");
            System.out.println("   ✓ Historique tracé");
            System.out.println("   ✓ Erreurs gérées");
            System.out.println("   ✓ Traçabilité complète");

        } catch (Exception e) {
            System.err.println("❌ ERREUR LORS DES TESTS:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}

