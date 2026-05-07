package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Address;
import org.example.model.User;
import org.example.service.AddressService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.List;

public class FrontMesAdressesController {
    @FXML private VBox addressesContainer;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;

    private final AddressService addressService = new AddressService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        loadAddresses();
    }

    private void loadAddresses() {
        if (addressesContainer == null) {
            return;
        }

        addressesContainer.getChildren().clear();
        User current = userService.getCurrentUser();
        if (current == null) {
            Label label = new Label("Veuillez vous connecter pour voir vos adresses.");
            label.getStyleClass().add("admin-subtitle");
            addressesContainer.getChildren().add(label);
            return;
        }

        List<Address> addresses = addressService.getByUserId(current.getId());
        if (addresses.isEmpty()) {
            Label empty = new Label("Aucune adresse enregistree.");
            empty.getStyleClass().add("admin-subtitle");
            empty.getStyleClass().add("address-empty");
            addressesContainer.getChildren().add(empty);
            return;
        }

        for (Address address : addresses) {
            addressesContainer.getChildren().add(buildAddressCard(address));
        }
    }

    private VBox buildAddressCard(Address address) {
        VBox card = new VBox(8);
        card.getStyleClass().add("address-card");
        card.setPadding(new Insets(12));

        HBox nameRow = new HBox(8);
        Label dot = new Label("\u2022");
        dot.getStyleClass().add("address-dot");
        Label name = new Label(address.getFullName());
        name.getStyleClass().add("address-name");
        nameRow.getChildren().addAll(dot, name);

        Label details = new Label(buildAddressDetails(address));
        details.getStyleClass().add("address-details");
        details.setWrapText(true);

        Label tag = new Label("Adresse livraison");
        tag.getStyleClass().add("address-tag");

        HBox actions = new HBox(8);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("btn-admin-outline");
        edit.setOnAction(e -> openEditForm(address.getId()));

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("btn-admin-danger");
        delete.setOnAction(e -> confirmDelete(address));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(edit, spacer, delete);

        card.getChildren().addAll(nameRow, details, tag, actions);
        return card;
    }

    private String buildAddressDetails(Address address) {
        StringBuilder sb = new StringBuilder();
        sb.append(value(address.getLine1()));
        if (address.getLine2() != null && !address.getLine2().isBlank()) {
            sb.append("\n").append(address.getLine2().trim());
        }
        sb.append("\n")
                .append(value(address.getCity()))
                .append(", ")
                .append(value(address.getRegion()))
                .append(" ")
                .append(value(address.getPostalCode()))
                .append("\n")
                .append(value(address.getCountry()));
        if (address.getPhone() != null && !address.getPhone().isBlank()) {
            sb.append("\nTel: ").append(address.getPhone().trim());
        }
        return sb.toString();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    @FXML
    private void openNewAddressForm() throws IOException {
        FrontAdresseFormController.setEditingAddressId(null);
        navigate("/fxml/FrontAdresseForm.fxml");
    }

    private void openEditForm(int addressId) {
        try {
            FrontAdresseFormController.setEditingAddressId(addressId);
            navigate("/fxml/FrontAdresseForm.fxml");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur ouverture formulaire: " + e.getMessage()).showAndWait();
        }
    }

    private void confirmDelete(Address address) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette adresse ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                User current = userService.getCurrentUser();
                if (current != null) {
                    addressService.deleteForUser(address.getId(), current.getId());
                    loadAddresses();
                }
            }
        });
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
    private void showFrontServices() throws IOException {
        navigate("/fxml/FrontServices.fxml");
    }

    @FXML
    private void showFrontPanier() throws IOException {
        navigate("/fxml/FrontCommande.fxml");
    }

    @FXML
    private void showFrontAddresses() {
        // already on addresses page
    }

    @FXML
    private void showFrontTracking() throws IOException {
        navigate("/fxml/FrontMesCommandes.fxml");
    }

    @FXML
    private void handleSearch() {
        if (addressesContainer != null) {
            addressesContainer.requestFocus();
        }
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
}
