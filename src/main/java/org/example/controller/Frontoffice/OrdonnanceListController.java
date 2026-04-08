package org.example.controller.Frontoffice;

import org.example.entities.Ordonnance;
import org.example.entities.Traitement;
import org.example.services.OrdonnanceService;
import org.example.services.TraitementService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class OrdonnanceListController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private TextField tfSearch;
    @FXML private Label lblPatientName, lblPatientInfo;

    private final OrdonnanceService service = new OrdonnanceService();
    private final TraitementService traitService = new TraitementService();
    private List<Ordonnance> allOrdonnances;
    private List<Traitement> allTraitements;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblPatientName.setText(FrontofficeController.connectedUserName);
        lblPatientInfo.setText("ID Utilisateur: " + FrontofficeController.connectedUserId);
        loadData();
        tfSearch.textProperty().addListener((obs, o, n) -> filterCards(n));
    }

    private void loadData() {
        try {
            allOrdonnances = service.getAll();
            allTraitements = traitService.getAll();
            displayCards(allOrdonnances);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterCards(String kw) {
        if (kw == null || kw.isEmpty()) { displayCards(allOrdonnances); return; }
        String lower = kw.toLowerCase();
        displayCards(allOrdonnances.stream()
            .filter(o -> (o.getNumeroOrdonnance() != null && o.getNumeroOrdonnance().toLowerCase().contains(lower))
                || (o.getSignatureMedecin() != null && o.getSignatureMedecin().toLowerCase().contains(lower))
                || (o.getStatut() != null && o.getStatut().toLowerCase().contains(lower)))
            .toList());
    }

    private void displayCards(List<Ordonnance> list) {
        cardsContainer.getChildren().clear();
        for (Ordonnance o : list) {
            List<Traitement> traitements = allTraitements.stream()
                .filter(t -> t.getIdOrdonnance() == o.getIdOrdonnance()).toList();

            VBox card = new VBox(12);
            card.getStyleClass().add("fo-card");
            card.setPadding(new Insets(20));

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblTitle = new Label(o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : "Ordonnance #" + o.getIdOrdonnance());
            lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(o.getStatut());
            header.getChildren().addAll(lblTitle, spacer, badge);

            Label lblPatient = new Label("👤 " + FrontofficeController.connectedUserName);
            lblPatient.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-font-weight: bold;");
            Label lblDoctor = new Label("👨‍⚕️ Dr. " + (o.getSignatureMedecin() != null ? o.getSignatureMedecin() : "-"));
            lblDoctor.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            Label lblDate = new Label("📅 " + (o.getDateOrdonnance() != null ? o.getDateOrdonnance() : "-")
                + "  →  Exp: " + (o.getDateExpiration() != null ? o.getDateExpiration() : "-"));
            lblDate.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
            Label lblTraitCount = new Label("💊 " + traitements.size() + " traitement(s) associé(s)");
            lblTraitCount.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: bold;");

            card.getChildren().addAll(header, lblPatient, lblDoctor, lblDate, new Separator(), lblTraitCount);

            for (Traitement t : traitements) {
                VBox sub = new VBox(6);
                sub.setPadding(new Insets(12));
                sub.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-border-width: 1;");
                HBox sh = new HBox(10);
                sh.setAlignment(Pos.CENTER_LEFT);
                Label ld = new Label(t.getDosage() != null ? t.getDosage() : "Traitement #" + t.getIdTraitement());
                ld.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label sb = createBadge(t.getStatus());
                sh.getChildren().addAll(ld, sp, sb);
                Label lf = new Label("💊 " + t.getDureeJours() + " jours  |  " + (t.getFrequence() != null ? t.getFrequence() : "-"));
                lf.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
                Label ldt = new Label("📅 " + (t.getDateDebut() != null ? t.getDateDebut() : "-") + "  →  " + (t.getDateFin() != null ? t.getDateFin() : "-"));
                ldt.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
                sub.getChildren().addAll(sh, lf, ldt);
                card.getChildren().add(sub);
            }

            if (traitements.isEmpty()) {
                Label lblNo = new Label("Aucun traitement associé.");
                lblNo.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 12px; -fx-padding: 8 0;");
                card.getChildren().add(lblNo);
            }

            if (o.getNoteMedical() != null && !o.getNoteMedical().isEmpty()) {
                Label lblNote = new Label("📝 " + o.getNoteMedical());
                lblNote.setWrapText(true);
                lblNote.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 12px;");
                card.getChildren().add(lblNote);
            }

            // Actions
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));
            Button btnDel = new Button("🗑 Supprimer");
            btnDel.getStyleClass().add("btn-delete");
            btnDel.setStyle("-fx-font-size: 12px; -fx-padding: 6 14;");
            btnDel.setOnAction(e -> {
                try { service.delete(o.getIdOrdonnance()); loadData(); }
                catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage(), ButtonType.OK).showAndWait(); }
            });
            actions.getChildren().add(btnDel);
            card.getChildren().add(actions);

            cardsContainer.getChildren().add(card);
        }
    }

    private Label createBadge(String statut) {
        Label badge = new Label(statut != null ? statut : "N/A");
        if (statut == null) { badge.getStyleClass().add("badge-expiree"); return badge; }
        String s = statut.toLowerCase();
        if (s.contains("activ") || s.contains("en cours") || s.contains("valid")) badge.getStyleClass().add("badge-en-cours");
        else if (s.contains("complet") || s.contains("termin")) badge.getStyleClass().add("badge-completee");
        else if (s.contains("annul")) badge.getStyleClass().add("badge-annulee");
        else badge.getStyleClass().add("badge-expiree");
        return badge;
    }
}
