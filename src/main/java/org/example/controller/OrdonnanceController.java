package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.UserService;
import org.example.util.DatabaseUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrdonnanceController {

    @FXML private Label numeroBannerLabel;
    @FXML private TextField numeroField;
    @FXML private DatePicker dateOrdonnanceField;
    @FXML private DatePicker dateExpirationField;
    @FXML private TextArea noteMedicalField;
    @FXML private Label errorLabel;
    @FXML private Button profileButton;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;

    // Traitement info labels
    @FXML private VBox traitementInfoBox;
    @FXML private Label traitProduitLabel;
    @FXML private Label traitStatusLabel;

    private UserService userService = new UserService();
    private int traitementId = -1;
    private int ordonnanceId = -1;
    private String numeroOrdonnance;

    public void setTraitementId(int traitementId) {
        this.traitementId = traitementId;
        loadTraitementInfo();
    }

    public void setOrdonnanceId(int ordonnanceId, String numero) {
        this.ordonnanceId = ordonnanceId;
        this.numeroOrdonnance = numero;
        numeroBannerLabel.setText(numero);
        numeroField.setText(numero);
    }

    @FXML
    public void initialize() {
        // Generate ordonnance number
        numeroOrdonnance = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (int)(Math.random() * 10);
        numeroBannerLabel.setText(numeroOrdonnance);
        numeroField.setText(numeroOrdonnance);

        // Set dates
        dateOrdonnanceField.setValue(LocalDate.now());
        dateExpirationField.setValue(LocalDate.now().plusYears(1));

        // Ordonnance hover dropdown
        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> { ordonnanceDropdown.setVisible(true); ordonnanceDropdown.setManaged(true); });
            ordonnanceMenuContainer.setOnMouseExited(e -> { ordonnanceDropdown.setVisible(false); ordonnanceDropdown.setManaged(false); });
        }
    }

    private void loadTraitementInfo() {
        if (traitementId <= 0) return;
        try {
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.dosage, t.frequence, t.duree_jours, t.status, t.repas, t.notes, " +
                    "p.nom AS produit_nom " +
                    "FROM traitement t " +
                    "LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "WHERE t.id_traitement = ?");
            ps.setInt(1, traitementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                traitProduitLabel.setText("Produit : " + (rs.getString("produit_nom") != null ? rs.getString("produit_nom") : "N/A"));
                traitStatusLabel.setText("Statut : " + rs.getString("status"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Erreur chargement traitement: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmitOrdonnance() {
        errorLabel.setText("");

        if (dateOrdonnanceField.getValue() == null) {
            errorLabel.setText("Veuillez indiquer la date de l'ordonnance.");
            return;
        }
        if (dateExpirationField.getValue() == null) {
            errorLabel.setText("Veuillez indiquer la date d'expiration.");
            return;
        }

        try {
            Connection conn = DatabaseUtil.getConnection();

            if (ordonnanceId > 0) {
                // Update existing ordonnance (created from traitement page)
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE ordonnance SET date_ordonnance = ?, date_expiration = ?, statut = ?, note_medical = ? WHERE id_ordonnance = ?");
                ps.setTimestamp(1, Timestamp.valueOf(dateOrdonnanceField.getValue().atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(dateExpirationField.getValue().atStartOfDay()));
                ps.setString(3, "en_attente");
                ps.setString(4, noteMedicalField.getText() != null ? noteMedicalField.getText().trim() : "");
                ps.setInt(5, ordonnanceId);
                ps.executeUpdate();
                ps.close();
            } else {
                // Create new ordonnance
                User currentUser = userService.getCurrentUser();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, numeroOrdonnance);
                ps.setTimestamp(2, Timestamp.valueOf(dateOrdonnanceField.getValue().atStartOfDay()));
                ps.setTimestamp(3, Timestamp.valueOf(dateExpirationField.getValue().atStartOfDay()));
                ps.setString(4, "en_attente");
                ps.setString(5, noteMedicalField.getText() != null ? noteMedicalField.getText().trim() : "");
                ps.setInt(6, currentUser != null ? currentUser.getId() : 0);
                ps.executeUpdate();
                ps.close();
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succ\u00e8s");
            alert.setHeaderText(null);
            alert.setContentText("Votre ordonnance " + numeroOrdonnance + " a \u00e9t\u00e9 envoy\u00e9e avec succ\u00e8s.");
            alert.showAndWait();

            goToMesOrdonnances();
        } catch (SQLException e) {
            errorLabel.setText("Erreur: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAccueil() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) numeroField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToTraitement() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Traitement.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) numeroField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToMesOrdonnances() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesOrdonnances.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) numeroField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToCreerOrdonnance() {
        // Already on this page
    }
}
