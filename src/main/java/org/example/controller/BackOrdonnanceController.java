package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.util.DatabaseUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

// Contrôleur back-office pour la gestion des ordonnances par l'administrateur (CRUD + statistiques + PieChart)
public class BackOrdonnanceController {

    @FXML private VBox pageContainer; // Conteneur principal de la page (remplacé dynamiquement selon la vue)

    @FXML
    public void initialize() {
        org.example.util.AuditService.getInstance().initTable();
        org.example.util.ElectronicSignatureService.getInstance().initColonnes();
        showList();
    }

    public void openNewForm() { showForm(null); } // Ouvrir le formulaire d'ajout (appelé depuis le Dashboard)

    private void showList() {
        pageContainer.getChildren().clear();
        HBox greenBar = new HBox(); greenBar.setMinHeight(6); greenBar.setStyle("-fx-background-color: #1f6f5c;");
        VBox header = new VBox(5); header.setPadding(new Insets(25, 30, 25, 30));
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0.2, 0, 2);");
        Label t = new Label("Gestion des Ordonnances"); t.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label s = new Label("Liste de toutes les ordonnances"); s.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
        header.getChildren().addAll(t, s);
        HBox actionBar = new HBox(15); actionBar.setPadding(new Insets(20, 30, 10, 30)); actionBar.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("+ Nouvelle Ordonnance");
        addBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showForm(null));
        Button statsBtn = new Button("\u2699 Statistiques");
        statsBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        statsBtn.setOnAction(e -> showStats());
        actionBar.getChildren().addAll(addBtn, statsBtn);

        VBox filterCard = new VBox(10); filterCard.setPadding(new Insets(15, 30, 15, 30));
        filterCard.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0.2, 0, 2);");
        VBox.setMargin(filterCard, new Insets(10, 30, 10, 30));
        HBox filters = new HBox(15); filters.setAlignment(Pos.CENTER_LEFT);
        DatePicker dOrd = new DatePicker(); dOrd.setPromptText("Date ordonnance"); dOrd.setPrefWidth(160);
        DatePicker dExp = new DatePicker(); dExp.setPromptText("Date expiration"); dExp.setPrefWidth(160);
        TextField cf = new TextField(); cf.setPromptText("Nom utilisateur"); cf.setPrefWidth(160);
        ComboBox<String> sf = new ComboBox<>(FXCollections.observableArrayList("Tous les statuts","brouillon","en_attente","valid\u00e9e","expir\u00e9e"));
        sf.setValue("Tous les statuts"); sf.setPrefWidth(140);
        ComboBox<String> triF = new ComboBox<>(FXCollections.observableArrayList("Date Ordonnance (Plus r\u00e9cent)","Date Ordonnance (Plus ancien)"));
        triF.setValue("Date Ordonnance (Plus r\u00e9cent)"); triF.setPrefWidth(220);
        filters.getChildren().addAll(dOrd, dExp, cf, sf, triF);
        filterCard.getChildren().addAll(new Label("\uD83D\uDD0D Filtrer par :"), filters);

        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(400);
        String[] hdrs = {"ID","Num\u00e9ro","Patient","Date Ordonnance","Date Expiration","Statut","Traitements","Signature","Actions"};
        for (int i = 0; i < hdrs.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> c = new TableColumn<>(hdrs[i]);
            c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(col < p.getValue().size() ? p.getValue().get(col) : ""));

            // Rendu visuel de la colonne Signature
            if (i == hdrs.length - 2) { // colonne "Signature"
                c.setMinWidth(160); c.setPrefWidth(180);
                c.setCellFactory(tc -> new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.isBlank()) {
                            setGraphic(null);
                            setText(null);
                        } else if (item.equals("NON_SIGNE")) {
                            Label lbl = new Label("⬜ Non signé");
                            lbl.setStyle("-fx-font-size:11; -fx-text-fill:#aaa;");
                            setGraphic(lbl); setText(null);
                        } else {
                            VBox box = new VBox(2);
                            Label check = new Label("✅ Signé");
                            check.setStyle("-fx-font-size:11; -fx-font-weight:bold; -fx-text-fill:#27ae60;");
                            Label nom = new Label("Dr. " + item);
                            nom.setStyle("-fx-font-size:10; -fx-text-fill:#555;");
                            box.getChildren().addAll(check, nom);
                            setGraphic(box); setText(null);
                        }
                    }
                });
            }

            // Largeur fixe pour la colonne Actions
            if (i == hdrs.length - 1) {
                c.setMinWidth(280);
                c.setPrefWidth(300);
                c.setResizable(false);
                c.setCellFactory(tc -> new TableCell<>() {
                    final Button eb = new Button("Modifier");
                    final Button db2 = new Button("Supprimer");
                    final Button signBtn = new Button("✍ Signer");
                    final Button histBtn = new Button("📋 Historique");
                    final HBox bx = new HBox(5, eb, histBtn, signBtn, db2);
                    {
                      eb.setStyle("-fx-background-color:#f39c12;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      db2.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      signBtn.setStyle("-fx-background-color:#1f6f5c;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      histBtn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      eb.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()).get(0)));
                      histBtn.setOnAction(e -> {
                          ObservableList<String> row = getTableView().getItems().get(getIndex());
                          showHistoriqueDialog("ordonnance", row.get(0), row.get(1));
                      });
                      signBtn.setOnAction(e -> {
                          ObservableList<String> row = getTableView().getItems().get(getIndex());
                          showSignatureMedecinDialog(row.get(0), row.get(1));
                      });
                      db2.setOnAction(e -> { String id = getTableView().getItems().get(getIndex()).get(0);
                          ObservableList<String> rowDel = getTableView().getItems().get(getIndex());
                          String numDel = rowDel.size() > 1 ? rowDel.get(1) : "#" + id;
                          if (org.example.util.DialogService.showDeleteConfirmation("l'ordonnance " + numDel)) {
                              try {
                                  org.example.util.AuditService.getInstance().logSuppression("ordonnance", id, "Ordonnance #"+id+" supprimée", "Admin");
                                  PreparedStatement p2=DatabaseUtil.getInstance().getConnection().prepareStatement("DELETE FROM ordonnance WHERE id_ordonnance=?");
                                  p2.setInt(1,Integer.parseInt(id));p2.executeUpdate();p2.close();showList();
                              } catch(SQLException ex){ org.example.util.DialogService.showError("Erreur", ex.getMessage()); }
                          }});}
                    @Override protected void updateItem(String item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : bx); }
                });
            }
            table.getColumns().add(c);
        }
        // ── Pagination ────────────────────────────────────────────────────
        final int PAGE_SIZE = 4;
        final int[] currentPage = {0}; // page courante (0-indexée)
        final ObservableList<ObservableList<String>>[] allData = new ObservableList[]{FXCollections.observableArrayList()};

        // Barre de pagination
        HBox pagBar = new HBox(10);
        pagBar.setAlignment(Pos.CENTER);
        pagBar.setPadding(new Insets(10, 30, 20, 30));
        Button prevBtn = new Button("◀ Précédent");
        prevBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 18; -fx-cursor: hand;");
        Button nextBtn = new Button("Suivant ▶");
        nextBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 18; -fx-cursor: hand;");
        Label pageLabel = new Label("Page 1 / 1");
        pageLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label countLabel = new Label("0 ordonnance(s)");
        countLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
        pagBar.getChildren().addAll(prevBtn, pageLabel, nextBtn, new Label("  "), countLabel);

        // Affiche la page courante dans le tableau
        Runnable showPage = () -> {
            int total = allData[0].size();
            int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            if (currentPage[0] >= totalPages) currentPage[0] = totalPages - 1;
            int from = currentPage[0] * PAGE_SIZE;
            int to   = Math.min(from + PAGE_SIZE, total);
            table.setItems(FXCollections.observableArrayList(allData[0].subList(from, to)));
            pageLabel.setText("Page " + (currentPage[0] + 1) + " / " + totalPages);
            countLabel.setText(total + " ordonnance(s)");
            prevBtn.setDisable(currentPage[0] == 0);
            nextBtn.setDisable(currentPage[0] >= totalPages - 1);
            if (total == 0) table.setPlaceholder(new Label("Aucune ordonnance trouvée"));
        };

        prevBtn.setOnAction(e -> { currentPage[0]--; showPage.run(); });
        nextBtn.setOnAction(e -> { currentPage[0]++; showPage.run(); });

        // Charger toutes les données puis afficher la page 1
        Runnable apply = () -> {
            currentPage[0] = 0;
            allData[0] = loadDataPaged(dOrd.getValue(), dExp.getValue(), cf.getText().trim(), sf.getValue(), triF.getValue());
            showPage.run();
        };
        apply.run();
        dOrd.valueProperty().addListener((o,a,b) -> apply.run());
        dExp.valueProperty().addListener((o,a,b) -> apply.run());
        cf.textProperty().addListener((o,a,b) -> apply.run());
        sf.valueProperty().addListener((o,a,b) -> apply.run());
        triF.valueProperty().addListener((o,a,b) -> apply.run());

        VBox tw = new VBox(table); tw.setPadding(new Insets(0, 30, 0, 30)); VBox.setVgrow(table, Priority.ALWAYS);
        pageContainer.getChildren().addAll(greenBar, header, actionBar, filterCard, tw, pagBar);
    }

    // Charge toutes les données filtrées et retourne la liste complète (pour la pagination)
    private ObservableList<ObservableList<String>> loadDataPaged(LocalDate dO, LocalDate dE, String cl, String st, String tri) {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try {
            StringBuilder sql = new StringBuilder("SELECT o.id_ordonnance, o.numero_ordonnance, u.nom AS unom, o.date_ordonnance, o.date_expiration, o.statut, (SELECT COUNT(*) FROM traitement t WHERE t.id_ordonnance_id=o.id_ordonnance) AS nb_trait, o.signature_medecin FROM ordonnance o LEFT JOIN utilisateur u ON o.id_utilisateur_id=u.id_utilisateur WHERE 1=1 ");
            java.util.List<Object> params = new java.util.ArrayList<>();
            if (dO != null) { sql.append("AND o.date_ordonnance >= ? "); params.add(Timestamp.valueOf(dO.atStartOfDay())); }
            if (dE != null) { sql.append("AND o.date_expiration <= ? "); params.add(Timestamp.valueOf(dE.atTime(23,59,59))); }
            if (!cl.isEmpty()) { sql.append("AND u.nom LIKE ? "); params.add("%" + cl + "%"); }
            if (st != null && !"Tous les statuts".equals(st)) { sql.append("AND o.statut = ? "); params.add(st); }
            sql.append("ORDER BY o.date_ordonnance ").append(tri.contains("ancien") ? "ASC" : "DESC");
            PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                else ps.setString(i + 1, (String) p);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("id_ordonnance")));
                row.add(rs.getString("numero_ordonnance") != null ? rs.getString("numero_ordonnance") : "");
                row.add(rs.getString("unom") != null ? rs.getString("unom") : "N/A");
                row.add(rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getTimestamp("date_expiration") != null ? rs.getTimestamp("date_expiration").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getString("statut") != null ? rs.getString("statut") : "");
                row.add(String.valueOf(rs.getInt("nb_trait")));
                // Colonne Signature : extraire le nom du médecin depuis "Nom|date|hash"
                String sigVal = rs.getString("signature_medecin");
                String sigNom = org.example.util.ElectronicSignatureService.getInstance().extraireNomSignataire(sigVal);
                row.add(sigNom != null ? sigNom : "NON_SIGNE");
                row.add(""); data.add(row);
            } rs.close(); ps.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return data;
    }

    // Afficher le formulaire d'ajout ou de modification d'une ordonnance
    private void showForm(String editId) {
        pageContainer.getChildren().clear(); // Vider le conteneur
        boolean isEdit = editId != null; // Mode édition si un ID est fourni

        VBox card = new VBox(0);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0.3, 0, 3);");

        HBox hdr = new HBox(12); hdr.setAlignment(Pos.CENTER_LEFT); hdr.setPadding(new Insets(25, 30, 15, 30));
        Label ic = new Label("\uD83D\uDCC4"); ic.setStyle("-fx-font-size: 28; -fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 10;");
        Label ti = new Label(isEdit ? "Modifier Ordonnance #" + editId : "Ajouter une Ordonnance");
        ti.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        hdr.getChildren().addAll(ic, ti);

        VBox form = new VBox(12); form.setPadding(new Insets(15, 30, 25, 30));

        // Numero + Patient side by side
        HBox r1 = new HBox(20);
        VBox numB = new VBox(4); TextField numF = new TextField();
        numF.setPromptText("Num\u00e9ro d'ordonnance");
        if (!isEdit) numF.setText("ORD-" + java.time.LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000)));
        numB.getChildren().addAll(new Label("Num\u00e9ro d'ordonnance"), numF);
        VBox patB = new VBox(4); ComboBox<String> userC = new ComboBox<>(); userC.setPrefWidth(220); userC.setPromptText("S\u00e9lectionner un patient");
        patB.getChildren().addAll(new Label("Patient"), userC);
        r1.getChildren().addAll(numB, patB); HBox.setHgrow(numB, Priority.ALWAYS); HBox.setHgrow(patB, Priority.ALWAYS);

        // Dates side by side
        HBox r2 = new HBox(20);
        VBox doB = new VBox(4); DatePicker doP = new DatePicker(LocalDate.now()); doB.getChildren().addAll(new Label("Date de l'ordonnance"), doP);
        VBox deB = new VBox(4); DatePicker deP = new DatePicker(LocalDate.now().plusMonths(3)); deB.getChildren().addAll(new Label("Date d'expiration"), deP);
        r2.getChildren().addAll(doB, deB); HBox.setHgrow(doB, Priority.ALWAYS); HBox.setHgrow(deB, Priority.ALWAYS);

        // Status banner
        HBox banner = new HBox(8); banner.setAlignment(Pos.CENTER_LEFT); banner.setPadding(new Insets(10, 15, 10, 15));
        banner.setStyle("-fx-background-color: #e8f4fd; -fx-background-radius: 8; -fx-border-color: #bee5eb; -fx-border-radius: 8;");
        banner.getChildren().addAll(new Label("\u2139"), new Label("Le statut sera automatiquement d\u00e9fini \u00e0 \"En attente\" lors de la cr\u00e9ation."));

        ComboBox<String> statC = new ComboBox<>(FXCollections.observableArrayList("brouillon","en_attente","valid\u00e9e","expir\u00e9e"));
        statC.setPrefWidth(220); statC.setValue("en_attente");

        TextArea noteF = new TextArea(); noteF.setPromptText("Note m\u00e9dicale"); noteF.setPrefRowCount(4); noteF.setWrapText(true);
        Label err = new Label(); err.setStyle("-fx-text-fill: #e74c3c;");

        try {
            ResultSet rs = DatabaseUtil.getInstance().getConnection().createStatement().executeQuery("SELECT id_utilisateur, nom, prenom, email FROM utilisateur ORDER BY nom");
            while (rs.next()) userC.getItems().add(rs.getInt(1) + " - " + rs.getString(2) + " " + rs.getString(3) + " (" + rs.getString(4) + ")");
            rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        if (isEdit) {
            try {
                PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement("SELECT * FROM ordonnance WHERE id_ordonnance=?");
                ps.setInt(1, Integer.parseInt(editId)); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    numF.setText(rs.getString("numero_ordonnance"));
                    if (rs.getTimestamp("date_ordonnance") != null) doP.setValue(rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate());
                    if (rs.getTimestamp("date_expiration") != null) deP.setValue(rs.getTimestamp("date_expiration").toLocalDateTime().toLocalDate());
                    statC.setValue(rs.getString("statut"));
                    noteF.setText(rs.getString("note_medical") != null ? rs.getString("note_medical") : "");
                    String uid = String.valueOf(rs.getInt("id_utilisateur_id"));
                    for (String x : userC.getItems()) if (x.startsWith(uid+" -")) { userC.setValue(x); break; }
                } rs.close(); ps.close();
            } catch (SQLException e) { System.out.println(e.getMessage()); }
        }

        HBox btns = new HBox(15); btns.setAlignment(Pos.CENTER); btns.setPadding(new Insets(10, 0, 0, 0));
        Button cancel = new Button("\u2190 Retour"); cancel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        cancel.setOnAction(e -> showList());
        Button save = new Button(isEdit ? "\uD83D\uDCBE Mettre \u00e0 jour" : "\uD83D\uDCBE Ajouter");
        save.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        save.setOnAction(e -> {
            err.setText("");
            // Cas 10 : anti double-clic
            save.setDisable(true);

            // Contrôle : patient obligatoire
            if (userC.getValue() == null) {
                err.setText("Veuillez sélectionner un patient.");
                save.setDisable(false);
                return;
            }

            // Vérifier existence du patient en base
            int patientId = Integer.parseInt(userC.getValue().split(" - ")[0]);
            try {
                PreparedStatement psPatient = DatabaseUtil.getInstance().getConnection().prepareStatement("SELECT COUNT(*) AS nb FROM utilisateur WHERE id_utilisateur = ?");
                psPatient.setInt(1, patientId);
                ResultSet rsPatient = psPatient.executeQuery();
                if (rsPatient.next() && rsPatient.getInt("nb") == 0) {
                    err.setText("Le patient sélectionné n'existe pas en base.");
                    save.setDisable(false);
                    rsPatient.close(); psPatient.close();
                    return;
                }
                rsPatient.close(); psPatient.close();
            } catch (SQLException ex) {
                err.setText("Erreur vérification patient: " + ex.getMessage());
                save.setDisable(false);
                return;
            }

            // Contrôle : numéro d'ordonnance obligatoire et format
            String numero = numF.getText() != null ? numF.getText().trim() : "";
            if (numero.isEmpty()) {
                err.setText("Le numéro d'ordonnance est obligatoire.");
                save.setDisable(false);
                return;
            }
            if (!numero.matches("^ORD-\\d{4}-\\d{4}$")) {
                err.setText("Le numéro d'ordonnance doit respecter le format ORD-AAAA-XXXX (ex: ORD-2026-4837).");
                save.setDisable(false);
                return;
            }

            // Cas 8/9/11 : vérifier unicité du numéro (trim + insensible casse)
            try {
                String sqlUniq = isEdit
                        ? "SELECT COUNT(*) AS nb FROM ordonnance WHERE LOWER(TRIM(numero_ordonnance)) = LOWER(?) AND id_ordonnance != ?"
                        : "SELECT COUNT(*) AS nb FROM ordonnance WHERE LOWER(TRIM(numero_ordonnance)) = LOWER(?)";
                PreparedStatement psUniq = DatabaseUtil.getInstance().getConnection().prepareStatement(sqlUniq);
                psUniq.setString(1, numero);
                if (isEdit) psUniq.setInt(2, Integer.parseInt(editId));
                ResultSet rsUniq = psUniq.executeQuery();
                if (rsUniq.next() && rsUniq.getInt("nb") > 0) {
                    err.setText("Ce numéro d'ordonnance existe déjà.");
                    save.setDisable(false);
                    rsUniq.close(); psUniq.close();
                    return;
                }
                rsUniq.close(); psUniq.close();
            } catch (SQLException ex) {
                err.setText("Erreur vérification numéro: " + ex.getMessage());
                save.setDisable(false);
                return;
            }

            // Contrôle : date ordonnance obligatoire
            if (doP.getValue() == null) {
                err.setText("Date invalide");
                save.setDisable(false);
                return;
            }
            // Contrôle : date ordonnance pas dans le futur
            if (doP.getValue().isAfter(java.time.LocalDate.now())) {
                err.setText("La date de l'ordonnance ne peut pas être dans le futur.");
                save.setDisable(false);
                return;
            }
            // Contrôle : date expiration obligatoire
            if (deP.getValue() == null) {
                err.setText("Date invalide");
                save.setDisable(false);
                return;
            }
            // Contrôle : date expiration après date ordonnance
            if (!deP.getValue().isAfter(doP.getValue())) {
                err.setText("La date d'expiration doit être postérieure à la date de l'ordonnance.");
                save.setDisable(false);
                return;
            }

            // Contrôle : note médicale longueur max
            String noteText = noteF.getText() != null ? noteF.getText().trim() : "";
            if (noteText.length() > 1000) {
                err.setText("La note médicale ne doit pas dépasser 1000 caractères.");
                save.setDisable(false);
                return;
            }

            // Doublon : même patient + même date ordonnance
            try {
                String sqlDup = isEdit
                        ? "SELECT COUNT(*) AS nb FROM ordonnance WHERE id_utilisateur_id = ? AND DATE(date_ordonnance) = ? AND id_ordonnance != ?"
                        : "SELECT COUNT(*) AS nb FROM ordonnance WHERE id_utilisateur_id = ? AND DATE(date_ordonnance) = ?";
                PreparedStatement psDup = DatabaseUtil.getInstance().getConnection().prepareStatement(sqlDup);
                psDup.setInt(1, patientId);
                psDup.setDate(2, java.sql.Date.valueOf(doP.getValue()));
                if (isEdit) psDup.setInt(3, Integer.parseInt(editId));
                ResultSet rsDup = psDup.executeQuery();
                if (rsDup.next() && rsDup.getInt("nb") > 0) {
                    err.setText("Une ordonnance existe déjà pour ce patient à cette date.");
                    save.setDisable(false);
                    rsDup.close(); psDup.close();
                    return;
                }
                rsDup.close(); psDup.close();
            } catch (SQLException ex) {
                err.setText("Erreur vérification doublon: " + ex.getMessage());
                save.setDisable(false);
                return;
            }

            // Validation en modification : vérifier que les traitements liés existent en base
            if (isEdit) {
                try {
                    PreparedStatement psTrait = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT COUNT(*) AS nb FROM traitement WHERE id_ordonnance_id = ?");
                    psTrait.setInt(1, Integer.parseInt(editId));
                    ResultSet rsTrait = psTrait.executeQuery();
                    // Info seulement, pas bloquant pour la modification
                    rsTrait.close(); psTrait.close();
                } catch (SQLException ex) {
                    // Non bloquant
                }
            }

            try {
                if (isEdit) {
                    // Lire les anciennes valeurs pour l'audit
                    String oldNumero = "", oldStatut = "", oldNote = "";
                    String oldDateOrd = "", oldDateExp = "";
                    try {
                        PreparedStatement psOld = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT numero_ordonnance, statut, note_medical, date_ordonnance, date_expiration FROM ordonnance WHERE id_ordonnance=?");
                        psOld.setInt(1, Integer.parseInt(editId));
                        ResultSet rsOld = psOld.executeQuery();
                        if (rsOld.next()) {
                            oldNumero  = rsOld.getString("numero_ordonnance") != null ? rsOld.getString("numero_ordonnance") : "";
                            oldStatut  = rsOld.getString("statut") != null ? rsOld.getString("statut") : "";
                            oldNote    = rsOld.getString("note_medical") != null ? rsOld.getString("note_medical") : "";
                            oldDateOrd = rsOld.getTimestamp("date_ordonnance") != null
                                ? rsOld.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate().toString() : "";
                            oldDateExp = rsOld.getTimestamp("date_expiration") != null
                                ? rsOld.getTimestamp("date_expiration").toLocalDateTime().toLocalDate().toString() : "";
                        }
                        rsOld.close(); psOld.close();
                    } catch (Exception ignored) {}

                    PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement("UPDATE ordonnance SET numero_ordonnance=?,date_ordonnance=?,date_expiration=?,statut=?,note_medical=?,id_utilisateur_id=? WHERE id_ordonnance=?");
                    ps.setString(1, numero); ps.setTimestamp(2, Timestamp.valueOf(doP.getValue().atStartOfDay()));
                    ps.setTimestamp(3, Timestamp.valueOf(deP.getValue().atStartOfDay())); ps.setString(4, statC.getValue());
                    ps.setString(5, noteText); ps.setInt(6, patientId);
                    ps.setInt(7, Integer.parseInt(editId)); ps.executeUpdate(); ps.close();

                    // Audit : loguer uniquement les champs réellement modifiés
                    org.example.util.AuditService audit = org.example.util.AuditService.getInstance();
                    audit.logSiModifie("ordonnance", editId, "Numéro", oldNumero, numero, "Admin");
                    audit.logSiModifie("ordonnance", editId, "Statut", oldStatut, statC.getValue(), "Admin");
                    audit.logSiModifie("ordonnance", editId, "Note médicale", oldNote, noteText, "Admin");
                    audit.logSiModifie("ordonnance", editId, "Date ordonnance",
                        oldDateOrd, doP.getValue().toString(), "Admin");
                    audit.logSiModifie("ordonnance", editId, "Date expiration",
                        oldDateExp, deP.getValue().toString(), "Admin");

                    // Propagation automatique du statut ordonnance vers les traitements liés
                    String newStatut = statC.getValue();
                    if ("validée".equals(newStatut)) {
                        PreparedStatement ps2 = DatabaseUtil.getInstance().getConnection().prepareStatement("UPDATE traitement SET status='actif' WHERE id_ordonnance_id=? AND status='en_attente'");
                        ps2.setInt(1, Integer.parseInt(editId)); ps2.executeUpdate(); ps2.close();
                        // Audit propagation statut traitements
                        if (!oldStatut.equals("validée")) {
                            audit.log("ordonnance", editId, "MODIFICATION",
                                "Propagation statut traitements",
                                "en_attente", "actif (suite validation ordonnance)", "Admin");
                        }
                        // Envoi SMS au patient
                        try {
                            PreparedStatement psTel = DatabaseUtil.getInstance().getConnection().prepareStatement(
                                "SELECT telephone, nom FROM utilisateur WHERE id_utilisateur = ?");
                            psTel.setInt(1, patientId);
                            ResultSet rsTel = psTel.executeQuery();
                            String telephone = null;
                            String nomPatient = "Patient";
                            if (rsTel.next()) {
                                telephone = rsTel.getString("telephone");
                                nomPatient = rsTel.getString("nom") != null ? rsTel.getString("nom") : "Patient";
                            }
                            rsTel.close(); psTel.close();
                            String smsMsg = "Bonjour " + nomPatient + ", votre ordonnance " + numero + " a été validée. Vos traitements sont maintenant actifs. - CuraVita";
                            org.example.util.SmsService.getInstance().send(telephone, smsMsg);
                        } catch (Exception smsEx) {
                            System.err.println("[SMS] Erreur récupération téléphone : " + smsEx.getMessage());
                        }
                    } else if ("expirée".equals(newStatut)) {
                        PreparedStatement ps2 = DatabaseUtil.getInstance().getConnection().prepareStatement("UPDATE traitement SET status='terminé' WHERE id_ordonnance_id=? AND status IN ('en_attente','actif')");
                        ps2.setInt(1, Integer.parseInt(editId)); ps2.executeUpdate(); ps2.close();
                        if (!oldStatut.equals("expirée")) {
                            audit.log("ordonnance", editId, "MODIFICATION",
                                "Propagation statut traitements",
                                "en_attente/actif", "terminé (suite expiration ordonnance)", "Admin");
                        }
                    } else if ("brouillon".equals(newStatut)) {
                        PreparedStatement ps2 = DatabaseUtil.getInstance().getConnection().prepareStatement("UPDATE traitement SET status='en_attente' WHERE id_ordonnance_id=?");
                        ps2.setInt(1, Integer.parseInt(editId)); ps2.executeUpdate(); ps2.close();
                        if (!oldStatut.equals("brouillon")) {
                            audit.log("ordonnance", editId, "MODIFICATION",
                                "Propagation statut traitements",
                                "actif", "en_attente (remise en brouillon)", "Admin");
                        }
                    }
                } else {
                    // Insérer la nouvelle ordonnance et récupérer son ID
                    PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "INSERT INTO ordonnance (numero_ordonnance,date_ordonnance,date_expiration,statut,note_medical,id_utilisateur_id) VALUES (?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, numero); ps.setTimestamp(2, Timestamp.valueOf(doP.getValue().atStartOfDay()));
                    ps.setTimestamp(3, Timestamp.valueOf(deP.getValue().atStartOfDay())); ps.setString(4, "en_attente");
                    ps.setString(5, noteText); ps.setInt(6, patientId);
                    ps.executeUpdate();
                    // Récupérer l'ID de l'ordonnance créée
                    ResultSet genKeys = ps.getGeneratedKeys();
                    int newOrdId = 0;
                    if (genKeys.next()) newOrdId = genKeys.getInt(1);
                    genKeys.close(); ps.close();
                    // Audit création
                    if (newOrdId > 0) {
                        org.example.util.AuditService.getInstance().logCreation(
                            "ordonnance", String.valueOf(newOrdId),
                            "Ordonnance " + numero + " créée (statut: en_attente)", "Admin");
                    }

                    // Créer automatiquement un traitement vide lié à cette ordonnance (pour qu'elle apparaisse dans la liste traitements)
                    if (newOrdId > 0) {
                        PreparedStatement psTrait = DatabaseUtil.getInstance().getConnection().prepareStatement(
                                "INSERT INTO traitement (id_utilisateur_id, dosage, frequence, duree_jours, date_debut, status, notes, id_ordonnance_id, id_produit_id, repas) VALUES (?,?,?,?,?,?,?,?,NULL,?)");
                        psTrait.setInt(1, patientId); // Même patient que l'ordonnance
                        psTrait.setString(2, ""); // Dosage vide (à remplir par l'admin)
                        psTrait.setString(3, ""); // Fréquence vide
                        psTrait.setInt(4, 0); // Durée = 0
                        psTrait.setTimestamp(5, Timestamp.valueOf(doP.getValue().atStartOfDay())); // Date début = date ordonnance
                        psTrait.setString(6, "en_attente"); // Statut en attente
                        psTrait.setString(7, ""); // Notes vides
                        psTrait.setInt(8, newOrdId); // Lier à l'ordonnance créée
                        psTrait.setString(9, ""); // Repas vide
                        psTrait.executeUpdate(); psTrait.close();
                    }
                }
                showList();
            } catch (SQLException ex) {
                err.setText("Erreur: " + ex.getMessage());
                save.setDisable(false);
            }
        });
        btns.getChildren().addAll(cancel, save);

        form.getChildren().addAll(r1, r2);
        if (isEdit) form.getChildren().addAll(new Label("Statut"), statC); else form.getChildren().add(banner);
        form.getChildren().addAll(new Label("Note m\u00e9dicale"), noteF, err, new Separator(), btns);
        card.getChildren().addAll(hdr, new Separator(), form);

        VBox wrap = new VBox(card); wrap.setAlignment(Pos.TOP_CENTER); wrap.setPadding(new Insets(30)); wrap.setStyle("-fx-background-color: #f5f5f5;");
        ScrollPane sc = new ScrollPane(wrap); sc.setFitToWidth(true); sc.setStyle("-fx-background-color: #f5f5f5;");
        pageContainer.getChildren().add(sc); VBox.setVgrow(sc, Priority.ALWAYS);
    }

    // Fenêtre d'historique des modifications
    private void showHistoriqueDialog(String entite, String entiteId, String entiteLabel) {
        java.util.List<org.example.util.AuditService.AuditEntry> entries =
                org.example.util.AuditService.getInstance().getHistorique(entite, entiteId);

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Historique — " + entiteLabel);
        dialog.setResizable(true);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f5f5f5;");
        root.setMinWidth(620); root.setPrefWidth(680);

        // En-tête
        VBox header = new VBox(5);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color: #2980b9;");
        Label titleLbl = new Label("📋 Historique des modifications");
        titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label(entite.substring(0,1).toUpperCase() + entite.substring(1) + " #" + entiteId + " — " + entiteLabel);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        header.getChildren().addAll(titleLbl, subLbl);

        // Corps
        VBox body = new VBox(8);
        body.setPadding(new Insets(16, 20, 16, 20));

        if (entries.isEmpty()) {
            Label empty = new Label("Aucune modification enregistrée pour cet élément.");
            empty.setStyle("-fx-font-size: 13; -fx-text-fill: #888; -fx-padding: 20;");
            body.getChildren().add(empty);
        } else {
            for (org.example.util.AuditService.AuditEntry e : entries) {
                VBox card = new VBox(5);
                card.setPadding(new Insets(10, 14, 10, 14));

                // Couleur selon action
                String bg, border, actionColor;
                switch (e.action) {
                    case "CRÉATION":   bg = "#eafaf1"; border = "#27ae60"; actionColor = "#27ae60"; break;
                    case "SUPPRESSION": bg = "#fdecea"; border = "#e74c3c"; actionColor = "#e74c3c"; break;
                    default:           bg = "#eaf4fb"; border = "#2980b9"; actionColor = "#2980b9"; break;
                }
                card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:8; " +
                        "-fx-border-color:" + border + "; -fx-border-width:0 0 0 4; -fx-border-radius:0 8 8 0;");

                // Ligne 1 : action + horodatage + admin
                HBox topRow = new HBox(10);
                topRow.setAlignment(Pos.CENTER_LEFT);
                Label actionLbl = new Label(e.action);
                actionLbl.setStyle("-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:" + actionColor +
                        "; -fx-background-color:white; -fx-padding:2 8; -fx-background-radius:10;");
                Label dateLbl = new Label("🕐 " + e.modifieAt);
                dateLbl.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
                Label adminLbl = new Label("👤 " + e.modifiePar);
                adminLbl.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
                topRow.getChildren().addAll(actionLbl, dateLbl, adminLbl);

                card.getChildren().add(topRow);

                // Ligne 2 : champ modifié
                if (e.champ != null && !e.champ.isBlank()) {
                    Label champLbl = new Label("Champ : " + e.champ);
                    champLbl.setStyle("-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#2c3e50;");
                    card.getChildren().add(champLbl);
                }

                // Ligne 3 : avant → après
                if (e.ancienneValeur != null || e.nouvelleValeur != null) {
                    HBox diffRow = new HBox(8);
                    diffRow.setAlignment(Pos.CENTER_LEFT);
                    if (e.ancienneValeur != null && !e.ancienneValeur.isBlank()) {
                        Label avantLbl = new Label("Avant : " + truncate(e.ancienneValeur, 60));
                        avantLbl.setStyle("-fx-font-size:11; -fx-text-fill:#e74c3c; -fx-wrap-text:true;");
                        avantLbl.setWrapText(true);
                        diffRow.getChildren().add(avantLbl);
                    }
                    if (e.ancienneValeur != null && !e.ancienneValeur.isBlank()
                            && e.nouvelleValeur != null && !e.nouvelleValeur.isBlank()) {
                        Label arrow = new Label("→");
                        arrow.setStyle("-fx-font-size:13; -fx-text-fill:#888;");
                        diffRow.getChildren().add(arrow);
                    }
                    if (e.nouvelleValeur != null && !e.nouvelleValeur.isBlank()) {
                        Label apresLbl = new Label("Après : " + truncate(e.nouvelleValeur, 60));
                        apresLbl.setStyle("-fx-font-size:11; -fx-text-fill:#27ae60; -fx-wrap-text:true;");
                        apresLbl.setWrapText(true);
                        diffRow.getChildren().add(apresLbl);
                    }
                    card.getChildren().add(diffRow);
                }
                body.getChildren().add(card);
            }
        }

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 20, 18, 20));
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#2980b9; -fx-text-fill:white; -fx-font-weight:bold; " +
                "-fx-background-radius:20; -fx-padding:10 40; -fx-cursor:hand;");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        root.getChildren().addAll(header, scroll, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // Fenêtre de signature électronique médecin
    private void showSignatureMedecinDialog(String ordonnanceId, String numeroOrdonnance) {
        // Vérifier si déjà signé
        boolean[] sigs = org.example.util.ElectronicSignatureService.getInstance()
                .verifierSignatures(Integer.parseInt(ordonnanceId));

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Signature Électronique Médecin — " + numeroOrdonnance);
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(500);

        // En-tête
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(22, 20, 16, 20));
        header.setStyle("-fx-background-color: #1f6f5c;");
        Label titleLbl = new Label("✍  Signature Électronique Médecin");
        titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label("Ordonnance : " + numeroOrdonnance);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.8);");
        header.getChildren().addAll(titleLbl, subLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 10, 24));

        // Statut signature existante
        if (sigs[0]) {
            HBox alreadySigned = new HBox(8);
            alreadySigned.setAlignment(Pos.CENTER_LEFT);
            alreadySigned.setPadding(new Insets(10, 14, 10, 14));
            alreadySigned.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 8;");
            Label ok = new Label("✅  Cette ordonnance est déjà signée par le médecin.");
            ok.setStyle("-fx-font-size: 13; -fx-text-fill: #155724; -fx-font-weight: bold;");
            alreadySigned.getChildren().add(ok);
            body.getChildren().add(alreadySigned);
        }

        // Champ nom du médecin
        Label nomLbl = new Label("Nom du médecin signataire :");
        nomLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextField nomField = new TextField();
        nomField.setPromptText("Dr. Nom Prénom");
        nomField.setStyle("-fx-font-size: 13; -fx-padding: 8; -fx-background-radius: 8;");

        // Canvas de signature manuscrite
        Label canvasLbl = new Label("Tracez votre signature ci-dessous :");
        canvasLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");

        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(450, 140);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, 450, 140);
        gc.setStroke(javafx.scene.paint.Color.web("#1f6f5c"));
        gc.setLineWidth(2.5);

        // Ligne de base
        gc.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeLine(20, 110, 430, 110);
        gc.setStroke(javafx.scene.paint.Color.web("#1f6f5c"));
        gc.setLineWidth(2.5);

        final double[] lastPos = {-1, -1};
        canvas.setOnMousePressed(e -> { lastPos[0] = e.getX(); lastPos[1] = e.getY(); });
        canvas.setOnMouseDragged(e -> {
            gc.strokeLine(lastPos[0], lastPos[1], e.getX(), e.getY());
            lastPos[0] = e.getX(); lastPos[1] = e.getY();
        });
        canvas.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Bouton effacer
        Button clearBtn = new Button("🗑 Effacer");
        clearBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 11; " +
                "-fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillRect(0, 0, 450, 140);
            gc.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
            gc.setLineWidth(1);
            gc.strokeLine(20, 110, 430, 110);
            gc.setStroke(javafx.scene.paint.Color.web("#1f6f5c"));
            gc.setLineWidth(2.5);
        });

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");

        body.getChildren().addAll(nomLbl, nomField, canvasLbl, canvas, clearBtn, errLbl);

        // Footer boutons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 20, 24));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button signBtn = new Button("✍  Signer l'ordonnance");
        signBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-size: 13;");
        signBtn.setOnAction(e -> {
            String nom = nomField.getText() != null ? nomField.getText().trim() : "";
            if (nom.isEmpty()) { errLbl.setText("Veuillez saisir le nom du médecin."); return; }

            // Capturer le canvas en base64 comme données de signature
            javafx.scene.image.WritableImage snapshot = canvas.snapshot(null, null);
            String signatureData = nom + "|" + System.currentTimeMillis();

            org.example.util.ElectronicSignatureService.SignatureResult result =
                    org.example.util.ElectronicSignatureService.getInstance()
                            .signer(numeroOrdonnance, nom, "medecin", signatureData);

            if (!result.success) { errLbl.setText(result.message); return; }

            boolean saved = org.example.util.ElectronicSignatureService.getInstance()
                    .sauvegarderSignatureMedecin(Integer.parseInt(ordonnanceId), result);

            if (saved) {
                dialog.close();
                showList(); // Rafraîchir la table pour afficher la signature dans la colonne
                showSignatureSuccessDialog("médecin", nom, result.signedAt, result.signatureHash);
            } else {
                errLbl.setText("Erreur lors de la sauvegarde de la signature.");
            }
        });

        footer.getChildren().addAll(cancelBtn, signBtn);
        root.getChildren().addAll(header, body, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // Fenêtre de confirmation après signature réussie
    private void showSignatureSuccessDialog(String role, String nom, String signedAt, String hash) {
        javafx.stage.Stage d = new javafx.stage.Stage();
        d.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        d.setTitle("Signature enregistrée");
        d.setResizable(false);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: white;");
        root.setMinWidth(420);

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 48;");
        Label title = new Label("Signature enregistrée !");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");

        VBox details = new VBox(6);
        details.setPadding(new Insets(12, 16, 12, 16));
        details.setStyle("-fx-background-color: #f0f7f4; -fx-background-radius: 10;");
        details.getChildren().addAll(
            styledDetail("Signataire", nom),
            styledDetail("Rôle", role),
            styledDetail("Date & heure", signedAt),
            styledDetail("Hash SHA-256", hash.substring(0, Math.min(32, hash.length())) + "...")
        );

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 40; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> d.close());

        root.getChildren().addAll(icon, title, details, closeBtn);
        d.setScene(new javafx.scene.Scene(root));
        d.showAndWait();
    }

    private javafx.scene.layout.HBox styledDetail(String label, String value) {
        javafx.scene.layout.HBox line = new javafx.scene.layout.HBox(8);
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555; -fx-min-width: 100;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12; -fx-text-fill: #333; -fx-wrap-text: true;");
        val.setMaxWidth(260);
        val.setWrapText(true);
        line.getChildren().addAll(lbl, val);
        return line;
    }

    // Afficher la page de statistiques avec PieCharts et cartes récapitulatives
    private void showStats() {
        pageContainer.getChildren().clear();
        VBox page = new VBox(20); page.setPadding(new Insets(30)); page.setStyle("-fx-background-color: #f5f5f5;");

        HBox hdr = new HBox(15); hdr.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("\u2190 Retour \u00e0 la liste");
        backBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        backBtn.setOnAction(e -> showList());
        Label title = new Label("\uD83D\uDCCA Statistiques G\u00e9n\u00e9rales - Ordonnances & Traitements");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        hdr.getChildren().addAll(backBtn, title);

        int totalOrd=0, ordBrouillon=0, ordAttente=0, ordValidee=0, ordExpiree=0;
        int totalTrait=0, traitActif=0, traitAttente=0, totalPatients=0;
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance"); if(rs.next()) totalOrd=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='brouillon'"); if(rs.next()) ordBrouillon=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='en_attente'"); if(rs.next()) ordAttente=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='valid\u00e9e'"); if(rs.next()) ordValidee=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='expir\u00e9e'"); if(rs.next()) ordExpiree=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement"); if(rs.next()) totalTrait=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='actif'"); if(rs.next()) traitActif=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='en_attente'"); if(rs.next()) traitAttente=rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(DISTINCT id_utilisateur_id) AS c FROM ordonnance"); if(rs.next()) totalPatients=rs.getInt("c"); rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        HBox row1 = new HBox(20); row1.setAlignment(Pos.CENTER);
        row1.getChildren().addAll(
            statCard("\uD83D\uDCC4", String.valueOf(totalOrd), "Total Ordonnances", "#2980b9", "white"),
            statCard("\uD83D\uDC8A", String.valueOf(totalTrait), "Total Traitements", "#1f6f5c", "white"),
            statCard("\uD83D\uDC64", String.valueOf(totalPatients), "Patients", "#8e44ad", "white")
        );

        Label ordTitle = new Label("\uD83D\uDCC4 R\u00e9partition des Ordonnances");
        ordTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        HBox row2 = new HBox(20); row2.setAlignment(Pos.CENTER);
        row2.getChildren().addAll(
            statCard("\uD83D\uDCDD", String.valueOf(ordBrouillon), "Brouillon", "#95a5a6", "white"),
            statCard("\u23F3", String.valueOf(ordAttente), "En attente", "#f39c12", "white"),
            statCard("\u2705", String.valueOf(ordValidee), "Valid\u00e9es", "#27ae60", "white"),
            statCard("\u26A0", String.valueOf(ordExpiree), "Expir\u00e9es", "#e74c3c", "white")
        );

        Label traitTitle = new Label("\uD83D\uDC8A Traitements associ\u00e9s");
        traitTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        HBox row3 = new HBox(20); row3.setAlignment(Pos.CENTER);
        row3.getChildren().addAll(
            statCard("\u23F3", String.valueOf(traitAttente), "En attente", "#f39c12", "white"),
            statCard("\u2705", String.valueOf(traitActif), "Actifs", "#27ae60", "white")
        );

        // Dernières ordonnances
        Label recentTitle = new Label("\uD83D\uDD52 Derni\u00e8res ordonnances cr\u00e9\u00e9es");
        recentTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        VBox recentCard = new VBox(8); recentCard.setPadding(new Insets(20));
        recentCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0.3, 0, 2);");
        try {
            ResultSet rs = DatabaseUtil.getInstance().getConnection().createStatement().executeQuery(
                "SELECT o.numero_ordonnance, u.nom, o.statut, o.date_ordonnance FROM ordonnance o LEFT JOIN utilisateur u ON o.id_utilisateur_id=u.id_utilisateur ORDER BY o.id_ordonnance DESC LIMIT 5");
            while (rs.next()) {
                String date = rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate().toString() : "";
                Label row = new Label("\uD83D\uDCC4 " + rs.getString("numero_ordonnance") + " \u2014 " + (rs.getString("nom")!=null?rs.getString("nom"):"N/A") + " \u2014 " + rs.getString("statut") + " \u2014 " + date);
                row.setStyle("-fx-font-size: 13; -fx-text-fill: #333; -fx-padding: 5 10;");
                recentCard.getChildren().add(row);
            } rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        if (recentCard.getChildren().isEmpty()) recentCard.getChildren().add(new Label("Aucune donn\u00e9e"));

        page.getChildren().addAll(hdr);

        // PieChart Ordonnances
        javafx.scene.chart.PieChart ordPie = new javafx.scene.chart.PieChart(
                javafx.collections.FXCollections.observableArrayList(
                        new javafx.scene.chart.PieChart.Data("Brouillon (" + ordBrouillon + ")", ordBrouillon),
                        new javafx.scene.chart.PieChart.Data("En attente (" + ordAttente + ")", ordAttente),
                        new javafx.scene.chart.PieChart.Data("Validées (" + ordValidee + ")", ordValidee),
                        new javafx.scene.chart.PieChart.Data("Expirées (" + ordExpiree + ")", ordExpiree)
                ));
        ordPie.setTitle("Répartition des Ordonnances (en %)");
        ordPie.setLabelsVisible(true);
        ordPie.setLegendVisible(true);
        ordPie.setPrefHeight(350);

        // PieChart Traitements
        javafx.scene.chart.PieChart traitPie = new javafx.scene.chart.PieChart(
                javafx.collections.FXCollections.observableArrayList(
                        new javafx.scene.chart.PieChart.Data("En attente (" + traitAttente + ")", traitAttente),
                        new javafx.scene.chart.PieChart.Data("Actifs (" + traitActif + ")", traitActif)
                ));
        traitPie.setTitle("Répartition des Traitements (en %)");
        traitPie.setLabelsVisible(true);
        traitPie.setLegendVisible(true);
        traitPie.setPrefHeight(350);

        HBox chartsRow = new HBox(30); chartsRow.setAlignment(Pos.CENTER);
        chartsRow.getChildren().addAll(ordPie, traitPie);

        page.getChildren().addAll(chartsRow, recentTitle, recentCard);
        ScrollPane sc = new ScrollPane(page); sc.setFitToWidth(true); sc.setStyle("-fx-background-color: #f5f5f5;");
        pageContainer.getChildren().add(sc); VBox.setVgrow(sc, Priority.ALWAYS);
    }

    // Créer une carte statistique colorée avec icône, valeur et label
    private VBox statCard(String icon, String value, String label, String bgColor, String textColor) {
        VBox card = new VBox(5); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(20, 30, 20, 30));
        card.setMinWidth(180); card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.3, 0, 3);");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 28;");
        Label val = new Label(value); val.setStyle("-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        Label lbl = new Label(label); lbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        card.getChildren().addAll(ic, val, lbl);
        return card;
    }

    @FXML private void goToDashboard() throws IOException { nav("/fxml/Dashboard.fxml"); } // Navigation vers le tableau de bord
    @FXML private void goToTraitements() throws IOException { nav("/fxml/BackTraitement.fxml"); } // Navigation vers la gestion des traitements
    @FXML private void logout() throws IOException { nav("/fxml/Login.fxml"); } // Déconnexion et retour au login
    // Méthode utilitaire de navigation entre les pages
    private void nav(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Scene scene = new Scene(root); scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) pageContainer.getScene().getWindow(); stage.setScene(scene); stage.setFullScreen(true);
    }
}
