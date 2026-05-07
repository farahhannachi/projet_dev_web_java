package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Service;
import org.example.model.Reservation;
import org.example.service.ServiceService;
import org.example.service.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontServicesController implements Initializable {

    @FXML private VBox servicesContainer;
    @FXML private ComboBox<String> typeFilter;
    @FXML private TextField searchField;
    @FXML private Label totalServicesLabel;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;

    private final ServiceService serviceService = ServiceService.getInstance();
    private ObservableList<Service> allServices = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le filtre de type
        typeFilter.setItems(FXCollections.observableArrayList("Tous", "Médecin", "Infirmier"));
        typeFilter.setValue("Tous");

        // Écouteurs pour les filtres
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Charger les données
        loadServices();
    }

    private void loadServices() {
        List<Service> services = serviceService.getAll();
        allServices.setAll(services);
        totalServicesLabel.setText(String.valueOf(services.size()));
        displayServices(services);
    }

    private void applyFilters() {
        String selectedType = typeFilter.getValue();
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";

        List<Service> filtered = allServices.stream()
                .filter(service -> {
                    // Filtre par type
                    if (!"Tous".equals(selectedType)) {
                        if (!selectedType.equals(service.getType())) {
                            return false;
                        }
                    }

                    // Filtre par recherche
                    if (!searchText.isEmpty()) {
                        return service.getNom().toLowerCase().contains(searchText) ||
                               service.getSpecialite().toLowerCase().contains(searchText) ||
                               service.getType().toLowerCase().contains(searchText);
                    }

                    return true;
                })
                .collect(Collectors.toList());

        displayServices(filtered);
    }

    private void displayServices(List<Service> services) {
        servicesContainer.getChildren().clear();

        if (services.isEmpty()) {
            Label noResultsLabel = new Label("Aucun service trouvé");
            noResultsLabel.getStyleClass().add("text-secondary");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-padding: 20px;");
            servicesContainer.getChildren().add(noResultsLabel);
            return;
        }

        for (Service service : services) {
            VBox serviceCard = createServiceCard(service);
            servicesContainer.getChildren().add(serviceCard);
        }
    }

    private VBox createServiceCard(Service service) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20px; -fx-background-color: white; -fx-background-radius: 12px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0.4, 0, 3); " +
                     "-fx-cursor: hand; -fx-transition: all 0.3s ease;");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + " -fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(31,111,92,0.2), 15, 0.5, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-padding: 20px; -fx-background-color: white; -fx-background-radius: 12px; " +
                                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0.4, 0, 3); " +
                                               "-fx-cursor: hand; -fx-transition: all 0.3s ease;"));

        // Header avec icône et nom
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Icône selon le type
        Label iconLabel = new Label();
        if ("Médecin".equals(service.getType())) {
            iconLabel.setText("👨‍⚕️");
            iconLabel.setStyle("-fx-font-size: 24px;");
        } else if ("Infirmier".equals(service.getType())) {
            iconLabel.setText("👩‍⚕️");
            iconLabel.setStyle("-fx-font-size: 24px;");
        } else {
            iconLabel.setText("🏥");
            iconLabel.setStyle("-fx-font-size: 24px;");
        }

        Label nameLabel = new Label(service.getNom());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1F6F54;");
        header.getChildren().addAll(iconLabel, nameLabel);

        // Type badge
        Label typeBadge = new Label(service.getType());
        typeBadge.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #1F6F54; -fx-padding: 4 8; " +
                          "-fx-background-radius: 12px; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox.setHgrow(typeBadge, javafx.scene.layout.Priority.ALWAYS);
        typeBadge.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        header.getChildren().add(typeBadge);

        // Spécialité
        Label specialiteLabel = new Label("Spécialité: " + service.getSpecialite());
        specialiteLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // Contact info
        VBox contactBox = new VBox(5);
        Label phoneLabel = new Label("📞 " + service.getTelephone());
        Label emailLabel = new Label("✉️ " + service.getEmail());
        phoneLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        contactBox.getChildren().addAll(phoneLabel, emailLabel);

        // Bouton Réserver
        Button reserveBtn = new Button("📅 Réserver");
        reserveBtn.setStyle("-fx-background-color: #1F6F54; -fx-text-fill: white; -fx-font-size: 13px; " +
                           "-fx-padding: 10 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        reserveBtn.setOnAction(e -> openReservationForm(service));

        // Hover effect pour le bouton
        reserveBtn.setOnMouseEntered(e -> reserveBtn.setStyle(
            "-fx-background-color: #2E8B57; -fx-text-fill: white; -fx-font-size: 13px; " +
            "-fx-padding: 10 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        reserveBtn.setOnMouseExited(e -> reserveBtn.setStyle(
            "-fx-background-color: #1F6F54; -fx-text-fill: white; -fx-font-size: 13px; " +
            "-fx-padding: 10 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"));

        card.getChildren().addAll(header, specialiteLabel, contactBox, reserveBtn);
        return card;
    }

    private void openReservationForm(Service service) {
        try {
            // Charger la vue de réservation
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationForm.fxml"));
            Parent root = loader.load();

            // Passer les données du service à la vue de réservation
            ReservationFormController controller = loader.getController();
            controller.setService(service, () -> loadServices());

            // Créer et afficher la nouvelle scène
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Réservation - " + service.getNom());
            stage.initOwner(servicesContainer.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Rafraîchir la liste des services après la fermeture du formulaire
            loadServices();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire de réservation.");
        }
    }

    // Navigation methods
    @FXML
    private void goToAccueil() throws IOException {
        navigateTo("/fxml/Accueil.fxml");
    }

    @FXML
    private void showDepots() throws IOException {
        System.out.println("🔄 Navigation: Chargement du module Dépôts...");
        navigateTo("/fxml/FrontDepots.fxml");
        System.out.println("✅ Module Dépôts chargé avec succès!");
    }

    @FXML
    private void showStocks() throws IOException {
        System.out.println("🔄 Navigation: Chargement du module Stocks...");
        navigateTo("/fxml/FrontStocks.fxml");
        System.out.println("✅ Module Stocks chargé avec succès!");
    }

    @FXML
    private void showServices() throws IOException {
        // Already on services page, do nothing or refresh
        System.out.println("✅ Déjà sur la page Services");
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search clicked");
    }

    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown != null) {
            boolean isVisible = profileDropdown.isVisible();
            profileDropdown.setVisible(!isVisible);
            profileDropdown.setManaged(!isVisible);
        }
    }

    @FXML
    private void showProfile() {
        System.out.println("Profile clicked");
    }

    @FXML
    private void goToDashboard() throws IOException {
        navigateTo("/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() throws IOException {
        UserService userService = new UserService();
        userService.logout();
        navigateTo("/fxml/Login.fxml");
    }

    private void navigateTo(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) servicesContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    private void showError(String message) {
        // Méthode utilitaire pour afficher les messages d'erreur
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(servicesContainer.getScene().getWindow());
        alert.show();
    }
}
