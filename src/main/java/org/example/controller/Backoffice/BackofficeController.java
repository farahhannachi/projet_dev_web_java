package org.example.controller.Backoffice;

import org.example.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class BackofficeController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnNavOrdonnances;
    @FXML private Button btnNavTraitements;
    @FXML private Label lblPageTitle;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadContent("/Backoffice/BackofficeOrdonnance.fxml");
        setActiveNav(btnNavOrdonnances);
        lblPageTitle.setText("Ordonnances");
    }

    @FXML
    private void goToDashboard() {
        MainApp.navigateTo("/Backoffice/Dashboard.fxml", "CuraVita - Dashboard");
    }

    @FXML
    private void showOrdonnances() {
        loadContent("/Backoffice/BackofficeOrdonnance.fxml");
        setActiveNav(btnNavOrdonnances);
        lblPageTitle.setText("Ordonnances");
    }

    @FXML
    private void showTraitements() {
        loadContent("/Backoffice/BackofficeTraitement.fxml");
        setActiveNav(btnNavTraitements);
        lblPageTitle.setText("Traitements");
    }

    private void loadContent(String fxmlPath) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(Button active) {
        btnNavOrdonnances.getStyleClass().setAll("sidebar-btn");
        btnNavTraitements.getStyleClass().setAll("sidebar-btn");
        active.getStyleClass().setAll("sidebar-btn-active");
    }
}
