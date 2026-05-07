package org.example;

import org.example.model.Ordonnance;
import org.example.model.Traitement;
import org.example.service.OrdonnanceService;
import org.example.service.TraitementService;

import java.util.List;

public class TestOrdonnanceTraitementJointure {
    public static void main(String[] args) {
        OrdonnanceService ordonnanceService = OrdonnanceService.getInstance();
        TraitementService traitementService = TraitementService.getInstance();

        System.out.println("=== Test Jointure Ordonnance + Traitement ===");

        // Récupérer toutes les ordonnances
        List<Ordonnance> ordonnances = ordonnanceService.getAll();
        System.out.println("Nombre d'ordonnances trouvées: " + ordonnances.size());

        for (Ordonnance ord : ordonnances) {
            System.out.println("\nOrdonnance ID: " + ord.getIdOrdonnance());
            System.out.println("  Numéro: " + ord.getNumeroOrdonnance());
            System.out.println("  Statut: " + ord.getStatut());
            System.out.println("  Date: " + ord.getDateOrdonnance());
            System.out.println("  Expiration: " + ord.getDateExpiration());

            // Récupérer les traitements liés à cette ordonnance
            List<Traitement> traitements = traitementService.getByOrdonnanceId(ord.getIdOrdonnance());
            System.out.println("  Traitements associés: " + traitements.size());

            for (Traitement t : traitements) {
                System.out.println("    - Traitement #" + t.getIdTraitement()
                        + " | Dosage: " + t.getDosage()
                        + " | Fréquence: " + t.getFrequence()
                        + " | Repas: " + t.getRepas()
                        + " | Statut: " + t.getStatus()
                        + " | Produit ID: " + t.getIdProduitId());
            }
            System.out.println("  ---");
        }

        // Test getById
        if (!ordonnances.isEmpty()) {
            int testId = ordonnances.get(0).getIdOrdonnance();
            System.out.println("\n=== Test getById(" + testId + ") ===");
            Ordonnance ord = ordonnanceService.getById(testId);
            if (ord != null) {
                System.out.println("Ordonnance trouvée: " + ord.getNumeroOrdonnance());
                List<Traitement> traitements = traitementService.getByOrdonnanceId(testId);
                System.out.println("Traitements liés: " + traitements.size());
            } else {
                System.out.println("Ordonnance non trouvée!");
            }
        }

        System.out.println("\n=== Test terminé ===");
    }
}
