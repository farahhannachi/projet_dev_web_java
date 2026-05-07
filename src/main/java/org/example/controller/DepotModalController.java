package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.example.model.Depot;

public class DepotModalController {
    @FXML private StackPane modalRoot;
    @FXML private Label modalTitle;
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField capaciteField;
    @FXML private TextField responsableField;
    @FXML private TextField telephoneField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;
    @FXML private Button closeBtn;

    private Depot depotToEdit;
    private Runnable onSave;

    public void setDepot(Depot depot) {
        this.depotToEdit = depot;
        if (depot != null) {
            modalTitle.setText("Modifier le dépôt");
            nomField.setText(depot.getNom());
            adresseField.setText(depot.getAdresse());
            villeField.setText(depot.getVille());
            capaciteField.setText(String.valueOf(depot.getCapaciteDepot()));
            responsableField.setText(depot.getResponsableDepot());
            telephoneField.setText(depot.getResponsableTelephone());
            latitudeField.setText(String.valueOf(depot.getLatitude()));
            longitudeField.setText(String.valueOf(depot.getLongitude()));
        } else {
            modalTitle.setText("Ajouter un dépôt");
        }
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    @FXML
    private void handleSave() {
        // Validation
        if (nomField.getText().trim().isEmpty() || adresseField.getText().trim().isEmpty() || villeField.getText().trim().isEmpty() || capaciteField.getText().trim().isEmpty() || responsableField.getText().trim().isEmpty() || telephoneField.getText().trim().isEmpty()) {
            errorLabel.setText("Tous les champs obligatoires doivent être remplis.");
            return;
        }
        if (!telephoneField.getText().matches("^0[1-9](\\d{8})$")) {
            errorLabel.setText("Téléphone invalide (format FR: 0X XX XX XX XX)");
            return;
        }
        try {
            Integer.parseInt(capaciteField.getText());
        } catch (NumberFormatException e) {
            errorLabel.setText("Capacité doit être un nombre entier.");
            return;
        }
        // Si tout est OK
        errorLabel.setText("");
        if (onSave != null) onSave.run();
        closeModal();
    }

    @FXML
    private void handleClose() {
        closeModal();
    }

    public void showModal() {
        modalRoot.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), modalRoot);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public void closeModal() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), modalRoot);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> modalRoot.setVisible(false));
        ft.play();
    }

    public String getNom() {
        return nomField.getText();
    }

    public String getAdresse() {
        return adresseField.getText();
    }

    public String getVille() {
        return villeField.getText();
    }

    public int getCapacite() {
        return Integer.parseInt(capaciteField.getText());
    }

    public String getResponsable() {
        return responsableField.getText();
    }

    public String getTelephone() {
        return telephoneField.getText();
    }

    public double getLatitude() {
        return Double.parseDouble(latitudeField.getText());
    }

    public double getLongitude() {
        return Double.parseDouble(longitudeField.getText());
    }
}
