package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.model.Depot;
import org.example.service.DepotService;
import org.example.service.LocationGeocodingService;
import org.example.util.NotificationUtil;
import org.example.util.DepotValidator;
import org.example.util.ValidationException;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur pour le formulaire d'ajout/modification de dépôt
 * Affiche le formulaire dans le content pane (pas de nouvelle fenêtre)
 * Architecture SPA - Navigation sans Stage
 *
 * ⚠️ VALIDATION CÔTÉ SERVEUR (BACKEND) :
 * - Toutes les validations sont faites côté serveur (DepotValidator)
 * - Pas de validation JavaScript
 * - Les erreurs sont affichées après validation serveur
 */
public class DepotFormController {
    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField capaciteField;
    @FXML private TextField responsableField;
    @FXML private TextField telephoneField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private TextField locationNameField;
    @FXML private Label errorLabel;

    private final DepotService depotService = DepotService.getInstance();
    private final LocationGeocodingService geocodingService = new LocationGeocodingService();
    private Depot depotToEdit;
    private DepotController parentController; // Référence au contrôleur parent
    private static final Logger LOGGER = Logger.getLogger(DepotFormController.class.getName());

    public void setDepotToEdit(Depot depot) {
        this.depotToEdit = depot;
        if (depot != null) {
            formTitle.setText("Modifier le dépôt");
            nomField.setText(depot.getNom());
            adresseField.setText(depot.getAdresse());
            villeField.setText(depot.getVille());
            capaciteField.setText(String.valueOf(depot.getCapaciteDepot()));
            responsableField.setText(depot.getResponsableDepot());
            telephoneField.setText(depot.getResponsableTelephone());
            latitudeField.setText(String.valueOf(depot.getLatitude()));
            longitudeField.setText(String.valueOf(depot.getLongitude()));
            locationNameField.setText(depot.getLocationName());
        } else {
            formTitle.setText("Ajouter un dépôt");
        }
    }

    public void setParentController(DepotController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleSave() {
        // Récupérer les valeurs du formulaire
        String nom = nomField.getText();
        String adresse = adresseField.getText();
        String ville = villeField.getText();
        String capacite = capaciteField.getText();
        String responsable = responsableField.getText();
        String telephone = telephoneField.getText();
        String latitude = latitudeField.getText();
        String longitude = longitudeField.getText();
        String locationName = locationNameField.getText();

        // ⚠️ VALIDATION CÔTÉ SERVEUR (BACKEND)
        // Créer un validateur pour vérifier tous les champs
        DepotValidator validator = new DepotValidator();
        if (!validator.validate(nom, adresse, ville, capacite, responsable, telephone, latitude, longitude, locationName)) {
            // Afficher les erreurs de validation
            String errorMessage = validator.getErrorMessage();
            errorLabel.setText(errorMessage);
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            return;
        }

        // Effacer les erreurs si validation OK
        errorLabel.setText("");

        try {
            if (depotToEdit == null) {
                // Ajout d'un nouveau dépôt
                Depot newDepot = new Depot();
                newDepot.setNom(nom.trim());
                newDepot.setAdresse(adresse.trim());
                newDepot.setVille(ville.trim());
                newDepot.setCapaciteDepot(Integer.parseInt(capacite.trim()));
                newDepot.setResponsableDepot(responsable.trim());
                newDepot.setResponsableTelephone(telephone.trim());
                newDepot.setDateCreation(LocalDateTime.now());

                try {
                    newDepot.setLatitude(latitude.trim().isEmpty() ? 0 : Double.parseDouble(latitude.trim()));
                    newDepot.setLongitude(longitude.trim().isEmpty() ? 0 : Double.parseDouble(longitude.trim()));
                } catch (NumberFormatException e) {
                    newDepot.setLatitude(0);
                    newDepot.setLongitude(0);
                }
                newDepot.setLocationName(locationName.trim());

                // Validation + insertion côté serveur
                depotService.add(newDepot);
                NotificationUtil.showSuccess("✅ Dépôt ajouté avec succès!");
            } else {
                // Modification d'un dépôt existant
                depotToEdit.setNom(nom.trim());
                depotToEdit.setAdresse(adresse.trim());
                depotToEdit.setVille(ville.trim());
                depotToEdit.setCapaciteDepot(Integer.parseInt(capacite.trim()));
                depotToEdit.setResponsableDepot(responsable.trim());
                depotToEdit.setResponsableTelephone(telephone.trim());

                try {
                    depotToEdit.setLatitude(latitude.trim().isEmpty() ? 0 : Double.parseDouble(latitude.trim()));
                    depotToEdit.setLongitude(longitude.trim().isEmpty() ? 0 : Double.parseDouble(longitude.trim()));
                } catch (NumberFormatException e) {
                    depotToEdit.setLatitude(0);
                    depotToEdit.setLongitude(0);
                }
                depotToEdit.setLocationName(locationName.trim());

                // Validation + update côté serveur
                depotService.update(depotToEdit);
                NotificationUtil.showSuccess("✅ Dépôt modifié avec succès!");
            }

            // Revenir à la liste des dépôts
            handleCancel();
        } catch (ValidationException e) {
            // Erreur de validation côté serveur
            errorLabel.setText("❌ " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
        } catch (Exception e) {
            // Erreur d'insertion/modification
            errorLabel.setText("❌ Erreur: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        // Revenir à la vue de la liste des dépôts
        if (parentController != null) {
            parentController.showTableView();
        }
    }

    /**
     * Ouvrir la modal de carte en plein écran
     */
    @FXML
    private void handleOpenMap() {
        double lat = 0;
        double lng = 0;

        // Récupérer la latitude/longitude actuelle s'ils existent
        try {
            if (!latitudeField.getText().isEmpty()) {
                lat = Double.parseDouble(latitudeField.getText());
            }
            if (!longitudeField.getText().isEmpty()) {
                lng = Double.parseDouble(longitudeField.getText());
            }
        } catch (NumberFormatException e) {
            lat = 46.603354;
            lng = 1.888334;
        }

        // Ouvrir la modal de carte
        MapModalController.showMapModal(this, lat, lng);
    }

    /**
     * Appelé depuis MapModalController après sélection d'une location
     */
    public void updateLocationFromMap(double latitude, double longitude, String locationName) {
        javafx.application.Platform.runLater(() -> {
            latitudeField.setText(String.valueOf(latitude));
            longitudeField.setText(String.valueOf(longitude));
            locationNameField.setText(locationName);

            // Optionally, reverse geocode to get better location name
            new Thread(() -> {
                try {
                    var result = geocodingService.reverseGeocode(latitude, longitude);
                    javafx.application.Platform.runLater(() -> {
                        if (!result.locationName().isBlank()) {
                            locationNameField.setText(result.locationName());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to reverse geocode", e);
                }
            }).start();
        });
    }
}
