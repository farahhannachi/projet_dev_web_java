package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.service.UserService;

import java.io.IOException;

public class AccueilController {

    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    
    // Navbar elements
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarAvatarLabel;
    @FXML private Label navbarUsername;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        
        // Load navbar user data
        loadNavbarUserData();
    }
    
    private void loadNavbarUserData() {
        var currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            // Set username in navbar
            if (navbarUsername != null) {
                String username = currentUser.getNom();
                if (username != null && !username.isEmpty()) {
                    // Get first name only for cleaner look
                    String firstName = username.split(" ")[0];
                    navbarUsername.setText(firstName);
                } else {
                    navbarUsername.setText("Utilisateur");
                }
            }
            
            // Set avatar circle color
            if (navbarAvatarCircle != null) {
                navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
            }
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
    private void showProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("Press ESC to exit fullscreen");
        
        // Hide dropdown
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileContainer.getScene().getWindow();
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
        Stage stage = (Stage) profileContainer.getScene().getWindow();
        stage.setScene(scene);
    }
}
