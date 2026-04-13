package org.example.controller; // Déclaration du package

import javafx.fxml.FXML; // Annotation pour lier les éléments FXML
import javafx.fxml.FXMLLoader; // Chargeur de fichiers FXML
import javafx.scene.Parent; // Noeud racine de la scène
import javafx.scene.Scene; // Scène JavaFX
import javafx.scene.control.*; // Composants UI (Label, TextField, DatePicker, etc.)
import javafx.scene.layout.StackPane; // Conteneur empilé
import javafx.scene.layout.VBox; // Conteneur vertical
import javafx.stage.Stage; // Fenêtre principale
import org.example.model.User; // Modèle utilisateur
import org.example.service.UserService; // Service de gestion des utilisateurs
import org.example.util.DatabaseUtil; // Utilitaire de connexion à la base de données

import java.io.IOException; // Exception d'entrée/sortie
import java.sql.*; // Classes JDBC (Connection, PreparedStatement, ResultSet, etc.)
import java.time.LocalDate; // Date sans heure
import java.time.LocalDateTime; // Date avec heure
import java.time.format.DateTimeFormatter; // Formateur de date

// Contrôleur front-office pour la création/soumission d'une ordonnance par le client
public class OrdonnanceController {

    @FXML private Label numeroBannerLabel; // Label affichant le numéro d'ordonnance dans le bandeau
    @FXML private TextField numeroField; // Champ texte du numéro d'ordonnance (non éditable)
    @FXML private DatePicker dateOrdonnanceField; // Sélecteur de date de l'ordonnance
    @FXML private DatePicker dateExpirationField; // Sélecteur de date d'expiration
    @FXML private TextArea noteMedicalField; // Zone de texte pour la note médicale (optionnelle)
    @FXML private Label errorLabel; // Label pour afficher les messages d'erreur
    @FXML private Button profileButton; // Bouton profil dans la navbar
    @FXML private Button submitButton; // Bouton de soumission (désactivé après clic pour anti double-clic)
    @FXML private StackPane ordonnanceMenuContainer; // Conteneur du menu déroulant ordonnance
    @FXML private VBox ordonnanceDropdown; // Menu déroulant ordonnance (créer / mes ordonnances)

    @FXML private VBox traitementInfoBox; // Conteneur des infos du traitement associé
    @FXML private Label traitProduitLabel; // Label affichant le nom du produit du traitement
    @FXML private Label traitStatusLabel; // Label affichant le statut du traitement

    private UserService userService = new UserService(); // Service pour récupérer l'utilisateur connecté
    private int traitementId = -1; // ID du traitement associé (-1 = aucun)
    private int ordonnanceId = -1; // ID de l'ordonnance en cours de modification (-1 = nouvelle)
    private String numeroOrdonnance; // Numéro d'ordonnance généré automatiquement

    // Setter appelé depuis TraitementController pour passer l'ID du traitement créé
    public void setTraitementId(int traitementId) {
        this.traitementId = traitementId; // Stocker l'ID du traitement
        loadTraitementInfo(); // Charger les infos du traitement depuis la base
    }

    // Setter appelé depuis TraitementController pour passer l'ID et le numéro de l'ordonnance
    public void setOrdonnanceId(int ordonnanceId, String numero) {
        this.ordonnanceId = ordonnanceId; // Stocker l'ID de l'ordonnance
        this.numeroOrdonnance = numero; // Stocker le numéro
        numeroBannerLabel.setText(numero); // Afficher le numéro dans le bandeau
        numeroField.setText(numero); // Afficher le numéro dans le champ texte
    }

    @FXML
    public void initialize() {
        // Générer un numéro d'ordonnance unique au format ORD-ANNEE-4CHIFFRES
        numeroOrdonnance = "ORD-" + LocalDateTime.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 10000));
        numeroBannerLabel.setText(numeroOrdonnance); // Afficher dans le bandeau
        numeroField.setText(numeroOrdonnance); // Afficher dans le champ

        dateOrdonnanceField.setValue(LocalDate.now()); // Date ordonnance = aujourd'hui par défaut
        dateExpirationField.setValue(LocalDate.now().plusYears(1)); // Date expiration = dans 1 an par défaut

        // Gestion du menu déroulant ordonnance au survol de la souris
        if (ordonnanceMenuContainer != null && ordonnanceDropdown != null) {
            ordonnanceMenuContainer.setOnMouseEntered(e -> { ordonnanceDropdown.setVisible(true); ordonnanceDropdown.setManaged(true); }); // Afficher au survol
            ordonnanceMenuContainer.setOnMouseExited(e -> { ordonnanceDropdown.setVisible(false); ordonnanceDropdown.setManaged(false); }); // Masquer à la sortie
        }
    }

    // Charger les informations du traitement associé depuis la base de données
    private void loadTraitementInfo() {
        if (traitementId <= 0) return; // Pas de traitement associé, on sort
        try {
            Connection conn = DatabaseUtil.getConnection(); // Obtenir la connexion à la base
            // Requête pour récupérer les infos du traitement avec jointure sur le produit
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.dosage, t.frequence, t.duree_jours, t.status, t.repas, t.notes, " +
                    "p.nom AS produit_nom " +
                    "FROM traitement t " +
                    "LEFT JOIN produit p ON t.id_produit_id = p.id_produit " +
                    "WHERE t.id_traitement = ?");
            ps.setInt(1, traitementId); // Paramètre : ID du traitement
            ResultSet rs = ps.executeQuery(); // Exécuter la requête
            if (rs.next()) { // Si un résultat existe
                traitProduitLabel.setText("Produit : " + (rs.getString("produit_nom") != null ? rs.getString("produit_nom") : "N/A")); // Afficher le nom du produit
                traitStatusLabel.setText("Statut : " + rs.getString("status")); // Afficher le statut
            }
            rs.close(); // Fermer le ResultSet
            ps.close(); // Fermer le PreparedStatement
        } catch (SQLException e) {
            System.out.println("Erreur chargement traitement: " + e.getMessage()); // Log de l'erreur
        }
    }

    // Méthode appelée lors du clic sur "Envoyer mon ordonnance"
    @FXML
    private void handleSubmitOrdonnance() {
        errorLabel.setText(""); // Réinitialiser le message d'erreur

        submitButton.setDisable(true); // Désactiver le bouton pour éviter le double-clic

        // Vérifier que le patient est connecté
        User currentUser = userService.getCurrentUser(); // Récupérer l'utilisateur connecté
        if (currentUser == null) { // Si pas connecté
            errorLabel.setText("Veuillez vous connecter avant de soumettre une ordonnance."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return; // Arrêter le traitement
        }

        // Contrôle : date ordonnance obligatoire
        if (dateOrdonnanceField.getValue() == null) { // Si aucune date sélectionnée
            errorLabel.setText("Date invalide"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }
        // Contrôle : date ordonnance ne doit pas être dans le futur
        if (dateOrdonnanceField.getValue().isAfter(LocalDate.now())) { // Si date future
            errorLabel.setText("La date de l'ordonnance ne peut pas être dans le futur."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }
        // Contrôle : date expiration obligatoire
        if (dateExpirationField.getValue() == null) { // Si aucune date d'expiration
            errorLabel.setText("Date invalide"); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }
        // Contrôle : date expiration doit être postérieure à la date ordonnance
        if (!dateExpirationField.getValue().isAfter(dateOrdonnanceField.getValue())) { // Si expiration <= ordonnance
            errorLabel.setText("La date d'expiration doit être postérieure à la date de l'ordonnance."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }

        // Contrôle : au moins un traitement doit être associé
        if (traitementId <= 0) { // Si aucun traitement lié
            errorLabel.setText("Veuillez d'abord créer un traitement avant de soumettre l'ordonnance."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }

        // Contrôle : note médicale optionnelle mais limitée à 1000 caractères
        String note = noteMedicalField.getText() != null ? noteMedicalField.getText().trim() : ""; // Récupérer et nettoyer la note
        if (note.length() > 1000) { // Si trop longue
            errorLabel.setText("La note médicale ne doit pas dépasser 1000 caractères."); // Message d'erreur
            submitButton.setDisable(false); // Réactiver le bouton
            return;
        }

        try {
            Connection conn = DatabaseUtil.getConnection(); // Obtenir la connexion

            // Vérification d'unicité : même patient + même date = doublon interdit
            if (ordonnanceId <= 0) { // Seulement pour les nouvelles ordonnances
                PreparedStatement psDup = conn.prepareStatement(
                        "SELECT COUNT(*) AS nb FROM ordonnance WHERE id_utilisateur_id = ? AND DATE(date_ordonnance) = ?"); // Requête de vérification doublon
                psDup.setInt(1, currentUser.getId()); // Paramètre : ID du patient
                psDup.setDate(2, java.sql.Date.valueOf(dateOrdonnanceField.getValue())); // Paramètre : date ordonnance
                ResultSet rsDup = psDup.executeQuery(); // Exécuter
                if (rsDup.next() && rsDup.getInt("nb") > 0) { // Si doublon trouvé
                    errorLabel.setText("Une ordonnance existe déjà pour cette date."); // Message d'erreur
                    submitButton.setDisable(false); // Réactiver le bouton
                    rsDup.close(); psDup.close(); // Fermer les ressources
                    return;
                }
                rsDup.close(); psDup.close(); // Fermer les ressources
            }

            if (ordonnanceId > 0) { // Si c'est une mise à jour d'ordonnance existante
                // Mise à jour de l'ordonnance créée depuis la page traitement
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE ordonnance SET date_ordonnance = ?, date_expiration = ?, statut = ?, note_medical = ? WHERE id_ordonnance = ?");
                ps.setTimestamp(1, Timestamp.valueOf(dateOrdonnanceField.getValue().atStartOfDay())); // Date ordonnance
                ps.setTimestamp(2, Timestamp.valueOf(dateExpirationField.getValue().atStartOfDay())); // Date expiration
                ps.setString(3, "en_attente"); // Statut = en attente
                ps.setString(4, noteMedicalField.getText() != null ? noteMedicalField.getText().trim() : ""); // Note médicale nettoyée
                ps.setInt(5, ordonnanceId); // ID de l'ordonnance à mettre à jour
                ps.executeUpdate(); // Exécuter la mise à jour
                ps.close(); // Fermer
            } else { // Sinon, créer une nouvelle ordonnance
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS); // Insertion avec récupération de l'ID généré
                ps.setString(1, numeroOrdonnance); // Numéro d'ordonnance généré
                ps.setTimestamp(2, Timestamp.valueOf(dateOrdonnanceField.getValue().atStartOfDay())); // Date ordonnance
                ps.setTimestamp(3, Timestamp.valueOf(dateExpirationField.getValue().atStartOfDay())); // Date expiration
                ps.setString(4, "en_attente"); // Statut initial
                ps.setString(5, noteMedicalField.getText() != null ? noteMedicalField.getText().trim() : ""); // Note médicale
                ps.setInt(6, currentUser != null ? currentUser.getId() : 0); // ID du patient connecté
                ps.executeUpdate(); // Exécuter l'insertion
                ps.close(); // Fermer
            }

            // Afficher un message de succès
            Alert alert = new Alert(Alert.AlertType.INFORMATION); // Créer une alerte info
            alert.setTitle("Succès"); // Titre
            alert.setHeaderText(null); // Pas de header
            alert.setContentText("Votre ordonnance " + numeroOrdonnance + " a été envoyée avec succès."); // Message
            alert.showAndWait(); // Afficher et attendre la fermeture

            goToMesOrdonnances(); // Rediriger vers la page "Mes Ordonnances"
        } catch (SQLException e) { // En cas d'erreur SQL
            errorLabel.setText("Erreur: " + e.getMessage()); // Afficher l'erreur
            submitButton.setDisable(false); // Réactiver le bouton
        } catch (IOException e) { // En cas d'erreur de navigation
            e.printStackTrace(); // Log de l'erreur
            submitButton.setDisable(false); // Réactiver le bouton
        }
    }

    // Navigation vers la page d'accueil
    @FXML
    private void goToAccueil() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml")); // Charger le FXML Accueil
        Parent root = loader.load(); // Charger le noeud racine
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // Ajouter le CSS
        Stage stage = (Stage) numeroField.getScene().getWindow(); // Récupérer la fenêtre actuelle
        stage.setScene(scene); // Changer la scène
        stage.setFullScreen(true); // Plein écran
    }

    // Navigation vers la page de demande de traitement
    @FXML
    private void goToTraitement() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Traitement.fxml")); // Charger le FXML Traitement
        Parent root = loader.load(); // Charger le noeud racine
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // Ajouter le CSS
        Stage stage = (Stage) numeroField.getScene().getWindow(); // Récupérer la fenêtre
        stage.setScene(scene); // Changer la scène
        stage.setFullScreen(true); // Plein écran
    }

    // Navigation vers la page "Mes Ordonnances"
    @FXML
    private void goToMesOrdonnances() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesOrdonnances.fxml")); // Charger le FXML MesOrdonnances
        Parent root = loader.load(); // Charger le noeud racine
        Scene scene = new Scene(root); // Créer la scène
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm()); // Ajouter le CSS
        Stage stage = (Stage) numeroField.getScene().getWindow(); // Récupérer la fenêtre
        stage.setScene(scene); // Changer la scène
        stage.setFullScreen(true); // Plein écran
    }

    // Action "Créer une ordonnance" dans le menu - déjà sur cette page
    @FXML
    private void goToCreerOrdonnance() {
        // Déjà sur cette page, rien à faire
    }
}
