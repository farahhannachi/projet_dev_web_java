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
    @FXML private Button triButton;
    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;
    @FXML private StackPane ordonnanceMenuContainer;
    @FXML private VBox ordonnanceDropdown;
    @FXML private VBox profileDropdown;
    @FXML private javafx.scene.chart.PieChart statPieChart;
    @FXML private VBox statsContainer;
    @FXML private Button statsToggleBtn;

    private UserService userService = UserService.getInstance();
    private boolean triRecent = true;
    private String filtreStatut = null;

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
            Connection conn = DatabaseUtil.getInstance().getConnection();
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
            if (filtreStatut != null) {
                sql += "AND o.statut = ? ";
            }
            sql += "ORDER BY o.date_ordonnance " + (triRecent ? "DESC" : "ASC");

            PreparedStatement ps = conn.prepareStatement(sql);
            int paramIdx = 1;
            ps.setInt(paramIdx++, currentUser.getId());
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                ps.setString(paramIdx++, like);
                ps.setString(paramIdx++, like);
                ps.setString(paramIdx++, like);
                ps.setString(paramIdx++, like);
            }
            if (filtreStatut != null) {
                ps.setString(paramIdx++, filtreStatut);
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

        // Bouton Télécharger PDF (uniquement pour les ordonnances validées)
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        if ("validée".equals(statut) || "valid\u00e9e".equals(statut)) {
            Button pdfBtn = new Button("\uD83D\uDCC4 Télécharger PDF");
            pdfBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            pdfBtn.setOnAction(e -> exportOrdonnancePDF(numero, dateOrd, dateExp, statut, note));
            btnBox.getChildren().add(pdfBtn);
        }

        header.getChildren().addAll(numLabel, statutLabel, spacer, btnBox);

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
    private void toggleTri() {
        triRecent = !triRecent;
        triButton.setText(triRecent ? "\uD83D\uDCC5 Plus récent" : "\uD83D\uDCC5 Plus ancien");
        loadOrdonnances(searchField.getText().trim());
    }

    // Filtres par statut
    @FXML private void filterAll() { filtreStatut = null; loadOrdonnances(searchField.getText().trim()); }

    // Afficher/masquer le PieChart statistiques
    @FXML
    private void toggleStats() {
        boolean visible = statsContainer.isVisible();
        statsContainer.setVisible(!visible);
        statsContainer.setManaged(!visible);
        statsToggleBtn.setText(visible ? "\uD83D\uDCCA Statistiques" : "\uD83D\uDCCA Masquer");
        if (!visible) loadStats(); // Recharger les stats à l'ouverture
    }
    @FXML private void filterEnAttente() { filtreStatut = "en_attente"; loadOrdonnances(searchField.getText().trim()); }
    @FXML private void filterValidee() { filtreStatut = "validée"; loadOrdonnances(searchField.getText().trim()); }
    @FXML private void filterBrouillon() { filtreStatut = "brouillon"; loadOrdonnances(searchField.getText().trim()); }
    @FXML private void filterExpiree() { filtreStatut = "expirée"; loadOrdonnances(searchField.getText().trim()); }

    // Statistiques PieChart
    private void loadStats() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || statPieChart == null) return;
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            int attente = 0, validee = 0, brouillon = 0, expiree = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM ordonnance WHERE id_utilisateur_id = ? AND statut = 'en_attente'");
            ps.setInt(1, currentUser.getId()); ResultSet rs = ps.executeQuery(); if (rs.next()) attente = rs.getInt("c"); rs.close(); ps.close();
            ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM ordonnance WHERE id_utilisateur_id = ? AND statut = 'validée'");
            ps.setInt(1, currentUser.getId()); rs = ps.executeQuery(); if (rs.next()) validee = rs.getInt("c"); rs.close(); ps.close();
            ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM ordonnance WHERE id_utilisateur_id = ? AND statut = 'brouillon'");
            ps.setInt(1, currentUser.getId()); rs = ps.executeQuery(); if (rs.next()) brouillon = rs.getInt("c"); rs.close(); ps.close();
            ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM ordonnance WHERE id_utilisateur_id = ? AND statut = 'expirée'");
            ps.setInt(1, currentUser.getId()); rs = ps.executeQuery(); if (rs.next()) expiree = rs.getInt("c"); rs.close(); ps.close();
            statPieChart.setData(javafx.collections.FXCollections.observableArrayList(
                    new javafx.scene.chart.PieChart.Data("En attente (" + attente + ")", attente),
                    new javafx.scene.chart.PieChart.Data("Validées (" + validee + ")", validee),
                    new javafx.scene.chart.PieChart.Data("Brouillon (" + brouillon + ")", brouillon),
                    new javafx.scene.chart.PieChart.Data("Expirées (" + expiree + ")", expiree)
            ));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    // Export PDF (texte simple)
    @FXML
    private void exportPDF() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter mes ordonnances");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
        fc.setInitialFileName("mes_ordonnances.txt");
        java.io.File file = fc.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("=== MES ORDONNANCES - CuraVita ===");
            pw.println("Patient: " + (currentUser.getNom() != null ? currentUser.getNom() : "") + " - " + (currentUser.getEmail() != null ? currentUser.getEmail() : ""));
            pw.println("Date export: " + java.time.LocalDate.now());
            pw.println("=========================================\n");
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT o.numero_ordonnance, o.date_ordonnance, o.date_expiration, o.statut, o.note_medical, " +
                    "t.dosage, t.frequence, t.repas, t.duree_jours, t.status, p.nom AS produit_nom " +
                    "FROM ordonnance o LEFT JOIN traitement t ON t.id_ordonnance_id = o.id_ordonnance " +
                    "LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "WHERE o.id_utilisateur_id = ? ORDER BY o.date_ordonnance DESC");
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            String lastNum = "";
            while (rs.next()) {
                String num = rs.getString("numero_ordonnance");
                if (!num.equals(lastNum)) {
                    pw.println("Ordonnance: " + num);
                    pw.println("  Date: " + (rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate() : "N/A"));
                    pw.println("  Expiration: " + (rs.getTimestamp("date_expiration") != null ? rs.getTimestamp("date_expiration").toLocalDateTime().toLocalDate() : "N/A"));
                    pw.println("  Statut: " + rs.getString("statut"));
                    String note = rs.getString("note_medical");
                    if (note != null && !note.trim().isEmpty()) pw.println("  Note: " + note);
                    pw.println("  Traitements:");
                    lastNum = num;
                }
                if (rs.getString("produit_nom") != null) {
                    pw.println("    - " + rs.getString("produit_nom") + " | " +
                            (rs.getString("dosage") != null ? rs.getString("dosage") : "") + " | " +
                            (rs.getString("frequence") != null ? rs.getString("frequence") : "") + " | " +
                            rs.getInt("duree_jours") + "j | " +
                            (rs.getString("status") != null ? rs.getString("status") : ""));
                }
            }
            rs.close(); ps.close();
            pw.println("\n=========================================");
            pw.println("Généré par CuraVita");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export réussi");
            alert.setHeaderText(null);
            alert.setContentText("Vos ordonnances ont été exportées vers:\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de l'export: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleSearch() {
        // Focus on search field
        searchField.requestFocus();
    }

    // Export PDF d'une ordonnance individuelle validée
    private void exportOrdonnancePDF(String numero, Timestamp dateOrd, Timestamp dateExp, String statut, String note) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Télécharger l'ordonnance");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
        fc.setInitialFileName("ordonnance_" + (numero != null ? numero : "export") + ".txt");
        java.io.File file = fc.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("══════════════════════════════════════════");
            pw.println("        ORDONNANCE - CuraVita             ");
            pw.println("══════════════════════════════════════════");
            pw.println();
            pw.println("Numéro      : " + (numero != null ? numero : "N/A"));
            pw.println("Statut      : " + (statut != null ? statut : "N/A"));
            pw.println("Patient     : " + (currentUser.getNom() != null ? currentUser.getNom() : ""));
            pw.println("Email       : " + (currentUser.getEmail() != null ? currentUser.getEmail() : ""));
            pw.println("Date        : " + (dateOrd != null ? dateOrd.toLocalDateTime().toLocalDate() : "N/A"));
            pw.println("Expiration  : " + (dateExp != null ? dateExp.toLocalDateTime().toLocalDate() : "N/A"));
            if (note != null && !note.trim().isEmpty()) {
                pw.println("Note        : " + note);
            }
            pw.println();
            pw.println("--- Traitements associés ---");
            pw.println();
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.dosage, t.frequence, t.repas, t.duree_jours, t.status, p.nom AS produit_nom " +
                    "FROM traitement t LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "LEFT JOIN ordonnance o ON t.id_ordonnance_id = o.id_ordonnance " +
                    "WHERE o.numero_ordonnance = ? AND o.id_utilisateur_id = ?");
            ps.setString(1, numero);
            ps.setInt(2, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            int idx = 1;
            while (rs.next()) {
                pw.println("  " + idx + ". " + (rs.getString("produit_nom") != null ? rs.getString("produit_nom") : "N/A"));
                pw.println("     Dosage    : " + (rs.getString("dosage") != null ? rs.getString("dosage") : "-"));
                pw.println("     Fréquence : " + (rs.getString("frequence") != null ? rs.getString("frequence") : "-"));
                pw.println("     Repas     : " + (rs.getString("repas") != null ? rs.getString("repas") : "-"));
                pw.println("     Durée     : " + rs.getInt("duree_jours") + " jours");
                pw.println("     Statut    : " + (rs.getString("status") != null ? rs.getString("status") : "-"));
                pw.println();
                idx++;
            }
            rs.close(); ps.close();
            if (idx == 1) pw.println("  Aucun traitement associé.");
            pw.println();
            pw.println("──────────────────────────────────────────");
            pw.println("Généré le " + java.time.LocalDate.now() + " par CuraVita");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Téléchargement réussi");
            alert.setHeaderText(null);
            alert.setContentText("Ordonnance " + numero + " exportée vers:\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur: " + e.getMessage());
            alert.showAndWait();
        }
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
