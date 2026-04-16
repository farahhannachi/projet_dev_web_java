package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.UserService;

import java.io.IOException;

public class AccueilController {

    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;

    private UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        // Ordonnance hover dropdown
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
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void showProfile() {
        System.out.println("Profile clicked");
        // Hide dropdown
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToTraitement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Traitement.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showCreerOrdonnance() {
        try {
            ordonnanceDropdown.setVisible(false);
            ordonnanceDropdown.setManaged(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showMesOrdonnances() {
        try {
            ordonnanceDropdown.setVisible(false);
            ordonnanceDropdown.setManaged(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesOrdonnances.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void logout() throws IOException {
        userService.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
    }
}
