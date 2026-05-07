package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.ClientService;
import org.example.service.CommandeService;
import org.example.service.ProduitService;
import org.example.service.StockService;

import java.io.IOException;
import java.util.logging.Level;
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
    private String currentViewPath = null;

    private final ClientService clientService = new ClientService();
    private final ProduitService produitService = ProduitService.getInstance();
    private final CommandeService commandeService = new CommandeService();
    private final StockService stockService = StockService.getInstance();

    @FXML
    public void initialize() {
        dashboardHomeContent = mainPane.getCenter();
        loadStats();
        currentViewPath = null;
    }

    private void loadStats() {
        totalClientsLabel.setText(String.valueOf(clientService.getAll().size()));
        totalProduitsLabel.setText(String.valueOf(produitService.getAll().size()));
        totalCommandesLabel.setText(String.valueOf(commandeService.getAll().size()));
        alertesStockLabel.setText(String.valueOf(stockService.getStocksFaibles().size()));
    }

    @FXML
    private void handleNouveauClient() {
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
        logger.info("Show Clients");
    }

    @FXML
    private void showProduits() {
        loadViewWithControllerFactory("/fxml/Produits.fxml", "Produits");
    }

    @FXML
    private void showCommandes() {
        loadViewWithControllerFactory("/fxml/Commandes.fxml", "Commandes");
    }

    @FXML
    private void showPromotions() {
        loadViewWithControllerFactory("/fxml/Promotions.fxml", "Promotions");
    }

    @FXML
    private void showCoupons() {
        loadViewWithControllerFactory("/fxml/Coupons.fxml", "Coupons");
    }

    @FXML
    private void showDepots() {
        navigateToSimple("/fxml/Depots.fxml", "Depots");
    }

    @FXML
    private void showStocks() {
        navigateToSimple("/fxml/Stocks.fxml", "Stocks");
    }

    @FXML
    private void showServices() {
        navigateToSimple("/fxml/Services.fxml", "Services");
    }

    @FXML
    private void showConsommation() {
        navigateToSimple("/fxml/Consommation.fxml", "Consommation");
    }

    @FXML
    private void showReservations() {
        navigateToSimple("/fxml/Reservations.fxml", "Reservations");
    }

    public void showDashboardHome() {
        if (dashboardHomeContent != null) {
            mainPane.setCenter(dashboardHomeContent);
            currentViewPath = null;
        }
        loadStats();
    }

    private void loadViewWithControllerFactory(String fxmlPath, String label) {
        if (fxmlPath != null && fxmlPath.equals(currentViewPath)) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(type -> {
                try {
                    Object controller = type.getDeclaredConstructor().newInstance();
                    if (controller instanceof ProduitsController produitsController) {
                        produitsController.setDashboardController(this);
                    } else if (controller instanceof CommandesController commandesController) {
                        commandesController.setDashboardController(this);
                    } else if (controller instanceof PromotionsController promotionsController) {
                        promotionsController.setDashboardController(this);
                    } else if (controller instanceof CouponsController couponsController) {
                        couponsController.setDashboardController(this);
                    }
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent view = loader.load();
            mainPane.setCenter(view);
            currentViewPath = fxmlPath;
        } catch (IOException e) {
            showLoadError(label, e);
        }
    }

    private void navigateToSimple(String fxmlPath, String label) {
        if (fxmlPath != null && fxmlPath.equals(currentViewPath)) {
            logger.info("Navigation ignored, already on view: " + label);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainPane.setCenter(view);
            currentViewPath = fxmlPath;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement de " + fxmlPath, e);
            showLoadError(label, e);
        }
    }

    private void showLoadError(String label, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Impossible de charger " + label);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
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
