package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.UserService;
import org.example.util.DatabaseUtil;
import org.example.util.QRCodeService;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MesOrdonnancesController {

    @FXML private Button profileButton;
    @FXML private javafx.scene.layout.HBox profileContainer;
    @FXML private javafx.scene.shape.Circle navbarAvatarCircle;
    @FXML private javafx.scene.control.Label navbarUsername;
    @FXML private javafx.scene.control.Label navbarAvatarLabel;
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

        // Charger le nom dans la navbar avatar
        User currentUser = userService.getCurrentUser();
        if (navbarUsername != null && currentUser != null) {
            String nom = currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }

        // Vérification automatique des ordonnances qui expirent dans <= 7 jours
        javafx.application.Platform.runLater(this::verifierExpirationsProches);
    }

    /**
     * Vérifie si le patient a des ordonnances qui expirent dans les 7 prochains jours
     * et affiche une alerte de notification.
     */
    private void verifierExpirationsProches() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;

        List<String[]> expirantes = new ArrayList<>(); // [numero, date_expiration, jours_restants]

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT numero_ordonnance, date_expiration, " +
                "DATEDIFF(date_expiration, CURDATE()) AS jours_restants " +
                "FROM ordonnance " +
                "WHERE id_utilisateur_id = ? " +
                "AND statut NOT IN ('expirée', 'brouillon') " +
                "AND date_expiration IS NOT NULL " +
                "AND DATEDIFF(date_expiration, CURDATE()) BETWEEN 0 AND 7 " +
                "ORDER BY date_expiration ASC"
            );
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                expirantes.add(new String[]{
                    rs.getString("numero_ordonnance"),
                    rs.getTimestamp("date_expiration").toLocalDateTime().toLocalDate().toString(),
                    String.valueOf(rs.getInt("jours_restants"))
                });
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println("[Expiration] Erreur SQL : " + e.getMessage());
            return;
        }

        if (expirantes.isEmpty()) return;

        // Construire et afficher la fenêtre d'alerte
        showExpirationAlert(expirantes);
    }

    private void showExpirationAlert(List<String[]> expirantes) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("⚠ Alerte d'expiration");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(480);
        root.setMaxWidth(480);

        // En-tête orange
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 20, 18, 20));
        header.setStyle("-fx-background-color: #e67e22;");
        Label bellIcon = new Label("🔔");
        bellIcon.setStyle("-fx-font-size: 42;");
        Label titleLbl = new Label("Alerte d'expiration");
        titleLbl.setStyle("-fx-font-size: 19; -fx-font-weight: bold; -fx-text-fill: white;");
        String nb = expirantes.size() == 1
            ? "1 ordonnance expire bientôt"
            : expirantes.size() + " ordonnances expirent bientôt";
        Label subLbl = new Label(nb);
        subLbl.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.9);");
        header.getChildren().addAll(bellIcon, titleLbl, subLbl);

        // Corps — une carte par ordonnance avec bouton Renouveler intégré
        VBox body = new VBox(12);
        body.setPadding(new Insets(18, 22, 10, 22));

        for (String[] ord : expirantes) {
            String numero = ord[0];
            String dateExp = ord[1];
            int jours = Integer.parseInt(ord[2]);

            String bgColor, borderColor, joursText;
            if (jours == 0) {
                bgColor = "#fdecea"; borderColor = "#e74c3c";
                joursText = "⛔ Expire AUJOURD'HUI !";
            } else if (jours <= 2) {
                bgColor = "#fdecea"; borderColor = "#e74c3c";
                joursText = "⛔ Expire dans " + jours + " jour" + (jours > 1 ? "s" : "") + " !";
            } else if (jours <= 4) {
                bgColor = "#fff3cd"; borderColor = "#f39c12";
                joursText = "⚠️ Expire dans " + jours + " jours";
            } else {
                bgColor = "#fff8e1"; borderColor = "#f0ad4e";
                joursText = "⏳ Expire dans " + jours + " jours";
            }

            VBox card = new VBox(8);
            card.setPadding(new Insets(12, 16, 12, 16));
            card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; " +
                    "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; " +
                    "-fx-border-radius: 0 10 10 0;");

            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);
            Label numLbl = new Label("📄 " + numero);
            numLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            topRow.getChildren().add(numLbl);

            Label joursLbl = new Label(joursText);
            joursLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " +
                    (jours <= 2 ? "#e74c3c" : "#e67e22") + ";");
            Label dateLbl = new Label("Date d'expiration : " + dateExp);
            dateLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

            // Bouton Renouveler intégré dans la carte
            Button renewCardBtn = new Button("🔄 Renouveler cette ordonnance");
            renewCardBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-font-size: 12; -fx-background-radius: 8; " +
                    "-fx-padding: 7 16; -fx-cursor: hand;");
            renewCardBtn.setOnAction(e -> {
                dialog.close();
                demanderRenouvellement(numero);
            });

            card.getChildren().addAll(topRow, joursLbl, dateLbl, renewCardBtn);
            body.getChildren().add(card);
        }

        // Conseil
        VBox conseilBox = new VBox(4);
        conseilBox.setPadding(new Insets(12, 16, 12, 16));
        conseilBox.setStyle("-fx-background-color: #eaf4fb; -fx-background-radius: 8; " +
                "-fx-border-color: #2980b9; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
        Label conseilIcon = new Label("💡 Pourquoi renouveler ?");
        conseilIcon.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
        Label conseilText = new Label(
            "Un traitement interrompu peut nuire à votre santé. " +
            "Le renouvellement copie vos traitements actuels et les soumet au pharmacien.");
        conseilText.setStyle("-fx-font-size: 11; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        conseilText.setMaxWidth(410);
        conseilText.setWrapText(true);
        conseilBox.getChildren().addAll(conseilIcon, conseilText);
        body.getChildren().add(conseilBox);

        // Footer — deux boutons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 22, 20, 22));

        Button plusTardBtn = new Button("Plus tard");
        plusTardBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand;");
        plusTardBtn.setOnAction(e -> dialog.close());

        // Si une seule ordonnance → bouton global Renouveler en bas aussi
        if (expirantes.size() == 1) {
            Button renewAllBtn = new Button("🔄 Renouveler maintenant");
            renewAllBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 20; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13;");
            renewAllBtn.setOnAction(e -> {
                dialog.close();
                demanderRenouvellement(expirantes.get(0)[0]);
            });
            footer.getChildren().addAll(plusTardBtn, renewAllBtn);
        } else {
            Label noteLabel = new Label("Cliquez sur \"Renouveler\" dans chaque carte ci-dessus.");
            noteLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");
            footer.getChildren().addAll(plusTardBtn, noteLabel);
        }

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setMaxHeight(340);
        scrollBody.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, scrollBody, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.show();
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

            Button qrBtn = new Button("📷 QR Code");
            qrBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            qrBtn.setOnAction(e -> showQRCodeDialog(numero));

            // Bouton renouvellement UNIQUEMENT si expire dans <= 7 jours
            long joursRestants = dateExp != null
                ? java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    dateExp.toLocalDateTime().toLocalDate())
                : Long.MAX_VALUE;
            if (joursRestants <= 7) {
                Button renewBtn = new Button("🔄 Renouveler");
                renewBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
                renewBtn.setOnAction(e -> demanderRenouvellement(numero));
                btnBox.getChildren().add(renewBtn);
            }

            btnBox.getChildren().addAll(pdfBtn, qrBtn);
        }

        // Bouton renouvellement pour ordonnances expirées
        if ("expirée".equals(statut) || "expir\u00e9e".equals(statut)) {
            Button renewBtn = new Button("🔄 Renouveler");
            renewBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            renewBtn.setOnAction(e -> demanderRenouvellement(numero));
            btnBox.getChildren().add(renewBtn);
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

    @FXML
    private void handleSearch() {
        // Focus on search field
        searchField.requestFocus();
    }

    // Export PDF d'une ordonnance individuelle validée (iText7)
    private void exportOrdonnancePDF(String numero, Timestamp dateOrd, Timestamp dateExp, String statut, String note) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Télécharger l'ordonnance PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("ordonnance_" + (numero != null ? numero : "export") + ".pdf");
        java.io.File file = fc.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file == null) return;

        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(file);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);

            // Couleurs
            com.itextpdf.kernel.colors.Color green = com.itextpdf.kernel.colors.ColorConstants.GREEN;
            com.itextpdf.kernel.colors.Color darkGreen = new com.itextpdf.kernel.colors.DeviceRgb(31, 111, 92);
            com.itextpdf.kernel.colors.Color lightGray = new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245);
            com.itextpdf.kernel.colors.Color red = com.itextpdf.kernel.colors.ColorConstants.RED;

            // Titre
            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("ORDONNANCE MÉDICALE")
                .setFontSize(22)
                .setBold()
                .setFontColor(darkGreen)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(4);
            doc.add(title);

            com.itextpdf.layout.element.Paragraph subtitle = new com.itextpdf.layout.element.Paragraph("CuraVita — Système de Gestion Pharmaceutique")
                .setFontSize(11)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(15);
            doc.add(subtitle);

            // Ligne séparatrice
            com.itextpdf.layout.element.LineSeparator line = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f));
            doc.add(line);

            // Infos ordonnance
            com.itextpdf.layout.element.Table infoTable = new com.itextpdf.layout.element.Table(2).useAllAvailableWidth().setMarginTop(15).setMarginBottom(15);
            infoTable.setBackgroundColor(lightGray);

            addTableRow(infoTable, "Numéro", numero != null ? numero : "N/A", darkGreen);
            addTableRow(infoTable, "Statut", statut != null ? statut.toUpperCase() : "N/A", darkGreen);
            addTableRow(infoTable, "Patient", currentUser.getNom() != null ? currentUser.getNom() : "", darkGreen);
            addTableRow(infoTable, "Email", currentUser.getEmail() != null ? currentUser.getEmail() : "", darkGreen);
            addTableRow(infoTable, "Date ordonnance", dateOrd != null ? dateOrd.toLocalDateTime().toLocalDate().toString() : "N/A", darkGreen);
            addTableRow(infoTable, "Date expiration", dateExp != null ? dateExp.toLocalDateTime().toLocalDate().toString() : "N/A", darkGreen);
            if (note != null && !note.trim().isEmpty()) {
                addTableRow(infoTable, "Note médicale", note, darkGreen);
            }
            doc.add(infoTable);

            // Section traitements
            doc.add(new com.itextpdf.layout.element.Paragraph("Traitements Prescrits")
                .setFontSize(14).setBold().setFontColor(darkGreen).setMarginBottom(8));

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
                com.itextpdf.layout.element.Table traitTable = new com.itextpdf.layout.element.Table(2)
                    .useAllAvailableWidth().setMarginBottom(10);

                // En-tête produit
                com.itextpdf.layout.element.Cell headerCell = new com.itextpdf.layout.element.Cell(1, 2)
                    .add(new com.itextpdf.layout.element.Paragraph(idx + ". " + (rs.getString("produit_nom") != null ? rs.getString("produit_nom") : "N/A"))
                        .setBold().setFontSize(13).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE))
                    .setBackgroundColor(darkGreen).setPadding(8);
                traitTable.addCell(headerCell);

                addTraitRow(traitTable, "Dosage", rs.getString("dosage"));
                addTraitRow(traitTable, "Fréquence", rs.getString("frequence"));
                addTraitRow(traitTable, "Repas", rs.getString("repas"));
                addTraitRow(traitTable, "Durée", rs.getInt("duree_jours") + " jours");
                addTraitRow(traitTable, "Statut", rs.getString("status"));

                doc.add(traitTable);
                idx++;
            }
            rs.close(); ps.close();

            if (idx == 1) {
                doc.add(new com.itextpdf.layout.element.Paragraph("Aucun traitement associé.")
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY).setItalic());
            }

            // Pied de page
            doc.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)).setMarginTop(20));
            doc.add(new com.itextpdf.layout.element.Paragraph("Document généré le " + java.time.LocalDate.now() + " par CuraVita")
                .setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setMarginTop(8));

            doc.close();

            org.example.util.DialogService.showSuccess("PDF généré",
                "Ordonnance " + numero + " exportée en PDF :\n" + file.getAbsolutePath());

        } catch (Exception e) {
            org.example.util.DialogService.showError("Erreur PDF",
                "Erreur génération PDF : " + e.getMessage());
        }
    }

    private void addTableRow(com.itextpdf.layout.element.Table table, String label, String value,
                              com.itextpdf.kernel.colors.Color labelColor) {
        table.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(label).setBold().setFontColor(labelColor).setFontSize(11))
            .setPadding(6).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(value != null ? value : "-").setFontSize(11))
            .setPadding(6).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private void addTraitRow(com.itextpdf.layout.element.Table table, String label, String value) {
        com.itextpdf.kernel.colors.Color bg = new com.itextpdf.kernel.colors.DeviceRgb(248, 249, 250);
        table.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(label).setBold().setFontSize(11))
            .setBackgroundColor(bg).setPadding(6));
        table.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(value != null && !value.isEmpty() ? value : "-").setFontSize(11))
            .setPadding(6));
    }


    /**
     * Demande de renouvellement d'ordonnance.
     * Copie l'ordonnance + ses traitements et crée une nouvelle demande en brouillon.
     */
    private void demanderRenouvellement(String numeroSource) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;

        // Charger les données de l'ordonnance source
        String noteSource = "";
        List<QRCodeService.TraitementInfo> traitements = new ArrayList<>();

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Récupérer la note médicale
            PreparedStatement psOrd = conn.prepareStatement(
                "SELECT note_medical FROM ordonnance WHERE numero_ordonnance = ? AND id_utilisateur_id = ?");
            psOrd.setString(1, numeroSource);
            psOrd.setInt(2, currentUser.getId());
            ResultSet rsOrd = psOrd.executeQuery();
            if (rsOrd.next()) noteSource = rsOrd.getString("note_medical") != null ? rsOrd.getString("note_medical") : "";
            rsOrd.close(); psOrd.close();

            // Récupérer les traitements associés
            PreparedStatement psTrait = conn.prepareStatement(
                "SELECT p.nom, t.dosage, t.frequence, t.repas, t.duree_jours, t.id_produit_id " +
                "FROM traitement t JOIN produit p ON t.id_produit_id = p.id_produit " +
                "JOIN ordonnance o ON t.id_ordonnance_id = o.id_ordonnance " +
                "WHERE o.numero_ordonnance = ? AND o.id_utilisateur_id = ?");
            psTrait.setString(1, numeroSource);
            psTrait.setInt(2, currentUser.getId());
            ResultSet rsTrait = psTrait.executeQuery();
            while (rsTrait.next()) {
                traitements.add(new QRCodeService.TraitementInfo(
                    rsTrait.getString("nom"),
                    rsTrait.getString("dosage"),
                    rsTrait.getString("frequence"),
                    rsTrait.getString("repas"),
                    rsTrait.getInt("duree_jours"),
                    "en_attente"
                ));
            }
            rsTrait.close(); psTrait.close();
        } catch (SQLException e) {
            org.example.util.DialogService.showError("Erreur", "Erreur chargement ordonnance : " + e.getMessage());
            return;
        }

        // Afficher la fenêtre de confirmation de renouvellement
        showRenouvellementDialog(numeroSource, noteSource, traitements);
    }

    private void showRenouvellementDialog(String numeroSource, String noteSource,
                                           List<QRCodeService.TraitementInfo> traitements) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Renouvellement d'ordonnance");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(480);
        root.setMaxWidth(480);

        // En-tête
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(22, 20, 16, 20));
        header.setStyle("-fx-background-color: #e67e22;");
        Label titleLbl = new Label("🔄 Renouvellement d'ordonnance");
        titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label("Basé sur : " + numeroSource);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        header.getChildren().addAll(titleLbl, subLbl);

        // Corps
        VBox body = new VBox(12);
        body.setPadding(new Insets(18, 22, 10, 22));

        // Info
        VBox infoBox = new VBox(4);
        infoBox.setPadding(new Insets(10, 14, 10, 14));
        infoBox.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 8; " +
                "-fx-border-color: #f39c12; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
        Label infoLbl = new Label("ℹ️  L'ordonnance sera prolongée avec une nouvelle date d'expiration.\nLe statut passera à \"en_attente\" pour validation.");
        infoLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #7f5200; -fx-wrap-text: true;");
        infoLbl.setWrapText(true);
        infoLbl.setMaxWidth(420);
        infoBox.getChildren().add(infoLbl);

        // Sélecteur de nouvelle date d'expiration
        Label dateLbl = new Label("Nouvelle date d'expiration :");
        dateLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        javafx.scene.control.DatePicker newDatePicker = new javafx.scene.control.DatePicker(
            java.time.LocalDate.now().plusMonths(3)); // Par défaut : +3 mois
        newDatePicker.setMaxWidth(Double.MAX_VALUE);
        newDatePicker.setStyle("-fx-font-size: 13;");
        // Bloquer les dates passées
        newDatePicker.setDayCellFactory(dp -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(java.time.LocalDate.now().plusDays(1)));
            }
        });

        // Traitements qui seront conservés
        Label traitTitle = new Label("Traitements associés :");
        traitTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        body.getChildren().addAll(infoBox, dateLbl, newDatePicker, traitTitle);

        for (QRCodeService.TraitementInfo t : traitements) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #f0f7f4; -fx-background-radius: 8;");
            row.setAlignment(Pos.CENTER_LEFT);
            Label prodLbl = new Label("💊 " + t.produit);
            prodLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f6f5c; -fx-min-width: 140;");
            Label detailLbl = new Label(
                (t.dosage != null && !t.dosage.isBlank() ? t.dosage : "") +
                (t.frequence != null && !t.frequence.isBlank() ? " · " + t.frequence : "") +
                (t.dureeJours > 0 ? " · " + t.dureeJours + "j" : ""));
            detailLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");
            row.getChildren().addAll(prodLbl, detailLbl);
            body.getChildren().add(row);
        }

        if (traitements.isEmpty()) {
            Label noTrait = new Label("Aucun traitement associé à cette ordonnance.");
            noTrait.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
            body.getChildren().add(noTrait);
        }

        // Note médicale modifiable
        Label noteLbl = new Label("Message pour le pharmacien (optionnel) :");
        noteLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextArea noteArea = new TextArea(noteSource);
        noteArea.setPromptText("Précisez un changement de dosage, une allergie...");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-font-size: 12; -fx-background-radius: 8;");
        body.getChildren().addAll(noteLbl, noteArea);

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        body.getChildren().add(errLbl);

        // Footer
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 22, 20, 22));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("✅ Confirmer le renouvellement");
        confirmBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13;");
        confirmBtn.setOnAction(e -> {
            // Valider la date choisie
            if (newDatePicker.getValue() == null || !newDatePicker.getValue().isAfter(java.time.LocalDate.now())) {
                errLbl.setText("Choisissez une date d'expiration dans le futur.");
                return;
            }
            confirmBtn.setDisable(true);
            try {
                Connection conn = DatabaseUtil.getInstance().getConnection();

                // Mettre à jour l'ordonnance existante : nouvelle date d'expiration + statut en_attente
                PreparedStatement psUpd = conn.prepareStatement(
                    "UPDATE ordonnance SET date_expiration = ?, statut = 'en_attente', " +
                    "note_medical = ? " +
                    "WHERE numero_ordonnance = ? AND id_utilisateur_id = ?");
                psUpd.setTimestamp(1, Timestamp.valueOf(newDatePicker.getValue().atTime(23, 59, 59)));
                psUpd.setString(2, noteArea.getText() != null ? noteArea.getText().trim() : noteSource);
                psUpd.setString(3, numeroSource);
                psUpd.setInt(4, currentUser.getId());
                int rows = psUpd.executeUpdate();
                psUpd.close();

                if (rows == 0) {
                    errLbl.setText("Ordonnance introuvable.");
                    confirmBtn.setDisable(false);
                    return;
                }

                // Audit
                org.example.util.AuditService.getInstance().logCreation(
                    "ordonnance", numeroSource,
                    "Renouvellement : nouvelle expiration " + newDatePicker.getValue(),
                    currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail());

                dialog.close();

                // Confirmation succès
                javafx.stage.Stage ok = new javafx.stage.Stage();
                ok.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                ok.setTitle("Renouvellement confirmé");
                VBox okRoot = new VBox(16);
                okRoot.setAlignment(Pos.CENTER);
                okRoot.setPadding(new Insets(30, 40, 30, 40));
                okRoot.setStyle("-fx-background-color: white;");
                okRoot.setMinWidth(380);
                Label okIcon = new Label("✅"); okIcon.setStyle("-fx-font-size: 48;");
                Label okTitle = new Label("Ordonnance prolongée !");
                okTitle.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
                Label okSub = new Label("L'ordonnance " + numeroSource +
                    "\na été prolongée jusqu'au\n" + newDatePicker.getValue() +
                    "\nElle est en attente de validation.");
                okSub.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-text-alignment: center; -fx-wrap-text: true;");
                okSub.setWrapText(true);
                okSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                Button okBtn = new Button("Voir mes ordonnances");
                okBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand;");
                okBtn.setOnAction(ev -> { ok.close(); loadOrdonnances(""); });
                okRoot.getChildren().addAll(okIcon, okTitle, okSub, okBtn);
                ok.setScene(new javafx.scene.Scene(okRoot));
                ok.showAndWait();

            } catch (SQLException ex) {
                errLbl.setText("Erreur : " + ex.getMessage());
                confirmBtn.setDisable(false);
            }
        });

        footer.getChildren().addAll(cancelBtn, confirmBtn);

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setMaxHeight(360);
        scrollBody.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, scrollBody, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    /**
     * Affiche une fenêtre avec le QR code de l'ordonnance.
     * Le QR code encode les instructions d'utilisation de chaque traitement.
     */
    private void showQRCodeDialog(String numeroOrdonnance) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return;

        // Charger les traitements de cette ordonnance
        List<QRCodeService.TraitementInfo> traitements = new ArrayList<>();
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.nom, t.dosage, t.frequence, t.repas, t.duree_jours, t.status " +
                "FROM traitement t " +
                "JOIN produit p ON t.id_produit_id = p.id_produit " +
                "JOIN ordonnance o ON t.id_ordonnance_id = o.id_ordonnance " +
                "WHERE o.numero_ordonnance = ? AND o.id_utilisateur_id = ?"
            );
            ps.setString(1, numeroOrdonnance);
            ps.setInt(2, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                traitements.add(new QRCodeService.TraitementInfo(
                    rs.getString("nom"),
                    rs.getString("dosage"),
                    rs.getString("frequence"),
                    rs.getString("repas"),
                    rs.getInt("duree_jours"),
                    rs.getString("status")
                ));
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            org.example.util.DialogService.showError("Erreur", "Erreur chargement traitements : " + e.getMessage());
            return;
        }

        // Générer le PDF et obtenir l'URL à encoder dans le QR code
        String qrContent;
        try {
            String patient = currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail();
            qrContent = org.example.util.QRPdfServerService.getInstance()
                .genererPdfEtGetUrl(numeroOrdonnance, patient, traitements);
            System.out.println("[QR] URL encodée : " + qrContent);
        } catch (Exception e) {
            // Fallback : encoder le texte brut si le serveur ne démarre pas
            qrContent = QRCodeService.getInstance().buildQRContent(
                numeroOrdonnance,
                currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail(),
                traitements
            );
            System.err.println("[QR] Fallback texte brut : " + e.getMessage());
        }

        // Générer l'image QR
        javafx.scene.image.Image qrImage;
        try {
            qrImage = QRCodeService.getInstance().generateQRImage(qrContent, 300);
        } catch (Exception e) {
            org.example.util.DialogService.showError("Erreur QR", "Erreur génération QR code : " + e.getMessage());
            return;
        }

        // Construire la fenêtre
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("QR Code — " + numeroOrdonnance);
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(500);
        root.setMaxWidth(500);

        // En-tête
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 20, 16, 20));
        header.setStyle("-fx-background-color: #1f6f5c;");
        Label titleLbl = new Label("📷 QR Code Ordonnance");
        titleLbl.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label(numeroOrdonnance);
        subLbl.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.8);");
        header.getChildren().addAll(titleLbl, subLbl);

        // QR image
        VBox qrBox = new VBox(10);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setPadding(new Insets(20, 20, 10, 20));
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(280);
        qrView.setFitHeight(280);
        qrView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        Label scanHint = new Label("Scannez avec l'appareil photo de votre téléphone → PDF s'ouvre automatiquement");
        scanHint.setStyle("-fx-font-size: 12; -fx-text-fill: #1f6f5c; -fx-font-weight: bold; -fx-wrap-text: true; -fx-text-alignment: center;");
        scanHint.setMaxWidth(320);
        scanHint.setWrapText(true);

        String ipInfo = "📡 Serveur local : " + org.example.util.QRPdfServerService.getInstance().getLocalIP() + ":8765";
        Label ipLabel = new Label(ipInfo);
        ipLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #aaa;");

        qrBox.getChildren().addAll(qrView, scanHint, ipLabel);

        // Instructions texte (aperçu)
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(12, 20, 12, 20));
        infoBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1 0 1 0;");
        Label infoTitle = new Label("📋 Contenu encodé — Instructions d'utilisation :");
        infoTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");
        infoBox.getChildren().add(infoTitle);

        for (QRCodeService.TraitementInfo t : traitements) {
            VBox traitBox = new VBox(3);
            traitBox.setPadding(new Insets(8, 10, 8, 10));
            traitBox.setStyle("-fx-background-color: #f0f7f4; -fx-background-radius: 8;");
            Label prodLbl = new Label("💊 " + t.produit);
            prodLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");
            traitBox.getChildren().add(prodLbl);
            if (t.dosage != null && !t.dosage.isBlank())
                traitBox.getChildren().add(styledInfoLine("Dosage", t.dosage));
            if (t.frequence != null && !t.frequence.isBlank())
                traitBox.getChildren().add(styledInfoLine("Fréquence", t.frequence));
            if (t.repas != null && !t.repas.isBlank())
                traitBox.getChildren().add(styledInfoLine("Repas", t.repas));
            if (t.dureeJours > 0)
                traitBox.getChildren().add(styledInfoLine("Durée", t.dureeJours + " jours"));
            infoBox.getChildren().add(traitBox);
        }

        if (traitements.isEmpty()) {
            Label noTrait = new Label("Aucun traitement associé.");
            noTrait.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
            infoBox.getChildren().add(noTrait);
        }

        // Bandeau de sensibilisation au bon dosage
        VBox sensiBox = new VBox(10);
        sensiBox.setPadding(new Insets(14, 20, 14, 20));
        sensiBox.setStyle("-fx-background-color: #fff8e1; -fx-border-color: #f39c12; " +
                "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0; -fx-background-radius: 0 8 8 0;");

        Label sensiIcon = new Label("⚠️  Respectez toujours le dosage prescrit");
        sensiIcon.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        Label sensiText = new Label(
            "Ne dépassez jamais la dose recommandée, même en cas de douleur persistante. " +
            "Un surdosage peut entraîner des effets graves sur le foie, les reins ou le cœur. " +
            "En cas de doute, consultez votre médecin ou pharmacien avant toute prise.");
        sensiText.setStyle("-fx-font-size: 11; -fx-text-fill: #7f5200; -fx-wrap-text: true;");
        sensiText.setMaxWidth(400);
        sensiText.setWrapText(true);
        sensiBox.getChildren().addAll(sensiIcon, sensiText);

        // Images de sensibilisation
        javafx.scene.layout.HBox imagesBox = new javafx.scene.layout.HBox(10);
        imagesBox.setAlignment(Pos.CENTER);
        imagesBox.setPadding(new Insets(10, 20, 10, 20));
        imagesBox.setStyle("-fx-background-color: white;");

        String[] imagePaths = {
            "/images/conseils_medicaments.png",
            "/images/sensibilisation_pharmacie.png"
        };
        for (String imgPath : imagePaths) {
            try {
                java.io.InputStream is = getClass().getResourceAsStream(imgPath);
                if (is != null) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(is);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(190);
                    iv.setFitHeight(190);
                    iv.setPreserveRatio(true);
                    iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);");
                    imagesBox.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        // Bouton fermer
        VBox footer = new VBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 20, 20, 20));
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 50; -fx-cursor: hand; -fx-font-size: 13;");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);

        ScrollPane scrollPane = new ScrollPane(infoBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(220);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, qrBox, scrollPane, imagesBox, sensiBox, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** Ligne d'info formatée label: valeur */
    private javafx.scene.layout.HBox styledInfoLine(String label, String value) {
        javafx.scene.layout.HBox line = new javafx.scene.layout.HBox(6);
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #555; -fx-min-width: 70;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");
        line.getChildren().addAll(lbl, val);
        return line;
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
    private void handleLogout() {
        try {
            userService.logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTraitement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Traitement.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToMesOrdonnances() {
        // Already on this page
    }

    @FXML
    private void goToCreerOrdonnance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profil.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
