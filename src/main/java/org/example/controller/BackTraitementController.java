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

// Contrôleur back-office pour la gestion des traitements par l'administrateur (CRUD + statistiques + PieChart)
public class BackTraitementController {

    @FXML private VBox pageContainer; // Conteneur principal de la page (remplacé dynamiquement)

    @FXML
    public void initialize() {
        org.example.util.AuditService.getInstance().initTable();
        showList();
    }

    public void openNewForm() { showForm(null); } // Ouvrir le formulaire d'ajout (appelé depuis le Dashboard)

    private void showList() {
        pageContainer.getChildren().clear();
        HBox greenBar = new HBox(); greenBar.setMinHeight(6); greenBar.setStyle("-fx-background-color: #1f6f5c;");
        VBox header = new VBox(5); header.setPadding(new Insets(25, 30, 25, 30));
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0.2, 0, 2);");
        Label t = new Label("Gestion des Traitements"); t.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label s = new Label("Liste de tous les traitements"); s.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
        header.getChildren().addAll(t, s);
        HBox actionBar = new HBox(15); actionBar.setPadding(new Insets(20, 30, 10, 30)); actionBar.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("+ Nouveau Traitement");
        addBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showForm(null));
        Button statsBtn = new Button("\u2699 Statistiques");
        statsBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        statsBtn.setOnAction(e -> showStats());
        actionBar.getChildren().addAll(addBtn, statsBtn);

        // Filters
        VBox filterCard = new VBox(10); filterCard.setPadding(new Insets(15, 30, 15, 30));
        filterCard.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0.2, 0, 2);");
        VBox.setMargin(filterCard, new Insets(10, 30, 10, 30));
        HBox filters = new HBox(15); filters.setAlignment(Pos.CENTER_LEFT);
        DatePicker dd = new DatePicker(); dd.setPromptText("Date d\u00e9but"); dd.setPrefWidth(150);
        DatePicker df = new DatePicker(); df.setPromptText("Date fin"); df.setPrefWidth(150);
        TextField cf = new TextField(); cf.setPromptText("Client"); cf.setPrefWidth(160);
        TextField pf = new TextField(); pf.setPromptText("Produit"); pf.setPrefWidth(140);
        ComboBox<String> sf = new ComboBox<>(FXCollections.observableArrayList("Tous les statuts","en_attente","actif","termin\u00e9","annul\u00e9"));
        sf.setValue("Tous les statuts"); sf.setPrefWidth(140);
        ComboBox<String> tf2 = new ComboBox<>(FXCollections.observableArrayList("Plus r\u00e9cent","Plus ancien"));
        tf2.setValue("Plus r\u00e9cent"); tf2.setPrefWidth(130);
        filters.getChildren().addAll(dd, df, cf, pf, sf, tf2);
        filterCard.getChildren().addAll(new Label("\uD83D\uDD0D Filtrer par :"), filters);

        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("modern-table"); table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); table.setMinHeight(400);
        String[] hdrs = {"ID","Ordonnance","Patient","Produit","Dosage","Fr\u00e9quence","Dur\u00e9e (jours)","Date D\u00e9but","Date Fin","Statut","Actions"};
        for (int i = 0; i < hdrs.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> c = new TableColumn<>(hdrs[i]);
            c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(col < p.getValue().size() ? p.getValue().get(col) : ""));
            // Largeur fixe pour la colonne Actions
            if (i == hdrs.length - 1) {
                c.setMinWidth(310);
                c.setPrefWidth(340);
                c.setResizable(false);
                c.setCellFactory(tc -> new TableCell<>() {
                    final Button eb = new Button("Modifier");
                    final Button db = new Button("Supprimer");
                    final Button histBtn = new Button("📋 Historique");
                    final Button predBtn = new Button("🧠 Prédiction");
                    final HBox bx = new HBox(5, eb, predBtn, histBtn, db);
                    {
                        eb.setStyle("-fx-background-color:#f39c12;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                        db.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                        histBtn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                        predBtn.setStyle("-fx-background-color:#6c3483;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                        eb.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()).get(0)));
                        predBtn.setOnAction(e -> {
                            ObservableList<String> row = getTableView().getItems().get(getIndex());
                            String patientNom = row.size() > 2 ? row.get(2) : "Patient";
                            int traitId = Integer.parseInt(row.get(0));
                            showAdherenceDialog(traitId, patientNom);
                        });
                        histBtn.setOnAction(e -> {
                            ObservableList<String> row = getTableView().getItems().get(getIndex());
                            String label = row.size() > 3 ? row.get(3) : "Traitement #" + row.get(0);
                            showHistoriqueDialog("traitement", row.get(0), label);
                        });
                        db.setOnAction(e -> {
                            String id = getTableView().getItems().get(getIndex()).get(0);
                            ObservableList<String> row = getTableView().getItems().get(getIndex());
                            String produitNom = row.size() > 3 ? row.get(3) : "?";
                            String dosage    = row.size() > 4 ? row.get(4) : "";
                            String statut    = row.size() > 9 ? row.get(9) : "";
                            if (org.example.util.DialogService.showDeleteConfirmation("le traitement #" + id + " (" + produitNom + ")")) {
                                try {
                                    org.example.util.AuditService.getInstance().logSuppression(
                                        "traitement", id,
                                        "Produit=" + produitNom + " | dosage=" + dosage + " | statut=" + statut,
                                        "Admin");
                                    PreparedStatement p2 = DatabaseUtil.getInstance().getConnection().prepareStatement("DELETE FROM traitement WHERE id_traitement=?");
                                    p2.setInt(1, Integer.parseInt(id)); p2.executeUpdate(); p2.close(); showList();
                                } catch (SQLException ex) { org.example.util.DialogService.showError("Erreur", ex.getMessage()); }
                            }
                        });
                    }
                    @Override protected void updateItem(String item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : bx); }
                });
            }
            table.getColumns().add(c);
        }
        loadData(table, null, null, "", "", "Tous les statuts", "Plus r\u00e9cent");
        Runnable apply = () -> loadData(table, dd.getValue(), df.getValue(), cf.getText().trim(), pf.getText().trim(), sf.getValue(), tf2.getValue());
        dd.valueProperty().addListener((o,a,b) -> apply.run()); df.valueProperty().addListener((o,a,b) -> apply.run());
        cf.textProperty().addListener((o,a,b) -> apply.run()); pf.textProperty().addListener((o,a,b) -> apply.run());
        sf.valueProperty().addListener((o,a,b) -> apply.run()); tf2.valueProperty().addListener((o,a,b) -> apply.run());
        VBox tw = new VBox(table); tw.setPadding(new Insets(0, 30, 30, 30)); VBox.setVgrow(table, Priority.ALWAYS);
        pageContainer.getChildren().addAll(greenBar, header, actionBar, filterCard, tw);
    }

    // Charger les données de la table avec filtres dynamiques, groupées par ordonnance
    private void loadData(TableView<ObservableList<String>> table, LocalDate dD, LocalDate dF, String cl, String pr, String st, String tri) {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try {
            // Requête groupée par ordonnance : concatène les produits, dosages, fréquences
            StringBuilder sql = new StringBuilder(
                "SELECT MIN(t.id_traitement) AS id_traitement, o.numero_ordonnance, u.nom AS unom, " +
                "GROUP_CONCAT(p.nom SEPARATOR ', ') AS pnoms, " +
                "GROUP_CONCAT(IFNULL(t.dosage,'') SEPARATOR ', ') AS dosages, " +
                "GROUP_CONCAT(IFNULL(t.frequence,'') SEPARATOR ', ') AS frequences, " +
                "MAX(t.duree_jours) AS duree_jours, MIN(t.date_debut) AS date_debut, MAX(t.date_fin) AS date_fin, " +
                "t.status, t.id_ordonnance_id " +
                "FROM traitement t " +
                "LEFT JOIN utilisateur u ON t.id_utilisateur_id=u.id_utilisateur " +
                "LEFT JOIN produit p ON t.id_produit_id=p.id_produit " +
                "LEFT JOIN ordonnance o ON t.id_ordonnance_id=o.id_ordonnance " +
                "WHERE 1=1 ");
            java.util.List<Object> params = new java.util.ArrayList<>();
            if (dD != null) { sql.append("AND t.date_debut >= ? "); params.add(Timestamp.valueOf(dD.atStartOfDay())); }
            if (dF != null) { sql.append("AND t.date_debut <= ? "); params.add(Timestamp.valueOf(dF.atTime(23,59,59))); }
            if (!cl.isEmpty()) { sql.append("AND u.nom LIKE ? "); params.add("%" + cl + "%"); }
            if (!pr.isEmpty()) { sql.append("AND p.nom LIKE ? "); params.add("%" + pr + "%"); }
            if (st != null && !"Tous les statuts".equals(st)) { sql.append("AND t.status = ? "); params.add(st); }
            sql.append("GROUP BY t.id_ordonnance_id, o.numero_ordonnance, u.nom, t.status ");
            sql.append("ORDER BY MIN(t.id_traitement) ").append("Plus ancien".equals(tri) ? "ASC" : "DESC");
            PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                else ps.setString(i + 1, (String) p);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("id_traitement"))); // ID du premier traitement (pour modifier)
                row.add(rs.getString("numero_ordonnance") != null ? rs.getString("numero_ordonnance") : "N/A");
                row.add(rs.getString("unom") != null ? rs.getString("unom") : "N/A");
                row.add(rs.getString("pnoms") != null ? rs.getString("pnoms") : "N/A"); // Produits concaténés
                row.add(rs.getString("dosages") != null ? rs.getString("dosages") : ""); // Dosages concaténés
                row.add(rs.getString("frequences") != null ? rs.getString("frequences") : ""); // Fréquences concaténées
                row.add(String.valueOf(rs.getInt("duree_jours")));
                row.add(rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getString("status") != null ? rs.getString("status") : "");
                row.add(""); data.add(row);
            } rs.close(); ps.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        table.setItems(data);
        if (data.isEmpty()) table.setPlaceholder(new Label("Aucun traitement trouv\u00e9"));
    }

    // Afficher le formulaire d'ajout ou de modification d'un traitement (sélection multiple de produits)
    private void showForm(String editId) {
        pageContainer.getChildren().clear(); // Vider le conteneur
        boolean isEdit = editId != null; // Mode édition si un ID est fourni

        VBox card = new VBox(0);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0.3, 0, 3);");

        HBox hdr = new HBox(12); hdr.setAlignment(Pos.CENTER_LEFT); hdr.setPadding(new Insets(25, 30, 15, 30));
        Label ic = new Label("\uD83D\uDC8A"); ic.setStyle("-fx-font-size: 28; -fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 10;");
        Label ti = new Label(isEdit ? "Modifier Traitement #" + editId : "Ajouter un Traitement");
        ti.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        hdr.getChildren().addAll(ic, ti);

        VBox form = new VBox(12); form.setPadding(new Insets(15, 30, 25, 30));

        ComboBox<String> ordC = new ComboBox<>(); ordC.setPrefWidth(220); ordC.setPromptText("S\u00e9lectionner une ordonnance");
        ComboBox<String> prodC = new ComboBox<>(); prodC.setPrefWidth(220); prodC.setPromptText("S\u00e9lectionner un produit");
        // Structure : chaque produit a ses propres champs dosage/fréquence/repas
        java.util.List<java.util.Map<String, Object>> produitEntries = new java.util.ArrayList<>();
        VBox produitsFieldsBox = new VBox(10); // Conteneur des blocs produit

        // Rafraîchir l'affichage des blocs produit avec champs individuels
        Runnable[] refreshRef = new Runnable[1]; // Wrapper pour auto-référence
        refreshRef[0] = () -> {
            produitsFieldsBox.getChildren().clear();
            for (java.util.Map<String, Object> entry : new java.util.ArrayList<>(produitEntries)) {
                String prodName = ((String) entry.get("produit")).split(" - ")[1];
                VBox block = new VBox(6);
                block.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 12; -fx-border-color: #1f6f5c; -fx-border-radius: 10; -fx-border-width: 1;");
                HBox titleRow = new HBox(10); titleRow.setAlignment(Pos.CENTER_LEFT);
                Label prodLabel = new Label("\uD83D\uDC8A " + prodName);
                prodLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                Button rmBtn = new Button("\u00d7"); rmBtn.getStyleClass().add("traitement-btn-remove");
                final java.util.Map<String, Object> ref = entry;
                rmBtn.setOnAction(ev -> { produitEntries.remove(ref); refreshRef[0].run(); });
                titleRow.getChildren().addAll(prodLabel, sp, rmBtn);
                HBox fieldsRow = new HBox(15);
                VBox dBox = new VBox(3, new Label("Dosage"), (TextField) entry.get("dosage")); HBox.setHgrow(dBox, Priority.ALWAYS);
                VBox fBox = new VBox(3, new Label("Fr\u00e9quence"), (TextField) entry.get("frequence")); HBox.setHgrow(fBox, Priority.ALWAYS);
                fieldsRow.getChildren().addAll(dBox, fBox);
                VBox rBox = new VBox(3, new Label("Repas"), (ComboBox<?>) entry.get("repas"));
                block.getChildren().addAll(titleRow, fieldsRow, rBox);
                produitsFieldsBox.getChildren().add(block);
            }
        };

        Button addProdBtn = new Button("+"); addProdBtn.getStyleClass().add("traitement-btn-add");
        addProdBtn.setOnAction(ev -> {
            String sel = prodC.getValue();
            if (sel == null) return;
            for (java.util.Map<String, Object> e2 : produitEntries) { if (sel.equals(e2.get("produit"))) return; }
            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("produit", sel);
            TextField dF = new TextField(); dF.setPromptText("Ex: 500mg");
            TextField fF = new TextField(); fF.setPromptText("Ex: 3 fois par jour");
            ComboBox<String> rC = new ComboBox<>(FXCollections.observableArrayList("Avant le repas","Pendant le repas","Apr\u00e8s le repas","En dehors des repas"));
            rC.setPrefWidth(220); rC.setPromptText("-- Moment du repas --");
            entry.put("dosage", dF); entry.put("frequence", fF); entry.put("repas", rC);
            produitEntries.add(entry);
            refreshRef[0].run();
            prodC.setValue(null);
        });
        HBox prodRow = new HBox(10, prodC, addProdBtn); prodRow.setAlignment(Pos.CENTER_LEFT); javafx.scene.layout.HBox.setHgrow(prodC, Priority.ALWAYS);
        ComboBox<String> userC = new ComboBox<>(); userC.setPrefWidth(220); userC.setPromptText("S\u00e9lectionner un patient");
        TextArea notesF = new TextArea(); notesF.setPromptText("Exemple:\nAnt\u00e9c\u00e9dents: Allergie au parac\u00e9tamol\nSympt\u00f4mes: Maux de t\u00eate\nPrescription: Ibuprof\u00e8ne 400mg");
        notesF.setPrefRowCount(5); notesF.setWrapText(true);

        TextField dureeF = new TextField("7"); dureeF.setPromptText("7");

        HBox r2 = new HBox(20);
        VBox ddB = new VBox(4); DatePicker ddP = new DatePicker(LocalDate.now()); ddB.getChildren().addAll(new Label("Date de début"), ddP);
        VBox dfB = new VBox(4); DatePicker dfP = new DatePicker(LocalDate.now().plusDays(7)); dfB.getChildren().addAll(new Label("Date de fin (auto-calculée)"), dfP);
        r2.getChildren().addAll(ddB, dfB); HBox.setHgrow(ddB, Priority.ALWAYS); HBox.setHgrow(dfB, Priority.ALWAYS);

        // Calcul automatique : date fin = date début + durée
        Runnable calcDateFin = () -> {
            if (ddP.getValue() != null && !dureeF.getText().trim().isEmpty()) {
                try {
                    int d = Integer.parseInt(dureeF.getText().trim());
                    if (d > 0) dfP.setValue(ddP.getValue().plusDays(d));
                } catch (NumberFormatException ignored) {}
            }
        };
        dureeF.textProperty().addListener((o, a, b) -> calcDateFin.run());
        ddP.valueProperty().addListener((o, a, b) -> calcDateFin.run());

        HBox banner = new HBox(8); banner.setAlignment(Pos.CENTER_LEFT); banner.setPadding(new Insets(10, 15, 10, 15));
        banner.setStyle("-fx-background-color: #e8f4fd; -fx-background-radius: 8; -fx-border-color: #bee5eb; -fx-border-radius: 8;");
        banner.getChildren().addAll(new Label("\u2139"), new Label("Le statut sera automatiquement d\u00e9fini \u00e0 \"En attente\" lors de la cr\u00e9ation."));

        ComboBox<String> statC = new ComboBox<>(FXCollections.observableArrayList("en_attente","actif","termin\u00e9","annul\u00e9"));
        statC.setPrefWidth(220); statC.setValue("en_attente");
        Label err = new Label(); err.setStyle("-fx-text-fill: #e74c3c;");

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT id_utilisateur, nom, prenom, email FROM utilisateur ORDER BY nom");
            while (rs.next()) userC.getItems().add(rs.getInt(1) + " - " + rs.getString(2) + " " + rs.getString(3) + " (" + rs.getString(4) + ")");
            rs.close();
            rs = conn.createStatement().executeQuery("SELECT id_produit, nom FROM produit ORDER BY nom");
            while (rs.next()) prodC.getItems().add(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
            if (isEdit) {
                // En modification, afficher toutes les ordonnances (pour garder la valeur actuelle)
                rs = conn.createStatement().executeQuery("SELECT id_ordonnance, numero_ordonnance FROM ordonnance ORDER BY id_ordonnance DESC");
            } else {
                // En ajout, afficher uniquement les ordonnances validées sans traitement
                rs = conn.createStatement().executeQuery(
                        "SELECT o.id_ordonnance, o.numero_ordonnance FROM ordonnance o " +
                        "WHERE o.statut = 'validée' " +
                        "AND NOT EXISTS (SELECT 1 FROM traitement t WHERE t.id_ordonnance_id = o.id_ordonnance) " +
                        "ORDER BY o.id_ordonnance DESC");
            }
            while (rs.next()) ordC.getItems().add(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        if (isEdit) {
            try {
                // Charger les infos communes depuis le premier traitement
                PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement("SELECT * FROM traitement WHERE id_traitement=?");
                ps.setInt(1, Integer.parseInt(editId)); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    dureeF.setText(String.valueOf(rs.getInt("duree_jours")));
                    notesF.setText(rs.getString("notes") != null ? rs.getString("notes") : "");
                    statC.setValue(rs.getString("status"));
                    if (rs.getTimestamp("date_debut") != null) ddP.setValue(rs.getTimestamp("date_debut").toLocalDateTime().toLocalDate());
                    if (rs.getTimestamp("date_fin") != null) dfP.setValue(rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate());
                    String uid=String.valueOf(rs.getInt("id_utilisateur_id")), oid=String.valueOf(rs.getInt("id_ordonnance_id"));
                    for (String x : userC.getItems()) if (x.startsWith(uid+" -")) { userC.setValue(x); break; }
                    for (String x : ordC.getItems()) if (x.startsWith(oid+" -")) { ordC.setValue(x); break; }
                } rs.close(); ps.close();

                // Charger tous les produits liés à cette ordonnance avec leurs champs individuels
                PreparedStatement psProd = DatabaseUtil.getInstance().getConnection().prepareStatement(
                        "SELECT t.id_produit_id, t.dosage, t.frequence, t.repas FROM traitement t WHERE t.id_ordonnance_id = (SELECT id_ordonnance_id FROM traitement WHERE id_traitement = ?)");
                psProd.setInt(1, Integer.parseInt(editId));
                ResultSet rsProd = psProd.executeQuery();
                while (rsProd.next()) {
                    String pidStr = String.valueOf(rsProd.getInt("id_produit_id"));
                    String prodStr = null;
                    for (String item : prodC.getItems()) { if (item.startsWith(pidStr + " - ")) { prodStr = item; break; } }
                    if (prodStr != null) {
                        java.util.Map<String, Object> entry = new java.util.HashMap<>();
                        entry.put("produit", prodStr);
                        TextField dF = new TextField(rsProd.getString("dosage") != null ? rsProd.getString("dosage") : ""); dF.setPromptText("Ex: 500mg");
                        TextField fF = new TextField(rsProd.getString("frequence") != null ? rsProd.getString("frequence") : ""); fF.setPromptText("Ex: 3 fois par jour");
                        ComboBox<String> rC = new ComboBox<>(FXCollections.observableArrayList("Avant le repas","Pendant le repas","Apr\u00e8s le repas","En dehors des repas"));
                        rC.setPrefWidth(220); rC.setValue(rsProd.getString("repas"));
                        entry.put("dosage", dF); entry.put("frequence", fF); entry.put("repas", rC);
                        produitEntries.add(entry);
                    }
                }
                rsProd.close(); psProd.close();
                refreshRef[0].run();
            } catch (SQLException e) { System.out.println(e.getMessage()); }
        }

        // Bouton IA suggestion dosage/fréquence/repas/durée
        Button iaBtn = new Button("\uD83E\uDD16 Suggestion IA");
        iaBtn.setStyle("-fx-background-color: #6c3483; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        iaBtn.setOnAction(e -> {
            if (produitEntries.isEmpty()) { err.setText("Ajoutez au moins un produit avant de demander une suggestion IA."); return; }
            String notes = notesF.getText() != null ? notesF.getText().trim() : "";
            StringBuilder rapport = new StringBuilder();
            for (java.util.Map<String, Object> entry : produitEntries) {
                String prodStr = (String) entry.get("produit");
                int prodId = Integer.parseInt(prodStr.split(" - ")[0]);
                String prodNom = prodStr.split(" - ")[1];
                org.example.util.TreatmentSuggestionService.Suggestion sug =
                        org.example.util.TreatmentSuggestionService.getInstance().suggerer(prodId, notes);
                ((TextField) entry.get("dosage")).setText(sug.dosage);
                ((TextField) entry.get("frequence")).setText(sug.frequence);
                ((ComboBox<String>) entry.get("repas")).setValue(sug.repas);
                dureeF.setText(String.valueOf(sug.dureeJours));
                rapport.append("\uD83D\uDCE6 ").append(prodNom).append("\n")
                       .append("  Dosage    : ").append(sug.dosage).append("\n")
                       .append("  Fréquence : ").append(sug.frequence).append("\n")
                       .append("  Repas     : ").append(sug.repas).append("\n")
                       .append("  Durée     : ").append(sug.dureeJours).append(" jours\n")
                       .append("  Source    : ").append(sug.source).append("\n\n");
            }
            refreshRef[0].run();
            org.example.util.DialogService.showSuccess("🤖 Suggestion IA appliquée",
                "Les champs ont été remplis automatiquement.\n\n" + rapport.toString());
            err.setText("");
        });

        HBox btns = new HBox(15); btns.setAlignment(Pos.CENTER); btns.setPadding(new Insets(10, 0, 0, 0));
        Button cancel = new Button("\u2190 Retour"); cancel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        cancel.setOnAction(e -> showList());
        Button save = new Button(isEdit ? "\uD83D\uDCBE Mettre \u00e0 jour" : "\uD83D\uDCBE Ajouter");
        save.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        save.setOnAction(e -> {
            err.setText("");
            // Anti double-clic
            save.setDisable(true);

            // Contrôle : ordonnance obligatoire
            if (ordC.getValue() == null) {
                err.setText("Veuillez sélectionner une ordonnance.");
                save.setDisable(false);
                return;
            }

            // Contrôle : ordonnance doit être validée et sans traitement (en ajout)
            if (!isEdit) {
                try {
                    int ordIdCheck = Integer.parseInt(ordC.getValue().split(" - ")[0]);
                    PreparedStatement psOrdCheck = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT statut FROM ordonnance WHERE id_ordonnance = ?");
                    psOrdCheck.setInt(1, ordIdCheck);
                    ResultSet rsOrdCheck = psOrdCheck.executeQuery();
                    if (rsOrdCheck.next()) {
                        String statut = rsOrdCheck.getString("statut");
                        if (!"validée".equals(statut)) {
                            err.setText("L'ordonnance sélectionnée n'est pas validée (statut actuel : " + statut + ").");
                            save.setDisable(false);
                            rsOrdCheck.close(); psOrdCheck.close();
                            return;
                        }
                    }
                    rsOrdCheck.close(); psOrdCheck.close();

                    PreparedStatement psTraitCheck = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT COUNT(*) AS nb FROM traitement WHERE id_ordonnance_id = ?");
                    psTraitCheck.setInt(1, ordIdCheck);
                    ResultSet rsTraitCheck = psTraitCheck.executeQuery();
                    if (rsTraitCheck.next() && rsTraitCheck.getInt("nb") > 0) {
                        err.setText("Cette ordonnance a déjà un traitement associé.");
                        save.setDisable(false);
                        rsTraitCheck.close(); psTraitCheck.close();
                        return;
                    }
                    rsTraitCheck.close(); psTraitCheck.close();
                } catch (SQLException ex) {
                    err.setText("Erreur vérification ordonnance: " + ex.getMessage());
                    save.setDisable(false);
                    return;
                }
            }
            // Auto-ajouter le produit sélectionné dans le ComboBox si pas encore ajouté
            String pendingProd = prodC.getValue();
            if (pendingProd != null) {
                boolean exists = false;
                for (java.util.Map<String, Object> e2 : produitEntries) { if (pendingProd.equals(e2.get("produit"))) { exists = true; break; } }
                if (!exists) {
                    java.util.Map<String, Object> entry = new java.util.HashMap<>();
                    entry.put("produit", pendingProd);
                    TextField dF = new TextField(); dF.setPromptText("Ex: 500mg");
                    TextField fF = new TextField(); fF.setPromptText("Ex: 3 fois par jour");
                    ComboBox<String> rC = new ComboBox<>(FXCollections.observableArrayList("Avant le repas","Pendant le repas","Apr\u00e8s le repas","En dehors des repas"));
                    rC.setPrefWidth(220);
                    entry.put("dosage", dF); entry.put("frequence", fF); entry.put("repas", rC);
                    produitEntries.add(entry);
                    refreshRef[0].run();
                    prodC.setValue(null);
                }
            }
            if (produitEntries.isEmpty()) {
                err.setText("Veuillez ajouter au moins un produit.");
                save.setDisable(false);
                return;
            }
            // Contrôle : patient obligatoire
            if (userC.getValue() == null) {
                err.setText("Veuillez sélectionner un patient.");
                save.setDisable(false);
                return;
            }
            // Contrôle : dosage/fréquence/repas obligatoires pour chaque produit
            for (java.util.Map<String, Object> entry : produitEntries) {
                String pName = ((String) entry.get("produit")).split(" - ")[1];
                String dos = ((TextField) entry.get("dosage")).getText() != null ? ((TextField) entry.get("dosage")).getText().trim() : "";
                if (dos.isEmpty()) { err.setText("Dosage invalide pour " + pName); save.setDisable(false); return; }
                String dosNum = dos.replaceAll("[^0-9.]", "");
                if (dosNum.isEmpty()) { err.setText("Dosage invalide pour " + pName); save.setDisable(false); return; }
                try { double dv = Double.parseDouble(dosNum); if (dv <= 0) { err.setText("Dosage invalide pour " + pName); save.setDisable(false); return; } }
                catch (NumberFormatException ex2) { err.setText("Dosage invalide pour " + pName); save.setDisable(false); return; }
                String freq = ((TextField) entry.get("frequence")).getText() != null ? ((TextField) entry.get("frequence")).getText().trim() : "";
                if (freq.isEmpty() || freq.length() < 3) { err.setText("Fr\u00e9quence obligatoire pour " + pName); save.setDisable(false); return; }
                if (((ComboBox<?>) entry.get("repas")).getValue() == null) { err.setText("Repas obligatoire pour " + pName); save.setDisable(false); return; }
            }
            // Contrôle : durée doit être un nombre positif
            String dureeText = dureeF.getText() != null ? dureeF.getText().trim() : "";
            if (dureeText.isEmpty()) {
                err.setText("La durée en jours est obligatoire.");
                save.setDisable(false);
                return;
            }
            int dureeVal;
            try {
                dureeVal = Integer.parseInt(dureeText);
            } catch (NumberFormatException ex2) {
                err.setText("La durée doit être un nombre entier valide.");
                save.setDisable(false);
                return;
            }
            if (dureeVal <= 0) {
                err.setText("La durée doit être supérieure à 0 jours.");
                save.setDisable(false);
                return;
            }
            if (dureeVal > 365) {
                err.setText("La durée ne peut pas dépasser 365 jours.");
                save.setDisable(false);
                return;
            }
            // Contrôle : date début obligatoire
            if (ddP.getValue() == null) {
                err.setText("Date invalide");
                save.setDisable(false);
                return;
            }
            // Contrôle : date début pas dans le futur
            if (ddP.getValue().isAfter(java.time.LocalDate.now())) {
                err.setText("La date de début ne peut pas être dans le futur.");
                save.setDisable(false);
                return;
            }
            // Contrôle : date fin obligatoire
            if (dfP.getValue() == null) {
                err.setText("Date invalide");
                save.setDisable(false);
                return;
            }
            // Contrôle : date fin après date début
            if (!dfP.getValue().isAfter(ddP.getValue())) {
                err.setText("La date de fin doit être postérieure à la date de début.");
                save.setDisable(false);
                return;
            }
            // Contrôle : notes longueur max
            String notesText = notesF.getText() != null ? notesF.getText().trim() : "";
            if (notesText.length() > 2000) {
                err.setText("Les notes ne doivent pas dépasser 2000 caractères.");
                save.setDisable(false);
                return;
            }

            // Unicité : même patient + même produit (seulement pour les nouveaux produits en mode edit)
            try {
                int checkUserId = Integer.parseInt(userC.getValue().split(" - ")[0]);
                int currentOrdId = Integer.parseInt(ordC.getValue().split(" - ")[0]);
                // En mode edit, récupérer les produits déjà existants sur cette ordonnance
                java.util.Set<Integer> existingProdIds = new java.util.HashSet<>();
                if (isEdit) {
                    PreparedStatement psExist = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT id_produit_id FROM traitement WHERE id_ordonnance_id = ?");
                    psExist.setInt(1, currentOrdId);
                    ResultSet rsExist = psExist.executeQuery();
                    while (rsExist.next()) existingProdIds.add(rsExist.getInt("id_produit_id"));
                    rsExist.close(); psExist.close();
                }
                for (java.util.Map<String, Object> entry : produitEntries) {
                    int checkProdId = Integer.parseInt(((String) entry.get("produit")).split(" - ")[0]);
                    // En mode edit, ignorer les produits déjà liés à cette ordonnance
                    if (isEdit && existingProdIds.contains(checkProdId)) continue;
                    PreparedStatement psDup = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT COUNT(*) AS nb FROM traitement WHERE id_utilisateur_id = ? AND id_produit_id = ? AND status IN ('en_attente','actif')");
                    psDup.setInt(1, checkUserId); psDup.setInt(2, checkProdId);
                    ResultSet rsDup = psDup.executeQuery();
                    if (rsDup.next() && rsDup.getInt("nb") > 0) {
                        err.setText("Le traitement \"" + ((String) entry.get("produit")).split(" - ")[1] + "\" existe d\u00e9j\u00e0 pour ce patient.");
                        save.setDisable(false); rsDup.close(); psDup.close(); return;
                    }
                    rsDup.close(); psDup.close();
                }
            } catch (SQLException ex) { err.setText("Erreur: " + ex.getMessage()); save.setDisable(false); return; }

            try {
                int duree = dureeVal;
                int userId = Integer.parseInt(userC.getValue().split(" - ")[0]);
                int ordId = Integer.parseInt(ordC.getValue().split(" - ")[0]);
                if (isEdit) {
                    // ── Lire les anciennes valeurs AVANT suppression pour l'audit ──
                    java.util.List<String> anciennesLignes = new java.util.ArrayList<>();
                    try {
                        PreparedStatement psOldT = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT p.nom, t.dosage, t.frequence, t.repas, t.duree_jours, t.status, t.notes " +
                            "FROM traitement t LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                            "WHERE t.id_ordonnance_id = ?");
                        psOldT.setInt(1, ordId);
                        ResultSet rsOldT = psOldT.executeQuery();
                        while (rsOldT.next()) {
                            anciennesLignes.add(
                                (rsOldT.getString("nom") != null ? rsOldT.getString("nom") : "?") +
                                " | dosage=" + (rsOldT.getString("dosage") != null ? rsOldT.getString("dosage") : "") +
                                " | freq=" + (rsOldT.getString("frequence") != null ? rsOldT.getString("frequence") : "") +
                                " | repas=" + (rsOldT.getString("repas") != null ? rsOldT.getString("repas") : "") +
                                " | durée=" + rsOldT.getInt("duree_jours") + "j" +
                                " | statut=" + (rsOldT.getString("status") != null ? rsOldT.getString("status") : "")
                            );
                        }
                        rsOldT.close(); psOldT.close();
                    } catch (Exception ignored) {}

                    PreparedStatement psDel = DatabaseUtil.getInstance().getConnection().prepareStatement("DELETE FROM traitement WHERE id_ordonnance_id = ?");
                    psDel.setInt(1, ordId); psDel.executeUpdate(); psDel.close();

                    // Audit suppression des anciens traitements
                    org.example.util.AuditService audit = org.example.util.AuditService.getInstance();
                    for (String ancienne : anciennesLignes) {
                        audit.log("traitement", editId, "SUPPRESSION",
                            "Traitement supprimé (remplacement)", ancienne, null, "Admin");
                    }

                    for (java.util.Map<String, Object> entry : produitEntries) {
                        int prodId = Integer.parseInt(((String) entry.get("produit")).split(" - ")[0]);
                        String dos = ((TextField) entry.get("dosage")).getText().trim();
                        String freq = ((TextField) entry.get("frequence")).getText().trim();
                        String repas = ((ComboBox<?>) entry.get("repas")).getValue() != null ? ((ComboBox<?>) entry.get("repas")).getValue().toString() : "";
                        PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement("INSERT INTO traitement (id_utilisateur_id,dosage,frequence,duree_jours,date_debut,date_fin,status,notes,id_ordonnance_id,id_produit_id,repas) VALUES (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                        ps.setInt(1, userId); ps.setString(2, dos); ps.setString(3, freq); ps.setInt(4, duree);
                        ps.setTimestamp(5, ddP.getValue() != null ? Timestamp.valueOf(ddP.getValue().atStartOfDay()) : null);
                        ps.setTimestamp(6, dfP.getValue() != null ? Timestamp.valueOf(dfP.getValue().atStartOfDay()) : null);
                        ps.setString(7, statC.getValue()); ps.setString(8, notesText);
                        ps.setInt(9, ordId); ps.setInt(10, prodId); ps.setString(11, repas); ps.executeUpdate();

                        // Audit création du nouveau traitement
                        ResultSet gk = ps.getGeneratedKeys();
                        if (gk.next()) {
                            String newId = String.valueOf(gk.getInt(1));
                            String prodNom = ((String) entry.get("produit")).contains(" - ") ? ((String) entry.get("produit")).split(" - ", 2)[1] : (String) entry.get("produit");
                            String nouvelleLigne = prodNom +
                                " | dosage=" + dos +
                                " | freq=" + freq +
                                " | repas=" + repas +
                                " | durée=" + duree + "j" +
                                " | statut=" + statC.getValue();
                            audit.log("traitement", newId, "CRÉATION",
                                "Nouveau traitement (remplacement)", null, nouvelleLigne, "Admin");
                        }
                        gk.close(); ps.close();
                    }
                } else {
                    String traitStatus = "en_attente";
                    PreparedStatement psCheck = DatabaseUtil.getInstance().getConnection().prepareStatement("SELECT statut FROM ordonnance WHERE id_ordonnance=?");
                    psCheck.setInt(1, ordId); ResultSet rsCheck = psCheck.executeQuery();
                    if (rsCheck.next() && "valid\u00e9e".equals(rsCheck.getString("statut"))) { traitStatus = "actif"; }
                    rsCheck.close(); psCheck.close();
                    for (java.util.Map<String, Object> entry : produitEntries) {
                        int prodId = Integer.parseInt(((String) entry.get("produit")).split(" - ")[0]);
                        String dos = ((TextField) entry.get("dosage")).getText().trim();
                        String freq = ((TextField) entry.get("frequence")).getText().trim();
                        String repas = ((ComboBox<?>) entry.get("repas")).getValue() != null ? ((ComboBox<?>) entry.get("repas")).getValue().toString() : "";
                        PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement("INSERT INTO traitement (id_utilisateur_id,dosage,frequence,duree_jours,date_debut,date_fin,status,notes,id_ordonnance_id,id_produit_id,repas) VALUES (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                        ps.setInt(1, userId); ps.setString(2, dos); ps.setString(3, freq); ps.setInt(4, duree);
                        ps.setTimestamp(5, ddP.getValue() != null ? Timestamp.valueOf(ddP.getValue().atStartOfDay()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps.setTimestamp(6, dfP.getValue() != null ? Timestamp.valueOf(dfP.getValue().atStartOfDay()) : null);
                        ps.setString(7, traitStatus); ps.setString(8, notesText);
                        ps.setInt(9, ordId); ps.setInt(10, prodId); ps.setString(11, repas); ps.executeUpdate();
                        // Audit création
                        ResultSet gk = ps.getGeneratedKeys();
                        if (gk.next()) {
                            String newId = String.valueOf(gk.getInt(1));
                            String prodNom = ((String) entry.get("produit")).contains(" - ") ? ((String) entry.get("produit")).split(" - ", 2)[1] : (String) entry.get("produit");
                            org.example.util.AuditService.getInstance().logCreation("traitement", newId,
                                prodNom + " | dosage=" + dos + " | freq=" + freq + " | statut=" + traitStatus, "Admin");
                        }
                        gk.close(); ps.close();
                    }
                }
                // Envoi email au patient après modification (mode edit uniquement)
                if (isEdit) {
                    try {
                        // Récupérer email + nom du patient
                        PreparedStatement psEmail = DatabaseUtil.getInstance().getConnection().prepareStatement(
                            "SELECT u.email, u.nom, u.prenom, o.numero_ordonnance FROM utilisateur u " +
                            "JOIN ordonnance o ON o.id_utilisateur_id = u.id_utilisateur " +
                            "WHERE u.id_utilisateur = ? AND o.id_ordonnance = ?");
                        psEmail.setInt(1, userId);
                        psEmail.setInt(2, ordId);
                        ResultSet rsEmail = psEmail.executeQuery();
                        if (rsEmail.next()) {
                            String email = rsEmail.getString("email");
                            String prenom = rsEmail.getString("prenom") != null ? rsEmail.getString("prenom") : "";
                            String nom = rsEmail.getString("nom") != null ? rsEmail.getString("nom") : "";
                            String numOrd = rsEmail.getString("numero_ordonnance");

                            // Construire le détail des produits
                            StringBuilder produitsHtml = new StringBuilder();
                            for (java.util.Map<String, Object> entry : produitEntries) {
                                String prodNom = ((String) entry.get("produit")).split(" - ")[1];
                                String dos = ((TextField) entry.get("dosage")).getText().trim();
                                String freq = ((TextField) entry.get("frequence")).getText().trim();
                                String rep = ((ComboBox<?>) entry.get("repas")).getValue() != null ? ((ComboBox<?>) entry.get("repas")).getValue().toString() : "-";
                                produitsHtml.append("<tr>")
                                    .append("<td style='padding:8px;border:1px solid #ddd;'>").append(prodNom).append("</td>")
                                    .append("<td style='padding:8px;border:1px solid #ddd;'>").append(dos).append("</td>")
                                    .append("<td style='padding:8px;border:1px solid #ddd;'>").append(freq).append("</td>")
                                    .append("<td style='padding:8px;border:1px solid #ddd;'>").append(rep).append("</td>")
                                    .append("<td style='padding:8px;border:1px solid #ddd;'>").append(duree).append(" jours</td>")
                                    .append("</tr>");
                            }

                            String htmlBody =
                                "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>" +
                                "<div style='background-color:#1f6f5c;padding:25px;text-align:center;'>" +
                                "<h1 style='color:white;margin:0;font-size:24px;'>CuraVita Pharmacie</h1>" +
                                "<p style='color:rgba(255,255,255,0.85);margin:5px 0 0;font-size:13px;'>Votre santé, notre priorité</p>" +
                                "</div>" +
                                "<div style='padding:30px;background:#fff;'>" +
                                "<p style='font-size:16px;color:#333;'>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
                                "<p style='font-size:14px;color:#555;line-height:1.6;'>Nous avons le plaisir de vous informer que votre ordonnance <strong style='color:#1f6f5c;'>" + numOrd + "</strong> a été traitée avec succès par notre équipe pharmaceutique. Votre traitement est désormais complet et prêt à être suivi.</p>" +
                                "<div style='background:#f0f8f5;border-left:4px solid #1f6f5c;padding:15px;border-radius:6px;margin:20px 0;'>" +
                                "<p style='margin:0;font-size:13px;color:#1f6f5c;font-weight:bold;'>📋 Détail de votre traitement :</p>" +
                                "</div>" +
                                "<table style='width:100%;border-collapse:collapse;font-size:13px;'>" +
                                "<thead><tr style='background:#1f6f5c;color:white;'>" +
                                "<th style='padding:10px;text-align:left;'>Produit</th>" +
                                "<th style='padding:10px;text-align:left;'>Dosage</th>" +
                                "<th style='padding:10px;text-align:left;'>Fréquence</th>" +
                                "<th style='padding:10px;text-align:left;'>Repas</th>" +
                                "<th style='padding:10px;text-align:left;'>Durée</th>" +
                                "</tr></thead><tbody>" + produitsHtml + "</tbody></table>" +
                                "<p style='font-size:14px;color:#555;margin-top:25px;line-height:1.6;'>Connectez-vous à votre espace <strong>CuraVita</strong> pour consulter l'intégralité de votre dossier médical et suivre l'évolution de vos traitements.</p>" +
                                "<p style='font-size:12px;color:#999;margin-top:30px;border-top:1px solid #eee;padding-top:15px;'>Cet email a été envoyé automatiquement par CuraVita Pharmacie. Merci de ne pas y répondre.</p>" +
                                "</div></div>";

                            org.example.util.EmailService.getInstance().send(
                                email,
                                "✅ Votre traitement est prêt — Ordonnance " + numOrd,
                                htmlBody
                            );
                        }
                        rsEmail.close(); psEmail.close();
                    } catch (Exception emailEx) {
                        System.err.println("[Email] Erreur : " + emailEx.getMessage());
                    }
                }
                showList();
            } catch (SQLException ex) {
                err.setText("Erreur: " + ex.getMessage());
                save.setDisable(false);
            }
        });
        btns.getChildren().addAll(cancel, iaBtn, save);

        form.getChildren().addAll(new Label("Ordonnance"), ordC, new Label("Produit(s)"), prodRow, produitsFieldsBox, new Label("Patient"), userC,
                new Label("Notes"), notesF, new Label("Dur\u00e9e (jours)"), dureeF, r2);
        if (isEdit) form.getChildren().addAll(new Label("Statut"), statC); else form.getChildren().add(banner);
        form.getChildren().addAll(err, new Separator(), btns);
        card.getChildren().addAll(hdr, new Separator(), form);

        VBox wrap = new VBox(card); wrap.setAlignment(Pos.TOP_CENTER); wrap.setPadding(new Insets(30)); wrap.setStyle("-fx-background-color: #f5f5f5;");
        ScrollPane sc = new ScrollPane(wrap); sc.setFitToWidth(true); sc.setStyle("-fx-background-color: #f5f5f5;");
        pageContainer.getChildren().add(sc); VBox.setVgrow(sc, Priority.ALWAYS);
    }

    // Fenêtre popup prédiction non-adhérence pour un traitement spécifique
    private void showAdherenceDialog(int traitId, String patientNom) {
        // Récupérer l'ID utilisateur depuis le traitement
        int userId = 0;
        try {
            PreparedStatement ps = DatabaseUtil.getInstance().getConnection()
                    .prepareStatement("SELECT id_utilisateur_id FROM traitement WHERE id_traitement=?");
            ps.setInt(1, traitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) userId = rs.getInt(1);
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        // Calculer le risque
        org.example.util.AdherencePredictor.PatientRisk risk = null;
        try {
            java.util.List<org.example.util.AdherencePredictor.PatientRisk> tous =
                    org.example.util.AdherencePredictor.getInstance().predireTousLesRisques();
            for (org.example.util.AdherencePredictor.PatientRisk r : tous) {
                if (r.userId == userId) { risk = r; break; }
            }
        } catch (Exception e) { System.err.println(e.getMessage()); }

        double score = risk != null ? risk.scoreRisque : 0;

        // Couleur et niveau selon score
        String headerColor, iconText, niveauText, bodyBg, borderColor;
        if (score >= 0.6) {
            headerColor = "#e74c3c"; iconText = "✕"; niveauText = "ÉLEVÉ";
            bodyBg = "#fdecea"; borderColor = "#e74c3c";
        } else if (score >= 0.3) {
            headerColor = "#e67e22"; iconText = "⚠️"; niveauText = "MODÉRÉ";
            bodyBg = "#fff8e1"; borderColor = "#f39c12";
        } else {
            headerColor = "#27ae60"; iconText = "✅"; niveauText = "FAIBLE";
            bodyBg = "#eafaf1"; borderColor = "#27ae60";
        }

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Prédiction Non-Adhérence");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(440); root.setMaxWidth(440);

        // ── En-tête coloré ────────────────────────────────────────────
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(22, 20, 16, 20));
        header.setStyle("-fx-background-color: " + headerColor + ";");

        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
        iconCircle.setMinSize(56, 56); iconCircle.setMaxSize(56, 56);
        iconCircle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 28;");
        Label iconLbl = new Label(iconText);
        iconLbl.setStyle("-fx-font-size: 24; -fx-text-fill: white;");
        iconCircle.getChildren().add(iconLbl);

        Label titleLbl = new Label("🧠 Prédiction Non-Adhérence");
        titleLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label("Patient : " + patientNom);
        subLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        header.getChildren().addAll(iconCircle, titleLbl, subLbl);

        // ── Corps ─────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(18, 24, 10, 24));

        // Bandeau niveau de risque
        VBox niveauBand = new VBox(6);
        niveauBand.setPadding(new Insets(12, 14, 12, 14));
        niveauBand.setStyle("-fx-background-color: " + bodyBg + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");

        Label niveauLbl = new Label("Niveau de risque : " + niveauText);
        niveauLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + headerColor + ";");

        // Barre de progression
        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(score);
        bar.setPrefWidth(370); bar.setPrefHeight(12);
        bar.setStyle("-fx-accent: " + headerColor + ";");

        Label scoreVal = new Label(String.format("Score : %.0f%%", score * 100));
        scoreVal.setStyle("-fx-font-size: 12; -fx-text-fill: " + headerColor + "; -fx-font-weight: bold;");
        niveauBand.getChildren().addAll(niveauLbl, bar, scoreVal);
        body.getChildren().add(niveauBand);

        // Facteurs détectés
        VBox factorsBox = new VBox(5);
        factorsBox.setPadding(new Insets(10, 14, 10, 14));
        factorsBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8;");
        Label factTitle = new Label("🔍 Facteurs détectés :");
        factTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        factorsBox.getChildren().add(factTitle);

        if (risk != null && !risk.facteurs.isEmpty()) {
            for (String f : risk.facteurs) {
                Label fl = new Label("  • " + f);
                fl.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
                fl.setMaxWidth(370); fl.setWrapText(true);
                factorsBox.getChildren().add(fl);
            }
        } else {
            Label fl = new Label("  • Aucun facteur de risque détecté");
            fl.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");
            factorsBox.getChildren().add(fl);
        }
        body.getChildren().add(factorsBox);

        // Recommandation
        String conseil;
        if (score >= 0.6) conseil = "Suivi renforcé recommandé. Contacter le patient rapidement.";
        else if (score >= 0.3) conseil = "Surveiller l'évolution du traitement de près.";
        else conseil = "Patient adhérent. Continuer le suivi normal.";

        VBox conseilBox = new VBox(4);
        conseilBox.setPadding(new Insets(10, 14, 10, 14));
        conseilBox.setStyle("-fx-background-color: #eaf4fb; -fx-background-radius: 8; " +
                "-fx-border-color: #2980b9; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
        Label conseilTitle = new Label("💡 Recommandation :");
        conseilTitle.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
        Label conseilLbl = new Label(conseil);
        conseilLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        conseilLbl.setMaxWidth(370); conseilLbl.setWrapText(true);
        conseilBox.getChildren().addAll(conseilTitle, conseilLbl);
        body.getChildren().add(conseilBox);

        // ── Footer ────────────────────────────────────────────────────
        VBox footer = new VBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 20, 24));
        Button closeBtn = new Button("Compris");
        closeBtn.setStyle("-fx-background-color: " + headerColor + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 50; " +
                "-fx-cursor: hand; -fx-font-size: 13;");
        closeBtn.setOnAction(ev -> dialog.close());
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, body, footer);
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    // Prédiction de non-adhérence au traitement
    private void showAdherencePrediction() {
        pageContainer.getChildren().clear();
        HBox greenBar = new HBox(); greenBar.setMinHeight(6); greenBar.setStyle("-fx-background-color: #6c3483;");

        VBox header = new VBox(5); header.setPadding(new Insets(25, 30, 25, 30));
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0.2, 0, 2);");
        Label t = new Label("\uD83E\uDDE0 Prédiction de Non-Adhérence");
        t.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #6c3483;");
        Label s = new Label("Patients à risque d'abandonner leur traitement — basé sur l'historique");
        s.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
        Button backBtn = new Button("\u2190 Retour");
        backBtn.setStyle("-fx-background-color: #eee; -fx-background-radius: 15; -fx-padding: 6 16; -fx-cursor: hand;");
        backBtn.setOnAction(e -> showList());
        header.getChildren().addAll(backBtn, t, s);

        java.util.List<org.example.util.AdherencePredictor.PatientRisk> risques =
                org.example.util.AdherencePredictor.getInstance().predireTousLesRisques();

        long nbEleve = risques.stream().filter(r -> "Élevé".equals(r.niveau)).count();
        long nbModere = risques.stream().filter(r -> "Modéré".equals(r.niveau)).count();
        long nbFaible = risques.stream().filter(r -> "Faible".equals(r.niveau)).count();

        HBox summary = new HBox(20); summary.setPadding(new Insets(20, 30, 10, 30)); summary.setAlignment(Pos.CENTER_LEFT);
        summary.getChildren().addAll(
            riskCard("\uD83D\uDD34 Risque Élevé", String.valueOf(nbEleve), "#e74c3c"),
            riskCard("\uD83D\uDFE1 Risque Modéré", String.valueOf(nbModere), "#f39c12"),
            riskCard("\uD83D\uDFE2 Risque Faible", String.valueOf(nbFaible), "#27ae60")
        );

        TableView<ObservableList<String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(350);

        String[] cols = {"Patient","Total","Abandonnés","Ord. Expirées","En attente +30j","Score","Niveau","Facteurs"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(cols[i]);
            col.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                    idx < p.getValue().size() ? p.getValue().get(idx) : ""));
            if (i == 6) {
                col.setCellFactory(tc -> new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); return; }
                        setText(item);
                        if ("Élevé".equals(item)) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        else if ("Modéré".equals(item)) setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                });
            }
            table.getColumns().add(col);
        }

        javafx.collections.ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        for (org.example.util.AdherencePredictor.PatientRisk r : risques) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(r.nomPatient);
            row.add(String.valueOf(r.totalTraitements));
            row.add(String.valueOf(r.traitementsAbandonnés));
            row.add(String.valueOf(r.ordonnancesExpirees));
            row.add(String.valueOf(r.traitementsEnAttenteTropLongs));
            row.add(String.format("%.0f%%", r.scoreRisque * 100));
            row.add(r.niveau);
            row.add(r.facteurs.isEmpty() ? "Aucun facteur détecté" : String.join(" | ", r.facteurs));
            data.add(row);
        }
        data.sort((a, b) -> b.get(5).compareTo(a.get(5)));
        table.setItems(data);

        VBox tw = new VBox(table); tw.setPadding(new Insets(10, 30, 30, 30)); VBox.setVgrow(table, Priority.ALWAYS);
        pageContainer.getChildren().addAll(greenBar, header, summary, tw);
        VBox.setVgrow(tw, Priority.ALWAYS);
    }

    private HBox riskCard(String label, String value, String color) {
        HBox card = new HBox(10); card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15 25; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0.2, 0, 2); -fx-border-color: " + color +
                "; -fx-border-radius: 10; -fx-border-width: 2;");
        Label lbl = new Label(label); lbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label val = new Label(value); val.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(val, lbl);
        return card;
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
                String bg, border, actionColor;
                switch (e.action) {
                    case "CRÉATION":    bg = "#eafaf1"; border = "#27ae60"; actionColor = "#27ae60"; break;
                    case "SUPPRESSION": bg = "#fdecea"; border = "#e74c3c"; actionColor = "#e74c3c"; break;
                    default:            bg = "#eaf4fb"; border = "#2980b9"; actionColor = "#2980b9"; break;
                }
                card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:8; " +
                        "-fx-border-color:" + border + "; -fx-border-width:0 0 0 4; -fx-border-radius:0 8 8 0;");

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

                if (e.champ != null && !e.champ.isBlank()) {
                    Label champLbl = new Label("Champ : " + e.champ);
                    champLbl.setStyle("-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#2c3e50;");
                    card.getChildren().add(champLbl);
                }
                if (e.ancienneValeur != null || e.nouvelleValeur != null) {
                    HBox diffRow = new HBox(8);
                    diffRow.setAlignment(Pos.CENTER_LEFT);
                    if (e.ancienneValeur != null && !e.ancienneValeur.isBlank()) {
                        Label av = new Label("Avant : " + trunc(e.ancienneValeur, 60));
                        av.setStyle("-fx-font-size:11; -fx-text-fill:#e74c3c;");
                        diffRow.getChildren().add(av);
                    }
                    if (e.ancienneValeur != null && !e.ancienneValeur.isBlank()
                            && e.nouvelleValeur != null && !e.nouvelleValeur.isBlank()) {
                        diffRow.getChildren().add(new Label("→"));
                    }
                    if (e.nouvelleValeur != null && !e.nouvelleValeur.isBlank()) {
                        Label ap = new Label("Après : " + trunc(e.nouvelleValeur, 60));
                        ap.setStyle("-fx-font-size:11; -fx-text-fill:#27ae60;");
                        diffRow.getChildren().add(ap);
                    }
                    card.getChildren().add(diffRow);
                }
                body.getChildren().add(card);
            }
        }

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

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // Afficher la page de statistiques avec PieCharts, cartes récapitulatives et top produits
    private void showStats() {
        pageContainer.getChildren().clear();

        VBox page = new VBox(20);
        page.setPadding(new Insets(30));
        page.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        HBox hdr = new HBox(15); hdr.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("\u2190 Retour \u00e0 la liste");
        backBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        backBtn.setOnAction(e -> showList());
        Label title = new Label("\uD83D\uDCCA Statistiques G\u00e9n\u00e9rales - Traitements & Ordonnances");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        hdr.getChildren().addAll(backBtn, title);

        // Stats cards row 1
        HBox row1 = new HBox(20); row1.setAlignment(Pos.CENTER);
        int totalTrait = 0, enAttente = 0, actifs = 0, termines = 0, annules = 0;
        int totalOrd = 0, ordAttente = 0, ordValidee = 0, ordBrouillon = 0, ordExpiree = 0;
        int totalProduits = 0, totalPatients = 0;

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement"); if (rs.next()) totalTrait = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='en_attente'"); if (rs.next()) enAttente = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='actif'"); if (rs.next()) actifs = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='termin\u00e9'"); if (rs.next()) termines = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM traitement WHERE status='annul\u00e9'"); if (rs.next()) annules = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance"); if (rs.next()) totalOrd = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='en_attente'"); if (rs.next()) ordAttente = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='valid\u00e9e'"); if (rs.next()) ordValidee = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='brouillon'"); if (rs.next()) ordBrouillon = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM ordonnance WHERE statut='expir\u00e9e'"); if (rs.next()) ordExpiree = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(DISTINCT id_produit_id) AS c FROM traitement"); if (rs.next()) totalProduits = rs.getInt("c"); rs.close();
            rs = conn.createStatement().executeQuery("SELECT COUNT(DISTINCT id_utilisateur_id) AS c FROM traitement"); if (rs.next()) totalPatients = rs.getInt("c"); rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        row1.getChildren().addAll(
            statCard("\uD83D\uDC8A", String.valueOf(totalTrait), "Total Traitements", "#1f6f5c", "white"),
            statCard("\uD83D\uDCC4", String.valueOf(totalOrd), "Total Ordonnances", "#2980b9", "white"),
            statCard("\uD83D\uDC64", String.valueOf(totalPatients), "Patients concern\u00e9s", "#8e44ad", "white"),
            statCard("\uD83D\uDCE6", String.valueOf(totalProduits), "Produits utilis\u00e9s", "#e67e22", "white")
        );

        // Traitements breakdown
        Label traitTitle = new Label("\uD83D\uDC8A R\u00e9partition des Traitements par statut");
        traitTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        HBox row2 = new HBox(20); row2.setAlignment(Pos.CENTER);
        row2.getChildren().addAll(
            statCard("\u23F3", String.valueOf(enAttente), "En attente", "#f39c12", "white"),
            statCard("\u2705", String.valueOf(actifs), "Actifs", "#27ae60", "white"),
            statCard("\u2714", String.valueOf(termines), "Termin\u00e9s", "#3498db", "white"),
            statCard("\u274C", String.valueOf(annules), "Annul\u00e9s", "#e74c3c", "white")
        );

        // Ordonnances breakdown
        Label ordTitle = new Label("\uD83D\uDCC4 R\u00e9partition des Ordonnances par statut");
        ordTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        HBox row3 = new HBox(20); row3.setAlignment(Pos.CENTER);
        row3.getChildren().addAll(
            statCard("\uD83D\uDCDD", String.valueOf(ordBrouillon), "Brouillon", "#95a5a6", "white"),
            statCard("\u23F3", String.valueOf(ordAttente), "En attente", "#f39c12", "white"),
            statCard("\u2705", String.valueOf(ordValidee), "Valid\u00e9es", "#27ae60", "white"),
            statCard("\u26A0", String.valueOf(ordExpiree), "Expir\u00e9es", "#e74c3c", "white")
        );

        // Top produits
        Label topTitle = new Label("\uD83C\uDFC6 Top Produits les plus prescrits");
        topTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        VBox topCard = new VBox(8); topCard.setPadding(new Insets(20));
        topCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0.3, 0, 2);");
        try {
            ResultSet rs = DatabaseUtil.getInstance().getConnection().createStatement().executeQuery(
                "SELECT p.nom, COUNT(*) AS nb FROM traitement t JOIN produit p ON t.id_produit_id=p.id_produit GROUP BY p.nom ORDER BY nb DESC LIMIT 5");
            int rank = 1;
            while (rs.next()) {
                String medal = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "\u2022";
                Label row = new Label(medal + "  " + rs.getString("nom") + " \u2014 " + rs.getInt("nb") + " prescription(s)");
                row.setStyle("-fx-font-size: 14; -fx-text-fill: #333; -fx-padding: 5 10;");
                topCard.getChildren().add(row);
                rank++;
            }
            rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        if (topCard.getChildren().isEmpty()) topCard.getChildren().add(new Label("Aucune donn\u00e9e disponible"));

        page.getChildren().addAll(hdr);

        // PieChart Traitements
        javafx.scene.chart.PieChart traitPie = new javafx.scene.chart.PieChart(
                javafx.collections.FXCollections.observableArrayList(
                        new javafx.scene.chart.PieChart.Data("En attente (" + enAttente + ")", enAttente),
                        new javafx.scene.chart.PieChart.Data("Actifs (" + actifs + ")", actifs),
                        new javafx.scene.chart.PieChart.Data("Terminés (" + termines + ")", termines),
                        new javafx.scene.chart.PieChart.Data("Annulés (" + annules + ")", annules)
                ));
        traitPie.setTitle("Répartition des Traitements (en %)");
        traitPie.setLabelsVisible(true);
        traitPie.setLegendVisible(true);
        traitPie.setPrefHeight(350);

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

        HBox chartsRow = new HBox(30); chartsRow.setAlignment(Pos.CENTER);
        chartsRow.getChildren().addAll(traitPie, ordPie);

        page.getChildren().addAll(chartsRow, topTitle, topCard);

        // Derniers traitements créés
        Label recentTitle = new Label("\uD83D\uDD52 Derniers traitements créés");
        recentTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");
        VBox recentCard = new VBox(8); recentCard.setPadding(new Insets(20));
        recentCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0.3, 0, 2);");
        try {
            ResultSet rs = DatabaseUtil.getInstance().getConnection().createStatement().executeQuery(
                "SELECT t.id_traitement, o.numero_ordonnance, u.nom, p.nom AS pnom, t.status, t.date_debut " +
                "FROM traitement t LEFT JOIN utilisateur u ON t.id_utilisateur_id=u.id_utilisateur " +
                "LEFT JOIN produit p ON t.id_produit_id=p.id_produit " +
                "LEFT JOIN ordonnance o ON t.id_ordonnance_id=o.id_ordonnance " +
                "ORDER BY t.id_traitement DESC LIMIT 5");
            while (rs.next()) {
                String date = rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime().toLocalDate().toString() : "";
                Label row = new Label("\uD83D\uDC8A " + (rs.getString("numero_ordonnance") != null ? rs.getString("numero_ordonnance") : "N/A") +
                        " \u2014 " + (rs.getString("nom") != null ? rs.getString("nom") : "N/A") +
                        " \u2014 " + (rs.getString("pnom") != null ? rs.getString("pnom") : "N/A") +
                        " \u2014 " + rs.getString("status") + " \u2014 " + date);
                row.setStyle("-fx-font-size: 13; -fx-text-fill: #333; -fx-padding: 5 10;");
                recentCard.getChildren().add(row);
            } rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        if (recentCard.getChildren().isEmpty()) recentCard.getChildren().add(new Label("Aucune donnée"));

        page.getChildren().addAll(recentTitle, recentCard);

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
    @FXML private void goToOrdonnances() throws IOException { nav("/fxml/BackOrdonnance.fxml"); } // Navigation vers la gestion des ordonnances
    @FXML private void logout() throws IOException { nav("/fxml/Login.fxml"); } // Déconnexion et retour au login
    // Méthode utilitaire de navigation entre les pages
    private void nav(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Scene scene = new Scene(root); scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) pageContainer.getScene().getWindow(); stage.setScene(scene); stage.setFullScreen(true);
    }
}
