package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.model.Depot;
import org.example.model.Stock;
import org.example.service.DepotService;
import org.example.service.StockService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class FrontStockController {

    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> depotFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox stocksContainer;

    private final StockService stockService = StockService.getInstance();
    private final DepotService depotService = DepotService.getInstance();
    private final UserService userService = new UserService();
    private List<Stock> allStocks;

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        // Initialize depot filter
        List<Depot> depots = depotService.getAll();
        depotFilter.getItems().add("");
        depots.forEach(d -> depotFilter.getItems().add(d.getNom()));
        depotFilter.setValue("");

        // Initialize status filter
        statusFilter.getItems().addAll("", "En stock", "Stock faible", "Rupture");
        statusFilter.setValue("");

        // Load stocks
        loadStocks();

        // Add listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterStocks());
        depotFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterStocks());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterStocks());
    }

    private void loadStocks() {
        allStocks = stockService.getAll();
        displayStocks(allStocks);
    }

    private void filterStocks() {
        String search = searchField.getText().toLowerCase();
        String depot = depotFilter.getValue();
        String status = statusFilter.getValue();

        List<Stock> filtered = allStocks.stream()
                .filter(s -> search.isEmpty() ||
                           (s.getProduit() != null && s.getProduit().getNom() != null &&
                            s.getProduit().getNom().toLowerCase().contains(search)))
                .filter(s -> depot == null || depot.isEmpty() ||
                           (s.getDepot() != null && s.getDepot().getNom() != null &&
                            s.getDepot().getNom().equalsIgnoreCase(depot)))
                .filter(s -> {
                    if (status == null || status.isEmpty()) return true;
                    switch (status) {
                        case "En stock": return s.getQuantiteDisponible() > s.getSeuilMinimum();
                        case "Stock faible": return s.getQuantiteDisponible() <= s.getSeuilMinimum() && s.getQuantiteDisponible() > 0;
                        case "Rupture": return s.getQuantiteDisponible() == 0;
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        displayStocks(filtered);
    }

    private void displayStocks(List<Stock> stocks) {
        stocksContainer.getChildren().clear();

        // Create rows of cards (3 per row)
        for (int i = 0; i < stocks.size(); i += 3) {
            HBox row = new HBox(30);
            row.setAlignment(javafx.geometry.Pos.CENTER);

            for (int j = 0; j < 3 && i + j < stocks.size(); j++) {
                Stock stock = stocks.get(i + j);
                VBox card = createStockCard(stock);
                row.getChildren().add(card);
            }

            stocksContainer.getChildren().add(row);
        }
    }

    private VBox createStockCard(Stock stock) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setPrefHeight(200);

        // Icon based on status
        VBox iconContainer = new VBox();
        iconContainer.getStyleClass().add("card-icon-container");
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
        String iconText;
        if (stock.getQuantiteDisponible() == 0) {
            iconText = "❌"; // Rupture
        } else if (stock.getQuantiteDisponible() <= stock.getSeuilMinimum()) {
            iconText = "⚠️"; // Stock faible
        } else {
            iconText = "✅"; // En stock
        }
        Label icon = new Label(iconText);
        icon.getStyleClass().add("card-icon");
        iconContainer.getChildren().add(icon);

        // Title
        Label title = new Label(stock.getProduit() != null ? stock.getProduit().getNom() : "N/A");
        title.getStyleClass().add("card-title");

        // Details
        VBox details = new VBox(5);
        Label quantite = new Label("📦 Quantité: " + stock.getQuantiteDisponible());
        Label seuil = new Label("🎯 Seuil min: " + stock.getSeuilMinimum());
        Label depot = new Label("🏢 Dépôt: " + (stock.getDepot() != null ? stock.getDepot().getNom() : "N/A"));

        String status;
        if (stock.getQuantiteDisponible() == 0) {
            status = "🔴 Rupture de stock";
        } else if (stock.getQuantiteDisponible() <= stock.getSeuilMinimum()) {
            status = "🟡 Stock faible";
        } else {
            status = "🟢 En stock";
        }
        Label statusLabel = new Label(status);

        details.getChildren().addAll(quantite, seuil, depot, statusLabel);

        card.getChildren().addAll(iconContainer, title, details);
        return card;
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search clicked");
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void showProfile() {
        System.out.println("Profile clicked");
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() throws IOException {
        navigateTo("/fxml/Dashboard.fxml");
    }

    @FXML
    private void goToAccueil() throws IOException {
        navigateTo("/fxml/Accueil.fxml");
    }

    @FXML
    private void showDepots() throws IOException {
        navigateTo("/fxml/FrontDepots.fxml");
    }

    @FXML
    private void showServices() throws IOException {
        navigateTo("/fxml/FrontServices.fxml");
    }

    @FXML
    private void logout() throws IOException {
        userService.logout();
        navigateTo("/fxml/Login.fxml");
    }

    @FXML
    private void showStocks() throws IOException {
        navigateTo("/fxml/FrontStocks.fxml");
    }

    private void navigateTo(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
