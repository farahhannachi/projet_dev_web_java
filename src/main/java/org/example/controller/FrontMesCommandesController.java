package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Commande;
import org.example.model.User;
import org.example.service.CommandeService;
import org.example.service.UserService;

import java.io.IOException;
import java.util.List;

public class FrontMesCommandesController {
    @FXML private TableView<Commande> commandesTable;
    @FXML private TableColumn<Commande, Integer> idCol;
    @FXML private TableColumn<Commande, String> statutCol;
    @FXML private TableColumn<Commande, Double> totalCol;
    @FXML private TableColumn<Commande, String> paiementCol;
    @FXML private TableColumn<Commande, String> adresseCol;
    @FXML private TableColumn<Commande, java.time.LocalDate> dateCol;

    @FXML private Label totalCommandesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label livreesLabel;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;

    private final CommandeService commandeService = new CommandeService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        paiementCol.setCellValueFactory(new PropertyValueFactory<>("modePaiement"));
        adresseCol.setCellValueFactory(new PropertyValueFactory<>("adresseLivraison"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateCommande"));

        loadMyOrders();
    }

    private void loadMyOrders() {
        User current = userService.getCurrentUser();
        if (current == null) {
            commandesTable.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Commande> all = commandeService.getAll();
        List<Commande> mine = all.stream()
                .filter(c -> {
                    if (c.getUtilisateurId() != null) {
                        return c.getUtilisateurId() == current.getId();
                    }
                    return c.getEmail() != null && c.getEmail().equalsIgnoreCase(current.getEmail());
                })
                .toList();

        long enAttente = mine.stream().filter(c -> "en_attente".equalsIgnoreCase(c.getStatut())).count();
        long livrees = mine.stream().filter(c -> "livree".equalsIgnoreCase(c.getStatut())).count();

        totalCommandesLabel.setText(String.valueOf(mine.size()));
        enAttenteLabel.setText(String.valueOf(enAttente));
        livreesLabel.setText(String.valueOf(livrees));

        commandesTable.setItems(FXCollections.observableArrayList(mine));
    }

    @FXML
    private void goBackHome() throws IOException {
        navigate("/fxml/Accueil.fxml");
    }

    @FXML
    private void goHome() throws IOException {
        goBackHome();
    }

    @FXML
    private void showFrontProduits() throws IOException {
        navigate("/fxml/FrontProduits.fxml");
    }

    @FXML
    private void showFrontPanier() throws IOException {
        navigate("/fxml/FrontCommande.fxml");
    }

    @FXML
    private void showFrontTracking() {
        // already on tracking page
    }

    @FXML
    private void handleSearch() {
        commandesTable.requestFocus();
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
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

    private void navigate(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) commandesTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
