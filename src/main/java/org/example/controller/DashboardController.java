package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.example.model.Client;
import org.example.model.Commande;
import org.example.model.Depot;
import org.example.model.Produit;
import org.example.model.Stock;
import org.example.service.ClientService;
import org.example.service.ProduitService;
import org.example.service.CommandeService;
import org.example.service.StockService;
import org.example.service.UserService;
import org.example.util.SceneNavigation;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public class DashboardController {

    /** Lorsque true, le premier {@link #initialize()} après chargement du Dashboard affiche la vue Contact / tickets. */
    private static final AtomicBoolean OPEN_CONTACT_SECTION_ON_NEXT_LOAD = new AtomicBoolean(false);

    /** Appelé depuis les autres écrans admin avant {@code replaceScene(..., "/fxml/Dashboard.fxml")}. */
    public static void requestOpenContactSection() {
        OPEN_CONTACT_SECTION_ON_NEXT_LOAD.set(true);
    }

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
    private final UserService userService = new UserService();
    private javafx.scene.Node dashboardCenter;

    @FXML
    public void initialize() {
        dashboardCenter = mainPane.getCenter();
        addSampleData();
        loadStats();
        if (OPEN_CONTACT_SECTION_ON_NEXT_LOAD.compareAndSet(true, false)) {
            Platform.runLater(this::showResponseQuestions);
        }
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
    private void goToFrontOffice() {
        SceneNavigation.replaceScene(frontOfficeBtn, "/fxml/Accueil.fxml");
    }

    @FXML
    private void showClients() {
        loadCenterContent("/fxml/UserManagement.fxml");
    }

    @FXML
    private void showProduits() {
        setCenterPlaceholder("Produits", "Vue catalogue détaillée — bientôt disponible.");
    }

    @FXML
    private void showCommandes() {
        setCenterPlaceholder("Commandes", "Vue commandes — bientôt disponible.");
    }

    @FXML
    private void showPromotions() {
        setCenterPlaceholder("Promotions", "Gestion des promotions — bientôt disponible.");
    }

    @FXML
    private void showCoupons() {
        setCenterPlaceholder("Coupons", "Gestion des coupons — bientôt disponible.");
    }

    @FXML
    private void showDepots() {
        setCenterPlaceholder("Dépôts", "Gestion des dépôts — bientôt disponible.");
    }

    @FXML
    private void showStocks() {
        setCenterPlaceholder("Stocks", "Gestion des stocks — bientôt disponible.");
    }

    @FXML
    private void showBackOrdonnances() {
        switchRootScene("/fxml/BackOrdonnance.fxml");
    }

    @FXML
    private void showBackTraitements() {
        switchRootScene("/fxml/BackTraitement.fxml");
    }

    private void setCenterPlaceholder(String title, String subtitle) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");
        Label detail = new Label(subtitle);
        VBox box = new VBox(12, heading, detail);
        box.setStyle("-fx-padding: 40;");
        mainPane.setCenter(box);
    }

    private void switchRootScene(String fxmlPath) {
        SceneNavigation.replaceScene(mainPane, fxmlPath);
    }

    @FXML
    private void showResponseQuestions() {
        loadCenterContent("/fxml/ResponseQuestionAdmin.fxml");
    }

    @FXML
    public void showDashboardHome() {
        if (dashboardCenter != null) {
            mainPane.setCenter(dashboardCenter);
        }
    }

    private void loadCenterContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            mainPane.setCenter(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        userService.logout();
        SceneNavigation.replaceScene(mainPane, "/fxml/Login.fxml");
    }
}
