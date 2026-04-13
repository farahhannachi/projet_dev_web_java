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

public class BackTraitementController {

    @FXML private VBox pageContainer;

    @FXML
    public void initialize() { showList(); }

    public void openNewForm() { showForm(null); }

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
        table.getStyleClass().add("modern-table"); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setMinHeight(400);
        String[] hdrs = {"ID","Ordonnance","Patient","Produit","Dosage","Fr\u00e9quence","Dur\u00e9e (jours)","Date D\u00e9but","Date Fin","Statut","Actions"};
        for (int i = 0; i < hdrs.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> c = new TableColumn<>(hdrs[i]);
            c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(col < p.getValue().size() ? p.getValue().get(col) : ""));
            if (i == hdrs.length - 1) {
                c.setCellFactory(tc -> new TableCell<>() {
                    final Button eb = new Button("Modifier"); final Button db = new Button("Supprimer"); final HBox bx = new HBox(5, eb, db);
                    { eb.setStyle("-fx-background-color:#f39c12;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      db.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      eb.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()).get(0)));
                      db.setOnAction(e -> { String id = getTableView().getItems().get(getIndex()).get(0);
                          new Alert(Alert.AlertType.CONFIRMATION,"Supprimer #"+id+" ?",ButtonType.YES,ButtonType.NO).showAndWait().ifPresent(b -> {
                              if(b==ButtonType.YES){try{PreparedStatement p2=DatabaseUtil.getConnection().prepareStatement("DELETE FROM traitement WHERE id_traitement=?");p2.setInt(1,Integer.parseInt(id));p2.executeUpdate();p2.close();showList();}catch(SQLException ex){}}});});}
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

    private void loadData(TableView<ObservableList<String>> table, LocalDate dD, LocalDate dF, String cl, String pr, String st, String tri) {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try {
            StringBuilder sql = new StringBuilder("SELECT t.id_traitement, o.numero_ordonnance, u.nom AS unom, p.nom AS pnom, t.dosage, t.frequence, t.duree_jours, t.date_debut, t.date_fin, t.status FROM traitement t LEFT JOIN utilisateur u ON t.id_utilisateur_id=u.id_utilisateur LEFT JOIN produit p ON t.id_produit_id=p.id_produit LEFT JOIN ordonnance o ON t.id_ordonnance_id=o.id_ordonnance WHERE 1=1 ");
            if (dD != null) sql.append("AND t.date_debut >= '").append(dD).append(" 00:00:00' ");
            if (dF != null) sql.append("AND t.date_debut <= '").append(dF).append(" 23:59:59' ");
            if (!cl.isEmpty()) sql.append("AND u.nom LIKE '%").append(cl.replace("'","''")).append("%' ");
            if (!pr.isEmpty()) sql.append("AND p.nom LIKE '%").append(pr.replace("'","''")).append("%' ");
            if (st != null && !"Tous les statuts".equals(st)) sql.append("AND t.status='").append(st).append("' ");
            sql.append("ORDER BY t.id_traitement ").append("Plus ancien".equals(tri) ? "ASC" : "DESC");
            ResultSet rs = DatabaseUtil.getConnection().createStatement().executeQuery(sql.toString());
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("id_traitement")));
                row.add(rs.getString("numero_ordonnance") != null ? rs.getString("numero_ordonnance") : "N/A");
                row.add(rs.getString("unom") != null ? rs.getString("unom") : "N/A");
                row.add(rs.getString("pnom") != null ? rs.getString("pnom") : "N/A");
                row.add(rs.getString("dosage") != null ? rs.getString("dosage") : "");
                row.add(rs.getString("frequence") != null ? rs.getString("frequence") : "");
                row.add(String.valueOf(rs.getInt("duree_jours")));
                row.add(rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getString("status") != null ? rs.getString("status") : "");
                row.add(""); data.add(row);
            } rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        table.setItems(data);
        if (data.isEmpty()) table.setPlaceholder(new Label("Aucun traitement trouv\u00e9"));
    }

    private void showForm(String editId) {
        pageContainer.getChildren().clear();
        boolean isEdit = editId != null;

        VBox card = new VBox(0);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0.3, 0, 3);");

        HBox hdr = new HBox(12); hdr.setAlignment(Pos.CENTER_LEFT); hdr.setPadding(new Insets(25, 30, 15, 30));
        Label ic = new Label("\uD83D\uDC8A"); ic.setStyle("-fx-font-size: 28; -fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 10;");
        Label ti = new Label(isEdit ? "Modifier Traitement #" + editId : "Ajouter un Traitement");
        ti.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        hdr.getChildren().addAll(ic, ti);

        VBox form = new VBox(12); form.setPadding(new Insets(15, 30, 25, 30));

        ComboBox<String> ordC = new ComboBox<>(); ordC.setMaxWidth(Double.MAX_VALUE); ordC.setPromptText("S\u00e9lectionner une ordonnance");
        ComboBox<String> prodC = new ComboBox<>(); prodC.setMaxWidth(Double.MAX_VALUE); prodC.setPromptText("S\u00e9lectionner un produit");
        ComboBox<String> userC = new ComboBox<>(); userC.setMaxWidth(Double.MAX_VALUE); userC.setPromptText("S\u00e9lectionner un patient");
        TextArea notesF = new TextArea(); notesF.setPromptText("Exemple:\nAnt\u00e9c\u00e9dents: Allergie au parac\u00e9tamol\nSympt\u00f4mes: Maux de t\u00eate\nPrescription: Ibuprof\u00e8ne 400mg");
        notesF.setPrefRowCount(5); notesF.setWrapText(true);

        HBox r1 = new HBox(20);
        VBox db = new VBox(4); TextField dosF = new TextField(); dosF.setPromptText("Ex: 500mg, 1 comprim\u00e9"); db.getChildren().addAll(new Label("Dosage"), dosF);
        VBox fb = new VBox(4); TextField freqF = new TextField(); freqF.setPromptText("Ex: 3 fois par jour"); fb.getChildren().addAll(new Label("Fr\u00e9quence"), freqF);
        r1.getChildren().addAll(db, fb); HBox.setHgrow(db, Priority.ALWAYS); HBox.setHgrow(fb, Priority.ALWAYS);

        ComboBox<String> repasC = new ComboBox<>(FXCollections.observableArrayList("Avant le repas","Pendant le repas","Apr\u00e8s le repas","En dehors des repas"));
        repasC.setMaxWidth(Double.MAX_VALUE); repasC.setPromptText("-- Moment du repas --");

        TextField dureeF = new TextField("7"); dureeF.setPromptText("7");

        HBox r2 = new HBox(20);
        VBox ddB = new VBox(4); DatePicker ddP = new DatePicker(LocalDate.now()); ddB.getChildren().addAll(new Label("Date de d\u00e9but"), ddP);
        VBox dfB = new VBox(4); DatePicker dfP = new DatePicker(LocalDate.now().plusDays(7)); dfB.getChildren().addAll(new Label("Date de fin"), dfP);
        r2.getChildren().addAll(ddB, dfB); HBox.setHgrow(ddB, Priority.ALWAYS); HBox.setHgrow(dfB, Priority.ALWAYS);

        HBox banner = new HBox(8); banner.setAlignment(Pos.CENTER_LEFT); banner.setPadding(new Insets(10, 15, 10, 15));
        banner.setStyle("-fx-background-color: #e8f4fd; -fx-background-radius: 8; -fx-border-color: #bee5eb; -fx-border-radius: 8;");
        banner.getChildren().addAll(new Label("\u2139"), new Label("Le statut sera automatiquement d\u00e9fini \u00e0 \"En attente\" lors de la cr\u00e9ation."));

        ComboBox<String> statC = new ComboBox<>(FXCollections.observableArrayList("en_attente","actif","termin\u00e9","annul\u00e9"));
        statC.setMaxWidth(Double.MAX_VALUE); statC.setValue("en_attente");
        Label err = new Label(); err.setStyle("-fx-text-fill: #e74c3c;");

        try {
            Connection conn = DatabaseUtil.getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT id_utilisateur, nom, prenom, email FROM utilisateur ORDER BY nom");
            while (rs.next()) userC.getItems().add(rs.getInt(1) + " - " + rs.getString(2) + " " + rs.getString(3) + " (" + rs.getString(4) + ")");
            rs.close();
            rs = conn.createStatement().executeQuery("SELECT id_produit, nom FROM produit ORDER BY nom");
            while (rs.next()) prodC.getItems().add(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
            rs = conn.createStatement().executeQuery("SELECT id_ordonnance, numero_ordonnance FROM ordonnance ORDER BY id_ordonnance DESC");
            while (rs.next()) ordC.getItems().add(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        if (isEdit) {
            try {
                PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("SELECT * FROM traitement WHERE id_traitement=?");
                ps.setInt(1, Integer.parseInt(editId)); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    dosF.setText(rs.getString("dosage") != null ? rs.getString("dosage") : "");
                    freqF.setText(rs.getString("frequence") != null ? rs.getString("frequence") : "");
                    dureeF.setText(String.valueOf(rs.getInt("duree_jours")));
                    notesF.setText(rs.getString("notes") != null ? rs.getString("notes") : "");
                    repasC.setValue(rs.getString("repas")); statC.setValue(rs.getString("status"));
                    if (rs.getTimestamp("date_debut") != null) ddP.setValue(rs.getTimestamp("date_debut").toLocalDateTime().toLocalDate());
                    if (rs.getTimestamp("date_fin") != null) dfP.setValue(rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate());
                    String uid=String.valueOf(rs.getInt("id_utilisateur_id")), pid=String.valueOf(rs.getInt("id_produit_id")), oid=String.valueOf(rs.getInt("id_ordonnance_id"));
                    for (String x : userC.getItems()) if (x.startsWith(uid+" -")) { userC.setValue(x); break; }
                    for (String x : prodC.getItems()) if (x.startsWith(pid+" -")) { prodC.setValue(x); break; }
                    for (String x : ordC.getItems()) if (x.startsWith(oid+" -")) { ordC.setValue(x); break; }
                } rs.close(); ps.close();
            } catch (SQLException e) { System.out.println(e.getMessage()); }
        }

        HBox btns = new HBox(15); btns.setAlignment(Pos.CENTER); btns.setPadding(new Insets(10, 0, 0, 0));
        Button cancel = new Button("\u2190 Retour"); cancel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        cancel.setOnAction(e -> showList());
        Button save = new Button(isEdit ? "\uD83D\uDCBE Mettre \u00e0 jour" : "\uD83D\uDCBE Ajouter");
        save.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        save.setOnAction(e -> {
            if (userC.getValue()==null||prodC.getValue()==null||ordC.getValue()==null) { err.setText("Remplissez les champs obligatoires."); return; }
            try {
                int duree = 0; try { duree = Integer.parseInt(dureeF.getText().trim()); } catch (NumberFormatException ignored) {}
                if (isEdit) {
                    PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("UPDATE traitement SET id_utilisateur_id=?,dosage=?,frequence=?,duree_jours=?,date_debut=?,date_fin=?,status=?,notes=?,id_ordonnance_id=?,id_produit_id=?,repas=? WHERE id_traitement=?");
                    ps.setInt(1,Integer.parseInt(userC.getValue().split(" - ")[0])); ps.setString(2,dosF.getText().trim()); ps.setString(3,freqF.getText().trim()); ps.setInt(4,duree);
                    ps.setTimestamp(5,ddP.getValue()!=null?Timestamp.valueOf(ddP.getValue().atStartOfDay()):null);
                    ps.setTimestamp(6,dfP.getValue()!=null?Timestamp.valueOf(dfP.getValue().atStartOfDay()):null);
                    ps.setString(7,statC.getValue()); ps.setString(8,notesF.getText()!=null?notesF.getText().trim():"");
                    ps.setInt(9,Integer.parseInt(ordC.getValue().split(" - ")[0])); ps.setInt(10,Integer.parseInt(prodC.getValue().split(" - ")[0]));
                    ps.setString(11,repasC.getValue()!=null?repasC.getValue():""); ps.setInt(12,Integer.parseInt(editId)); ps.executeUpdate(); ps.close();
                } else {
                    // Vérifier si l'ordonnance est déjà validée
                    int ordId = Integer.parseInt(ordC.getValue().split(" - ")[0]);
                    String traitStatus = "en_attente";
                    PreparedStatement psCheck = DatabaseUtil.getConnection().prepareStatement("SELECT statut FROM ordonnance WHERE id_ordonnance=?");
                    psCheck.setInt(1, ordId); ResultSet rsCheck = psCheck.executeQuery();
                    if (rsCheck.next() && "valid\u00e9e".equals(rsCheck.getString("statut"))) { traitStatus = "actif"; }
                    rsCheck.close(); psCheck.close();

                    PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("INSERT INTO traitement (id_utilisateur_id,dosage,frequence,duree_jours,date_debut,date_fin,status,notes,id_ordonnance_id,id_produit_id,repas) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                    ps.setInt(1,Integer.parseInt(userC.getValue().split(" - ")[0])); ps.setString(2,dosF.getText().trim()); ps.setString(3,freqF.getText().trim()); ps.setInt(4,duree);
                    ps.setTimestamp(5,ddP.getValue()!=null?Timestamp.valueOf(ddP.getValue().atStartOfDay()):Timestamp.valueOf(java.time.LocalDateTime.now()));
                    ps.setTimestamp(6,dfP.getValue()!=null?Timestamp.valueOf(dfP.getValue().atStartOfDay()):null);
                    ps.setString(7,traitStatus); ps.setString(8,notesF.getText()!=null?notesF.getText().trim():"");
                    ps.setInt(9,ordId); ps.setInt(10,Integer.parseInt(prodC.getValue().split(" - ")[0]));
                    ps.setString(11,repasC.getValue()!=null?repasC.getValue():""); ps.executeUpdate(); ps.close();
                }
                showList();
            } catch (SQLException ex) { err.setText("Erreur: " + ex.getMessage()); }
        });
        btns.getChildren().addAll(cancel, save);

        form.getChildren().addAll(new Label("Ordonnance"), ordC, new Label("Produit"), prodC, new Label("Patient"), userC,
                new Label("Notes"), notesF, r1, new Label("Repas"), repasC, new Label("Dur\u00e9e (jours)"), dureeF, r2);
        if (isEdit) form.getChildren().addAll(new Label("Statut"), statC); else form.getChildren().add(banner);
        form.getChildren().addAll(err, new Separator(), btns);
        card.getChildren().addAll(hdr, new Separator(), form);

        VBox wrap = new VBox(card); wrap.setAlignment(Pos.TOP_CENTER); wrap.setPadding(new Insets(30)); wrap.setStyle("-fx-background-color: #f5f5f5;");
        ScrollPane sc = new ScrollPane(wrap); sc.setFitToWidth(true); sc.setStyle("-fx-background-color: #f5f5f5;");
        pageContainer.getChildren().add(sc); VBox.setVgrow(sc, Priority.ALWAYS);
    }

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
            Connection conn = DatabaseUtil.getConnection();
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
            ResultSet rs = DatabaseUtil.getConnection().createStatement().executeQuery(
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

        page.getChildren().addAll(hdr, row1, traitTitle, row2, ordTitle, row3, topTitle, topCard);

        ScrollPane sc = new ScrollPane(page); sc.setFitToWidth(true); sc.setStyle("-fx-background-color: #f5f5f5;");
        pageContainer.getChildren().add(sc); VBox.setVgrow(sc, Priority.ALWAYS);
    }

    private VBox statCard(String icon, String value, String label, String bgColor, String textColor) {
        VBox card = new VBox(5); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(20, 30, 20, 30));
        card.setMinWidth(180); card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.3, 0, 3);");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 28;");
        Label val = new Label(value); val.setStyle("-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        Label lbl = new Label(label); lbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        card.getChildren().addAll(ic, val, lbl);
        return card;
    }

    @FXML private void goToDashboard() throws IOException { nav("/fxml/Dashboard.fxml"); }
    @FXML private void goToOrdonnances() throws IOException { nav("/fxml/BackOrdonnance.fxml"); }
    @FXML private void logout() throws IOException { nav("/fxml/Login.fxml"); }
    private void nav(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Scene scene = new Scene(root); scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) pageContainer.getScene().getWindow(); stage.setScene(scene); stage.setFullScreen(true);
    }
}
