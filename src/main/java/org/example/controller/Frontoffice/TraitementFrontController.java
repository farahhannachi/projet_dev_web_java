package org.example.controller.Frontoffice;

import org.example.entities.Traitement;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class TraitementFrontController implements Initializable {

    @FXML private Label lblPatientName, lblPatientInfo;

    @FXML private TextField tfDosage, tfFrequence, tfDureeJours;
    @FXML private TextField tfDateDebut, tfDateFin, tfIdProduit;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea taNotes;

    @FXML private Label lblErrDosage, lblErrFrequence, lblErrDuree;
    @FXML private Label lblErrDateDebut, lblErrDateFin, lblErrStatus, lblErrNotes, lblErrProduit;

    // Shared data: the traitement pending ordonnance creation
    public static Traitement pendingTraitement = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblPatientName.setText(FrontofficeController.connectedUserName);
        lblPatientInfo.setText("ID Utilisateur: " + FrontofficeController.connectedUserId);

        cbStatus.setItems(FXCollections.observableArrayList(
            "En Cours", "Complétée", "Annulée", "En Attente", "Suspendu"
        ));

        addLiveValidation();
    }

    private void addLiveValidation() {
        tfDosage.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrDosage.setText("Le dosage est obligatoire.");
            else if (n.length() < 2) lblErrDosage.setText("Minimum 2 caractères.");
            else if (n.length() > 255) lblErrDosage.setText("Maximum 255 caractères.");
            else lblErrDosage.setText("");
            highlightField(tfDosage, lblErrDosage.getText().isEmpty());
        });
        tfFrequence.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrFrequence.setText("La fréquence est obligatoire.");
            else if (n.length() < 2) lblErrFrequence.setText("Minimum 2 caractères.");
            else if (n.length() > 255) lblErrFrequence.setText("Maximum 255 caractères.");
            else lblErrFrequence.setText("");
            highlightField(tfFrequence, lblErrFrequence.getText().isEmpty());
        });
        tfDureeJours.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrDuree.setText("La durée est obligatoire.");
            else {
                try {
                    int val = Integer.parseInt(n);
                    if (val <= 0) lblErrDuree.setText("Doit être positif.");
                    else if (val > 365) lblErrDuree.setText("Maximum 365 jours.");
                    else lblErrDuree.setText("");
                } catch (NumberFormatException e) { lblErrDuree.setText("Nombre entier uniquement."); }
            }
            highlightField(tfDureeJours, lblErrDuree.getText().isEmpty());
        });
        tfDateDebut.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && !n.matches("\\d{4}-\\d{2}-\\d{2}.*")) lblErrDateDebut.setText("Format: YYYY-MM-DD.");
            else lblErrDateDebut.setText("");
            highlightField(tfDateDebut, lblErrDateDebut.getText().isEmpty());
        });
        tfDateFin.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && !n.matches("\\d{4}-\\d{2}-\\d{2}.*")) lblErrDateFin.setText("Format: YYYY-MM-DD.");
            else lblErrDateFin.setText("");
            highlightField(tfDateFin, lblErrDateFin.getText().isEmpty());
        });
        taNotes.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && n.length() < 5) lblErrNotes.setText("Minimum 5 caractères.");
            else if (n.length() > 1000) lblErrNotes.setText("Maximum 1000 caractères.");
            else lblErrNotes.setText("");
        });
        tfIdProduit.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty()) {
                try {
                    int val = Integer.parseInt(n);
                    if (val <= 0) lblErrProduit.setText("Doit être positif.");
                    else lblErrProduit.setText("");
                } catch (NumberFormatException e) { lblErrProduit.setText("Nombre entier."); }
            } else lblErrProduit.setText("");
            highlightField(tfIdProduit, lblErrProduit.getText().isEmpty());
        });
    }

    private void highlightField(TextField tf, boolean valid) {
        if (valid) tf.setStyle("-fx-min-height: 40; -fx-border-color: #D1D5DB; -fx-border-radius: 6; -fx-background-radius: 6;");
        else tf.setStyle("-fx-min-height: 40; -fx-border-color: #DC2626; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 2;");
    }

    @FXML
    private void save() {
        if (!validate()) return;

        int userId = FrontofficeController.connectedUserId;
        int duree = Integer.parseInt(tfDureeJours.getText().trim());
        int produit = tfIdProduit.getText().trim().isEmpty() ? 0 : Integer.parseInt(tfIdProduit.getText().trim());
        String notes = taNotes.getText() != null ? taNotes.getText().trim() : "";

        // Store traitement data temporarily — ordonnance ID will be set after ordonnance creation
        pendingTraitement = new Traitement(
            0, // id_ordonnance = 0 placeholder, will be set after ordonnance is created
            userId,
            tfDosage.getText().trim(),
            tfFrequence.getText().trim(),
            duree,
            tfDateDebut.getText().trim(),
            tfDateFin.getText().trim(),
            cbStatus.getValue(),
            notes,
            produit
        );

        // Redirect to ordonnance creation page
        FrontofficeController.navigateFromChild("/Frontoffice/FrontofficeOrdonnance.fxml");
    }

    private boolean validate() {
        boolean valid = true;
        clearErrors();

        String dosage = tfDosage.getText().trim();
        if (dosage.isEmpty()) { lblErrDosage.setText("Obligatoire."); highlightField(tfDosage, false); valid = false; }
        else if (dosage.length() < 2) { lblErrDosage.setText("Min 2 car."); highlightField(tfDosage, false); valid = false; }
        else if (dosage.length() > 255) { lblErrDosage.setText("Max 255 car."); highlightField(tfDosage, false); valid = false; }

        String freq = tfFrequence.getText().trim();
        if (freq.isEmpty()) { lblErrFrequence.setText("Obligatoire."); highlightField(tfFrequence, false); valid = false; }
        else if (freq.length() < 2) { lblErrFrequence.setText("Min 2 car."); highlightField(tfFrequence, false); valid = false; }

        String dureeStr = tfDureeJours.getText().trim();
        if (dureeStr.isEmpty()) { lblErrDuree.setText("Obligatoire."); highlightField(tfDureeJours, false); valid = false; }
        else {
            try {
                int d = Integer.parseInt(dureeStr);
                if (d <= 0) { lblErrDuree.setText("Positif."); highlightField(tfDureeJours, false); valid = false; }
                else if (d > 365) { lblErrDuree.setText("Max 365."); highlightField(tfDureeJours, false); valid = false; }
            } catch (NumberFormatException e) { lblErrDuree.setText("Nombre."); highlightField(tfDureeJours, false); valid = false; }
        }

        String dd = tfDateDebut.getText().trim();
        if (!dd.isEmpty() && !dd.matches("\\d{4}-\\d{2}-\\d{2}.*")) { lblErrDateDebut.setText("Format invalide."); highlightField(tfDateDebut, false); valid = false; }
        String df = tfDateFin.getText().trim();
        if (!df.isEmpty() && !df.matches("\\d{4}-\\d{2}-\\d{2}.*")) { lblErrDateFin.setText("Format invalide."); highlightField(tfDateFin, false); valid = false; }
        if (!dd.isEmpty() && !df.isEmpty() && df.compareTo(dd) < 0) { lblErrDateFin.setText("Après début."); highlightField(tfDateFin, false); valid = false; }

        if (cbStatus.getValue() == null) { lblErrStatus.setText("Obligatoire."); valid = false; }

        String notes = taNotes.getText() != null ? taNotes.getText().trim() : "";
        if (!notes.isEmpty() && notes.length() < 5) { lblErrNotes.setText("Min 5 car."); valid = false; }
        if (notes.length() > 1000) { lblErrNotes.setText("Max 1000 car."); valid = false; }

        String prodStr = tfIdProduit.getText().trim();
        if (!prodStr.isEmpty()) {
            try { if (Integer.parseInt(prodStr) <= 0) { lblErrProduit.setText("Positif."); highlightField(tfIdProduit, false); valid = false; } }
            catch (NumberFormatException e) { lblErrProduit.setText("Nombre."); highlightField(tfIdProduit, false); valid = false; }
        }

        return valid;
    }

    @FXML
    private void clearAll() {
        cbStatus.setValue(null);
        tfDosage.clear(); tfFrequence.clear(); tfDureeJours.clear();
        tfDateDebut.clear(); tfDateFin.clear(); taNotes.clear(); tfIdProduit.clear();
        clearErrors();
    }

    private void clearErrors() {
        lblErrDosage.setText(""); lblErrFrequence.setText(""); lblErrDuree.setText("");
        lblErrDateDebut.setText(""); lblErrDateFin.setText(""); lblErrStatus.setText("");
        lblErrNotes.setText(""); lblErrProduit.setText("");
        String n = "-fx-min-height: 40; -fx-border-color: #D1D5DB; -fx-border-radius: 6; -fx-background-radius: 6;";
        tfDosage.setStyle(n); tfFrequence.setStyle(n); tfDureeJours.setStyle(n);
        tfDateDebut.setStyle(n); tfDateFin.setStyle(n); tfIdProduit.setStyle(n);
    }
}
