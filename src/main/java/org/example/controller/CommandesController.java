package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Client;
import org.example.model.Commande;
import org.example.model.Coupon;
import org.example.model.Produit;
import org.example.service.CommandeService;
import org.example.service.CouponService;
import org.example.service.ProduitService;
import org.example.service.PromotionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class CommandesController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ]{8,20}$");

    @FXML private TableView<Commande> commandesTable;
    @FXML private TableColumn<Commande, Integer> idCol;
    @FXML private TableColumn<Commande, String> clientCol;
    @FXML private TableColumn<Commande, String> statutCol;
    @FXML private TableColumn<Commande, Double> totalHtCol;
    @FXML private TableColumn<Commande, Double> totalTtcCol;
    @FXML private TableColumn<Commande, LocalDate> dateCol;

    @FXML private Label totalCommandesLabel;
    @FXML private Label commandesEnCoursLabel;
    @FXML private Label commandesLivreesLabel;
    @FXML private Label commandesSuspectesLabel;
    @FXML private Label montantTotalLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private CheckBox fraudOnlyCheck;

    private final CommandeService commandeService = new CommandeService();
    private final CouponService couponService = new CouponService();
    private final ProduitService produitService = new ProduitService();
    private final PromotionService promotionService = new PromotionService();
    private ObservableList<Commande> commandesList;

    @FXML private VBox modalOverlay;
    @FXML private Label modalTitle;
    @FXML private TextField clientField, totalHtField, totalField;
    @FXML private ComboBox<String> statutField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> modePaiementField;
    @FXML private TextField adresseLivraisonField, telephoneField, emailField;
    @FXML private TextField couponCodeField, couponDiscountField, fraudScoreField, baseShippingCostField;
    @FXML private DatePicker estimatedDeliveryPicker;
    @FXML private TextArea messageArea, produitsIdsArea;
    @FXML private Button saveBtn, cancelBtn;
    @FXML private VBox deleteConfirmBox;
    @FXML private Button confirmDeleteBtn, cancelDeleteBtn;

    private Commande commandeEnCours = null;
    private DashboardController dashboardController;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientCol.setCellValueFactory(new PropertyValueFactory<>("clientNom"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        totalHtCol.setCellValueFactory(new PropertyValueFactory<>("totalHt"));
        totalTtcCol.setCellValueFactory(new PropertyValueFactory<>("totalTtc"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateCommande"));

        if (idCol != null) {
            idCol.setVisible(false);
        }

        statutField.setItems(FXCollections.observableArrayList("en_attente", "confirmee", "annulee", "livree", "review", "bloquee"));
        modePaiementField.setItems(FXCollections.observableArrayList("en_ligne", "espece", "cheque", "carte"));

        initializeFilters();
        attachFilterListeners();
        loadCommandes();
    }

    private void initializeFilters() {
        if (statutFilterCombo != null) {
            statutFilterCombo.setItems(FXCollections.observableArrayList(
                    "Tous statuts", "en_attente", "confirmee", "annulee", "livree", "review", "bloquee"
            ));
            statutFilterCombo.setValue("Tous statuts");
        }

        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Date recente", "Date ancienne", "Montant -", "Montant +", "Nom A-Z"
            ));
            sortCombo.setValue("Date recente");
        }
    }

    private void attachFilterListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (statutFilterCombo != null) {
            statutFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (sortCombo != null) {
            sortCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (fraudOnlyCheck != null) {
            fraudOnlyCheck.selectedProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void loadCommandes() {
        commandesList = FXCollections.observableArrayList(commandeService.getAll());
        refreshStats(commandesList);
        applyFilters();
    }

    private void refreshStats(List<Commande> commandes) {
        long total = commandes.size();
        long enCours = commandes.stream()
                .filter(c -> "en_attente".equalsIgnoreCase(c.getStatut()) || "confirmee".equalsIgnoreCase(c.getStatut()))
                .count();
        long livrees = commandes.stream()
                .filter(c -> "livree".equalsIgnoreCase(c.getStatut()))
                .count();
        long suspectes = commandes.stream()
                .filter(c -> c.getFraudScore() >= 70 || "review".equalsIgnoreCase(c.getStatut()) || "bloquee".equalsIgnoreCase(c.getStatut()))
                .count();
        double montantTotal = commandes.stream().mapToDouble(Commande::getTotal).sum();

        if (totalCommandesLabel != null) {
            totalCommandesLabel.setText(String.valueOf(total));
        }
        if (commandesEnCoursLabel != null) {
            commandesEnCoursLabel.setText(String.valueOf(enCours));
        }
        if (commandesLivreesLabel != null) {
            commandesLivreesLabel.setText(String.valueOf(livrees));
        }
        if (commandesSuspectesLabel != null) {
            commandesSuspectesLabel.setText(String.valueOf(suspectes));
        }
        if (montantTotalLabel != null) {
            montantTotalLabel.setText(String.format("%.2f DT", montantTotal));
        }
    }

    @FXML
    private void applyFilters() {
        if (commandesList == null) {
            return;
        }

        String search = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase()
                : "";
        String statut = statutFilterCombo != null ? statutFilterCombo.getValue() : "Tous statuts";
        String sort = sortCombo != null ? sortCombo.getValue() : "Date recente";
        boolean fraudOnly = fraudOnlyCheck != null && fraudOnlyCheck.isSelected();

        Comparator<Commande> comparator = resolveSortComparator(sort);

        List<Commande> filtered = commandesList.stream()
                .filter(c -> {
                    if (!search.isEmpty()) {
                        String client = c.getClientNom() == null ? "" : c.getClientNom().toLowerCase();
                        String email = c.getEmail() == null ? "" : c.getEmail().toLowerCase();
                        if (!client.contains(search) && !email.contains(search) && !String.valueOf(c.getId()).contains(search)) {
                            return false;
                        }
                    }

                    if (statut != null && !"Tous statuts".equalsIgnoreCase(statut)) {
                        if (c.getStatut() == null || !c.getStatut().equalsIgnoreCase(statut)) {
                            return false;
                        }
                    }

                    if (fraudOnly) {
                        return c.getFraudScore() >= 70 || "review".equalsIgnoreCase(c.getStatut()) || "bloquee".equalsIgnoreCase(c.getStatut());
                    }
                    return true;
                })
                .sorted(comparator)
                .toList();

        commandesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Commande> resolveSortComparator(String sort) {
        if ("Date ancienne".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Commande::getDateCommande, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("Montant -".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Commande::getTotal).reversed();
        }
        if ("Montant +".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Commande::getTotal);
        }
        if ("Nom A-Z".equalsIgnoreCase(sort)) {
            return Comparator.comparing(c -> c.getClientNom() == null ? "" : c.getClientNom().toLowerCase());
        }
        return Comparator.comparing(Commande::getDateCommande, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @FXML
    private void handleAjouterCommande() {
        showWarning("Ajout de commande interdit depuis le back-office. Utilisez le front-office comme dans Symfony.");
    }

    @FXML
    private void handleConfirmerCommande() {
        Commande selected = commandesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Veuillez sélectionner une commande à confirmer.");
            return;
        }

        String statut = selected.getStatut() == null ? "" : selected.getStatut().toLowerCase();
        if ("confirmee".equals(statut)) {
            showInfo("Cette commande est déjà confirmée.");
            return;
        }
        if ("livree".equals(statut) || "annulee".equals(statut) || "bloquee".equals(statut)) {
            showWarning("Cette commande ne peut pas être confirmée.");
            return;
        }

        selected.setStatut("confirmee");
        commandeService.update(selected);
        loadCommandes();
        commandesTable.refresh();
        showInfo("Commande confirmée avec succès.");
    }

    @FXML
    private void handleModifierCommande() {
        Commande selected = commandesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            modalTitle.setText("Modifier la commande");
            commandeEnCours = selected;
            fillForm(selected);
            showForm(true);
        } else {
            showWarning("Veuillez sélectionner une commande à modifier.");
        }
    }

    @FXML
    private void handleSaveCommande() {
        try {
            String clientNom = requireText(clientField, "Le nom client est obligatoire.");
            String statut = (statutField.getValue() == null || statutField.getValue().isBlank()) ? "en_attente" : statutField.getValue();
            LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            Client client = new Client();
            client.setNom(clientNom);
            String modePaiement = (modePaiementField.getValue() == null || modePaiementField.getValue().isBlank()) ? "en_ligne" : modePaiementField.getValue();
            String adresseLivraison = requireText(adresseLivraisonField, "L'adresse de livraison est obligatoire.");
            String telephone = requireText(telephoneField, "Le numéro de téléphone est obligatoire.");
            if (!PHONE_PATTERN.matcher(telephone).matches()) {
                showWarning("Numéro de téléphone invalide.");
                return;
            }
            String email = requireText(emailField, "L'email est obligatoire.");
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showWarning("Adresse email invalide.");
                return;
            }
            String message = messageArea.getText().trim().isEmpty() ? null : messageArea.getText().trim();
            String produitsIds = produitsIdsArea.getText().trim().isEmpty() ? null : produitsIdsArea.getText().trim();
            String couponCode = couponCodeField.getText().trim().isEmpty() ? null : couponCodeField.getText().trim();
            int fraudScore = fraudScoreField.getText().trim().isEmpty() ? 0 : Integer.parseInt(fraudScoreField.getText().trim());
            double baseShippingCost = baseShippingCostField.getText().trim().isEmpty() ? 0 : Double.parseDouble(baseShippingCostField.getText().trim());
            LocalDateTime estimatedDelivery = estimatedDeliveryPicker.getValue() != null ? estimatedDeliveryPicker.getValue().atStartOfDay() : null;

            if (fraudScore < 0 || fraudScore > 100) {
                showWarning("Le score fraude doit être entre 0 et 100.");
                return;
            }

            if (baseShippingCost < 0) {
                showWarning("Le coût de livraison ne peut pas être négatif.");
                return;
            }

            if (estimatedDelivery != null && estimatedDelivery.toLocalDate().isBefore(date)) {
                showWarning("La date de livraison estimée doit être après la date de commande.");
                return;
            }

            if (commandeEnCours == null) {
                showWarning("Ajout de commande interdit depuis le back-office.");
                showForm(false);
                return;
            }

            if (produitsIds != null && !produitsIds.isBlank() && parseProductIds(produitsIds).isEmpty()) {
                showWarning("Format des IDs produits invalide. Exemple: 1,2,3");
                return;
            }

            double subtotalFromProducts = computeSubtotalFromProducts(produitsIds);
            if (subtotalFromProducts <= 0) {
                subtotalFromProducts = totalHtField.getText().trim().isEmpty() ? 0 : Double.parseDouble(totalHtField.getText().trim());
            }

            if (subtotalFromProducts < 0) {
                showWarning("Le total HT ne peut pas être négatif.");
                return;
            }

            double couponDiscount = 0;
            if (couponCode != null && !couponCode.isBlank()) {
                CouponService.CouponValidationResult validation = couponService.validateCoupon(couponCode, subtotalFromProducts);
                if (!validation.valid) {
                    showError(validation.message);
                    return;
                }
                CouponService.CouponApplyResult applied = couponService.applyCoupon(subtotalFromProducts, validation.coupon);
                couponDiscount = applied.discount;
                subtotalFromProducts = applied.finalTotal;
            }

            double total = subtotalFromProducts + baseShippingCost;
            total = Math.max(0, total);
            total = Math.round(total * 100.0) / 100.0;

            totalHtField.setText(String.format("%.2f", subtotalFromProducts));
            totalField.setText(String.format("%.2f", total));
            couponDiscountField.setText(String.format("%.2f", couponDiscount));

            commandeEnCours.setClient(client);
            commandeEnCours.setNom(clientNom);
            commandeEnCours.setStatut(statut);
            commandeEnCours.setTotalHt(subtotalFromProducts);
            commandeEnCours.setTotalTtc(total);
            commandeEnCours.setDateCommande(date);
            commandeEnCours.setDateCommandeDateTime(date.atStartOfDay());
            commandeEnCours.setModePaiement(modePaiement);
            commandeEnCours.setAdresseLivraison(adresseLivraison);
            commandeEnCours.setTelephone(telephone);
            commandeEnCours.setEmail(email);
            commandeEnCours.setMessage(message);
            commandeEnCours.setProduitsIds(produitsIds);
            commandeEnCours.setCouponCode(couponCode);
            commandeEnCours.setCouponDiscount(couponDiscount);
            commandeEnCours.setFraudScore(fraudScore);
            commandeEnCours.setBaseShippingCost(baseShippingCost);
            commandeEnCours.setEstimatedDeliveryDate(estimatedDelivery);
            commandeService.update(commandeEnCours);
            showInfo("Commande modifiée avec succès.");
            loadCommandes();
            commandesTable.refresh();
            showForm(false);
        } catch (NumberFormatException e) {
            showWarning("Vérifiez les champs numériques (totaux, score fraude, frais livraison).");
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (Exception e) {
            showError("Erreur lors de la mise à jour de la commande: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelCommande() {
        showForm(false);
    }

    private void showForm(boolean show) {
        modalOverlay.setVisible(show);
        modalOverlay.setManaged(show);
    }

    private void clearForm() {
        clientField.setText("");
        statutField.setValue("en_attente");
        totalHtField.setText("");
        totalField.setText("");
        datePicker.setValue(null);
        modePaiementField.setValue("en_ligne");
        adresseLivraisonField.setText("");
        telephoneField.setText("");
        emailField.setText("");
        messageArea.setText("");
        produitsIdsArea.setText("");
        couponCodeField.setText("");
        couponDiscountField.setText("");
        fraudScoreField.setText("");
        baseShippingCostField.setText("");
        estimatedDeliveryPicker.setValue(null);
    }

    private void fillForm(Commande c) {
        clientField.setText(c.getClientNom());
        statutField.setValue(c.getStatut());
        totalHtField.setText(String.valueOf(c.getTotalHt()));
        totalField.setText(String.valueOf(c.getTotal()));
        datePicker.setValue(c.getDateCommande());
        modePaiementField.setValue(c.getModePaiement() != null ? c.getModePaiement() : "en_ligne");
        adresseLivraisonField.setText(c.getAdresseLivraison() != null ? c.getAdresseLivraison() : "");
        telephoneField.setText(c.getTelephone() != null ? c.getTelephone() : "");
        emailField.setText(c.getEmail() != null ? c.getEmail() : "");
        messageArea.setText(c.getMessage() != null ? c.getMessage() : "");
        produitsIdsArea.setText(c.getProduitsIds() != null ? c.getProduitsIds() : "");
        couponCodeField.setText(c.getCouponCode() != null ? c.getCouponCode() : "");
        couponDiscountField.setText(String.valueOf(c.getCouponDiscount()));
        fraudScoreField.setText(String.valueOf(c.getFraudScore()));
        baseShippingCostField.setText(String.valueOf(c.getBaseShippingCost()));
        estimatedDeliveryPicker.setValue(c.getEstimatedDeliveryDate() != null ? c.getEstimatedDeliveryDate().toLocalDate() : null);
    }

    @FXML
    private void handleSupprimerCommande() {
        Commande selected = commandesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showForm(true);
            showDeleteConfirmation(true);
        } else {
            showWarning("Veuillez sélectionner une commande à supprimer.");
        }
    }

    @FXML
    private void handleConfirmerSuppressionCommande() {
        Commande selected = commandesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            commandeService.delete(selected.getId());
            loadCommandes();
            commandesTable.refresh();
            showInfo("Commande supprimée avec succès.");
        }
        showDeleteConfirmation(false);
        showForm(false);
    }

    @FXML
    private void handleAnnulerSuppressionCommande() {
        showDeleteConfirmation(false);
        showForm(false);
    }

    private void showDeleteConfirmation(boolean show) {
        deleteConfirmBox.setVisible(show);
        deleteConfirmBox.setManaged(show);
        boolean formFieldsVisible = !show;
        clientField.setVisible(formFieldsVisible);
        clientField.setManaged(formFieldsVisible);
        statutField.setVisible(formFieldsVisible);
        statutField.setManaged(formFieldsVisible);
        totalHtField.setVisible(formFieldsVisible);
        totalHtField.setManaged(formFieldsVisible);
        totalField.setVisible(formFieldsVisible);
        totalField.setManaged(formFieldsVisible);
        datePicker.setVisible(formFieldsVisible);
        datePicker.setManaged(formFieldsVisible);
        modePaiementField.setVisible(formFieldsVisible);
        modePaiementField.setManaged(formFieldsVisible);
        adresseLivraisonField.setVisible(formFieldsVisible);
        adresseLivraisonField.setManaged(formFieldsVisible);
        telephoneField.setVisible(formFieldsVisible);
        telephoneField.setManaged(formFieldsVisible);
        emailField.setVisible(formFieldsVisible);
        emailField.setManaged(formFieldsVisible);
        messageArea.setVisible(formFieldsVisible);
        messageArea.setManaged(formFieldsVisible);
        produitsIdsArea.setVisible(formFieldsVisible);
        produitsIdsArea.setManaged(formFieldsVisible);
        couponCodeField.setVisible(formFieldsVisible);
        couponCodeField.setManaged(formFieldsVisible);
        couponDiscountField.setVisible(formFieldsVisible);
        couponDiscountField.setManaged(formFieldsVisible);
        fraudScoreField.setVisible(formFieldsVisible);
        fraudScoreField.setManaged(formFieldsVisible);
        baseShippingCostField.setVisible(formFieldsVisible);
        baseShippingCostField.setManaged(formFieldsVisible);
        estimatedDeliveryPicker.setVisible(formFieldsVisible);
        estimatedDeliveryPicker.setManaged(formFieldsVisible);
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
        Commande selected = commandesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            commandeService.delete(selected.getId());
            loadCommandes();
            commandesTable.refresh();
            showInfo("Commande supprimée avec succès.");
        }
        showDeleteConfirmation(false);
        showForm(false);
    }

    @FXML
    private void goBack() {
        if (dashboardController != null) {
            dashboardController.showDashboardHome();
            return;
        }

        Stage stage = (Stage) commandesTable.getScene().getWindow();
        stage.close();
    }

    private double computeSubtotalFromProducts(String produitsIds) {
        if (produitsIds == null || produitsIds.isBlank()) {
            return 0;
        }

        List<Integer> ids = parseProductIds(produitsIds);
        double subtotal = 0;
        for (Integer id : ids) {
            Produit produit = produitService.getById(id);
            if (produit == null) {
                continue;
            }
            double unitPrice = promotionService.getPromotionalPrice(produit.getId(), produit.getPrix());
            subtotal += unitPrice;
        }
        return Math.round(subtotal * 100.0) / 100.0;
    }

    private List<Integer> parseProductIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }

        String normalized = raw.trim();
        if (normalized.startsWith("[")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        String[] parts = normalized.split(",");
        List<Integer> ids = new ArrayList<>();
        for (String part : parts) {
            String s = part.trim().replace("\"", "");
            if (s.isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private String requireText(TextField field, String message) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
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
