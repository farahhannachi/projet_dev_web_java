package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.service.LocationGeocodingService;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapModalController {
    @FXML private Pane mapPane;
    @FXML private TextField searchField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label locationLabel;

    private Stage stage;
    private DepotFormController parentController;
    private double selectedLatitude = 36.8065;
    private double selectedLongitude = 10.1815;
    private String selectedLocationName = "Emplacement sélectionné";
    private Circle marker;

    private final LocationGeocodingService geocodingService = new LocationGeocodingService();
    private static final Logger LOGGER = Logger.getLogger(MapModalController.class.getName());

    @FXML
    private void initialize() {
        mapPane.widthProperty().addListener((obs, oldValue, newValue) -> redrawMap());
        mapPane.heightProperty().addListener((obs, oldValue, newValue) -> redrawMap());
        mapPane.setOnMouseClicked(event -> {
            Point2D latLng = pointToLatLng(event.getX(), event.getY());
            setSelectedLocation(latLng.getX(), latLng.getY(), "Position choisie sur la carte", true);
            reverseGeocodeSelection();
        });
    }

    public void initialize(DepotFormController parentController, double lat, double lng) {
        this.parentController = parentController;
        double initialLat = isValidCoordinate(lat, lng) ? lat : 36.8065;
        double initialLng = isValidCoordinate(lat, lng) ? lng : 10.1815;
        setSelectedLocation(initialLat, initialLng, "Emplacement actuel", true);
        Platform.runLater(this::redrawMap);
    }

    @FXML
    private void handleSearchLocation() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            locationLabel.setText("Entrez une adresse ou une ville à rechercher.");
            return;
        }

        locationLabel.setText("Recherche en cours...");
        new Thread(() -> {
            try {
                LocationGeocodingService.GeocodingResult result = geocodingService.geocode(query.trim());
                Platform.runLater(() -> {
                    setSelectedLocation(result.latitude(), result.longitude(), result.locationName(), true);
                    locationLabel.setText(result.displayName());
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Geocoding failed", e);
                Platform.runLater(() -> locationLabel.setText("Adresse introuvable. Cliquez directement sur la carte."));
            }
        }, "depot-location-search").start();
    }

    @FXML
    private void handleSaveLocation() {
        parentController.updateLocationFromMap(selectedLatitude, selectedLongitude, selectedLocationName);
        closeModal();
    }

    @FXML
    private void handleClose() {
        closeModal();
    }

    private void closeModal() {
        if (stage != null) {
            stage.close();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setSelectedLocation(double latitude, double longitude, String locationName, boolean updateMarker) {
        selectedLatitude = clamp(latitude, -85, 85);
        selectedLongitude = normalizeLongitude(longitude);
        selectedLocationName = locationName == null || locationName.isBlank()
                ? formatCoordinates(selectedLatitude, selectedLongitude)
                : locationName;

        latitudeField.setText(String.format(Locale.US, "%.6f", selectedLatitude));
        longitudeField.setText(String.format(Locale.US, "%.6f", selectedLongitude));
        locationLabel.setText(selectedLocationName + " (" + formatCoordinates(selectedLatitude, selectedLongitude) + ")");

        if (updateMarker) {
            updateMarkerPosition();
        }
    }

    private void reverseGeocodeSelection() {
        double lat = selectedLatitude;
        double lng = selectedLongitude;
        new Thread(() -> {
            try {
                LocationGeocodingService.GeocodingResult result = geocodingService.reverseGeocode(lat, lng);
                Platform.runLater(() -> setSelectedLocation(lat, lng, result.locationName(), false));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Reverse geocoding unavailable", e);
            }
        }, "depot-location-reverse").start();
    }

    private void redrawMap() {
        if (mapPane.getWidth() <= 0 || mapPane.getHeight() <= 0) {
            return;
        }

        mapPane.getChildren().clear();
        drawMapBackground();
        marker = new Circle(8, Color.web("#dc2626"));
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(3);
        mapPane.getChildren().add(marker);
        updateMarkerPosition();
    }

    private void drawMapBackground() {
        double width = mapPane.getWidth();
        double height = mapPane.getHeight();

        Rectangle world = new Rectangle(0, 0, width, height);
        world.setFill(Color.web("#e0f2fe"));
        mapPane.getChildren().add(world);

        drawLandMass(width * 0.07, height * 0.18, width * 0.23, height * 0.48, "#bbf7d0");
        drawLandMass(width * 0.28, height * 0.34, width * 0.18, height * 0.42, "#86efac");
        drawLandMass(width * 0.43, height * 0.16, width * 0.34, height * 0.48, "#bbf7d0");
        drawLandMass(width * 0.50, height * 0.58, width * 0.16, height * 0.20, "#fde68a");
        drawLandMass(width * 0.74, height * 0.58, width * 0.13, height * 0.18, "#86efac");

        for (int lon = -120; lon <= 120; lon += 60) {
            double x = longitudeToX(lon);
            Line line = new Line(x, 0, x, height);
            line.setStroke(Color.web("#bfdbfe"));
            mapPane.getChildren().add(line);
        }
        for (int lat = -60; lat <= 60; lat += 30) {
            double y = latitudeToY(lat);
            Line line = new Line(0, y, width, y);
            line.setStroke(Color.web("#bfdbfe"));
            mapPane.getChildren().add(line);
        }

        Text hint = new Text(18, 28, "Cliquez sur la carte pour placer le dépôt");
        hint.setFill(Color.web("#1e3a8a"));
        hint.setStyle("-fx-font-weight: bold;");
        mapPane.getChildren().add(hint);
    }

    private void drawLandMass(double x, double y, double width, double height, String color) {
        Rectangle land = new Rectangle(x, y, width, height);
        land.setArcWidth(80);
        land.setArcHeight(80);
        land.setFill(Color.web(color));
        land.setStroke(Color.web("#65a30d"));
        land.setStrokeWidth(1);
        mapPane.getChildren().add(land);
    }

    private void updateMarkerPosition() {
        if (marker == null || mapPane.getWidth() <= 0 || mapPane.getHeight() <= 0) {
            return;
        }
        marker.setCenterX(longitudeToX(selectedLongitude));
        marker.setCenterY(latitudeToY(selectedLatitude));
    }

    private Point2D pointToLatLng(double x, double y) {
        double longitude = (x / mapPane.getWidth()) * 360d - 180d;
        double latitude = 85d - (y / mapPane.getHeight()) * 170d;
        return new Point2D(latitude, longitude);
    }

    private double longitudeToX(double longitude) {
        return ((normalizeLongitude(longitude) + 180d) / 360d) * mapPane.getWidth();
    }

    private double latitudeToY(double latitude) {
        return ((85d - clamp(latitude, -85, 85)) / 170d) * mapPane.getHeight();
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
                && (Double.compare(latitude, 0d) != 0 || Double.compare(longitude, 0d) != 0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double normalizeLongitude(double longitude) {
        double normalized = longitude;
        while (normalized < -180) {
            normalized += 360;
        }
        while (normalized > 180) {
            normalized -= 360;
        }
        return normalized;
    }

    private String formatCoordinates(double latitude, double longitude) {
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
    }

    public static void showMapModal(DepotFormController parentController, double lat, double lng) {
        try {
            FXMLLoader loader = new FXMLLoader(MapModalController.class.getResource("/fxml/MapModal.fxml"));
            BorderPane root = loader.load();

            MapModalController controller = loader.getController();
            controller.initialize(parentController, lat, lng);

            Stage mapStage = new Stage();
            mapStage.setTitle("Sélectionner l'emplacement");
            mapStage.setMinWidth(850);
            mapStage.setMinHeight(600);
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setScene(new Scene(root));

            controller.setStage(mapStage);
            mapStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de la carte", e);
        }
    }
}
