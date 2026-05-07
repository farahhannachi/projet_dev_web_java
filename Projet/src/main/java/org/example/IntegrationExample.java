package org.example;

import org.example.model.*;
import org.example.service.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Exemple complet d'utilisation de l'intégration Produit/Commande/Stock/Dépôt
 * Démontre les cas d'usage principaux et les patterns d'interaction
 */
public class IntegrationExample {

    public static void main(String[] args) {
        System.out.println("=== EXEMPLE D'INTÉGRATION MODULES ===\n");

        // Initialiser les services
        ProduitService produitService = ProduitService.getInstance();
        StockService stockService = StockService.getInstance();
        DepotService depotService = DepotService.getInstance();
        CommandeService commandeService = new CommandeService();
        ClientService clientService = new ClientService();

        try {
            // --- ÉTAPE 1 : Créer des produits ---
            System.out.println("1. Création des produits...");
            Produit paracetamol = new Produit(0, "Paracétamol", "Antalgique", 5.50, 100, "Analgésiques", true);
            Produit ibuprofen = new Produit(0, "Ibuprofène", "Anti-inflammatoire", 4.20, 80, "Anti-inflammatoires", true);
            produitService.add(paracetamol);
            produitService.add(ibuprofen);
            System.out.println("✓ 2 produits créés\n");

            // --- ÉTAPE 2 : Créer des dépôts ---
            System.out.println("2. Création des dépôts...");
            Depot depotCentral = new Depot(0, "Dépôt Central", "10 Rue du Stock, Paris", "Paris", 1000, 
                    "Jean Dupont", "0145678901", null, 48.8566, 2.3522);
            Depot depotRegional = new Depot(0, "Dépôt Régional", "20 Boulevard Commercial, Lyon", "Lyon", 800,
                    "Marie Martin", "0276543210", null, 45.7640, 4.8357);
            depotService.add(depotCentral);
            depotService.add(depotRegional);
            System.out.println("✓ 2 dépôts créés\n");

            // Recharger pour obtenir les IDs générés par la BD
            paracetamol = produitService.getById(paracetamol.getId());
            ibuprofen = produitService.getById(ibuprofen.getId());
            List<Depot> depots = depotService.getAll();
            depotCentral = depots.get(0);
            depotRegional = depots.get(1);

            // --- ÉTAPE 3 : Créer des stocks ---
            System.out.println("3. Création des stocks...");
            Stock stock1 = new Stock(0, paracetamol, 50, 10, depotCentral);
            Stock stock2 = new Stock(0, paracetamol, 25, 10, depotRegional);
            Stock stock3 = new Stock(0, ibuprofen, 100, 15, depotCentral);
            Stock stock4 = new Stock(0, ibuprofen, 5, 15, depotRegional); // Stock faible
            stockService.add(stock1);
            stockService.add(stock2);
            stockService.add(stock3);
            stockService.add(stock4);
            System.out.println("✓ 4 stocks créés\n");

            // --- ÉTAPE 4 : Créer des clients ---
            System.out.println("4. Création des clients...");
            Client client1 = new Client(0, "Dupont", "Jean", "jean@email.com", "0123456789", 
                    LocalDate.of(1980, 5, 15), "123 Rue de la Paix, Paris");
            Client client2 = new Client(0, "Martin", "Marie", "marie@email.com", "0987654321",
                    LocalDate.of(1990, 3, 22), "456 Avenue des Champs, Lyon");
            clientService.add(client1);
            clientService.add(client2);
            System.out.println("✓ 2 clients créés\n");

            // --- ÉTAPE 5 : Démonstration des requêtes de stock ---
            System.out.println("5. Requêtes de stock...");
            System.out.println("   Quantité totale Paracétamol: " + 
                    stockService.getQuantiteTotaleProduit(paracetamol.getId()) + " unités");
            System.out.println("   Quantité totale Ibuprofène: " + 
                    stockService.getQuantiteTotaleProduit(ibuprofen.getId()) + " unités");
            
            System.out.println("   Stocks par Paracétamol:");
            List<Stock> stocksPara = stockService.getStocksByProduit(paracetamol.getId());
            for (Stock s : stocksPara) {
                System.out.println("     - " + s.getDepot().getNom() + ": " + s.getQuantiteDisponible() + " unités");
            }
            
            System.out.println("   Stocks faibles: " + stockService.getStocksFaibles().size());
            System.out.println("   Stocks critiques: " + stockService.getStocksCritiques().size() + "\n");

            // --- ÉTAPE 6 : Créer une commande valide ---
            System.out.println("6. Création d'une commande valide...");
            Commande commande1 = new Commande();
            commande1.setClient(client1);
            commande1.setDateCommande(LocalDate.now());
            commande1.setStatut("En attente");
            commande1.setDepot(depotCentral);

            // Ajouter les lignes
            LigneCommande ligne1 = new LigneCommande(paracetamol, 2, paracetamol.getPrix());
            LigneCommande ligne2 = new LigneCommande(ibuprofen, 3, ibuprofen.getPrix());
            commande1.addLigne(ligne1);
            commande1.addLigne(ligne2);
            
            commande1.calculerTotal();
            System.out.println("   Total avant création: " + commande1.getTotal() + "€");
            System.out.println("   Articles: " + commande1.getNombreArticles());

            try {
                commandeService.add(commande1);
                System.out.println("   ✓ Commande " + commande1.getId() + " créée avec succès\n");
            } catch (RuntimeException e) {
                System.out.println("   ✗ Erreur: " + e.getMessage() + "\n");
            }

            // --- ÉTAPE 7 : Créer une commande avec stock insuffisant ---
            System.out.println("7. Tentative de création avec stock insuffisant...");
            Commande commande2 = new Commande();
            commande2.setClient(client2);
            commande2.setDateCommande(LocalDate.now());
            commande2.setStatut("En attente");
            
            // Demander trop d'Ibuprofène au dépôt régional (stock faible)
            LigneCommande ligneFaible = new LigneCommande(ibuprofen, 10, ibuprofen.getPrix());
            commande2.addLigne(ligneFaible);

            try {
                commandeService.add(commande2);
                System.out.println("   ✓ Commande créée");
            } catch (RuntimeException e) {
                System.out.println("   ✗ Rejetée: " + e.getMessage() + "\n");
            }

            // --- ÉTAPE 8 : Opérations de stock ---
            System.out.println("8. Opérations de stock (après commandes)...");
            List<Stock> tousLesStocks = stockService.getAll();
            for (Stock s : tousLesStocks) {
                System.out.println("   " + s);
            }
            System.out.println();

            // --- ÉTAPE 9 : Requêtes sur les commandes ---
            System.out.println("9. Requêtes sur les commandes...");
            System.out.println("   Nombre total de commandes: " + commandeService.getAll().size());
            
            System.out.println("   Commandes du client Dupont:");
            List<Commande> commandesDupont = commandeService.getCommandesByClient(client1.getId());
            for (Commande c : commandesDupont) {
                System.out.println("     - Commande " + c.getId() + ": " + c.getNombreArticles() + 
                        " articles, Total: " + c.getTotal() + "€");
            }

            System.out.println("   Commandes en attente:");
            List<Commande> enAttente = commandeService.getCommandesByStatut("En attente");
            System.out.println("     " + enAttente.size() + " commande(s)\n");

            // --- ÉTAPE 10 : Modification de commande ---
            System.out.println("10. Modification de commande...");
            if (!commandesDupont.isEmpty()) {
                Commande cmd = commandesDupont.get(0);
                System.out.println("    Statut avant: " + cmd.getStatut());
                cmd.setStatut("Confirmée");
                commandeService.update(cmd);
                System.out.println("    Statut après: " + cmd.getStatut() + "\n");
            }

            // --- ÉTAPE 11 : Détails d'une commande ---
            System.out.println("11. Détails d'une commande...");
            Commande detailCmd = commandeService.getById(commande1.getId());
            if (detailCmd != null) {
                System.out.println("   Commande #" + detailCmd.getId());
                System.out.println("   Client: " + detailCmd.getClient());
                System.out.println("   Dépôt: " + (detailCmd.getDepot() != null ? detailCmd.getDepot().getNom() : "Non assigné"));
                System.out.println("   Statut: " + detailCmd.getStatut());
                System.out.println("   Date: " + detailCmd.getDateCommande());
                System.out.println("   Total: " + detailCmd.getTotal() + "€");
                System.out.println("   Lignes:");
                for (LigneCommande l : detailCmd.getLignes()) {
                    System.out.println("     - " + l.getProduit().getNom() + 
                            " x" + l.getQuantite() + " @ " + l.getPrixUnitaire() + "€");
                }
            }
            System.out.println();

            // --- ÉTAPE 12 : Synthèse ---
            System.out.println("=== SYNTHÈSE ===");
            System.out.println("Produits: " + produitService.getAll().size());
            System.out.println("Dépôts: " + depotService.getAll().size());
            System.out.println("Stocks: " + stockService.getAll().size());
            System.out.println("Clients: " + clientService.getAll().size());
            System.out.println("Commandes: " + commandeService.getAll().size());
            System.out.println("\n✓ Exemple d'intégration complété avec succès!");

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

