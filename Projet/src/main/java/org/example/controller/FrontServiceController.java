package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.model.Service;
import org.example.service.ServiceService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class FrontServiceController {

    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private VBox servicesContainer;

    private final ServiceService serviceService = ServiceService.getInstance();
    private final UserService userService = new UserService();
    private List<Service> allServices;

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        // Initialize type filter
        typeFilter.setValue("");

        // Load services
        loadServices();

        // Add listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterServices());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterServices());
    }

    private void loadServices() {
        allServices = serviceService.getAll();
        displayServices(allServices);
    }

    private void filterServices() {
        String search = searchField.getText().toLowerCase();
        String type = typeFilter.getValue();

        List<Service> filtered = allServices.stream()
                .filter(s -> search.isEmpty() ||
                           (s.getNom() != null && s.getNom().toLowerCase().contains(search)) ||
                           (s.getSpecialite() != null && s.getSpecialite().toLowerCase().contains(search)))
                .filter(s -> type == null || type.isEmpty() ||
                           (s.getType() != null && s.getType().equalsIgnoreCase(type)))
                .collect(Collectors.toList());

        displayServices(filtered);
    }

    private void displayServices(List<Service> services) {
        servicesContainer.getChildren().clear();

        // Create rows of cards (3 per row)
        for (int i = 0; i < services.size(); i += 3) {
            HBox row = new HBox(30);
            row.setAlignment(javafx.geometry.Pos.CENTER);

            for (int j = 0; j < 3 && i + j < services.size(); j++) {
                Service service = services.get(i + j);
                VBox card = createServiceCard(service);
                row.getChildren().add(card);
            }

            servicesContainer.getChildren().add(row);
        }
    }

    private VBox createServiceCard(Service service) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setPrefHeight(220);

        // Icon based on type
        VBox iconContainer = new VBox();
        iconContainer.getStyleClass().add("card-icon-container");
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
        String iconText = "👨‍⚕️"; // Default doctor
        if ("Infirmier".equalsIgnoreCase(service.getType())) {
            iconText = "👩‍⚕️"; // Nurse
        }
        Label icon = new Label(iconText);
        icon.getStyleClass().add("card-icon");
        iconContainer.getChildren().add(icon);

        // Title
        Label title = new Label(service.getNom());
        title.getStyleClass().add("card-title");

        // Details
        VBox details = new VBox(5);
        Label type = new Label("🏥 Type: " + service.getType());
        Label specialite = new Label("🔬 Spécialité: " + (service.getSpecialite() != null ? service.getSpecialite() : "N/A"));
        Label telephone = new Label("📞 Tel: " + service.getTelephone());
        Label email = new Label("📧 Email: " + service.getEmail());
        Label adresse = new Label("📍 Adresse: " + service.getAdresse());

        details.getChildren().addAll(type, specialite, telephone, email, adresse);

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
