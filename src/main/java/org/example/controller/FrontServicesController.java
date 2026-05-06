package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Service;
import org.example.service.ServiceService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FrontServicesController {
    @FXML private FlowPane servicesFlow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label totalServicesLabel;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;

    private final ServiceService serviceService = ServiceService.getInstance();
    private final UserService userService = new UserService();
    private ObservableList<Service> services = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        loadServices();
    }

    private void loadServices() {
        List<Service> loaded = serviceService.getAll();
        services.setAll(loaded);
        totalServicesLabel.setText(String.valueOf(loaded.size()));

        ObservableList<String> types = FXCollections.observableArrayList("Tous types");
        loaded.stream()
                .map(Service::getType)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .forEach(types::add);
        typeFilter.setItems(types);
        typeFilter.setValue("Tous types");

        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedType = typeFilter.getValue() == null ? "Tous types" : typeFilter.getValue();

        List<Service> filtered = services.stream()
                .filter(service -> matchesQuery(service, query))
                .filter(service -> "Tous types".equals(selectedType) || selectedType.equals(safe(service.getType())))
                .sorted(Comparator.comparing(service -> safe(service.getNom()).toLowerCase(Locale.ROOT)))
                .toList();

        renderServices(filtered);
    }

    private boolean matchesQuery(Service service, String query) {
        if (query.isBlank()) {
            return true;
        }
        return safe(service.getNom()).toLowerCase(Locale.ROOT).contains(query)
                || safe(service.getType()).toLowerCase(Locale.ROOT).contains(query)
                || safe(service.getSpecialite()).toLowerCase(Locale.ROOT).contains(query)
                || safe(service.getAdresse()).toLowerCase(Locale.ROOT).contains(query);
    }

    private void renderServices(List<Service> source) {
        servicesFlow.getChildren().clear();

        if (source.isEmpty()) {
            Label empty = new Label("Aucun service ne correspond aux filtres.");
            empty.getStyleClass().add("admin-subtitle");
            servicesFlow.getChildren().add(empty);
            return;
        }

        for (Service service : source) {
            servicesFlow.getChildren().add(buildServiceCard(service));
        }
    }

    private VBox buildServiceCard(Service service) {
        VBox card = new VBox(8);
        card.getStyleClass().add("front-product-card");
        card.setPrefWidth(290);
        card.setPadding(new Insets(12));

        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(safe(service.getNom(), "Service"));
        title.getStyleClass().add("front-product-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(safe(service.getType(), "Service"));
        badge.getStyleClass().add("admin-meta-badge");
        header.getChildren().addAll(title, spacer, badge);

        Label speciality = new Label("Specialite: " + safe(service.getSpecialite(), "Non renseignee"));
        speciality.getStyleClass().add("front-product-desc");
        speciality.setWrapText(true);

        Label phone = new Label("Telephone: " + safe(service.getTelephone(), "Non renseigne"));
        phone.getStyleClass().add("front-product-desc");

        Label email = new Label("Email: " + safe(service.getEmail(), "Non renseigne"));
        email.getStyleClass().add("front-product-desc");

        Label address = new Label("Adresse: " + safe(service.getAdresse(), "Non renseignee"));
        address.getStyleClass().add("front-product-desc");
        address.setWrapText(true);

        Button reserveButton = new Button("Reserver");
        reserveButton.getStyleClass().add("btn-admin-primary");
        reserveButton.setMaxWidth(Double.MAX_VALUE);
        reserveButton.setOnAction(event -> openReservationForm(service));

        card.getChildren().addAll(header, speciality, phone, email, address, reserveButton);
        return card;
    }

    private void openReservationForm(Service service) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationForm.fxml"));
            Parent root = loader.load();
            ReservationFormController controller = loader.getController();
            controller.setService(service, this::loadServices);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = new Stage();
            stage.setTitle("Reservation - " + safe(service.getNom(), "Service"));
            stage.setScene(scene);
            stage.initOwner(servicesFlow.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadServices();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur ouverture reservation: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void goHome() throws IOException {
        navigate("/fxml/Accueil.fxml");
    }

    @FXML
    private void showFrontProduits() throws IOException {
        navigate("/fxml/FrontProduits.fxml");
    }

    @FXML
    private void showFrontServices() {
        // already on services page
    }

    @FXML
    private void showFrontPanier() throws IOException {
        navigate("/fxml/FrontCommande.fxml");
    }

    @FXML
    private void showFrontAddresses() throws IOException {
        navigate("/fxml/FrontMesAdresses.fxml");
    }

    @FXML
    private void showFrontTracking() throws IOException {
        navigate("/fxml/FrontMesCommandes.fxml");
    }

    @FXML
    private void handleSearch() {
        searchField.requestFocus();
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean visible = profileDropdown.isVisible();
        profileDropdown.setVisible(!visible);
        profileDropdown.setManaged(!visible);
    }

    @FXML
    private void showProfile() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() throws IOException {
        navigate("/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() throws IOException {
        userService.logout();
        navigate("/fxml/Login.fxml");
    }

    private void navigate(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    private String safe(String value) {
        return safe(value, "");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
