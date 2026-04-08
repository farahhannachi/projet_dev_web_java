package org.example.controller.Frontoffice;

import org.example.entities.Ordonnance;
import org.example.entities.Traitement;
import org.example.services.OrdonnanceService;
import org.example.services.TraitementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class OrdonnanceFrontController implements Initializable {

    @FXML private VBox pendingTraitBox;
    @FXML private Region pendingSpacer;
    @FXML private Label lblPendingInfo;
    @FXML private Label lblFormTitle, lblPatientName, lblPatientInfo, lblBannerNumero;

    @FXML private TextField tfNumero, tfDateOrdonnance, tfDateExpiration;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfSignatureMedecin;
    @FXML private TextArea taNoteMedical;

    @FXML private Label lblErrNumero, lblErrDateOrd, lblErrDateExp, lblErrStatut;
    @FXML private Label lblErrMedecin, lblErrNote;

    private final OrdonnanceService service = new OrdonnanceService();
    private final TraitementService traitService = new TraitementService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblPatientName.setText(FrontofficeController.connectedUserName);
        lblPatientInfo.setText("ID Utilisateur: " + FrontofficeController.connectedUserId);

        cbStatut.setItems(FXCollections.observableArrayList(
            "active", "expirée", "annulée", "en cours", "complétée"
        ));

        addLiveValidation();

        // If pending traitement, show its info
        if (TraitementFrontController.pendingTraitement != null) {
            Traitement pt = TraitementFrontController.pendingTraitement;
            pendingTraitBox.setVisible(true);
            pendingTraitBox.setManaged(true);
            pendingSpacer.setPrefHeight(10);
            lblPendingInfo.setText("Dosage: " + pt.getDosage()
                + "  |  Fréquence: " + pt.getFrequence()
                + "  |  Durée: " + pt.getDureeJours() + " jours"
                + "  |  Status: " + pt.getStatus()
                + (pt.getNotes() != null && !pt.getNotes().isEmpty() ? "  |  Notes: " + pt.getNotes() : ""));
            lblFormTitle.setText("Créer l'Ordonnance pour votre Traitement");
        }
    }

    private void addLiveValidation() {
        tfNumero.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrNumero.setText("Obligatoire.");
            else if (n.length() < 3) lblErrNumero.setText("Min 3 car.");
            else if (n.length() > 100) lblErrNumero.setText("Max 100 car.");
            else lblErrNumero.setText("");
            highlightField(tfNumero, lblErrNumero.getText().isEmpty());
            lblBannerNumero.setText(n);
        });
        tfDateOrdonnance.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrDateOrd.setText("Obligatoire.");
            else if (!isValidDate(n)) lblErrDateOrd.setText("Format invalide.");
            else lblErrDateOrd.setText("");
            highlightField(tfDateOrdonnance, lblErrDateOrd.getText().isEmpty());
        });
        tfDateExpiration.textProperty().addListener((obs, o, n) -> {
            if (n.isEmpty()) lblErrDateExp.setText("Obligatoire.");
            else if (!isValidDate(n)) lblErrDateExp.setText("Format invalide.");
            else lblErrDateExp.setText("");
            highlightField(tfDateExpiration, lblErrDateExp.getText().isEmpty());
        });
        tfSignatureMedecin.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && n.length() < 2) lblErrMedecin.setText("Min 2 car.");
            else if (n.length() > 255) lblErrMedecin.setText("Max 255 car.");
            else lblErrMedecin.setText("");
            highlightField(tfSignatureMedecin, lblErrMedecin.getText().isEmpty());
        });
        taNoteMedical.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && n.length() < 5) lblErrNote.setText("Min 5 car.");
            else if (n.length() > 1000) lblErrNote.setText("Max 1000 car.");
            else lblErrNote.setText("");
        });
    }

    private boolean isValidDate(String d) {
        return d.matches("\\d{4}-\\d{2}-\\d{2}.*") || d.matches("\\d{2}/\\d{2}/\\d{4}");
    }

    private void highlightField(TextField tf, boolean valid) {
        if (valid) tf.setStyle("-fx-min-height: 40; -fx-border-color: #D1D5DB; -fx-border-radius: 6; -fx-background-radius: 6;");
        else tf.setStyle("-fx-min-height: 40; -fx-border-color: #DC2626; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 2;");
    }

    @FXML
    private void save() {
        if (!validate()) return;
        try {
            int userId = FrontofficeController.connectedUserId;
            String note = taNoteMedical.getText() != null ? taNoteMedical.getText().trim() : "";

            Ordonnance o = new Ordonnance(userId, tfNumero.getText().trim(),
                tfDateOrdonnance.getText().trim(), tfDateExpiration.getText().trim(),
                cbStatut.getValue(), note, false, tfSignatureMedecin.getText().trim());
            service.insert(o);

            // If pending traitement, link it to the new ordonnance
            if (TraitementFrontController.pendingTraitement != null) {
                List<Ordonnance> all = service.getAll();
                Ordonnance newest = all.get(all.size() - 1);

                Traitement pt = TraitementFrontController.pendingTraitement;
                pt.setIdOrdonnance(newest.getIdOrdonnance());
                traitService.insert(pt);
                TraitementFrontController.pendingTraitement = null;

                new Alert(Alert.AlertType.INFORMATION,
                    "Ordonnance et traitement créés avec succès.\nOrdonnance: " + newest.getNumeroOrdonnance(),
                    ButtonType.OK).showAndWait();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Ordonnance créée avec succès.", ButtonType.OK).showAndWait();
            }

            clearAll();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private boolean validate() {
        boolean valid = true;
        clearErrors();
        String num = tfNumero.getText().trim();
        if (num.isEmpty()) { lblErrNumero.setText("Obligatoire."); highlightField(tfNumero, false); valid = false; }
        else if (num.length() < 3) { lblErrNumero.setText("Min 3 car."); highlightField(tfNumero, false); valid = false; }
        String dOrd = tfDateOrdonnance.getText().trim();
        if (dOrd.isEmpty()) { lblErrDateOrd.setText("Obligatoire."); highlightField(tfDateOrdonnance, false); valid = false; }
        else if (!isValidDate(dOrd)) { lblErrDateOrd.setText("Format invalide."); highlightField(tfDateOrdonnance, false); valid = false; }
        String dExp = tfDateExpiration.getText().trim();
        if (dExp.isEmpty()) { lblErrDateExp.setText("Obligatoire."); highlightField(tfDateExpiration, false); valid = false; }
        else if (!isValidDate(dExp)) { lblErrDateExp.setText("Format invalide."); highlightField(tfDateExpiration, false); valid = false; }
        else if (!dOrd.isEmpty() && isValidDate(dOrd) && dExp.compareTo(dOrd) < 0) { lblErrDateExp.setText("Après ordonnance."); highlightField(tfDateExpiration, false); valid = false; }
        if (cbStatut.getValue() == null) { lblErrStatut.setText("Obligatoire."); valid = false; }
        String med = tfSignatureMedecin.getText().trim();
        if (!med.isEmpty() && med.length() < 2) { lblErrMedecin.setText("Min 2 car."); highlightField(tfSignatureMedecin, false); valid = false; }
        String note = taNoteMedical.getText() != null ? taNoteMedical.getText().trim() : "";
        if (!note.isEmpty() && note.length() < 5) { lblErrNote.setText("Min 5 car."); valid = false; }
        return valid;
    }

    @FXML
    private void clearAll() {
        tfNumero.clear(); tfDateOrdonnance.clear(); tfDateExpiration.clear();
        cbStatut.setValue(null); tfSignatureMedecin.clear(); taNoteMedical.clear();
        lblBannerNumero.setText("");
        clearErrors();
    }

    private void clearErrors() {
        lblErrNumero.setText(""); lblErrDateOrd.setText(""); lblErrDateExp.setText("");
        lblErrStatut.setText(""); lblErrMedecin.setText(""); lblErrNote.setText("");
        String n = "-fx-min-height: 40; -fx-border-color: #D1D5DB; -fx-border-radius: 6; -fx-background-radius: 6;";
        tfNumero.setStyle(n); tfDateOrdonnance.setStyle(n); tfDateExpiration.setStyle(n); tfSignatureMedecin.setStyle(n);
    }
}
