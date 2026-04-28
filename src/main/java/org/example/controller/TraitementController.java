package org.example.controller; // Déclaration du package

import javafx.collections.FXCollections; // Utilitaire pour créer des listes observables
import javafx.fxml.FXML; // Annotation pour lier les éléments FXML
import javafx.fxml.FXMLLoader; // Chargeur de fichiers FXML
import javafx.scene.Parent; // Noeud racine de la scène
import javafx.scene.Scene; // Scène JavaFX
import javafx.scene.control.*; // Composants UI (TextField, DatePicker, ListView, etc.)
import javafx.scene.layout.StackPane; // Conteneur empilé
import javafx.scene.layout.VBox; // Conteneur vertical
import javafx.stage.Stage; // Fenêtre principale
import org.example.model.User; // Modèle utilisateur
import org.example.service.UserService; // Service de gestion des utilisateurs
import org.example.util.DatabaseUtil; // Utilitaire de connexion à la base de données

import java.io.IOException; // Exception d'entrée/sortie
import java.sql.*; // Classes JDBC
import java.time.LocalDate; // Date sans heure
import java.time.LocalDateTime; // Date avec heure
import java.time.format.DateTimeFormatter; // Formateur de date
import java.util.List;

// Contrôleur front-office pour la demande de traitement par le client
public class TraitementController {

    @FXML private TextField nomPrenomField; // Champ nom et prénom (pré-rempli, non éditable)
    @FXML private TextField emailField; // Champ email (pré-rempli, non éditable)
    @FXML private DatePicker dateNaissanceField; // Sélecteur de date de naissance
    @FXML private TextArea antecedentsField; // Zone de texte pour les antécédents médicaux
    @FXML private TextArea symptomesField; // Zone de texte pour les symptômes
    @FXML private ComboBox<String> produitCombo; // ComboBox pour sélectionner un produit
    @FXML private VBox selectedProduitsBox; // Conteneur des produits ajoutés (tags avec bouton ×)
    @FXML private Button addProduitBtn; // Bouton "+" pour ajouter un produit
    @FXML private CheckBox conditionsCheck; // Case à cocher conditions générales
    @FXML private Label errorLabel; // Label pour afficher les messages d'erreur
    @FXML private Button profileButton; // Bouton profil dans la navbar
    @FXML private Button submitButton; // Bouton de soumission (anti double-clic)
    @FXML private StackPane ordonnanceMenuContainer; // Conteneur du menu déroulant ordonnance
    @FXML private VBox ordonnanceDropdown; // Menu déroulant ordonnance

    @FXML private VBox fdaInfoBox;
    @FXML private Label fdaTitleLabel;
    @FXML private Label fdaEffetsLabel;
    @FXML private Label fdaContraLabel;
    @FXML private Label fdaInterLabel;

    private UserService userService = UserService.getInstance(); // Service pour récupérer l'utilisateur connecté
    private java.util.List<String> selectedProduits = new java.util.ArrayList<>(); // Liste des produits ajoutés par le client

    @FXML
    public void initialize() {
        // Pré-remplir les informations de l'utilisateur connecté
        User currentUser = userService.getCurrentUser(); // Récupérer l'utilisateur connecté
        if (currentUser != null) { // Si connecté
            nomPrenomField.setText(currentUser.getNom() != null ? currentUser.getNom() : ""); // Remplir le nom
            emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : ""); // Remplir l'email
        }

        loadProduits(); // Charger la liste des produits depuis la base

        // Listener OpenFDA : afficher infos produit à chaque sélection
        produitCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                fdaInfoBox.setVisible(false);
                fdaInfoBox.setManaged(false);
                return;
            }
            String nomProduit = newVal.contains(" - ") ? newVal.split(" - ", 2)[1] : newVal;
            User   fdaUser = userService.getCurrentUser();
            int userId = fdaUser != null ? fdaUser.getId() : 0;

            // Appel en thread daemon séparé pour ne pas bloquer l'UI
            Thread t = new Thread(() -> {
                org.example.util.OpenFDAService.DrugInfo info =
                        org.example.util.OpenFDAService.getInstance().getInfo(nomProduit);
                javafx.application.Platform.runLater(() -> {
                    fdaTitleLabel.setText("ℹ️ Informations OpenFDA : " + nomProduit);

                    // Effets secondaires
                    fdaEffetsLabel.setText(info.effetsSecondaires);

                    // Contre-indications
                    fdaContraLabel.setText(info.contreIndications);

                    // Interactions : OpenFDA + interactions locales avec produits déjà ajoutés
                    StringBuilder interText = new StringBuilder(info.interactions);
                    if (!selectedProduits.isEmpty()) {
                        interText.append("\n\n🔗 Interactions avec vos produits sélectionnés :");
                        for (String p : selectedProduits) {
                            String autreNom = p.contains(" - ") ? p.split(" - ", 2)[1] : p;
                            // Vérifier dans les deux sens
                            java.util.List<String> alertes = org.example.util.DrugInteractionService
                                    .getInstance().verifierInteractionsLocales(nomProduit, userId);
                            // Aussi vérifier la paire directement
                            boolean paireTrouvee = false;
                            for (String[] paire : org.example.util.DrugInteractionService.INTERACTIONS_LOCALES) {
                                boolean m1 = nomProduit.toLowerCase().contains(paire[0]) && autreNom.toLowerCase().contains(paire[1]);
                                boolean m2 = nomProduit.toLowerCase().contains(paire[1]) && autreNom.toLowerCase().contains(paire[0]);
                                if (m1 || m2) { paireTrouvee = true; break; }
                            }
                            if (paireTrouvee || !alertes.isEmpty()) {
                                interText.append("\n• ").append(autreNom).append(" : ⚠️ Interaction détectée");
                            } else {
                                interText.append("\n• ").append(autreNom).append(" : ✅ Aucune interaction connue");
                            }
                        }
                    }
                    fdaInterLabel.setText(interText.toString());
                    fdaInfoBox.setVisible(true);
                    fdaInfoBox.setManaged(true);
                });
            });
            t.setDaemon(true);
            t.start();
        });

        // Gestion du menu déroulant ordonnance au survol
        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> { ordonnanceDropdown.setVisible(true); ordonnanceDropdown.setManaged(true); }); // Afficher au survol
            ordonnanceMenuContainer.setOnMouseExited(e -> { ordonnanceDropdown.setVisible(false); ordonnanceDropdown.setManaged(false); }); // Masquer à la sortie
        }
    }

    // Charger les produits depuis la base de données dans le ComboBox
    private void loadProduits() {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection(); // Obtenir la connexion
            Statement stmt = conn.createStatement(); // Créer un statement
            ResultSet rs = stmt.executeQuery("SELECT id_produit, nom FROM produit ORDER BY nom"); // Requête pour récupérer tous les produits
            javafx.collections.ObservableList<String> items = FXCollections.observableArrayList(); // Liste observable pour le ComboBox
            while (rs.next()) { // Parcourir les résultats
                items.add(rs.getInt("id_produit") + " - " + rs.getString("nom")); // Ajouter chaque produit (format "ID - Nom")
            }
            produitCombo.setItems(items); // Remplir le ComboBox
            rs.close(); // Fermer le ResultSet
            stmt.close(); // Fermer le Statement
        } catch (SQLException e) {
            System.out.println("Erreur chargement produits: " + e.getMessage()); // Log de l'erreur
        }
    }

    // Recommandation IA : suggère les produits les plus prescrits pour des symptômes similaires
    @FXML
    private void handleRecommandationIA() {
        String symptomes = symptomesField.getText() != null ? symptomesField.getText().trim() : "";
        if (symptomes.length() < 5) {
            errorLabel.setText("Décrivez vos symptômes (min 5 caractères) pour obtenir une recommandation.");
            return;
        }

        String antecedents = antecedentsField.getText() != null ? antecedentsField.getText().trim() : "";

        // Extraire les mots-clés (mots de plus de 3 lettres)
        String[] mots = symptomes.toLowerCase().split("[\\s,;.!?]+");
        java.util.List<String> motsCles = new java.util.ArrayList<>();
        for (String mot : mots) {
            if (mot.length() > 3) motsCles.add(mot);
        }
        if (motsCles.isEmpty()) {
            errorLabel.setText("Symptômes trop courts pour une recommandation.");
            return;
        }

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Chercher le produit le plus prescrit pour ces symptômes
            StringBuilder sql = new StringBuilder(
                "SELECT p.id_produit, p.nom, COUNT(t.id_traitement) AS nb_prescriptions " +
                "FROM traitement t " +
                "JOIN produit p ON t.id_produit_id = p.id_produit " +
                "JOIN ordonnance o ON t.id_ordonnance_id = o.id_ordonnance " +
                "WHERE t.status IN ('actif', 'terminé') AND ("
            );
            java.util.List<String> conditions = new java.util.ArrayList<>();
            for (String mot : motsCles) conditions.add("LOWER(o.note_medical) LIKE ?");
            sql.append(String.join(" OR ", conditions));
            sql.append(") GROUP BY p.id_produit, p.nom ORDER BY nb_prescriptions DESC LIMIT 10");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < motsCles.size(); i++) ps.setString(i + 1, "%" + motsCles.get(i) + "%");

            ResultSet rs = ps.executeQuery();
            java.util.List<String[]> candidats = new java.util.ArrayList<>(); // [id, nom, nb]
            while (rs.next()) {
                candidats.add(new String[]{
                    String.valueOf(rs.getInt("id_produit")),
                    rs.getString("nom"),
                    String.valueOf(rs.getInt("nb_prescriptions"))
                });
            }
            rs.close(); ps.close();

            // Fallback : produits les plus prescrits en général
            if (candidats.isEmpty()) {
                PreparedStatement psFb = conn.prepareStatement(
                    "SELECT p.id_produit, p.nom, COUNT(t.id_traitement) AS nb " +
                    "FROM traitement t JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "WHERE t.status IN ('actif', 'terminé') " +
                    "GROUP BY p.id_produit, p.nom ORDER BY nb DESC LIMIT 10");
                ResultSet rsFb = psFb.executeQuery();
                while (rsFb.next()) {
                    candidats.add(new String[]{
                        String.valueOf(rsFb.getInt("id_produit")),
                        rsFb.getString("nom"),
                        String.valueOf(rsFb.getInt("nb"))
                    });
                }
                rsFb.close(); psFb.close();
            }

            if (candidats.isEmpty()) {
                errorLabel.setText("Aucune recommandation disponible pour ces symptômes.");
                return;
            }

            // Filtrer les produits allergènes selon les antécédents
            org.example.util.DrugInteractionService dis = org.example.util.DrugInteractionService.getInstance();
            String[] meilleur = null;
            String raisonEviction = "";
            for (String[] candidat : candidats) {
                org.example.util.DrugInteractionService.AllergieResult allergie =
                    dis.verifierAllergieIntelligente(candidat[1], antecedents);
                if (!allergie.critique) {
                    meilleur = candidat;
                    break;
                } else {
                    raisonEviction = candidat[1] + " écarté (allergie détectée)";
                    System.out.println("[IA] " + raisonEviction);
                }
            }

            if (meilleur == null) {
                errorLabel.setText("⚠️ Tous les produits recommandés sont incompatibles avec vos antécédents.");
                return;
            }

            // Ajouter automatiquement le meilleur produit
            String produitEntry = meilleur[0] + " - " + meilleur[1];
            if (selectedProduits.contains(produitEntry)) {
                errorLabel.setText("\"" + meilleur[1] + "\" est déjà dans votre liste.");
                return;
            }

            selectedProduits.add(produitEntry);
            refreshProduitsBox();
            produitCombo.setValue(produitEntry);
            errorLabel.setText("");

            // Afficher une notification de confirmation (non bloquante)
            showIAConfirmation(meilleur[1], meilleur[2], symptomes);

        } catch (SQLException e) {
            errorLabel.setText("Erreur lors de la recommandation : " + e.getMessage());
        }
    }

    /** Notification légère après ajout automatique par l'IA */
    private void showIAConfirmation(String produitNom, String nbPrescriptions, String symptomes) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.NONE); // non bloquant
        popup.setTitle("🤖 Recommandation IA");
        popup.setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new javafx.geometry.Insets(24, 28, 20, 28));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        root.setMinWidth(360);

        // En-tête
        javafx.scene.layout.HBox hdr = new javafx.scene.layout.HBox(10);
        hdr.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 28;");
        VBox hdrText = new VBox(2);
        Label title = new Label("Produit ajouté automatiquement");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");
        Label sub = new Label("Basé sur l'analyse de vos symptômes");
        sub.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");
        hdrText.getChildren().addAll(title, sub);
        hdr.getChildren().addAll(icon, hdrText);

        // Produit sélectionné
        VBox prodBox = new VBox(5);
        prodBox.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        prodBox.setStyle("-fx-background-color: #f0f7f4; -fx-background-radius: 8; " +
                "-fx-border-color: #1f6f5c; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
        Label prodLbl = new Label("💊 " + produitNom);
        prodLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1f6f5c;");
        Label prescLbl = new Label("Prescrit " + nbPrescriptions + " fois pour des symptômes similaires");
        prescLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");
        prodBox.getChildren().addAll(prodLbl, prescLbl);

        // Symptômes analysés
        Label symptLbl = new Label("Symptômes analysés : " + (symptomes.length() > 60 ? symptomes.substring(0, 60) + "..." : symptomes));
        symptLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #888; -fx-wrap-text: true;");
        symptLbl.setWrapText(true);
        symptLbl.setMaxWidth(310);

        Button okBtn = new Button("Compris");
        okBtn.setStyle("-fx-background-color: #1f6f5c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 8 35; -fx-cursor: hand;");
        okBtn.setOnAction(e -> popup.close());

        VBox btnBox = new VBox();
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        btnBox.getChildren().add(okBtn);

        root.getChildren().addAll(hdr, prodBox, symptLbl, btnBox);
        popup.setScene(new javafx.scene.Scene(root));
        popup.show();

        // Fermeture automatique après 4 secondes
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
            javafx.util.Duration.seconds(4));
        pause.setOnFinished(e -> popup.close());
        pause.play();
    }

    // Ajouter un produit sélectionné à la liste (bouton "+")
    @FXML
    private void addProduit() {
        String selected = produitCombo.getValue();
        if (selected == null) return;
        if (selectedProduits.contains(selected)) return;

        // Extraire le nom du produit (format "ID - Nom")
        String nomProduit = selected.contains(" - ") ? selected.split(" - ", 2)[1] : selected;
        String antecedents = antecedentsField.getText() != null ? antecedentsField.getText().trim() : "";
        String symptomes = symptomesField.getText() != null ? symptomesField.getText().trim() : "";
        User currentUser = userService.getCurrentUser();
        int userId = currentUser != null ? currentUser.getId() : 0;

        // 1. Vérification allergie intelligente (par principe actif)
        org.example.util.DrugInteractionService dis = org.example.util.DrugInteractionService.getInstance();
        org.example.util.DrugInteractionService.AllergieResult allergieResult =
                dis.verifierAllergieIntelligente(nomProduit, antecedents);

        if (allergieResult.critique) {
            // Afficher la fenêtre "ACTION REQUISE" bloquante
            showAllergieBloquanteDialog(nomProduit, allergieResult, symptomes);
            return; // Bloqué — ne pas ajouter
        }

        // 2. Vérification interactions (non bloquant, demande confirmation)
        List<String> alertes = dis.verifierTout(nomProduit, antecedents, userId, selectedProduits);
        // Retirer les alertes d'allergie déjà traitées ci-dessus
        alertes.removeIf(a -> a.startsWith("⚠️ Allergie"));

        if (!alertes.isEmpty()) {
            boolean confirme = org.example.util.DialogService.showInteractionWarning(nomProduit, alertes);
            if (confirme) {
                selectedProduits.add(selected);
                refreshProduitsBox();
                produitCombo.setValue(null);
                errorLabel.setText("");
            }
        } else {
            selectedProduits.add(selected);
            refreshProduitsBox();
            produitCombo.setValue(null);
            errorLabel.setText("");
        }
    }

    /**
     * Affiche la fenêtre "ACTION REQUISE" bloquante en cas d'allergie critique.
     * Propose automatiquement une alternative IA.
     */
    private void showAllergieBloquanteDialog(String nomProduit,
            org.example.util.DrugInteractionService.AllergieResult result, String symptomes) {

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("ACTION REQUISE");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 16;");
        root.setMinWidth(420);
        root.setMaxWidth(420);

        // ── En-tête avec icône ✕ ──────────────────────────────────────────
        VBox header = new VBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        header.setPadding(new javafx.geometry.Insets(28, 20, 16, 20));

        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
        iconCircle.setMinSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: transparent; -fx-border-color: #e74c3c; " +
                "-fx-border-width: 3; -fx-border-radius: 32; -fx-background-radius: 32;");
        Label iconX = new Label("✕");
        iconX.setStyle("-fx-font-size: 28; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        iconCircle.getChildren().add(iconX);

        Label titleLabel = new Label("ACTION REQUISE !");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        header.getChildren().addAll(iconCircle, titleLabel);

        // ── Corps ─────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new javafx.geometry.Insets(0, 24, 16, 24));

        // Bandeau CRITIQUE
        javafx.scene.layout.HBox critiqueBand = new javafx.scene.layout.HBox(8);
        critiqueBand.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        critiqueBand.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        critiqueBand.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 8; " +
                "-fx-border-color: #f0ad4e; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label critiqueIcon = new Label("⚠");
        critiqueIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #e67e22;");
        Label critiqueLabel = new Label("CRITIQUE");
        critiqueLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        critiqueBand.getChildren().addAll(critiqueIcon, critiqueLabel);

        // Problèmes détectés
        VBox problemesBox = new VBox(6);
        Label problemesTitle = new Label("Problèmes détectés:");
        problemesTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        problemesBox.getChildren().add(problemesTitle);
        for (String pb : result.problemes) {
            Label pbLabel = new Label("  • " + pb);
            pbLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
            pbLabel.setMaxWidth(370);
            pbLabel.setWrapText(true);
            problemesBox.getChildren().add(pbLabel);
        }

        // Recommandation
        VBox recoBox = new VBox(6);
        recoBox.setPadding(new javafx.geometry.Insets(10, 12, 10, 12));
        recoBox.setStyle("-fx-background-color: #f0f7f4; -fx-background-radius: 8; " +
                "-fx-border-color: #27ae60; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
        Label recoTitle = new Label("💡 Recommandation:");
        recoTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        Label recoText = new Label(result.recommandation);
        recoText.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        recoText.setMaxWidth(370);
        recoText.setWrapText(true);
        recoBox.getChildren().addAll(recoTitle, recoText);

        // Alternative IA (si disponible)
        if (result.alternativeSuggestion != null && !result.alternativeSuggestion.isBlank()) {
            VBox altBox = new VBox(6);
            altBox.setPadding(new javafx.geometry.Insets(10, 12, 10, 12));
            altBox.setStyle("-fx-background-color: #eaf4fb; -fx-background-radius: 8; " +
                    "-fx-border-color: #2980b9; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");
            Label altTitle = new Label("🤖 Alternative suggérée par l'IA:");
            altTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
            Label altText = new Label(result.alternativeSuggestion);
            altText.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            altText.setMaxWidth(370);
            altText.setWrapText(true);
            altBox.getChildren().addAll(altTitle, altText);
            body.getChildren().addAll(critiqueBand, problemesBox, recoBox, altBox);
        } else {
            body.getChildren().addAll(critiqueBand, problemesBox, recoBox);
        }

        // ── Pied de page bloquant ─────────────────────────────────────────
        VBox footer = new VBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER);
        footer.setPadding(new javafx.geometry.Insets(12, 24, 24, 24));

        Label blocLabel = new Label("✖ VOUS NE POUVEZ PAS CONTINUER.");
        blocLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        Label blocSub = new Label("Veuillez consulter un médecin immédiatement.");
        blocSub.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");

        Button comprisBtn = new Button("J'ai compris");
        comprisBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 12 50; -fx-cursor: hand; -fx-font-size: 14;");
        comprisBtn.setOnAction(e -> dialog.close());

        footer.getChildren().addAll(blocLabel, blocSub, comprisBtn);

        root.getChildren().addAll(header, body, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Fenêtre custom "ACTION REQUISE" lors de la soumission finale avec allergies détectées.
     * Même style que showAllergieBloquanteDialog.
     */
    private void showAllergiesSoumissionDialog(
            List<org.example.util.DrugInteractionService.AllergieResult> resultats,
            List<String> nomsProduits) {

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("ACTION REQUISE");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 16;");
        root.setMinWidth(440);
        root.setMaxWidth(440);

        // ── En-tête ───────────────────────────────────────────────────
        VBox header = new VBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        header.setPadding(new javafx.geometry.Insets(28, 20, 16, 20));

        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
        iconCircle.setMinSize(64, 64); iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: transparent; -fx-border-color: #e74c3c; " +
                "-fx-border-width: 3; -fx-border-radius: 32; -fx-background-radius: 32;");
        Label iconX = new Label("✕");
        iconX.setStyle("-fx-font-size: 28; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        iconCircle.getChildren().add(iconX);

        Label titleLabel = new Label("ACTION REQUISE !");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subLabel = new Label("Impossible d'envoyer la demande");
        subLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #888;");
        header.getChildren().addAll(iconCircle, titleLabel, subLabel);

        // ── Corps scrollable ──────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new javafx.geometry.Insets(0, 24, 16, 24));

        // Bandeau CRITIQUE
        javafx.scene.layout.HBox critiqueBand = new javafx.scene.layout.HBox(8);
        critiqueBand.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        critiqueBand.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        critiqueBand.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 8; " +
                "-fx-border-color: #f0ad4e; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label critiqueIcon = new Label("⚠");
        critiqueIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #e67e22;");
        Label critiqueLabel = new Label("CRITIQUE — " + resultats.size() + " allergie(s) détectée(s)");
        critiqueLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        critiqueBand.getChildren().addAll(critiqueIcon, critiqueLabel);
        body.getChildren().add(critiqueBand);

        // Une carte par produit allergène
        for (int i = 0; i < resultats.size(); i++) {
            org.example.util.DrugInteractionService.AllergieResult r = resultats.get(i);
            String nomProduit = nomsProduits.get(i);

            VBox prodCard = new VBox(6);
            prodCard.setPadding(new javafx.geometry.Insets(10, 12, 10, 12));
            prodCard.setStyle("-fx-background-color: #fdecea; -fx-background-radius: 8; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 8 8 0;");

            Label prodTitle = new Label("💊 " + nomProduit);
            prodTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #c0392b;");
            prodCard.getChildren().add(prodTitle);

            for (String pb : r.problemes) {
                Label pbLabel = new Label("  • " + pb);
                pbLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
                pbLabel.setMaxWidth(380); pbLabel.setWrapText(true);
                prodCard.getChildren().add(pbLabel);
            }

            // Alternative IA si disponible
            if (r.alternativeSuggestion != null && !r.alternativeSuggestion.isBlank()) {
                VBox altBox = new VBox(4);
                altBox.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));
                altBox.setStyle("-fx-background-color: #eaf4fb; -fx-background-radius: 6; " +
                        "-fx-border-color: #2980b9; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 6 6 0;");
                Label altTitle = new Label("🤖 Alternative suggérée :");
                altTitle.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
                Label altText = new Label(r.alternativeSuggestion);
                altText.setStyle("-fx-font-size: 11; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
                altText.setMaxWidth(360); altText.setWrapText(true);
                altBox.getChildren().addAll(altTitle, altText);
                prodCard.getChildren().add(altBox);
            }
            body.getChildren().add(prodCard);
        }

        // ── Pied de page bloquant ─────────────────────────────────────
        VBox footer = new VBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER);
        footer.setPadding(new javafx.geometry.Insets(12, 24, 24, 24));

        Label blocLabel = new Label("✖ VOUS NE POUVEZ PAS ENVOYER CETTE DEMANDE.");
        blocLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        Label blocSub = new Label("Retirez les produits concernés ou consultez un médecin.");
        blocSub.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c;");

        Button comprisBtn = new Button("J'ai compris");
        comprisBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 12 50; -fx-cursor: hand; -fx-font-size: 14;");
        comprisBtn.setOnAction(e -> dialog.close());
        footer.getChildren().addAll(blocLabel, blocSub, comprisBtn);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, scroll, footer);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    // Supprimer un produit de la liste
    private void removeProduit(String produit) {
        selectedProduits.remove(produit); // Retirer de la liste interne
        refreshProduitsBox(); // Rafraîchir l'affichage
    }

    // Rafraîchir l'affichage des produits ajoutés (tags avec bouton ×)
    private void refreshProduitsBox() {
        selectedProduitsBox.getChildren().clear(); // Vider le conteneur
        for (String prod : selectedProduits) { // Pour chaque produit ajouté
            javafx.scene.layout.HBox tag = new javafx.scene.layout.HBox(8); // Créer un tag horizontal
            tag.setAlignment(javafx.geometry.Pos.CENTER_LEFT); // Aligner à gauche
            tag.getStyleClass().add("traitement-produit-tag"); // Appliquer le style CSS
            Label lbl = new Label(prod.split(" - ")[1]); // Afficher le nom du produit (sans l'ID)
            lbl.setStyle("-fx-font-size: 13; -fx-text-fill: #1f6f5c; -fx-font-weight: bold;"); // Style du texte
            Button removeBtn = new Button("×"); // Bouton de suppression
            removeBtn.getStyleClass().add("traitement-btn-remove"); // Style CSS
            removeBtn.setOnAction(e -> removeProduit(prod)); // Action : supprimer ce produit
            tag.getChildren().addAll(lbl, removeBtn); // Ajouter le label et le bouton au tag
            selectedProduitsBox.getChildren().add(tag); // Ajouter le tag au conteneur
        }
    }

    // Méthode appelée lors du clic sur "Envoyer la demande"
    @FXML
    private void handleSubmit() {
        errorLabel.setText(""); // Réinitialiser le message d'erreur

        submitButton.setDisable(true); // Désactiver le bouton pour éviter le double-clic

        // Contrôle : nom et prénom obligatoires
        String nomPrenom = nomPrenomField.getText() != null ? nomPrenomField.getText().trim() : ""; // Récupérer et nettoyer le nom
        if (nomPrenom.isEmpty()) { // Si vide
            errorLabel.setText("Veuillez remplir tous les champs obligatoires"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return; // Arrêter
        }
        if (nomPrenom.length() < 3) { // Si trop court
            errorLabel.setText("Le nom et prénom doivent contenir au moins 3 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        // Contrôle : nom ne doit contenir que des lettres, espaces et tirets
        if (!nomPrenom.matches("[a-zA-ZÀ-ÿ\\s\\-]+")) { // Regex pour lettres accentuées, espaces, tirets
            errorLabel.setText("Le nom et prénom ne doivent contenir que des lettres, espaces et tirets."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : email obligatoire et format valide
        String email = emailField.getText() != null ? emailField.getText().trim() : ""; // Récupérer et nettoyer l'email
        if (email.isEmpty()) { // Si vide
            errorLabel.setText("Veuillez remplir tous les champs obligatoires"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { // Regex de validation email
            errorLabel.setText("L'adresse email n'est pas valide."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : date de naissance obligatoire
        if (dateNaissanceField.getValue() == null) { // Si aucune date sélectionnée
            errorLabel.setText("Date invalide"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (dateNaissanceField.getValue().isAfter(LocalDate.now())) { // Si date dans le futur
            errorLabel.setText("Date invalide"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (dateNaissanceField.getValue().isAfter(LocalDate.now().minusYears(18))) { // Si âge < 18 ans
            errorLabel.setText("Vous devez avoir au moins 18 ans."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : antécédents médicaux (min 5, max 2000 caractères)
        String antecedents = antecedentsField.getText() != null ? antecedentsField.getText().trim() : ""; // Récupérer et nettoyer
        if (antecedents.length() < 5) { // Si trop court
            errorLabel.setText("Les antécédents médicaux doivent contenir au moins 5 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (antecedents.length() > 2000) { // Si trop long
            errorLabel.setText("Les antécédents médicaux ne doivent pas dépasser 2000 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : symptômes obligatoires (min 5, max 2000 caractères)
        String symptomes = symptomesField.getText() != null ? symptomesField.getText().trim() : ""; // Récupérer et nettoyer
        if (symptomes.isEmpty()) { // Si vide
            errorLabel.setText("Veuillez remplir tous les champs obligatoires"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (symptomes.length() < 5) { // Si trop court
            errorLabel.setText("La description des symptômes doit contenir au moins 5 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }
        if (symptomes.length() > 2000) { // Si trop long
            errorLabel.setText("La description des symptômes ne doit pas dépasser 2000 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : au moins un produit ajouté via le bouton "+"
        // Si un produit est sélectionné dans le ComboBox mais pas encore ajouté, l'ajouter automatiquement
        String pendingProd = produitCombo.getValue();
        if (pendingProd != null && !selectedProduits.contains(pendingProd)) {
            selectedProduits.add(pendingProd);
            refreshProduitsBox();
            produitCombo.setValue(null);
        }
        if (selectedProduits.isEmpty()) { // Si aucun produit ajouté
            errorLabel.setText("Veuillez ajouter au moins un produit."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Contrôle : conditions générales acceptées
        if (!conditionsCheck.isSelected()) { // Si case non cochée
            errorLabel.setText("Vous devez accepter les conditions générales."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver
            return;
        }

        // Vérification finale allergies sur tous les produits sélectionnés
        List<org.example.util.DrugInteractionService.AllergieResult> alertesAllergies = new java.util.ArrayList<>();
        List<String> nomsProduitsAllergenes = new java.util.ArrayList<>();
        for (String item : selectedProduits) {
            String nomProduit = item.contains(" - ") ? item.split(" - ", 2)[1] : item;
            org.example.util.DrugInteractionService.AllergieResult result =
                org.example.util.DrugInteractionService.getInstance()
                    .verifierAllergieIntelligente(nomProduit, antecedents);
            if (result.critique) {
                alertesAllergies.add(result);
                nomsProduitsAllergenes.add(nomProduit);
            }
        }
        if (!alertesAllergies.isEmpty()) {
            errorLabel.setText("Des allergies ont été détectées. Vérifiez vos produits.");
            showAllergiesSoumissionDialog(alertesAllergies, nomsProduitsAllergenes);
            submitButton.setDisable(false);
            return;
        }

        // Extraire les IDs des produits ajoutés depuis le format "ID - Nom"
        java.util.List<Integer> produitIds = new java.util.ArrayList<>(); // Liste des IDs
        for (String item : selectedProduits) { // Parcourir les produits ajoutés
            produitIds.add(Integer.parseInt(item.split(" - ")[0])); // Extraire l'ID avant le tiret
        }

        // Insertion en base de données
        try {
            User currentUser = userService.getCurrentUser();
            Connection conn = DatabaseUtil.getInstance().getConnection();
            int userId = currentUser != null ? currentUser.getId() : 0;

            // ── Détection de spam anti-abus ──────────────────────────────
            // Basé UNIQUEMENT sur id_utilisateur_id (non modifiable par le patient)
            // Peu importe si le patient change date de naissance, symptômes, antécédents

            // Règle 1 : max 2 ordonnances dans les 30 dernières minutes
            PreparedStatement psSpam = conn.prepareStatement(
                "SELECT COUNT(*) FROM ordonnance " +
                "WHERE id_utilisateur_id = ? AND date_ordonnance >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)"
            );
            psSpam.setInt(1, userId);
            ResultSet rsSpam = psSpam.executeQuery();
            int nbRecentes = 0;
            if (rsSpam.next()) nbRecentes = rsSpam.getInt(1);
            rsSpam.close(); psSpam.close();

            if (nbRecentes >= 2) {
                submitButton.setDisable(false);
                showSpamBlockDialog();
                return;
            }

            // Règle 2 : max 5 ordonnances dans les 24 heures
            PreparedStatement psSpam24 = conn.prepareStatement(
                "SELECT COUNT(*) FROM ordonnance " +
                "WHERE id_utilisateur_id = ? AND date_ordonnance >= DATE_SUB(NOW(), INTERVAL 24 HOUR)"
            );
            psSpam24.setInt(1, userId);
            ResultSet rsSpam24 = psSpam24.executeQuery();
            int nb24h = 0;
            if (rsSpam24.next()) nb24h = rsSpam24.getInt(1);
            rsSpam24.close(); psSpam24.close();

            if (nb24h >= 5) {
                submitButton.setDisable(false);
                showSpamBlockDialog24h();
                return;
            }

            // Règle 3 : détecter doublon exact — mêmes produits soumis dans les 24h
            // Construire la liste des IDs produits sélectionnés
            java.util.List<Integer> spamProduitIds = new java.util.ArrayList<>();
            for (String item : selectedProduits) {
                try { spamProduitIds.add(Integer.parseInt(item.split(" - ")[0])); } catch (Exception ignored) {}
            }
            if (!spamProduitIds.isEmpty()) {
                // Chercher une ordonnance récente avec les mêmes produits
                PreparedStatement psDouble = conn.prepareStatement(
                    "SELECT o.id_ordonnance FROM ordonnance o " +
                    "WHERE o.id_utilisateur_id = ? " +
                    "AND o.date_ordonnance >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                    "AND (SELECT COUNT(*) FROM traitement t " +
                    "     WHERE t.id_ordonnance_id = o.id_ordonnance " +
                    "     AND t.id_produit_id IN (" +
                    String.join(",", java.util.Collections.nCopies(spamProduitIds.size(), "?")) +
                    ")) = ? LIMIT 1"
                );
                psDouble.setInt(1, userId);
                for (int i = 0; i < spamProduitIds.size(); i++) psDouble.setInt(i + 2, spamProduitIds.get(i));
                psDouble.setInt(spamProduitIds.size() + 2, spamProduitIds.size());
                ResultSet rsDouble = psDouble.executeQuery();
                boolean doublon = rsDouble.next();
                rsDouble.close(); psDouble.close();

                if (doublon) {
                    submitButton.setDisable(false);
                    showDoublonDialog();
                    return;
                }
            }
            // ── Fin détection spam ───────────────────────────────────────

            // Créer l'ordonnance brouillon associée
            PreparedStatement psOrd = conn.prepareStatement(
                    "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS); // Insertion avec récupération de l'ID généré
            String tempNumero = "ORD-" + java.time.LocalDateTime.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000)); // Générer le numéro
            psOrd.setString(1, tempNumero); // Numéro d'ordonnance
            psOrd.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now())); // Date ordonnance = maintenant
            psOrd.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusYears(1))); // Expiration = dans 1 an
            psOrd.setString(4, "brouillon"); // Statut initial = brouillon
            psOrd.setString(5, ""); // Note vide
            psOrd.setInt(6, userId); // ID du patient
            psOrd.executeUpdate(); // Exécuter l'insertion
            ResultSet ordKeys = psOrd.getGeneratedKeys(); // Récupérer l'ID généré
            int newOrdonnanceId = 0; // Variable pour stocker l'ID
            if (ordKeys.next()) { // Si ID disponible
                newOrdonnanceId = ordKeys.getInt(1); // Stocker l'ID
            }
            ordKeys.close(); // Fermer
            psOrd.close(); // Fermer

            // Insérer un traitement par produit sélectionné
            int firstTraitementId = 0; // ID du premier traitement (pour la redirection)
            for (int produitId : produitIds) { // Pour chaque produit sélectionné
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO traitement (id_utilisateur_id, dosage, frequence, duree_jours, date_debut, status, notes, id_ordonnance_id, id_produit_id, repas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS); // Insertion traitement
                ps.setInt(1, userId); // ID patient
                ps.setString(2, ""); // Dosage vide (sera rempli par l'admin)
                ps.setString(3, ""); // Fréquence vide
                ps.setInt(4, 0); // Durée = 0 (sera définie par l'admin)
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now())); // Date début = maintenant
                ps.setString(6, "en_attente"); // Statut = en attente
                ps.setString(7, ""); // Notes vides
                ps.setInt(8, newOrdonnanceId); // Lier à l'ordonnance créée
                ps.setInt(9, produitId); // ID du produit
                ps.setString(10, ""); // Repas vide
                ps.executeUpdate(); // Exécuter l'insertion

                if (firstTraitementId == 0) { // Récupérer l'ID du premier traitement seulement
                    ResultSet traitKeys = ps.getGeneratedKeys(); // Récupérer l'ID généré
                    if (traitKeys.next()) { // Si disponible
                        firstTraitementId = traitKeys.getInt(1); // Stocker
                    }
                    traitKeys.close(); // Fermer
                }
                ps.close(); // Fermer le PreparedStatement
            }

            goToOrdonnance(firstTraitementId, newOrdonnanceId, tempNumero); // Rediriger vers la page ordonnance avec les paramètres
        } catch (SQLException e) { // En cas d'erreur SQL
            errorLabel.setText("Erreur lors de l'enregistrement: " + e.getMessage()); // Afficher l'erreur
            submitButton.setDisable(false); // Réactiver le bouton
        } catch (IOException e) { // En cas d'erreur de navigation
            e.printStackTrace(); // Log
            submitButton.setDisable(false); // Réactiver
        }
    }

    // Fenêtre de blocage spam
    private void showSpamBlockDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Limite atteinte");
        dialog.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(35, 45, 35, 45));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        root.setMinWidth(400);

        Label iconLabel = new Label("🚫");
        iconLabel.setStyle("-fx-font-size: 48;");

        Label titleLabel = new Label("Limite d'envoi atteinte");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Label msgLabel = new Label("Vous avez dépassé le seuil d'envoi d'ordonnances.\nVeuillez réessayer dans 30 minutes.");
        msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #555; -fx-text-alignment: center; -fx-wrap-text: true;");
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setMaxWidth(320);

        Label infoLabel = new Label("⚠️ Pour votre sécurité, le nombre d'ordonnances\nest limité à 2 par tranche de 30 minutes.");
        infoLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888; -fx-text-alignment: center; -fx-wrap-text: true; " +
                "-fx-background-color: #fff3cd; -fx-padding: 10; -fx-background-radius: 8;");
        infoLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        infoLabel.setMaxWidth(320);

        Button comprisBtn = new Button("Compris !");
        comprisBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 12 45; -fx-cursor: hand; -fx-font-size: 14;");
        comprisBtn.setOnAction(ev -> {
            dialog.close();
            try { goToAccueil(); } catch (IOException ex) { ex.printStackTrace(); }
        });

        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, infoLabel, comprisBtn);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // Blocage spam 24h (max 5 ordonnances/jour)
    private void showSpamBlockDialog24h() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Limite journalière atteinte");
        dialog.setResizable(false);

        VBox root = buildSpamRoot();

        Label iconLabel = new Label("🚫");
        iconLabel.setStyle("-fx-font-size: 48;");
        Label titleLabel = new Label("Limite journalière atteinte");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        Label msgLabel = new Label("Vous avez soumis 5 ordonnances aujourd'hui.\nVeuillez réessayer demain.");
        msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #555; -fx-text-alignment: center; -fx-wrap-text: true;");
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setMaxWidth(320);
        Label infoLabel = buildInfoLabel("Pour votre sécurité, le nombre d'ordonnances est limité à 5 par jour.");

        Button btn = buildSpamBtn("Compris", dialog);
        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, infoLabel, btn);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    // Blocage doublon — mêmes produits dans les 24h
    private void showDoublonDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Doublon détecté");
        dialog.setResizable(false);

        VBox root = buildSpamRoot();

        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 48;");
        Label titleLabel = new Label("Demande en double détectée");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        Label msgLabel = new Label("Vous avez déjà soumis une demande avec les mêmes produits dans les dernières 24 heures.\nConsultez vos ordonnances existantes.");
        msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #555; -fx-text-alignment: center; -fx-wrap-text: true;");
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setMaxWidth(320);
        Label infoLabel = buildInfoLabel("Cette vérification est basée sur votre compte et ne peut pas être contournée en modifiant vos informations.");

        Button btn = buildSpamBtn("Compris", dialog);
        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, infoLabel, btn);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    private VBox buildSpamRoot() {
        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(35, 45, 35, 45));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        root.setMinWidth(420);
        return root;
    }

    private Label buildInfoLabel(String text) {
        Label lbl = new Label("⚠️ " + text);
        lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #888; -fx-text-alignment: center; " +
                "-fx-wrap-text: true; -fx-background-color: #fff3cd; -fx-padding: 10; -fx-background-radius: 8;");
        lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lbl.setMaxWidth(340);
        lbl.setWrapText(true);
        return lbl;
    }

    private Button buildSpamBtn(String label, javafx.stage.Stage dialog) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 12 45; -fx-cursor: hand; -fx-font-size: 14;");
        btn.setOnAction(ev -> dialog.close());
        return btn;
    }

    // Annuler et retourner à l'accueil
    @FXML
    private void handleCancel() {
        try {
            goToAccueil(); // Naviguer vers l'accueil
        } catch (IOException e) {
            e.printStackTrace(); // Log de l'erreur
        }
    }

    // Navigation vers la page d'accueil
    @FXML
    private void goToAccueil() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml")); // Charger le FXML
        Parent root = loader.load(); // Charger le noeud racine
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // Ajouter le CSS
        Stage stage = (Stage) nomPrenomField.getScene().getWindow(); // Récupérer la fenêtre
        stage.setScene(scene); // Changer la scène
        stage.setFullScreen(true); // Plein écran
    }

    // Déjà sur la page traitement
    @FXML
    private void goToTraitement() {
        // Déjà sur cette page, rien à faire
    }

    // Navigation vers la page "Mes Ordonnances"
    @FXML
    private void goToMesOrdonnances() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesOrdonnances.fxml")); // Charger le FXML
        Parent root = loader.load(); // Charger
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // CSS
        Stage stage = (Stage) nomPrenomField.getScene().getWindow(); // Fenêtre
        stage.setScene(scene); // Changer
        stage.setFullScreen(true); // Plein écran
    }

    // Navigation vers la page de création d'ordonnance
    @FXML
    private void goToCreerOrdonnance() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml")); // Charger le FXML
        Parent root = loader.load(); // Charger
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // CSS
        Stage stage = (Stage) nomPrenomField.getScene().getWindow(); // Fenêtre
        stage.setScene(scene); // Changer
        stage.setFullScreen(true); // Plein écran
    }

    // Navigation vers la page ordonnance avec passage de paramètres (traitement + ordonnance)
    private void goToOrdonnance(int traitementId, int ordonnanceId, String numeroOrdonnance) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml")); // Charger le FXML
        Parent root = loader.load(); // Charger le noeud racine
        OrdonnanceController controller = loader.getController(); // Récupérer le contrôleur de la page ordonnance
        controller.setTraitementId(traitementId); // Passer l'ID du traitement
        controller.setOrdonnanceId(ordonnanceId, numeroOrdonnance); // Passer l'ID et le numéro de l'ordonnance
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // CSS
        Stage stage = (Stage) nomPrenomField.getScene().getWindow(); // Fenêtre
        stage.setScene(scene); // Changer
        stage.setFullScreen(true); // Plein écran
    }
}
