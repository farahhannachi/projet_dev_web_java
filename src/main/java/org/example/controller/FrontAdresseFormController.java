package org.example.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.model.Address;
import org.example.model.User;
import org.example.service.AddressService;
import org.example.service.MapsApiService;
import org.example.service.UserService;
import org.example.util.SceneNavigation;

import java.util.List;

public class FrontAdresseFormController {
    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField geoSearchField;
    @FXML private ListView<String> geoSuggestionsList;
    @FXML private ImageView mapPreviewImage;
    @FXML private Label mapStatusLabel;
    @FXML private TextField line1Field;
    @FXML private TextField line2Field;
    @FXML private TextField cityField;
    @FXML private TextField regionField;
    @FXML private TextField postalCodeField;
    @FXML private TextField countryField;
    @FXML private FrontShopNavBarController shopNavController;

    private static Integer editingAddressId;

    private final AddressService addressService = new AddressService();
    private final UserService userService = new UserService();
    private final MapsApiService mapsApiService = new MapsApiService();

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(350));
    private List<MapsApiService.GeocodeResult> currentSuggestions = List.of();
    private Address editingAddress;

    public static void setEditingAddressId(Integer id) {
        editingAddressId = id;
    }

    @FXML
    public void initialize() {
        if (shopNavController != null) {
            shopNavController.configure(FrontShopNavBarController.ActiveShopPage.ADRESSES, geoSearchField);
        }

        initMode();
        initMapUi();
        setDefaultValues();
    }

    private void initMode() {
        User current = userService.getCurrentUser();
        if (current == null) {
            return;
        }

        if (editingAddressId == null) {
            formTitleLabel.setText("Ajouter adresse");
            formSubtitleLabel.setText("Ajoute une nouvelle adresse de livraison.");
            return;
        }

        Address found = addressService.getByIdForUser(editingAddressId, current.getId());
        if (found == null) {
            formTitleLabel.setText("Ajouter adresse");
            formSubtitleLabel.setText("Adresse introuvable, creation d'une nouvelle adresse.");
            editingAddressId = null;
            return;
        }

        editingAddress = found;
        formTitleLabel.setText("Modifier adresse");
        formSubtitleLabel.setText("Mets a jour tes informations de livraison.");
        fillForm(found);
    }

    private void setDefaultValues() {
        if (countryField != null && (countryField.getText() == null || countryField.getText().isBlank())) {
            countryField.setText("Tunisie");
        }
        updateMapPreview(36.8065, 10.1815);
    }

    private void initMapUi() {
        searchDebounce.setOnFinished(e -> searchAddress());

        if (geoSearchField != null) {
            geoSearchField.textProperty().addListener((obs, oldV, newV) -> {
                String q = newV == null ? "" : newV.trim();
                if (q.length() < 3) {
                    geoSuggestionsList.getItems().clear();
                    currentSuggestions = List.of();
                    return;
                }
                searchDebounce.playFromStart();
            });
        }

        if (geoSuggestionsList != null) {
            geoSuggestionsList.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
                int idx = newV == null ? -1 : newV.intValue();
                if (idx < 0 || idx >= currentSuggestions.size()) {
                    return;
                }
                MapsApiService.GeocodeResult selected = currentSuggestions.get(idx);
                applyMapSelection(selected.latitude(), selected.longitude(), selected.displayName());
            });
        }
    }

    @FXML
    private void handleUseMyLocation() {
        mapStatusLabel.setText("Recherche de votre localisation...");
        Thread thread = new Thread(() -> {
            MapsApiService.LocationResult location = mapsApiService.approximateCurrentLocation();
            Platform.runLater(() -> {
                if (!location.found()) {
                    mapStatusLabel.setText("Localisation indisponible: " + location.error());
                    return;
                }
                applyMapSelection(location.latitude(), location.longitude(), location.cityLabel());
                mapStatusLabel.setText("Position detectee: " + location.cityLabel());
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void searchAddress() {
        String query = geoSearchField.getText() == null ? "" : geoSearchField.getText().trim();
        if (query.length() < 3) {
            return;
        }

        currentSuggestions = mapsApiService.searchAddresses(query, 6);
        if (currentSuggestions.isEmpty()) {
            mapStatusLabel.setText("Aucun resultat pour cette adresse.");
            geoSuggestionsList.getItems().clear();
            return;
        }

        geoSuggestionsList.getItems().setAll(currentSuggestions.stream().map(MapsApiService.GeocodeResult::displayName).toList());
        geoSuggestionsList.getSelectionModel().select(0);
    }

    private void applyMapSelection(double lat, double lon, String fallbackDisplay) {
        updateMapPreview(lat, lon);

        MapsApiService.ReverseAddressResult reverse = mapsApiService.reverseGeocodeDetailed(lat, lon);
        if (reverse.found()) {
            line1Field.setText(reverse.line1());
            cityField.setText(reverse.city());
            regionField.setText(reverse.region());
            postalCodeField.setText(reverse.postalCode());
            countryField.setText(reverse.country());
            mapStatusLabel.setText("Adresse choisie: " + reverse.displayName());
            return;
        }

        String display = fallbackDisplay == null ? "" : fallbackDisplay;
        if (display.isBlank()) {
            display = String.format("Lat %.5f, Lng %.5f", lat, lon);
        }
        line1Field.setText(display);
        mapStatusLabel.setText("Adresse choisie: " + display);
    }

    private void updateMapPreview(double lat, double lon) {
        String url = mapsApiService.buildStaticMapUrl(lat, lon, 14, 640, 280);
        mapPreviewImage.setImage(new Image(url, true));
    }

    @FXML
    private void handleSaveAddress() {
        User current = userService.getCurrentUser();
        if (current == null) {
            new Alert(Alert.AlertType.ERROR, "Session invalide. Veuillez vous reconnecter.").showAndWait();
            return;
        }

        String validation = validateForm();
        if (validation != null) {
            new Alert(Alert.AlertType.WARNING, validation).showAndWait();
            return;
        }

        Address address = editingAddress == null ? new Address() : editingAddress;
        address.setUserId(current.getId());
        address.setFullName(fullNameField.getText().trim());
        address.setPhone(phoneField.getText() == null ? null : phoneField.getText().trim());
        address.setLine1(line1Field.getText().trim());
        address.setLine2(line2Field.getText() == null ? null : line2Field.getText().trim());
        address.setCity(cityField.getText().trim());
        address.setRegion(regionField.getText().trim());
        address.setPostalCode(postalCodeField.getText().trim());
        address.setCountry(countryField.getText().trim());

        try {
            if (editingAddress == null) {
                int newId = addressService.add(address);
                address.setId(newId);
            } else {
                addressService.update(address);
            }
            navigate("/fxml/FrontMesAdresses.fxml");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur enregistrement adresse: " + e.getMessage()).showAndWait();
        }
    }

    private String validateForm() {
        if (isBlank(fullNameField)) {
            return "Nom complet obligatoire.";
        }
        if (isBlank(line1Field)) {
            return "Adresse obligatoire.";
        }
        if (isBlank(cityField)) {
            return "Ville obligatoire.";
        }
        if (isBlank(regionField)) {
            return "Region obligatoire.";
        }
        if (isBlank(postalCodeField)) {
            return "Code postal obligatoire.";
        }
        if (isBlank(countryField)) {
            return "Pays obligatoire.";
        }
        return null;
    }

    private boolean isBlank(TextField field) {
        return field == null || field.getText() == null || field.getText().trim().isBlank();
    }

    private void fillForm(Address address) {
        fullNameField.setText(address.getFullName());
        phoneField.setText(address.getPhone());
        line1Field.setText(address.getLine1());
        line2Field.setText(address.getLine2());
        cityField.setText(address.getCity());
        regionField.setText(address.getRegion());
        postalCodeField.setText(address.getPostalCode());
        countryField.setText(address.getCountry());
    }

    @FXML
    private void goBackToAddresses() {
        navigate("/fxml/FrontMesAdresses.fxml");
    }

    private void navigate(String fxmlPath) {
        SceneNavigation.replaceScene(geoSearchField, fxmlPath);
    }
}
