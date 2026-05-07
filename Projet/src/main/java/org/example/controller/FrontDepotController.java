package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.model.Depot;
import org.example.service.DepotService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class FrontDepotController {

    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> villeFilter;
    @FXML private VBox depotsContainer;

    private final DepotService depotService = DepotService.getInstance();
    private final UserService userService = new UserService();
    private List<Depot> allDepots;

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        // Initialize filters
        villeFilter.getItems().addAll("", "Tunis", "Sfax", "Sousse");
        villeFilter.setValue("");

        // Load depots
        loadDepots();

        // Add listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterDepots());
        villeFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterDepots());
    }

    private void loadDepots() {
        allDepots = depotService.getAll();
        displayDepots(allDepots);
    }

    private void filterDepots() {
        String search = searchField.getText().toLowerCase();
        String ville = villeFilter.getValue();

        List<Depot> filtered = allDepots.stream()
                .filter(d -> search.isEmpty() ||
                           (d.getNom() != null && d.getNom().toLowerCase().contains(search)) ||
                           (d.getAdresse() != null && d.getAdresse().toLowerCase().contains(search)))
                .filter(d -> ville == null || ville.isEmpty() ||
                           (d.getVille() != null && d.getVille().equalsIgnoreCase(ville)))
                .collect(Collectors.toList());

        displayDepots(filtered);
    }

    private void displayDepots(List<Depot> depots) {
        depotsContainer.getChildren().clear();

        // Create rows of cards (3 per row)
        for (int i = 0; i < depots.size(); i += 3) {
            HBox row = new HBox(30);
            row.setAlignment(javafx.geometry.Pos.CENTER);

            for (int j = 0; j < 3 && i + j < depots.size(); j++) {
                Depot depot = depots.get(i + j);
                VBox card = createDepotCard(depot);
                row.getChildren().add(card);
            }

            depotsContainer.getChildren().add(row);
        }
    }

    private VBox createDepotCard(Depot depot) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setPrefHeight(200);

        // Icon
        VBox iconContainer = new VBox();
        iconContainer.getStyleClass().add("card-icon-container");
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
        Label icon = new Label("🏢");
        icon.getStyleClass().add("card-icon");
        iconContainer.getChildren().add(icon);

        // Title
        Label title = new Label(depot.getNom());
        title.getStyleClass().add("card-title");

        // Details
        VBox details = new VBox(5);
        Label adresse = new Label("📍 " + depot.getAdresse());
        Label ville = new Label("🏙️ " + depot.getVille());
        Label capacite = new Label("📦 Capacité: " + depot.getCapaciteDepot());
        Label responsable = new Label("👤 " + depot.getResponsableDepot());
        Label telephone = new Label("📞 " + depot.getResponsableTelephone());

        details.getChildren().addAll(adresse, ville, capacite, responsable, telephone);

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
    private void showStocks() throws IOException {
        navigateTo("/fxml/FrontStocks.fxml");
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
