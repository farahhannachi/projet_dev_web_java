package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.StringConverter;
import org.example.model.Produit;
import org.example.model.Promotion;
import org.example.model.User;
import org.example.service.ProduitService;
import org.example.service.PromotionService;
import org.example.service.UserService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromotionsController {
    @FXML private TableView<Promotion> promotionsTable;
    @FXML private TableColumn<Promotion, Integer> idCol;
    @FXML private TableColumn<Promotion, String> produitIdCol;
    @FXML private TableColumn<Promotion, String> titreCol;
    @FXML private TableColumn<Promotion, Double> reductionCol;
    @FXML private TableColumn<Promotion, String> statutCol;
    @FXML private TableColumn<Promotion, java.time.LocalDateTime> debutCol;
    @FXML private TableColumn<Promotion, java.time.LocalDateTime> finCol;

    @FXML private VBox modalOverlay;
    @FXML private ComboBox<Produit> produitCombo;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField reductionField;
    @FXML private DatePicker debutPicker;
    @FXML private DatePicker finPicker;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Label adminInfoLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private CheckBox activeOnlyCheck;

    @FXML private Label totalPromotionsLabel;
    @FXML private Label activePromotionsLabel;
    @FXML private Label inactivePromotionsLabel;
    @FXML private Label upcomingPromotionsLabel;

    private final PromotionService promotionService = new PromotionService();
    private final ProduitService produitService = new ProduitService();
    private final UserService userService = new UserService();
    private ObservableList<Promotion> promotionsList;
    private final Map<Integer, String> produitNamesById = new HashMap<>();
    private Promotion promotionEnCours;
    private DashboardController dashboardController;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        produitIdCol.setCellValueFactory(cellData -> {
            Integer produitId = cellData.getValue().getProduitId();
            return new SimpleStringProperty(resolveProduitLabel(produitId));
        });
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        reductionCol.setCellValueFactory(new PropertyValueFactory<>("valeurReduction"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        debutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        finCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        statutCombo.setItems(FXCollections.observableArrayList("active", "inactive"));
        statutCombo.setValue("active");
        List<Produit> produits = produitService.getAll();
        produitCombo.setItems(FXCollections.observableArrayList(produits));
        produitNamesById.clear();
        for (Produit produit : produits) {
            produitNamesById.put(produit.getId(), produit.getNom());
        }
        produitCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produit produit) {
                if (produit == null) {
                    return "";
                }
                return produit.getNom();
            }

            @Override
            public Produit fromString(String string) {
                return null;
            }
        });

        statutFilterCombo.setItems(FXCollections.observableArrayList("Tous", "active", "inactive"));
        statutFilterCombo.setValue("Tous");
        sortCombo.setItems(FXCollections.observableArrayList("Date recente", "Reduction +", "Reduction -", "Titre A-Z"));
        sortCombo.setValue("Date recente");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        statutFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        activeOnlyCheck.selectedProperty().addListener((obs, oldV, newV) -> applyFilters());

        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            adminInfoLabel.setText(currentUser.getId() + " - " + currentUser.getEmail());
        } else {
            adminInfoLabel.setText("Session admin introuvable");
        }
        loadPromotions();
    }

    private void loadPromotions() {
        promotionsList = FXCollections.observableArrayList(promotionService.getAllPromotions());
        refreshStats(promotionsList);
        applyFilters();
    }

    private String resolveProduitLabel(Integer produitId) {
        if (produitId == null) {
            return "Aucun produit";
        }
        return produitNamesById.getOrDefault(produitId, "Produit #" + produitId);
    }

    private void refreshStats(List<Promotion> source) {
        long total = source.size();
        long active = source.stream().filter(Promotion::isActive).count();
        long inactive = source.stream().filter(p -> !"active".equalsIgnoreCase(p.getStatut())).count();
        long upcoming = source.stream()
                .filter(p -> p.getDateDebut() != null && p.getDateDebut().toLocalDate().isAfter(LocalDate.now()))
                .count();

        totalPromotionsLabel.setText(String.valueOf(total));
        activePromotionsLabel.setText(String.valueOf(active));
        inactivePromotionsLabel.setText(String.valueOf(inactive));
        upcomingPromotionsLabel.setText(String.valueOf(upcoming));
    }

    private void applyFilters() {
        if (promotionsList == null) {
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statut = statutFilterCombo.getValue();
        boolean activeOnly = activeOnlyCheck.isSelected();
        Comparator<Promotion> comparator = resolveSortComparator(sortCombo.getValue());

        List<Promotion> filtered = promotionsList.stream()
                .filter(p -> {
                    if (!search.isEmpty()) {
                        String titre = p.getTitre() == null ? "" : p.getTitre().toLowerCase();
                        String description = p.getDescription() == null ? "" : p.getDescription().toLowerCase();
                        String produitName = resolveProduitLabel(p.getProduitId()).toLowerCase();
                        if (!titre.contains(search) && !description.contains(search) && !produitName.contains(search)) {
                            return false;
                        }
                    }

                    if (statut != null && !"Tous".equalsIgnoreCase(statut)) {
                        if (p.getStatut() == null || !p.getStatut().equalsIgnoreCase(statut)) {
                            return false;
                        }
                    }

                    return !activeOnly || p.isActive();
                })
                .sorted(comparator)
                .toList();

        promotionsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Promotion> resolveSortComparator(String sort) {
        if ("Reduction +".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Promotion::getValeurReduction);
        }
        if ("Reduction -".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Promotion::getValeurReduction).reversed();
        }
        if ("Titre A-Z".equalsIgnoreCase(sort)) {
            return Comparator.comparing(p -> p.getTitre() == null ? "" : p.getTitre().toLowerCase());
        }
        return Comparator.comparing(Promotion::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @FXML
    private void handleAjouterPromotion() {
        promotionEnCours = null;
        clearForm();
        showForm(true);
    }

    @FXML
    private void handleModifierPromotion() {
        Promotion selected = promotionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez une promotion a modifier.");
            return;
        }
        promotionEnCours = selected;
        fillForm(selected);
        showForm(true);
    }

    @FXML
    private void handleSupprimerPromotion() {
        Promotion selected = promotionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez une promotion a supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la promotion \"" + selected.getTitre() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        promotionService.delete(selected.getId());
        loadPromotions();
        showInfo("Promotion supprimee avec succes.");
    }

    @FXML
    private void handleSavePromotion() {
        try {
            Promotion p = (promotionEnCours == null) ? new Promotion() : promotionEnCours;
            Produit produitSelectionne = produitCombo.getValue();
            if (produitSelectionne == null) {
                showWarning("Veuillez selectionner un produit.");
                return;
            }

            String titre = titreField.getText() == null ? "" : titreField.getText().trim();
            if (titre.isBlank()) {
                showWarning("Le titre de la promotion est obligatoire.");
                return;
            }

            double reduction;
            try {
                reduction = Double.parseDouble(reductionField.getText().trim());
            } catch (Exception e) {
                showWarning("La reduction doit etre un nombre.");
                return;
            }

            if (reduction <= 0 || reduction > 100) {
                showWarning("La reduction doit etre comprise entre 0 et 100.");
                return;
            }

            LocalDate debutDate = debutPicker.getValue() != null ? debutPicker.getValue() : LocalDate.now();
            LocalDate finDate = finPicker.getValue() != null ? finPicker.getValue() : LocalDate.now();
            if (finDate.isBefore(debutDate)) {
                showWarning("La date de fin doit etre apres la date de debut.");
                return;
            }

            String statut = statutCombo.getValue();
            if (statut == null || statut.isBlank()) {
                showWarning("Le statut de la promotion est obligatoire.");
                return;
            }

            p.setProduitId(produitSelectionne != null ? produitSelectionne.getId() : null);
            p.setTitre(titre);
            p.setDescription(descriptionArea.getText() == null ? "" : descriptionArea.getText().trim());
            p.setValeurReduction(reduction);
            p.setDateDebut(debutDate.atStartOfDay());
            p.setDateFin(finDate.atTime(23, 59, 59));
            p.setStatut(statut);
            p.setIdAdmin(resolveCurrentAdminId());

            if (promotionEnCours == null) {
                promotionService.add(p);
                showInfo("Promotion ajoutee avec succes.");
            } else {
                promotionService.update(p);
                showInfo("Promotion modifiee avec succes.");
            }

            showForm(false);
            loadPromotions();
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Erreur promotion: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseModal() {
        showForm(false);
    }

    @FXML
    private void goBack() {
        if (dashboardController != null) {
            dashboardController.showDashboardHome();
        }
    }

    private void clearForm() {
        produitCombo.setValue(null);
        titreField.setText("");
        descriptionArea.setText("");
        reductionField.setText("");
        debutPicker.setValue(LocalDate.now());
        finPicker.setValue(LocalDate.now().plusDays(7));
        statutCombo.setValue("active");
    }

    private void fillForm(Promotion p) {
        if (p.getProduitId() == null) {
            produitCombo.setValue(null);
        } else {
            produitCombo.getItems().stream()
                    .filter(prod -> prod.getId() == p.getProduitId())
                    .findFirst()
                    .ifPresent(produitCombo::setValue);
        }
        titreField.setText(p.getTitre());
        descriptionArea.setText(p.getDescription());
        reductionField.setText(String.valueOf(p.getValeurReduction()));
        debutPicker.setValue(p.getDateDebut() != null ? p.getDateDebut().toLocalDate() : LocalDate.now());
        finPicker.setValue(p.getDateFin() != null ? p.getDateFin().toLocalDate() : LocalDate.now());
        statutCombo.setValue(p.getStatut());
    }

    private void showForm(boolean show) {
        modalOverlay.setVisible(show);
        modalOverlay.setManaged(show);
    }

    private int resolveCurrentAdminId() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getType())) {
            throw new IllegalStateException("Session admin invalide. Reconnectez-vous.");
        }
        return currentUser.getId();
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
}
