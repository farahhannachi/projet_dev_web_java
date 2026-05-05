package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.UserService;

import java.io.IOException;

public class AccueilController {

    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;
    @FXML private HBox profileContainer;
    @FXML private javafx.scene.shape.Circle navbarAvatarCircle;
    @FXML private Label navbarUsername;

    private UserService userService = UserService.getInstance();

    // Méthode utilitaire pour récupérer la Stage depuis n'importe quel nœud lié
    private Stage getStage() {
        if (navbarUsername != null && navbarUsername.getScene() != null) {
            return (Stage) navbarUsername.getScene().getWindow();
        }
        if (profileContainer != null && profileContainer.getScene() != null) {
            return (Stage) profileContainer.getScene().getWindow();
        }
        return null;
    }

    private void nav(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.setFullScreen(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        org.example.model.User currentUser = userService.getCurrentUser();
        if (navbarUsername != null && currentUser != null) {
            String nom = currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }

        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> {
                ordonnanceDropdown.setVisible(true);
                ordonnanceDropdown.setManaged(true);
            });
            ordonnanceMenuContainer.setOnMouseExited(e -> {
                ordonnanceDropdown.setVisible(false);
                ordonnanceDropdown.setManaged(false);
            });
        }
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
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        nav("/fxml/Profil.fxml");
    }

    @FXML
    private void goToTraitement() {
        nav("/fxml/Traitement.fxml");
    }

    @FXML
    private void showCreerOrdonnance() {
        if (ordonnanceDropdown != null) {
            ordonnanceDropdown.setVisible(false);
            ordonnanceDropdown.setManaged(false);
        }
        nav("/fxml/Ordonnance.fxml");
    }

    @FXML
    private void showMesOrdonnances() {
        if (ordonnanceDropdown != null) {
            ordonnanceDropdown.setVisible(false);
            ordonnanceDropdown.setManaged(false);
        }
        nav("/fxml/MesOrdonnances.fxml");
    }

    @FXML
    private void goToDashboard() {
        nav("/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() {
        userService.logout();
        nav("/fxml/Login.fxml");
    }
}
