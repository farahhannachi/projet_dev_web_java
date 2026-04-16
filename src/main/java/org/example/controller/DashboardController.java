package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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

import java.io.IOException;
import java.time.LocalDate;

public class DashboardController {

    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;
    @FXML private Label totalClientsLabel;
    @FXML private Label totalProduitsLabel;
    @FXML private Label totalCommandesLabel;
    @FXML private Label alertesStockLabel;
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
        addSampleData();
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
    }

    @FXML
    private void handleNouveauClient() {
        // Open new client form
        System.out.println("Nouveau Client clicked");
    }

    @FXML
    private void handleAjouterProduit() {
        // Open add product form
        System.out.println("Ajouter Produit clicked");
    }

    @FXML
    private void handleNouvelleCommande() {
        // Open new order form
        System.out.println("Nouvelle Commande clicked");
    }

    @FXML
    private void goToFrontOffice() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    public void showClients(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserManagement.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (Exception e) {
            System.err.println("Error loading Clients page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void showProduits() {
        // Switch to products view
        System.out.println("Show Produits");
    }

    @FXML
    void showCommandes() {
        // Switch to orders view
        System.out.println("Show Commandes");
    }

    @FXML
    void showPromotions() {
        // Switch to promotions view
        System.out.println("Show Promotions");
    }

    @FXML
    void showCoupons() {
        // Switch to coupons view
        System.out.println("Show Coupons");
    }

    @FXML
    void showDepots() {
        // Switch to depots view
        System.out.println("Show Depots");
    }

    @FXML
    void showStocks() {
        // Switch to stocks view
        System.out.println("Show Stocks");
    }

    @FXML
    private void logout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    void showUserManagement() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserManagement.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
