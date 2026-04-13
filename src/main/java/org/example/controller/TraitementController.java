package org.example.controller;

import javafx.collections.FXCollections;
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

public class TraitementController {

    @FXML private TextField nomPrenomField;
    @FXML private TextField emailField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private TextArea antecedentsField;
    @FXML private TextArea symptomesField;
    @FXML private ComboBox<String> produitCombo;
    @FXML private CheckBox conditionsCheck;
    @FXML private Label errorLabel;
    @FXML private Button profileButton;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Pre-fill user info
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            nomPrenomField.setText(currentUser.getNom() != null ? currentUser.getNom() : "");
            emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }

        // Load produits from DB
        loadProduits();

        // Ordonnance hover dropdown
        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> { ordonnanceDropdown.setVisible(true); ordonnanceDropdown.setManaged(true); });
            ordonnanceMenuContainer.setOnMouseExited(e -> { ordonnanceDropdown.setVisible(false); ordonnanceDropdown.setManaged(false); });
        }
    }

    private void loadProduits() {
        try {
            Connection conn = DatabaseUtil.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id_produit, nom FROM produit ORDER BY nom");
            javafx.collections.ObservableList<String> items = FXCollections.observableArrayList();
            while (rs.next()) {
                items.add(rs.getInt("id_produit") + " - " + rs.getString("nom"));
            }
            produitCombo.setItems(items);
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Erreur chargement produits: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmit() {
        errorLabel.setText("");

        // Validation
        if (antecedentsField.getText() == null || antecedentsField.getText().trim().length() < 5) {
            errorLabel.setText("Les ant\u00e9c\u00e9dents m\u00e9dicaux doivent contenir au moins 5 caract\u00e8res.");
            return;
        }
        if (symptomesField.getText() == null || symptomesField.getText().trim().isEmpty()) {
            errorLabel.setText("Veuillez d\u00e9crire vos sympt\u00f4mes.");
            return;
        }
        if (produitCombo.getValue() == null) {
            errorLabel.setText("Veuillez s\u00e9lectionner un produit.");
            return;
        }
        if (!conditionsCheck.isSelected()) {
            errorLabel.setText("Vous devez accepter les conditions g\u00e9n\u00e9rales.");
            return;
        }

        // Extract IDs from combo selections
        int produitId = Integer.parseInt(produitCombo.getValue().split(" - ")[0]);

        // Insert into DB
        try {
            User currentUser = userService.getCurrentUser();
            Connection conn = DatabaseUtil.getConnection();

            // Create ordonnance placeholder
            PreparedStatement psOrd = conn.prepareStatement(
                    "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            String tempNumero = "ORD-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + (int)(Math.random() * 10);
            psOrd.setString(1, tempNumero);
            psOrd.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            psOrd.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusYears(1)));
            psOrd.setString(4, "brouillon");
            psOrd.setString(5, "");
            psOrd.setInt(6, currentUser != null ? currentUser.getId() : 0);
            psOrd.executeUpdate();
            ResultSet ordKeys = psOrd.getGeneratedKeys();
            int newOrdonnanceId = 0;
            if (ordKeys.next()) {
                newOrdonnanceId = ordKeys.getInt(1);
            }
            ordKeys.close();
            psOrd.close();

            // Insert traitement (dosage, frequence, repas, duree, notes left empty for admin)
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO traitement (id_utilisateur_id, dosage, frequence, duree_jours, date_debut, status, notes, id_ordonnance_id, id_produit_id, repas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, currentUser != null ? currentUser.getId() : 0);
            ps.setString(2, "");
            ps.setString(3, "");
            ps.setInt(4, 0);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, "en_attente");
            ps.setString(7, "");
            ps.setInt(8, newOrdonnanceId);
            ps.setInt(9, produitId);
            ps.setString(10, "");
            ps.executeUpdate();

            ResultSet traitKeys = ps.getGeneratedKeys();
            int newTraitementId = 0;
            if (traitKeys.next()) {
                newTraitementId = traitKeys.getInt(1);
            }
            traitKeys.close();
            ps.close();

            // Redirect to Ordonnance page with traitement info
            goToOrdonnance(newTraitementId, newOrdonnanceId, tempNumero);
        } catch (SQLException e) {
            errorLabel.setText("Erreur lors de l'enregistrement: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        try {
            goToAccueil();
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
        Stage stage = (Stage) nomPrenomField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToTraitement() {
        // Already on this page
    }

    @FXML
    private void goToMesOrdonnances() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesOrdonnances.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) nomPrenomField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToCreerOrdonnance() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) nomPrenomField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    private void goToOrdonnance(int traitementId, int ordonnanceId, String numeroOrdonnance) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
        Parent root = loader.load();
        OrdonnanceController controller = loader.getController();
        controller.setTraitementId(traitementId);
        controller.setOrdonnanceId(ordonnanceId, numeroOrdonnance);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) nomPrenomField.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
