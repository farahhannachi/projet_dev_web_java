package org.example.controller.Frontoffice;

import org.example.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class FrontofficeController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblUserId;
    @FXML private StackPane contentArea;
    @FXML private Button btnNavOrdonnances;
    @FXML private Button btnNavTraitements;
    @FXML private VBox ordonnanceNavBox;
    @FXML private VBox ordDropdown;

    public static int connectedUserId = 1;
    public static String connectedUserName = "Julie Martin";

    // Static reference for child controllers to navigate
    private static StackPane staticContentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(connectedUserName);
        lblUserId.setText("ID: " + connectedUserId);
        staticContentArea = contentArea;

        ordonnanceNavBox.setOnMouseEntered(e -> {
            ordDropdown.setVisible(true);
            ordDropdown.setManaged(true);
        });
        ordonnanceNavBox.setOnMouseExited(e -> {
            ordDropdown.setVisible(false);
            ordDropdown.setManaged(false);
        });
    }

    public static void navigateFromChild(String fxmlPath) {
        try {
            Node node = FXMLLoader.load(FrontofficeController.class.getResource(fxmlPath));
            staticContentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToDashboard() {
        MainApp.navigateTo("/Backoffice/Dashboard.fxml", "CuraVita - Dashboard");
    }

    @FXML
    private void showTraitements() {
        loadContent("/Frontoffice/FrontofficeTraitement.fxml");
        btnNavTraitements.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        btnNavOrdonnances.setStyle("");
    }

    // Flag to control whether to auto-open the form or show the list
    public static boolean autoOpenForm = false;

    @FXML
    private void showOrdonnanceForm() {
        // Clicking "Ordonnances" directly opens the creation form
        autoOpenForm = true;
        loadContent("/Frontoffice/FrontofficeOrdonnance.fxml");
        btnNavOrdonnances.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        btnNavTraitements.setStyle("");
    }

    @FXML
    private void showMesOrdonnances() {
        autoOpenForm = false;
        loadContent("/Frontoffice/FrontofficeOrdonnanceList.fxml");
        btnNavOrdonnances.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        btnNavTraitements.setStyle("");
        ordDropdown.setVisible(false);
        ordDropdown.setManaged(false);
    }

    private void loadContent(String fxmlPath) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
