package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.ClientService;
import org.example.service.ProduitService;
import org.example.service.CommandeService;
import org.example.service.StockService;

import java.io.IOException;
import java.util.logging.Logger;

public class DashboardController {

    private static final Logger logger = Logger.getLogger(DashboardController.class.getName());

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

    private Node dashboardHomeContent;

    private final ClientService clientService = new ClientService();
    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final StockService stockService = new StockService();

    @FXML
    public void initialize() {
        dashboardHomeContent = mainPane.getCenter();
        loadStats();
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
        logger.info("Nouveau Client clicked");
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
    private void goToFrontOffice() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String cssResource = getClass().getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(cssResource);
        Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void showClients() {
        // Switch to clients view
        logger.info("Show Clients");
    }

    @FXML
    private void showProduits() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Produits.fxml"));
            loader.setControllerFactory(type -> {
                if (type == ProduitsController.class) {
                    ProduitsController controller = new ProduitsController();
                    controller.setDashboardController(this);
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent produitsView = loader.load();
            mainPane.setCenter(produitsView);
        } catch (IOException e) {
            logger.severe("Erreur lors du chargement de Produits.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showCommandes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Commandes.fxml"));
            loader.setControllerFactory(type -> {
                if (type == CommandesController.class) {
                    CommandesController controller = new CommandesController();
                    controller.setDashboardController(this);
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent commandesView = loader.load();
            mainPane.setCenter(commandesView);
        } catch (IOException e) {
            logger.severe("Erreur lors du chargement de Commandes.fxml: " + e.getMessage());
        }
    }

    public void showDashboardHome() {
        if (dashboardHomeContent != null) {
            mainPane.setCenter(dashboardHomeContent);
        }
        loadStats();
    }

    @FXML
    private void showPromotions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Promotions.fxml"));
            loader.setControllerFactory(type -> {
                if (type == PromotionsController.class) {
                    PromotionsController controller = new PromotionsController();
                    controller.setDashboardController(this);
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent promotionsView = loader.load();
            mainPane.setCenter(promotionsView);
        } catch (IOException e) {
            logger.severe("Erreur lors du chargement de Promotions.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void showCoupons() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Coupons.fxml"));
            loader.setControllerFactory(type -> {
                if (type == CouponsController.class) {
                    CouponsController controller = new CouponsController();
                    controller.setDashboardController(this);
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent couponsView = loader.load();
            mainPane.setCenter(couponsView);
        } catch (IOException e) {
            logger.severe("Erreur lors du chargement de Coupons.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void showDepots() {
        // Switch to depots view
        logger.info("Show Depots");
    }

    @FXML
    private void showStocks() {
        // Switch to stocks view
        logger.info("Show Stocks");
    }

    @FXML
    private void logout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String cssResource = getClass().getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(cssResource);
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
