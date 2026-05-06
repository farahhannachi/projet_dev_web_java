package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.model.Client;
import org.example.model.Commande;
import org.example.model.Coupon;
import org.example.model.Depot;
import org.example.model.Produit;
import org.example.model.Stock;
import org.example.service.ClientService;
import org.example.service.ProduitService;
import org.example.service.CommandeService;
import org.example.service.StockService;
import org.example.util.DatabaseUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class DashboardController {

    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;
    @FXML private Label totalClientsLabel;
    @FXML private Label totalProduitsLabel;
    @FXML private Label totalCommandesLabel;
    @FXML private Label alertesStockLabel;
    @FXML private Label totalOrdonnancesLabel;
    @FXML private Label totalTraitementsLabel;
    @FXML private Button nouveauClientBtn;
    @FXML private Button ajouterProduitBtn;
    @FXML private Button nouvelleCommandeBtn;
    @FXML private Button frontOfficeBtn;

    private ClientService clientService = new ClientService();
    private ProduitService produitService = new ProduitService();
    private CommandeService commandeService = new CommandeService();
    private StockService stockService = new StockService();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void addSampleData() {
        // Sample products
        produitService.add(new Produit(0, "Paracétamol", "Antalgique et antipyrétique", 5.50, 100, "Analgésiques", true));
        produitService.add(new Produit(0, "Ibuprofène", "Anti-inflammatoire non stéroïdien", 4.20, 80, "Anti-inflammatoires", true));
        produitService.add(new Produit(0, "Aspirine", "Antalgique et antiagrégant", 3.80, 120, "Analgésiques", true));
        produitService.add(new Produit(0, "Vitamine C", "Supplément vitaminique", 8.90, 50, "Vitamines", true));

        // Sample clients
        clientService.add(new Client(0, "Dupont", "Jean", "jean.dupont@email.com", "0123456789", LocalDate.of(1980, 5, 15), "123 Rue de la Paix, Paris"));
        clientService.add(new Client(0, "Martin", "Marie", "marie.martin@email.com", "0987654321", LocalDate.of(1990, 3, 22), "456 Avenue des Champs, Lyon"));

        // Sample depots
        Depot depot1 = new Depot(0, "Dépôt Central", "10 Rue du Stock, Paris", "0145678901");
        Depot depot2 = new Depot(0, "Dépôt Régional", "20 Boulevard Commercial, Lyon", "0276543210");

        // Sample stocks
        stockService.add(new Stock(0, produitService.getAll().get(0), 50, 10, depot1));
        stockService.add(new Stock(0, produitService.getAll().get(1), 5, 10, depot1)); // Low stock

        // Sample orders
        commandeService.add(new Commande(0, clientService.getAll().get(0), java.util.Arrays.asList(produitService.getAll().get(0)), LocalDate.now(), 5.50, "Confirmée"));
    }

    private void loadStats() {
        totalClientsLabel.setText(String.valueOf(clientService.getAll().size()));
        totalProduitsLabel.setText(String.valueOf(produitService.getAll().size()));
        totalCommandesLabel.setText(String.valueOf(commandeService.getAll().size()));
        alertesStockLabel.setText(String.valueOf(stockService.getStocksFaibles().size()));

        // Charger les stats ordonnances et traitements depuis la base
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance");
            if (rs.next()) totalOrdonnancesLabel.setText(String.valueOf(rs.getInt("c")));
            rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement");
            if (rs.next()) totalTraitementsLabel.setText(String.valueOf(rs.getInt("c")));
            rs.close();
        } catch (SQLException e) {
            System.out.println("Erreur stats: " + e.getMessage());
        }
    }

    @FXML
    private void handleNouveauClient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserManagement.fxml"));
            Parent root = loader.load();
            UserManagementController ctrl = loader.getController();
            ctrl.handleAddClient();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) nouveauClientBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouterProduit() {
        org.example.util.DialogService.showSuccess("Ajouter Produit", "Fonctionnalité disponible dans la section Produits (à venir).");
    }

    @FXML
    private void handleNouvelleCommande() {
        org.example.util.DialogService.showSuccess("Nouvelle Commande", "Fonctionnalité disponible dans la section Commandes (à venir).");
    }

    @FXML
    private void handleAjouterOrdonnance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackOrdonnance.fxml"));
            Parent root = loader.load();
            BackOrdonnanceController ctrl = loader.getController();
            ctrl.openNewForm();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouterTraitement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackTraitement.fxml"));
            Parent root = loader.load();
            BackTraitementController ctrl = loader.getController();
            ctrl.openNewForm();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void goToFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur goToFrontOffice: " + e.getMessage());
        }
    }

    @FXML
    private void showClients() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserManagement.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur UserManagement: " + e.getMessage());
        }
    }

    @FXML
    private void showProduits() {
        org.example.util.DialogService.showSuccess("Produits", "Section Produits en cours de développement.");
    }

    @FXML
    private void showCommandes() {
        org.example.util.DialogService.showSuccess("Commandes", "Section Commandes en cours de développement.");
    }

    @FXML
    private void showPromotions() {
        org.example.util.DialogService.showSuccess("Promotions", "Section Promotions en cours de développement.");
    }

    @FXML
    private void showCoupons() {
        org.example.util.DialogService.showSuccess("Coupons", "Section Coupons en cours de développement.");
    }

    @FXML
    private void showDepots() {
        org.example.util.DialogService.showSuccess("Dépôts", "Section Dépôts en cours de développement.");
    }

    @FXML
    private void showStocks() {
        org.example.util.DialogService.showSuccess("Stocks", "Section Stocks en cours de développement.");
    }

    // =============================================
    // TRAITEMENT CRUD - loads separate FXML
    // =============================================

    @FXML
    private void showTraitements() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackTraitement.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    // =============================================
    // ORDONNANCE CRUD - loads separate FXML
    // =============================================

    @FXML
    private void showOrdonnances() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackOrdonnance.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            System.out.println("Erreur logout: " + e.getMessage());
        }
    }
}
