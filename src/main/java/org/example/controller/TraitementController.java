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

    // Ajouter un produit sélectionné à la liste (bouton "+")
    @FXML
    private void addProduit() {
        String selected = produitCombo.getValue(); // Récupérer le produit sélectionné
        if (selected == null) return; // Rien sélectionné, on sort
        if (selectedProduits.contains(selected)) return; // Déjà ajouté, on ignore
        selectedProduits.add(selected); // Ajouter à la liste interne
        refreshProduitsBox(); // Rafraîchir l'affichage
        produitCombo.setValue(null); // Réinitialiser le ComboBox
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

        // Extraire les IDs des produits ajoutés depuis le format "ID - Nom"
        java.util.List<Integer> produitIds = new java.util.ArrayList<>(); // Liste des IDs
        for (String item : selectedProduits) { // Parcourir les produits ajoutés
            produitIds.add(Integer.parseInt(item.split(" - ")[0])); // Extraire l'ID avant le tiret
        }

        // Insertion en base de données
        try {
            User currentUser = userService.getCurrentUser(); // Récupérer l'utilisateur connecté
            Connection conn = DatabaseUtil.getInstance().getConnection(); // Obtenir la connexion
            int userId = currentUser != null ? currentUser.getId() : 0; // ID du patient

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
