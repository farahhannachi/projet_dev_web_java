package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Contrôleur pour la modal de sélection de location
 * Permet de saisir manuellement une location (latitude/longitude)
 */
public class MapModalController {
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label locationLabel;

    private Stage stage;
    private DepotFormController parentController;
    private String selectedLocationName;
    private double selectedLatitude;
    private double selectedLongitude;
    private static final Logger LOGGER = Logger.getLogger(MapModalController.class.getName());

    /**
     * Initialiser la modal avec les paramètres du dépôt parent
     */
    public void initialize(DepotFormController parentController, double lat, double lng) {
        this.parentController = parentController;
        this.selectedLatitude = lat;
        this.selectedLongitude = lng;

        // Pré-remplir les champs
        latitudeField.setText(String.valueOf(lat));
        longitudeField.setText(String.valueOf(lng));
        locationLabel.setText("Saisissez les coordonnées GPS");
    }

    /**
     * Bouton Enregistrer - Sauvegarder et fermer
     */
    @FXML
    private void handleSaveLocation() {
        try {
            double lat = Double.parseDouble(latitudeField.getText().trim());
            double lng = Double.parseDouble(longitudeField.getText().trim());

            if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                // Appeler le contrôleur parent pour mettre à jour les champs
                parentController.updateLocationFromMap(lat, lng, "Coordonnées saisies");

                // Fermer la modal
                closeModal();
            } else {
                locationLabel.setText("❌ Coordonnées invalides! Lat: -90/90, Lng: -180/180");
            }
        } catch (NumberFormatException e) {
            locationLabel.setText("❌ Veuillez saisir des nombres valides!");
        }
    }

    /**
     * Bouton Fermer - Fermer sans sauvegarder
     */
    @FXML
    private void handleClose() {
        closeModal();
    }

    /**
     * Fermer la modal
     */
    private void closeModal() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Définir le stage de la modal
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Ouvrir la modal en plein écran
     */
    public static void showMapModal(DepotFormController parentController, double lat, double lng) {
        try {
            FXMLLoader loader = new FXMLLoader(MapModalController.class.getResource("/fxml/MapModal.fxml"));
            VBox root = loader.load();

            MapModalController controller = loader.getController();
            controller.initialize(parentController, lat, lng);

            Stage mapStage = new Stage();
            mapStage.setTitle("Sélectionnez une location");
            mapStage.setWidth(400);
            mapStage.setHeight(300);
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setScene(new Scene(root));

            controller.setStage(mapStage);

            mapStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de la modal de location", e);
        }
    }
}
