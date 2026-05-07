package org.example.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service centralisé de dialogues custom — style cohérent dans toute l'application.
 */
public class DialogService {

    // ── SUCCÈS ────────────────────────────────────────────────────────────
    public static void showSuccess(String titre, String message) {
        show("✅", titre, message, "#27ae60", "#eafaf1", "#27ae60", "Fermer", false);
    }

    // ── INFORMATION ───────────────────────────────────────────────────────
    public static void showInfo(String titre, String message) {
        show("ℹ️", titre, message, "#2980b9", "#eaf4fb", "#2980b9", "Compris", false);
    }

    // ── AVERTISSEMENT ─────────────────────────────────────────────────────
    public static void showWarning(String titre, String message) {
        show("⚠️", titre, message, "#e67e22", "#fff8e1", "#e67e22", "Compris", false);
    }

    // ── ERREUR ────────────────────────────────────────────────────────────
    public static void showError(String titre, String message) {
        show("✕", titre, message, "#e74c3c", "#fdecea", "#e74c3c", "J'ai compris", false);
    }

    // ── CONFIRMATION (retourne true si OK) ────────────────────────────────
    public static boolean showConfirmation(String titre, String message) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titre);
        dialog.setResizable(false);

        VBox root = buildRoot();

        // En-tête orange
        VBox header = buildHeader("❓", titre, "#e67e22");

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 24, 10, 24));

        VBox msgBox = new VBox(6);
        msgBox.setPadding(new Insets(12, 14, 12, 14));
        msgBox.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 8; " +
                "-fx-border-color: #f39c12; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        msgLbl.setMaxWidth(380); msgLbl.setWrapText(true);
        msgBox.getChildren().add(msgLbl);
        body.getChildren().add(msgBox);

        // Footer avec deux boutons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 22, 24));

        Button nonBtn = new Button("Non");
        nonBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 35; -fx-cursor: hand; -fx-font-size: 13;");
        nonBtn.setOnAction(e -> { result.set(false); dialog.close(); });

        Button ouiBtn = new Button("Oui, confirmer");
        ouiBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-size: 13;");
        ouiBtn.setOnAction(e -> { result.set(true); dialog.close(); });

        footer.getChildren().addAll(nonBtn, ouiBtn);
        root.getChildren().addAll(header, body, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
        return result.get();
    }

    // ── CONFIRMATION SUPPRESSION (rouge) ──────────────────────────────────
    public static boolean showDeleteConfirmation(String quoi) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmer la suppression");
        dialog.setResizable(false);

        VBox root = buildRoot();

        VBox header = buildHeader("🗑", "Confirmer la suppression", "#e74c3c");

        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 24, 10, 24));

        VBox msgBox = new VBox(6);
        msgBox.setPadding(new Insets(12, 14, 12, 14));
        msgBox.setStyle("-fx-background-color: #fdecea; -fx-background-radius: 8; " +
                "-fx-border-color: #e74c3c; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label msgLbl = new Label("Voulez-vous vraiment supprimer " + quoi + " ?\nCette action est irréversible.");
        msgLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        msgLbl.setMaxWidth(380); msgLbl.setWrapText(true);
        msgBox.getChildren().add(msgLbl);
        body.getChildren().add(msgBox);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 22, 24));

        Button annulerBtn = new Button("Annuler");
        annulerBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-size: 13;");
        annulerBtn.setOnAction(e -> { result.set(false); dialog.close(); });

        Button supprimerBtn = new Button("🗑 Supprimer");
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-size: 13;");
        supprimerBtn.setOnAction(e -> { result.set(true); dialog.close(); });

        footer.getChildren().addAll(annulerBtn, supprimerBtn);
        root.getChildren().addAll(header, body, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
        return result.get();
    }

    // ── INTERACTION MÉDICAMENTEUSE (warning, demande confirmation) ─────────
    public static boolean showInteractionWarning(String nomProduit, java.util.List<String> alertes) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Alerte médicamenteuse");
        dialog.setResizable(false);

        VBox root = buildRoot();
        root.setMinWidth(450); root.setMaxWidth(450);

        VBox header = buildHeader("⚠️", "Interaction détectée", "#e67e22");

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 24, 10, 24));

        // Bandeau warning
        HBox band = new HBox(8);
        band.setAlignment(Pos.CENTER_LEFT);
        band.setPadding(new Insets(10, 14, 10, 14));
        band.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 8; " +
                "-fx-border-color: #f0ad4e; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label bandLbl = new Label("⚠  Produit : " + nomProduit);
        bandLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        band.getChildren().add(bandLbl);
        body.getChildren().add(band);

        for (String alerte : alertes) {
            Label a = new Label("• " + alerte);
            a.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
            a.setMaxWidth(390); a.setWrapText(true);
            body.getChildren().add(a);
        }

        Label question = new Label("Voulez-vous quand même ajouter ce produit ?");
        question.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 6 0 0 0;");
        body.getChildren().add(question);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 22, 24));

        Button nonBtn = new Button("Non, annuler");
        nonBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 28; -fx-cursor: hand; -fx-font-size: 13;");
        nonBtn.setOnAction(e -> { result.set(false); dialog.close(); });

        Button ouiBtn = new Button("Oui, ajouter quand même");
        ouiBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 13;");
        ouiBtn.setOnAction(e -> { result.set(true); dialog.close(); });

        footer.getChildren().addAll(nonBtn, ouiBtn);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true); scroll.setMaxHeight(280);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, scroll, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
        return result.get();
    }

    // ── Helpers internes ──────────────────────────────────────────────────
    private static VBox buildRoot() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setMinWidth(420); root.setMaxWidth(420);
        return root;
    }

    private static VBox buildHeader(String iconText, String titre, String color) {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 20, 16, 20));
        header.setStyle("-fx-background-color: " + color + ";");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(60, 60); iconCircle.setMaxSize(60, 60);
        iconCircle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 30;");
        Label iconLbl = new Label(iconText);
        iconLbl.setStyle("-fx-font-size: 26; -fx-text-fill: white;");
        iconCircle.getChildren().add(iconLbl);

        Label titleLbl = new Label(titre);
        titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");

        header.getChildren().addAll(iconCircle, titleLbl);
        return header;
    }

    private static void show(String icon, String titre, String message,
                              String headerColor, String bodyBg, String borderColor,
                              String btnLabel, boolean autoClose) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titre);
        dialog.setResizable(false);

        VBox root = buildRoot();
        VBox header = buildHeader(icon, titre, headerColor);

        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 24, 10, 24));

        VBox msgBox = new VBox(6);
        msgBox.setPadding(new Insets(12, 14, 12, 14));
        msgBox.setStyle("-fx-background-color: " + bodyBg + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        msgLbl.setMaxWidth(370); msgLbl.setWrapText(true);
        msgBox.getChildren().add(msgLbl);
        body.getChildren().add(msgBox);

        VBox footer = new VBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 24, 22, 24));

        Button btn = new Button(btnLabel);
        btn.setStyle("-fx-background-color: " + headerColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 10 45; -fx-cursor: hand; -fx-font-size: 13;");
        btn.setOnAction(e -> dialog.close());
        footer.getChildren().add(btn);

        root.getChildren().addAll(header, body, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }
}
