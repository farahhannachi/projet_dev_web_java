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

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FrontServicesController {
    @FXML private FlowPane servicesFlow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label totalServicesLabel;
    @FXML private FrontShopNavBarController shopNavController;

    private final ServiceService serviceService = ServiceService.getInstance();
    private ObservableList<Service> services = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (shopNavController != null) {
            shopNavController.configure(FrontShopNavBarController.ActiveShopPage.SERVICES, searchField);
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

    private String safe(String value) {
        return safe(value, "");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
