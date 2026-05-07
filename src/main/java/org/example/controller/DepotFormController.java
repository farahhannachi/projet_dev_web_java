package org.example.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.example.model.Depot;
import org.example.service.DepotService;
import org.example.service.LocationGeocodingService;
import org.example.util.DepotValidator;
import org.example.util.NotificationUtil;
import org.example.util.ValidationException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    @FXML private TextField mapSearchField;
    @FXML private WebView mapWebView;
    @FXML private Label errorLabel;

    private final DepotService depotService = DepotService.getInstance();
    private final LocationGeocodingService geocodingService = new LocationGeocodingService();
    private Depot depotToEdit;
    private DepotController parentController;
    private WebEngine mapEngine;
    private boolean mapLoaded;
    private static final Logger LOGGER = Logger.getLogger(DepotFormController.class.getName());

    @FXML
    private void initialize() {
        loadEmbeddedMap();
    }

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
            latitudeField.setText(String.format(Locale.US, "%.6f", depot.getLatitude()));
            longitudeField.setText(String.format(Locale.US, "%.6f", depot.getLongitude()));
            locationNameField.setText(depot.getLocationName());
            mapSearchField.setText(depot.getLocationName());
            centerMap(depot.getLatitude(), depot.getLongitude(), depot.getLocationName());
        } else {
            formTitle.setText("Ajouter un dépôt");
        }
    }

    public void setParentController(DepotController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleSave() {
        String nom = nomField.getText();
        String adresse = adresseField.getText();
        String ville = villeField.getText();
        String capacite = capaciteField.getText();
        String responsable = responsableField.getText();
        String telephone = telephoneField.getText();
        String latitude = latitudeField.getText();
        String longitude = longitudeField.getText();
        String locationName = locationNameField.getText();

        DepotValidator validator = new DepotValidator();
        if (!validator.validate(nom, adresse, ville, capacite, responsable, telephone, latitude, longitude, locationName)) {
            errorLabel.setText(validator.getErrorMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            return;
        }

        errorLabel.setText("");

        try {
            if (depotToEdit == null) {
                Depot newDepot = new Depot();
                fillDepot(newDepot, nom, adresse, ville, capacite, responsable, telephone, latitude, longitude, locationName);
                newDepot.setDateCreation(LocalDateTime.now());
                depotService.add(newDepot);
                NotificationUtil.showSuccess("Dépôt ajouté avec succès!");
            } else {
                fillDepot(depotToEdit, nom, adresse, ville, capacite, responsable, telephone, latitude, longitude, locationName);
                depotService.update(depotToEdit);
                NotificationUtil.showSuccess("Dépôt modifié avec succès!");
            }

            handleCancel();
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
        } catch (Exception e) {
            errorLabel.setText("Erreur: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            LOGGER.log(Level.SEVERE, "Failed to save depot", e);
        }
    }

    private void fillDepot(Depot depot, String nom, String adresse, String ville, String capacite,
                           String responsable, String telephone, String latitude, String longitude, String locationName) {
        depot.setNom(nom.trim());
        depot.setAdresse(adresse.trim());
        depot.setVille(ville.trim());
        depot.setCapaciteDepot(Integer.parseInt(capacite.trim()));
        depot.setResponsableDepot(responsable.trim());
        depot.setResponsableTelephone(telephone.trim());
        depot.setLatitude(readDouble(latitude));
        depot.setLongitude(readDouble(longitude));
        depot.setLocationName(locationName == null ? "" : locationName.trim());
    }

    @FXML
    private void handleCancel() {
        if (parentController != null) {
            parentController.showTableView();
        }
    }

    @FXML
    private void handleOpenMap() {
        handleSearchMapLocation();
    }

    @FXML
    private void handleSearchMapLocation() {
        String query = mapSearchField.getText();
        if (query == null || query.isBlank()) {
            query = String.join(" ", safeText(adresseField.getText()), safeText(villeField.getText())).trim();
        }

        if (query.isBlank()) {
            errorLabel.setText("Saisissez une adresse ou cliquez directement sur la carte.");
            return;
        }

        String finalQuery = query;
        errorLabel.setText("Recherche de l'emplacement...");
        new Thread(() -> {
            try {
                var result = geocodingService.geocode(finalQuery);
                Platform.runLater(() -> {
                    updateLocationFromMap(result.latitude(), result.longitude(), result.locationName());
                    centerMap(result.latitude(), result.longitude(), result.locationName());
                    errorLabel.setText("");
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to geocode depot address", e);
                Platform.runLater(() -> errorLabel.setText("Adresse introuvable. Cliquez directement sur la carte."));
            }
        }, "depot-map-search").start();
    }

    public void updateLocationFromMap(double latitude, double longitude, String locationName) {
        Platform.runLater(() -> {
            latitudeField.setText(String.format(Locale.US, "%.6f", latitude));
            longitudeField.setText(String.format(Locale.US, "%.6f", longitude));
            locationNameField.setText(locationName);
            mapSearchField.setText(locationName);

            new Thread(() -> {
                try {
                    var result = geocodingService.reverseGeocode(latitude, longitude);
                    Platform.runLater(() -> {
                        if (!result.locationName().isBlank()) {
                            locationNameField.setText(result.locationName());
                            mapSearchField.setText(result.locationName());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Reverse geocode unavailable", e);
                }
            }, "depot-map-reverse").start();
        });
    }

    private void loadEmbeddedMap() {
        mapEngine = mapWebView.getEngine();
        mapEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) mapEngine.executeScript("window");
                window.setMember("javaBridge", new MapBridge());
                mapLoaded = true;
                centerMap(readDouble(latitudeField.getText()), readDouble(longitudeField.getText()), locationNameField.getText());
            }
        });

        var mapUrl = getClass().getResource("/html/depot-map.html");
        if (mapUrl == null) {
            errorLabel.setText("Carte introuvable.");
            return;
        }
        mapEngine.load(mapUrl.toExternalForm());
    }

    private void centerMap(double latitude, double longitude, String name) {
        if (!mapLoaded || mapEngine == null || !isValidCoordinate(latitude, longitude)) {
            return;
        }

        String safeName = escapeJavaScript(name == null || name.isBlank()
                ? String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
                : name);
        mapEngine.executeScript(String.format(Locale.US, "setMapCenter(%f, %f, '%s')", latitude, longitude, safeName));
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
                && (Double.compare(latitude, 0d) != 0 || Double.compare(longitude, 0d) != 0);
    }

    private double readDouble(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String escapeJavaScript(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public class MapBridge {
        public void mapReady() {
            Platform.runLater(() ->
                    centerMap(readDouble(latitudeField.getText()), readDouble(longitudeField.getText()), locationNameField.getText()));
        }

        public void locationSelected(double latitude, double longitude, String locationName) {
            updateLocationFromMap(latitude, longitude, locationName);
        }
    }
}
