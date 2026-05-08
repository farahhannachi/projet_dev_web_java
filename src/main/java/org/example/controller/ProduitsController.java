package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Produit;
import org.example.service.ProduitService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ProduitsController {
    @FXML private TableView<Produit> produitsTable;
    @FXML private TableColumn<Produit, Integer> idCol;
    @FXML private TableColumn<Produit, String> nomCol;
    @FXML private TableColumn<Produit, String> descriptionCol;
    @FXML private TableColumn<Produit, Double> prixCol;
    @FXML private TableColumn<Produit, Integer> quantiteCol;
    @FXML private TableColumn<Produit, String> categorieCol;
    @FXML private TableColumn<Produit, LocalDate> dateExpirationCol;
    @FXML private TableColumn<Produit, String> imageCol;
    @FXML private TableColumn<Produit, String> statutCol;

    @FXML private TextField searchField;
    @FXML private TextField prixMinField;
    @FXML private ComboBox<String> categorieFilterCombo;
    @FXML private ComboBox<String> statutFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    @FXML private Label totalProduitsLabel;
    @FXML private Label disponiblesLabel;
    @FXML private Label indisponiblesLabel;
    @FXML private Label ruptureLabel;

    private final ProduitService produitService = ProduitService.getInstance();
    private ObservableList<Produit> produitsList;

    // Champs pour le formulaire intégré
    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitle;
    @FXML private TextField nomField, descField, prixField, quantiteField, categorieField, imageField;
    @FXML private DatePicker dateExpirationPicker;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button browseImageBtn;
    @FXML private Button saveBtn, cancelBtn;
    @FXML private VBox deleteConfirmBox;
    @FXML private Button confirmDeleteBtn, cancelDeleteBtn;

    private Produit produitEnCours = null; // Pour savoir si on édite ou ajoute
    private DashboardController dashboardController;
    private String selectedImageRelativePath;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        produitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prix"));
        quantiteCol.setCellValueFactory(new PropertyValueFactory<>("quantiteStock"));
        categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        dateExpirationCol.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        imageCol.setCellValueFactory(new PropertyValueFactory<>("image"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        imageCol.setCellFactory(TextFieldTableCell.forTableColumn());

        statutCombo.setItems(FXCollections.observableArrayList("disponible", "indisponible", "rupture", "stock_critique"));
        statutCombo.setValue("disponible");

        statutFilterCombo.setItems(FXCollections.observableArrayList("Tous", "disponible", "indisponible", "rupture", "stock_critique"));
        statutFilterCombo.setValue("Tous");
        sortCombo.setItems(FXCollections.observableArrayList("Nom", "Prix +", "Prix -", "Stock -"));
        sortCombo.setValue("Nom");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        prixMinField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        categorieFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        statutFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        // Cacher la colonne ID
        idCol.setVisible(false);
        loadProduits();
    }

    private void loadProduits() {
        produitsList = FXCollections.observableArrayList(produitService.getAll());
        refreshCategorieFilter();
        refreshStats(produitsList);
        applyFilters();
    }

    private void refreshCategorieFilter() {
        String previous = categorieFilterCombo.getValue();
        ObservableList<String> items = FXCollections.observableArrayList("Toutes");
        produitsList.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .forEach(items::add);
        categorieFilterCombo.setItems(items);
        if (previous == null || !items.contains(previous)) {
            categorieFilterCombo.setValue("Toutes");
        } else {
            categorieFilterCombo.setValue(previous);
        }
    }

    private void refreshStats(List<Produit> source) {
        long total = source.size();
        long dispo = source.stream().filter(p -> "disponible".equalsIgnoreCase(p.getStatut())).count();
        long indispo = source.stream().filter(p -> "indisponible".equalsIgnoreCase(p.getStatut())).count();
        long rupture = source.stream().filter(p -> "rupture".equalsIgnoreCase(p.getStatut())).count();

        totalProduitsLabel.setText(String.valueOf(total));
        disponiblesLabel.setText(String.valueOf(dispo));
        indisponiblesLabel.setText(String.valueOf(indispo));
        ruptureLabel.setText(String.valueOf(rupture));
    }

    private void applyFilters() {
        if (produitsList == null) {
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String categorie = categorieFilterCombo.getValue();
        String statut = statutFilterCombo.getValue();
        double prixMin = parseDoubleSafe(prixMinField.getText());

        Comparator<Produit> comparator = resolveSortComparator(sortCombo.getValue());

        List<Produit> filtered = produitsList.stream()
                .filter(p -> {
                    if (!search.isEmpty()) {
                        String nom = p.getNom() == null ? "" : p.getNom().toLowerCase();
                        String desc = p.getDescription() == null ? "" : p.getDescription().toLowerCase();
                        if (!nom.contains(search) && !desc.contains(search)) {
                            return false;
                        }
                    }

                    if (categorie != null && !"Toutes".equalsIgnoreCase(categorie)) {
                        if (p.getCategorie() == null || !p.getCategorie().equalsIgnoreCase(categorie)) {
                            return false;
                        }
                    }

                    if (statut != null && !"Tous".equalsIgnoreCase(statut)) {
                        if (p.getStatut() == null || !p.getStatut().equalsIgnoreCase(statut)) {
                            return false;
                        }
                    }

                    return p.getPrix() >= prixMin;
                })
                .sorted(comparator)
                .toList();

        produitsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Produit> resolveSortComparator(String sort) {
        if ("Prix +".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Produit::getPrix);
        }
        if ("Prix -".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Produit::getPrix).reversed();
        }
        if ("Stock -".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(Produit::getQuantiteStock).reversed();
        }
        return Comparator.comparing(p -> p.getNom() == null ? "" : p.getNom().toLowerCase());
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    // Gestion du formulaire intégré
    @FXML
    private void handleAjouterProduit() {
        modalTitle.setText("Ajouter un produit");
        produitEnCours = null;
        clearForm();
        showForm(true);
    }

    @FXML
    private void handleModifierProduit() {
        Produit selected = produitsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            modalTitle.setText("Modifier le produit");
            produitEnCours = selected;
            fillForm(selected);
            showForm(true);
        } else {
            showWarning("Veuillez sélectionner un produit à modifier.");
        }
    }

    @FXML
    private void handleSaveProduit() {
        try {
            String nom = requireText(nomField, "Le nom du produit est obligatoire.");
            String desc = descField.getText() == null ? "" : descField.getText().trim();
            double prix = parsePositiveDouble(prixField, "Le prix doit être un nombre supérieur à 0.");
            int quantite = parseNonNegativeInt(quantiteField, "La quantité doit être un entier supérieur ou égal à 0.");
            String categorie = requireText(categorieField, "La catégorie est obligatoire.");
            LocalDate dateExpiration = dateExpirationPicker.getValue();

            if (dateExpiration != null && dateExpiration.isBefore(LocalDate.now())) {
                showWarning("La date d'expiration ne peut pas être dans le passé.");
                return;
            }

            String image = selectedImageRelativePath != null
                    ? selectedImageRelativePath
                    : (imageField.getText().trim().isEmpty() ? null : imageField.getText().trim());
            String statut = (statutCombo.getValue() == null || statutCombo.getValue().isBlank())
                    ? "disponible"
                    : statutCombo.getValue();

            if (produitEnCours == null) {
                Produit p = new Produit(0, nom, desc, prix, quantite, dateExpiration, categorie, image, statut);
                produitService.add(p);
                showInfo("Produit ajouté avec succès.");
            } else {
                produitEnCours.setNom(nom);
                produitEnCours.setDescription(desc);
                produitEnCours.setPrix(prix);
                produitEnCours.setQuantiteStock(quantite);
                produitEnCours.setCategorie(categorie);
                produitEnCours.setDateExpiration(dateExpiration);
                produitEnCours.setImage(image);
                produitEnCours.setStatut(statut);
                produitService.update(produitEnCours);
                showInfo("Produit modifié avec succès.");
            }
            loadProduits();
            produitsTable.refresh();
            showForm(false);
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement du produit: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelProduit() {
        showForm(false);
    }

    private void showForm(boolean show) {
        modalOverlay.setVisible(show);
        modalOverlay.setManaged(show);
    }

    private void clearForm() {
        nomField.setText("");
        descField.setText("");
        prixField.setText("");
        quantiteField.setText("");
        categorieField.setText("");
        dateExpirationPicker.setValue(null);
        imageField.setText("");
        selectedImageRelativePath = null;
        statutCombo.setValue("disponible");
    }

    private void fillForm(Produit p) {
        nomField.setText(p.getNom());
        descField.setText(p.getDescription());
        prixField.setText(String.valueOf(p.getPrix()));
        quantiteField.setText(String.valueOf(p.getQuantiteStock()));
        categorieField.setText(p.getCategorie());
        dateExpirationPicker.setValue(p.getDateExpiration());
        imageField.setText(p.getImage() != null ? p.getImage() : "");
        selectedImageRelativePath = p.getImage();
        statutCombo.setValue(p.getStatut() != null ? p.getStatut() : "disponible");
    }

    // Les méthodes handleAjouterProduit et handleModifierProduit sont maintenant gérées par le formulaire intégré

    @FXML
    private void handleSupprimerProduit() {
        Produit selected = produitsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Affiche la barre de confirmation dans le panneau formulaire
            showForm(true);
            showDeleteConfirmation(true);
        } else {
            showWarning("Veuillez sélectionner un produit à supprimer.");
        }
    }

    @FXML
    private void handleConfirmerSuppressionProduit() {
        Produit selected = produitsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            produitService.delete(selected.getId());
            loadProduits();
            produitsTable.refresh();
            showInfo("Produit supprimé avec succès.");
        }
        showDeleteConfirmation(false);
        showForm(false);
    }

    @FXML
    private void handleAnnulerSuppressionProduit() {
        showDeleteConfirmation(false);
        showForm(false);
    }

    private void showDeleteConfirmation(boolean show) {
        deleteConfirmBox.setVisible(show);
        deleteConfirmBox.setManaged(show);
        // Masquer le formulaire classique pendant la confirmation
        boolean formFieldsVisible = !show;
        nomField.setVisible(formFieldsVisible);
        nomField.setManaged(formFieldsVisible);
        descField.setVisible(formFieldsVisible);
        descField.setManaged(formFieldsVisible);
        prixField.setVisible(formFieldsVisible);
        prixField.setManaged(formFieldsVisible);
        quantiteField.setVisible(formFieldsVisible);
        quantiteField.setManaged(formFieldsVisible);
        categorieField.setVisible(formFieldsVisible);
        categorieField.setManaged(formFieldsVisible);
        dateExpirationPicker.setVisible(formFieldsVisible);
        dateExpirationPicker.setManaged(formFieldsVisible);
        imageField.setVisible(formFieldsVisible);
        imageField.setManaged(formFieldsVisible);
        browseImageBtn.setVisible(formFieldsVisible);
        browseImageBtn.setManaged(formFieldsVisible);
        statutCombo.setVisible(formFieldsVisible);
        statutCombo.setManaged(formFieldsVisible);
        saveBtn.setVisible(formFieldsVisible);
        saveBtn.setManaged(formFieldsVisible);
        cancelBtn.setVisible(formFieldsVisible);
        cancelBtn.setManaged(formFieldsVisible);
        modalTitle.setVisible(formFieldsVisible);
        modalTitle.setManaged(formFieldsVisible);
    }

    @FXML
    private void handleCloseModal() {
        showForm(false);
    }

    @FXML
    private void handleCancelDelete() {
        showDeleteConfirmation(false);
        showForm(false);
    }

    @FXML
    private void handleConfirmDelete() {
        Produit selected = produitsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            produitService.delete(selected.getId());
            loadProduits();
            produitsTable.refresh();
            showInfo("Produit supprimé avec succès.");
        }
        showDeleteConfirmation(false);
        showForm(false);
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image produit");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(modalOverlay.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            String extension = getExtension(selectedFile.getName());
            String safeName = UUID.randomUUID() + (extension.isEmpty() ? "" : ("." + extension));

            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "produits");
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(safeName);
            Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            selectedImageRelativePath = "uploads/produits/" + safeName;
            imageField.setText(selectedImageRelativePath);
        } catch (IOException e) {
            showError("Upload image impossible: " + e.getMessage());
        }
    }

    private String requireText(TextField field, String message) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private double parsePositiveDouble(TextField field, String message) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        try {
            double value = Double.parseDouble(raw);
            if (value <= 0) {
                throw new IllegalArgumentException(message);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private int parseNonNegativeInt(TextField field, String message) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        try {
            int value = Integer.parseInt(raw);
            if (value < 0) {
                throw new IllegalArgumentException(message);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    @FXML
    private void goBack() {
        if (dashboardController != null) {
            dashboardController.showDashboardHome();
            return;
        }

        Stage stage = (Stage) produitsTable.getScene().getWindow();
        stage.close();
    }
}
