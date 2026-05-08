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
import org.example.service.CommandeService;
import org.example.service.ProduitService;
import org.example.service.StockService;
import org.example.service.UserService;
import org.example.util.SceneNavigation;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DashboardController {

    private static final AtomicReference<String> PENDING_DASHBOARD_VIEW = new AtomicReference<>();

    /**
     * Ouvre le dashboard puis la section demandée après le premier {@code initialize()}
     * (ex. depuis Ordonnances admin / Traitements admin).
     */
    public static void requestDashboardView(String viewKey) {
        if (viewKey != null && !viewKey.isBlank()) {
            PENDING_DASHBOARD_VIEW.set(viewKey.trim().toLowerCase());
        }
    }

    public static void requestOpenContactSection() {
        requestDashboardView("contact");
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
    private final ProduitService produitService = ProduitService.getInstance();
    private CommandeService commandeService = new CommandeService();
    private final StockService stockService = StockService.getInstance();
    private final UserService userService = new UserService();
    private javafx.scene.Node dashboardCenter;

    @FXML
    public void initialize() {
        dashboardCenter = mainPane.getCenter();
        addSampleData();
        loadStats();
        String pending = PENDING_DASHBOARD_VIEW.getAndSet(null);
        if (pending != null) {
            Platform.runLater(() -> navigateToEmbeddedSection(pending));
        }
    }

    private void addSampleData() {
        produitService.add(new Produit(0, "Paracétamol", "Antalgique et antipyrétique", 5.50, 100, "Analgésiques", true));
        produitService.add(new Produit(0, "Ibuprofène", "Anti-inflammatoire non stéroïdien", 4.20, 80, "Anti-inflammatoires", true));
        produitService.add(new Produit(0, "Aspirine", "Antalgique et antiagrégant", 3.80, 120, "Analgésiques", true));
        produitService.add(new Produit(0, "Vitamine C", "Supplément vitaminique", 8.90, 50, "Vitamines", true));

        clientService.add(new Client(0, "Dupont", "Jean", "jean.dupont@email.com", "0123456789", LocalDate.of(1980, 5, 15), "123 Rue de la Paix, Paris"));
        clientService.add(new Client(0, "Martin", "Marie", "marie.martin@email.com", "0987654321", LocalDate.of(1990, 3, 22), "456 Avenue des Champs, Lyon"));

        Depot depot1 = new Depot(0, "Dépôt Central", "10 Rue du Stock, Paris", "0145678901");
        Depot depot2 = new Depot(0, "Dépôt Régional", "20 Boulevard Commercial, Lyon", "0276543210");

        stockService.add(new Stock(0, produitService.getAll().get(0), 50, 10, depot1));
        stockService.add(new Stock(0, produitService.getAll().get(1), 5, 10, depot1));

        // commandeService.add(...) retiré : les données de test ne doivent pas être insérées en base
        // commandeService.add(new Commande(0, clientService.getAll().get(0), java.util.Arrays.asList(produitService.getAll().get(0)), LocalDate.now(), 5.50, "Confirmée"));
    }

    private void loadStats() {
        totalClientsLabel.setText(String.valueOf(clientService.getAll().size()));
        totalProduitsLabel.setText(String.valueOf(produitService.getAll().size()));
        totalCommandesLabel.setText(String.valueOf(commandeService.getAll().size()));
        alertesStockLabel.setText(String.valueOf(stockService.getStocksFaibles().size()));
    }

    @FXML
    private void handleNouveauClient() {
        showClients();
    }

    @FXML
    private void handleAjouterProduit() {
        showProduits();
    }

    @FXML
    private void handleNouvelleCommande() {
        showCommandes();
    }

    @FXML
    private void goToFrontOffice() {
        SceneNavigation.replaceScene(frontOfficeBtn, "/fxml/Accueil.fxml");
    }

    @FXML
    private void showClients() {
        loadCenterContent("/fxml/UserManagement.fxml", null);
    }

    @FXML
    private void showProduits() {
        loadCenterContent("/fxml/Produits.fxml", ctrl -> {
            if (ctrl instanceof ProduitsController p) {
                p.setDashboardController(this);
            }
        });
    }

    @FXML
    private void showCommandes() {
        loadCenterContent("/fxml/Commandes.fxml", ctrl -> {
            if (ctrl instanceof CommandesController c) {
                c.setDashboardController(this);
            }
        });
    }

    @FXML
    private void showPromotions() {
        loadCenterContent("/fxml/Promotions.fxml", ctrl -> {
            if (ctrl instanceof PromotionsController p) {
                p.setDashboardController(this);
            }
        });
    }

    @FXML
    private void showCoupons() {
        loadCenterContent("/fxml/Coupons.fxml", ctrl -> {
            if (ctrl instanceof CouponsController c) {
                c.setDashboardController(this);
            }
        });
    }

    @FXML
    private void showDepots() {
        loadCenterContent("/fxml/Depots.fxml", null);
    }

    @FXML
    private void showStocks() {
        loadCenterContent("/fxml/Stocks.fxml", null);
    }

    @FXML
    private void showServices() {
        loadCenterContent("/fxml/Services.fxml", null);
    }

    @FXML
    private void showReservations() {
        loadCenterContent("/fxml/Reservations.fxml", null);
    }

    @FXML
    private void showConsommations() {
        loadCenterContent("/fxml/Consommations.fxml", null);
    }

    @FXML
    private void showBackOrdonnances() {
        switchRootScene("/fxml/BackOrdonnance.fxml");
    }

    @FXML
    private void showBackTraitements() {
        switchRootScene("/fxml/BackTraitement.fxml");
    }

    private void switchRootScene(String fxmlPath) {
        SceneNavigation.replaceScene(mainPane, fxmlPath);
    }

    @FXML
    private void showResponseQuestions() {
        loadCenterContent("/fxml/ResponseQuestionAdmin.fxml", null);
    }

    @FXML
    public void showDashboardHome() {
        if (dashboardCenter != null) {
            mainPane.setCenter(dashboardCenter);
            loadStats();
        }
    }

    private void loadCenterContent(String fxmlPath, Consumer<Object> dashboardInjector) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();
            if (dashboardInjector != null) {
                dashboardInjector.accept(loader.getController());
            }
            mainPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToEmbeddedSection(String key) {
        switch (key) {
            case "contact" -> showResponseQuestions();
            case "clients" -> showClients();
            case "home" -> showDashboardHome();
            case "produits" -> showProduits();
            case "commandes" -> showCommandes();
            case "promotions" -> showPromotions();
            case "coupons" -> showCoupons();
            case "depots", "depôts", "depot" -> showDepots();
            case "stocks", "stock" -> showStocks();
            case "services" -> showServices();
            case "reservations", "reservation" -> showReservations();
            case "consommations", "consommation" -> showConsommations();
            default -> showDashboardHome();
        }
    }

    @FXML
    private void logout() {
        userService.logout();
        SceneNavigation.replaceScene(mainPane, "/fxml/Login.fxml");
    }
}
