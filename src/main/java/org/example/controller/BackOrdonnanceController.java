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

public class BackOrdonnanceController {

    @FXML private VBox pageContainer;

    @FXML
    public void initialize() { showList(); }

    public void openNewForm() { showForm(null); }

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
        table.getStyleClass().add("modern-table"); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setMinHeight(400);
        String[] hdrs = {"ID","Num\u00e9ro","Patient","Date Ordonnance","Date Expiration","Statut","Traitements","Actions"};
        for (int i = 0; i < hdrs.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> c = new TableColumn<>(hdrs[i]);
            c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(col < p.getValue().size() ? p.getValue().get(col) : ""));
            if (i == hdrs.length - 1) {
                c.setCellFactory(tc -> new TableCell<>() {
                    final Button eb = new Button("Modifier"); final Button db2 = new Button("Supprimer"); final HBox bx = new HBox(5, eb, db2);
                    { eb.setStyle("-fx-background-color:#f39c12;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      db2.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:3 8;");
                      eb.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()).get(0)));
                      db2.setOnAction(e -> { String id = getTableView().getItems().get(getIndex()).get(0);
                          new Alert(Alert.AlertType.CONFIRMATION,"Supprimer ordonnance #"+id+" ?",ButtonType.YES,ButtonType.NO).showAndWait().ifPresent(b -> {
                              if(b==ButtonType.YES){try{PreparedStatement p2=DatabaseUtil.getConnection().prepareStatement("DELETE FROM ordonnance WHERE id_ordonnance=?");p2.setInt(1,Integer.parseInt(id));p2.executeUpdate();p2.close();showList();}catch(SQLException ex){}}});});}
                    @Override protected void updateItem(String item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : bx); }
                });
            }
            table.getColumns().add(c);
        }
        loadData(table, null, null, "", "Tous les statuts", "Date Ordonnance (Plus r\u00e9cent)");
        Runnable apply = () -> loadData(table, dOrd.getValue(), dExp.getValue(), cf.getText().trim(), sf.getValue(), triF.getValue());
        dOrd.valueProperty().addListener((o,a,b) -> apply.run()); dExp.valueProperty().addListener((o,a,b) -> apply.run());
        cf.textProperty().addListener((o,a,b) -> apply.run()); sf.valueProperty().addListener((o,a,b) -> apply.run());
        triF.valueProperty().addListener((o,a,b) -> apply.run());
        VBox tw = new VBox(table); tw.setPadding(new Insets(0, 30, 30, 30)); VBox.setVgrow(table, Priority.ALWAYS);
        pageContainer.getChildren().addAll(greenBar, header, actionBar, filterCard, tw);
    }

    private void loadData(TableView<ObservableList<String>> table, LocalDate dO, LocalDate dE, String cl, String st, String tri) {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try {
            StringBuilder sql = new StringBuilder("SELECT o.id_ordonnance, o.numero_ordonnance, u.nom AS unom, o.date_ordonnance, o.date_expiration, o.statut, (SELECT COUNT(*) FROM traitement t WHERE t.id_ordonnance_id=o.id_ordonnance) AS nb_trait FROM ordonnance o LEFT JOIN utilisateur u ON o.id_utilisateur_id=u.id_utilisateur WHERE 1=1 ");
            if (dO != null) sql.append("AND o.date_ordonnance >= '").append(dO).append(" 00:00:00' ");
            if (dE != null) sql.append("AND o.date_expiration <= '").append(dE).append(" 23:59:59' ");
            if (!cl.isEmpty()) sql.append("AND u.nom LIKE '%").append(cl.replace("'","''")).append("%' ");
            if (st != null && !"Tous les statuts".equals(st)) sql.append("AND o.statut='").append(st).append("' ");
            sql.append("ORDER BY o.date_ordonnance ").append(tri.contains("ancien") ? "ASC" : "DESC");
            ResultSet rs = DatabaseUtil.getConnection().createStatement().executeQuery(sql.toString());
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("id_ordonnance")));
                row.add(rs.getString("numero_ordonnance") != null ? rs.getString("numero_ordonnance") : "");
                row.add(rs.getString("unom") != null ? rs.getString("unom") : "N/A");
                row.add(rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getTimestamp("date_expiration") != null ? rs.getTimestamp("date_expiration").toLocalDateTime().toLocalDate().toString() : "");
                row.add(rs.getString("statut") != null ? rs.getString("statut") : "");
                row.add(String.valueOf(rs.getInt("nb_trait")));
                row.add(""); data.add(row);
            } rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        table.setItems(data);
        if (data.isEmpty()) table.setPlaceholder(new Label("Aucune ordonnance trouv\u00e9e"));
    }

    private void showForm(String editId) {
        pageContainer.getChildren().clear();
        boolean isEdit = editId != null;

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
        if (!isEdit) numF.setText("ORD-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + (int)(Math.random()*10));
        numB.getChildren().addAll(new Label("Num\u00e9ro d'ordonnance"), numF);
        VBox patB = new VBox(4); ComboBox<String> userC = new ComboBox<>(); userC.setMaxWidth(Double.MAX_VALUE); userC.setPromptText("S\u00e9lectionner un patient");
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
        statC.setMaxWidth(Double.MAX_VALUE); statC.setValue("en_attente");

        TextArea noteF = new TextArea(); noteF.setPromptText("Note m\u00e9dicale"); noteF.setPrefRowCount(4); noteF.setWrapText(true);
        Label err = new Label(); err.setStyle("-fx-text-fill: #e74c3c;");

        try {
            ResultSet rs = DatabaseUtil.getConnection().createStatement().executeQuery("SELECT id_utilisateur, nom, prenom, email FROM utilisateur ORDER BY nom");
            while (rs.next()) userC.getItems().add(rs.getInt(1) + " - " + rs.getString(2) + " " + rs.getString(3) + " (" + rs.getString(4) + ")");
            rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        if (isEdit) {
            try {
                PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("SELECT * FROM ordonnance WHERE id_ordonnance=?");
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
            if (userC.getValue()==null||doP.getValue()==null||deP.getValue()==null) { err.setText("Remplissez les champs obligatoires."); return; }
            try {
                if (isEdit) {
                    PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("UPDATE ordonnance SET numero_ordonnance=?,date_ordonnance=?,date_expiration=?,statut=?,note_medical=?,id_utilisateur_id=? WHERE id_ordonnance=?");
                    ps.setString(1,numF.getText().trim()); ps.setTimestamp(2,Timestamp.valueOf(doP.getValue().atStartOfDay()));
                    ps.setTimestamp(3,Timestamp.valueOf(deP.getValue().atStartOfDay())); ps.setString(4,statC.getValue());
                    ps.setString(5,noteF.getText()!=null?noteF.getText().trim():""); ps.setInt(6,Integer.parseInt(userC.getValue().split(" - ")[0]));
                    ps.setInt(7,Integer.parseInt(editId)); ps.executeUpdate(); ps.close();
                    // Si ordonnance validée, valider automatiquement tous les traitements liés
                    if ("valid\u00e9e".equals(statC.getValue())) {
                        PreparedStatement ps2 = DatabaseUtil.getConnection().prepareStatement("UPDATE traitement SET status='actif' WHERE id_ordonnance_id=? AND status='en_attente'");
                        ps2.setInt(1, Integer.parseInt(editId)); ps2.executeUpdate(); ps2.close();
                    }
                } else {
                    PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement("INSERT INTO ordonnance (numero_ordonnance,date_ordonnance,date_expiration,statut,note_medical,id_utilisateur_id) VALUES (?,?,?,?,?,?)");
                    ps.setString(1,numF.getText().trim()); ps.setTimestamp(2,Timestamp.valueOf(doP.getValue().atStartOfDay()));
                    ps.setTimestamp(3,Timestamp.valueOf(deP.getValue().atStartOfDay())); ps.setString(4,"en_attente");
                    ps.setString(5,noteF.getText()!=null?noteF.getText().trim():""); ps.setInt(6,Integer.parseInt(userC.getValue().split(" - ")[0]));
                    ps.executeUpdate(); ps.close();
                }
                showList();
            } catch (SQLException ex) { err.setText("Erreur: " + ex.getMessage()); }
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
            Connection conn = DatabaseUtil.getConnection();
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
            ResultSet rs = DatabaseUtil.getConnection().createStatement().executeQuery(
                "SELECT o.numero_ordonnance, u.nom, o.statut, o.date_ordonnance FROM ordonnance o LEFT JOIN utilisateur u ON o.id_utilisateur_id=u.id_utilisateur ORDER BY o.id_ordonnance DESC LIMIT 5");
            while (rs.next()) {
                String date = rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime().toLocalDate().toString() : "";
                Label row = new Label("\uD83D\uDCC4 " + rs.getString("numero_ordonnance") + " \u2014 " + (rs.getString("nom")!=null?rs.getString("nom"):"N/A") + " \u2014 " + rs.getString("statut") + " \u2014 " + date);
                row.setStyle("-fx-font-size: 13; -fx-text-fill: #333; -fx-padding: 5 10;");
                recentCard.getChildren().add(row);
            } rs.close();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        if (recentCard.getChildren().isEmpty()) recentCard.getChildren().add(new Label("Aucune donn\u00e9e"));

        page.getChildren().addAll(hdr, row1, ordTitle, row2, traitTitle, row3, recentTitle, recentCard);
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
    @FXML private void goToTraitements() throws IOException { nav("/fxml/BackTraitement.fxml"); }
    @FXML private void logout() throws IOException { nav("/fxml/Login.fxml"); }
    private void nav(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Scene scene = new Scene(root); scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) pageContainer.getScene().getWindow(); stage.setScene(scene); stage.setFullScreen(true);
    }
}
