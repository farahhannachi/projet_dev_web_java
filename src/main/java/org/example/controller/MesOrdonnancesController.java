package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.UserService;
import org.example.util.DatabaseUtil;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MesOrdonnancesController {

    @FXML private Button profileButton;
    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;
    @FXML private VBox profileDropdown;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadOrdonnances("");

        // Search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadOrdonnances(newVal.trim()));

        // Ordonnance hover dropdown
        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> { ordonnanceDropdown.setVisible(true); ordonnanceDropdown.setManaged(true); });
            ordonnanceMenuContainer.setOnMouseExited(e -> { ordonnanceDropdown.setVisible(false); ordonnanceDropdown.setManaged(false); });
        }
    }

    private void loadOrdonnances(String search) {
        cardsContainer.getChildren().clear();
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            cardsContainer.getChildren().add(new Label("Veuillez vous connecter pour voir vos ordonnances."));
            return;
        }

        try {
            Connection conn = DatabaseUtil.getConnection();
            String sql = "SELECT o.id_ordonnance, o.numero_ordonnance, o.date_ordonnance, o.date_expiration, o.statut AS ord_statut, o.note_medical, " +
                    "t.id_traitement, t.dosage, t.frequence, t.repas, t.duree_jours, t.status AS trait_statut, t.date_debut, t.notes, " +
                    "p.nom AS produit_nom " +
                    "FROM ordonnance o " +
                    "LEFT JOIN traitement t ON t.id_ordonnance_id = o.id_ordonnance " +
                    "LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "WHERE o.id_utilisateur_id = ? ";

            if (!search.isEmpty()) {
                sql += "AND (o.numero_ordonnance LIKE ? OR p.nom LIKE ? OR t.dosage LIKE ? OR o.statut LIKE ?) ";
            }
            sql += "ORDER BY o.date_ordonnance DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }

            ResultSet rs = ps.executeQuery();
            int lastOrdId = -1;
            VBox currentCard = null;

            while (rs.next()) {
                int ordId = rs.getInt("id_ordonnance");

                if (ordId != lastOrdId) {
                    // New ordonnance card
                    currentCard = createOrdonnanceCard(
                            rs.getString("numero_ordonnance"),
                            rs.getTimestamp("date_ordonnance"),
                            rs.getTimestamp("date_expiration"),
                            rs.getString("ord_statut"),
                            rs.getString("note_medical")
                    );
                    cardsContainer.getChildren().add(currentCard);
                    lastOrdId = ordId;
                }

                // Add traitement row if exists
                if (rs.getInt("id_traitement") > 0 && currentCard != null) {
                    HBox traitRow = createTraitementRow(
                            rs.getString("produit_nom"),
                            rs.getString("dosage"),
                            rs.getString("frequence"),
                            rs.getString("repas"),
                            rs.getInt("duree_jours"),
                            rs.getString("trait_statut")
                    );
                    currentCard.getChildren().add(traitRow);
                }
            }
            rs.close();
            ps.close();

            if (cardsContainer.getChildren().isEmpty()) {
                Label empty = new Label("Aucune ordonnance trouv\u00e9e.");
                empty.setStyle("-fx-font-size: 14; -fx-text-fill: #888;");
                cardsContainer.getChildren().add(empty);
            }

        } catch (SQLException e) {
            Label err = new Label("Erreur: " + e.getMessage());
            err.setStyle("-fx-text-fill: #E74C3C;");
            cardsContainer.getChildren().add(err);
        }
    }

    private VBox createOrdonnanceCard(String numero, Timestamp dateOrd, Timestamp dateExp, String statut, String note) {
        VBox card = new VBox(10);
        card.getStyleClass().add("mesord-card");
        card.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label numLabel = new Label(numero != null ? numero : "N/A");
        numLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");

        Label statutLabel = new Label(statut != null ? statut : "");
        statutLabel.getStyleClass().add("mesord-statut-badge");
        if ("en_attente".equals(statut)) {
            statutLabel.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11; -fx-font-weight: bold;");
        } else if ("brouillon".equals(statut)) {
            statutLabel.setStyle("-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11; -fx-font-weight: bold;");
        } else {
            statutLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11; -fx-font-weight: bold;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(numLabel, spacer, statutLabel);

        // Dates
        HBox dates = new HBox(30);
        String dateStr = dateOrd != null ? dateOrd.toLocalDateTime().toLocalDate().toString() : "N/A";
        String expStr = dateExp != null ? dateExp.toLocalDateTime().toLocalDate().toString() : "N/A";
        Label dateLabel = new Label("Date: " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        Label expLabel = new Label("Expiration: " + expStr);
        expLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        dates.getChildren().addAll(dateLabel, expLabel);

        card.getChildren().addAll(header, dates);

        // Note
        if (note != null && !note.trim().isEmpty()) {
            Label noteLabel = new Label("Note: " + note);
            noteLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888; -fx-wrap-text: true;");
            card.getChildren().add(noteLabel);
        }

        // Separator + traitement title
        Separator sep = new Separator();
        Label traitTitle = new Label("Traitements associ\u00e9s :");
        traitTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        card.getChildren().addAll(sep, traitTitle);

        return card;
    }

    private HBox createTraitementRow(String produit, String dosage, String frequence, String repas, int duree, String statut) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 15, 8, 15));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");

        Label prodLabel = new Label(produit != null ? produit : "N/A");
        prodLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f6f5c; -fx-min-width: 120;");

        Label dosLabel = new Label(dosage != null ? dosage : "");
        dosLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

        Label freqLabel = new Label(frequence != null ? frequence : "");
        freqLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

        Label repasLbl = new Label(repas != null ? repas : "");
        repasLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

        Label dureeLbl = new Label(duree + "j");
        dureeLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

        Label statutLbl = new Label(statut != null ? statut : "");
        if ("actif".equals(statut)) {
            statutLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else if ("en_attente".equals(statut)) {
            statutLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");
        } else {
            statutLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #555;");
        }

        row.getChildren().addAll(prodLabel, dosLabel, freqLabel, repasLbl, dureeLbl, statutLbl);
        return row;
    }

    @FXML
    private void handleSearch() {
        // Focus on search field
        searchField.requestFocus();
    }

    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown != null) {
            boolean vis = profileDropdown.isVisible();
            profileDropdown.setVisible(!vis);
            profileDropdown.setManaged(!vis);
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        userService.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void goToAccueil() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToTraitement() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Traitement.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToMesOrdonnances() {
        // Already on this page
    }

    @FXML
    private void goToCreerOrdonnance() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cardsContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
