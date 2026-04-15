package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.model.Coupon;
import org.example.service.CouponService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CouponsController {
    private static final Pattern COUPON_CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{4,30}$");

    @FXML private TableView<Coupon> couponsTable;
    @FXML private TableColumn<Coupon, Integer> idCol;
    @FXML private TableColumn<Coupon, String> codeCol;
    @FXML private TableColumn<Coupon, String> typeCol;
    @FXML private TableColumn<Coupon, Double> valeurCol;
    @FXML private TableColumn<Coupon, LocalDate> expirationCol;
    @FXML private TableColumn<Coupon, Integer> usageMaxCol;
    @FXML private TableColumn<Coupon, Integer> usageCountCol;
    @FXML private TableColumn<Coupon, Boolean> actifCol;
    @FXML private TableColumn<Coupon, Double> minimumCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> stateFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    @FXML private Label totalCouponsLabel;
    @FXML private Label activeCouponsLabel;
    @FXML private Label expiredCouponsLabel;
    @FXML private Label exhaustedCouponsLabel;

    @FXML private VBox modalOverlay;
    @FXML private TextField codeField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField valeurField;
    @FXML private DatePicker expirationPicker;
    @FXML private TextField usageMaxField;
    @FXML private TextField usageCountField;
    @FXML private CheckBox actifCheck;
    @FXML private TextField minimumField;

    private final CouponService couponService = new CouponService();
    private ObservableList<Coupon> couponsList;
    private Coupon couponEnCours;
    private DashboardController dashboardController;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        valeurCol.setCellValueFactory(new PropertyValueFactory<>("valeur"));
        expirationCol.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        usageMaxCol.setCellValueFactory(new PropertyValueFactory<>("usageMax"));
        usageCountCol.setCellValueFactory(new PropertyValueFactory<>("usageCount"));
        actifCol.setCellValueFactory(new PropertyValueFactory<>("actif"));
        minimumCol.setCellValueFactory(new PropertyValueFactory<>("montantMinimumPanier"));
        typeCombo.setItems(FXCollections.observableArrayList(Coupon.TYPE_PERCENTAGE, Coupon.TYPE_FIXED));
        typeCombo.setValue(Coupon.TYPE_PERCENTAGE);

        typeFilterCombo.setItems(FXCollections.observableArrayList("Tous types", Coupon.TYPE_PERCENTAGE, Coupon.TYPE_FIXED));
        typeFilterCombo.setValue("Tous types");
        stateFilterCombo.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs", "Expires", "Epuises"));
        stateFilterCombo.setValue("Tous");
        sortCombo.setItems(FXCollections.observableArrayList("Expiration proche", "Expiration lointaine", "Valeur -", "Valeur +", "Usage -"));
        sortCombo.setValue("Expiration proche");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        stateFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        loadCoupons();
    }

    private void loadCoupons() {
        couponsList = FXCollections.observableArrayList(couponService.getAll());
        refreshStats(couponsList);
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        if (couponsList == null) {
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String type = typeFilterCombo.getValue();
        String state = stateFilterCombo.getValue();
        String sort = sortCombo.getValue();

        Comparator<Coupon> comparator = resolveSortComparator(sort);

        List<Coupon> filtered = couponsList.stream()
                .filter(c -> {
                    if (!search.isEmpty()) {
                        String code = c.getCode() == null ? "" : c.getCode().toLowerCase();
                        if (!code.contains(search)) {
                            return false;
                        }
                    }

                    if (type != null && !"Tous types".equalsIgnoreCase(type)) {
                        if (c.getType() == null || !c.getType().equalsIgnoreCase(type)) {
                            return false;
                        }
                    }

                    if (state != null && !"Tous".equalsIgnoreCase(state)) {
                        if ("Actifs".equalsIgnoreCase(state) && !c.isActif()) {
                            return false;
                        }
                        if ("Inactifs".equalsIgnoreCase(state) && c.isActif()) {
                            return false;
                        }
                        if ("Expires".equalsIgnoreCase(state) && (c.getDateExpiration() == null || !c.getDateExpiration().isBefore(LocalDate.now()))) {
                            return false;
                        }
                        if ("Epuises".equalsIgnoreCase(state) && c.getUsageCount() < c.getUsageMax()) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(comparator)
                .toList();

        couponsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Coupon> resolveSortComparator(String sort) {
        if ("Expiration lointaine".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Coupon::getDateExpiration, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("Valeur -".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Coupon::getValeur).reversed();
        }
        if ("Valeur +".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Coupon::getValeur);
        }
        if ("Usage -".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(Coupon::getUsageCount).reversed();
        }
        return Comparator.comparing(Coupon::getDateExpiration, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void refreshStats(List<Coupon> source) {
        long total = source.size();
        long actifs = source.stream().filter(Coupon::isActif).count();
        long expires = source.stream()
                .filter(c -> c.getDateExpiration() != null && c.getDateExpiration().isBefore(LocalDate.now()))
                .count();
        long epuises = source.stream().filter(c -> c.getUsageCount() >= c.getUsageMax()).count();

        totalCouponsLabel.setText(String.valueOf(total));
        activeCouponsLabel.setText(String.valueOf(actifs));
        expiredCouponsLabel.setText(String.valueOf(expires));
        exhaustedCouponsLabel.setText(String.valueOf(epuises));
    }

    @FXML
    private void handleAjouterCoupon() {
        couponEnCours = null;
        clearForm();
        codeField.setText(generateCouponCode());
        showForm(true);
    }

    @FXML
    private void handleModifierCoupon() {
        Coupon selected = couponsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez un coupon a modifier.");
            return;
        }
        couponEnCours = selected;
        fillForm(selected);
        showForm(true);
    }

    @FXML
    private void handleSupprimerCoupon() {
        Coupon selected = couponsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionnez un coupon a supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le coupon " + selected.getCode() + " ?",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        couponService.delete(selected.getId());
        loadCoupons();
        showInfo("Coupon supprime avec succes.");
    }

    @FXML
    private void handleSaveCoupon() {
        try {
            Coupon c = (couponEnCours == null) ? new Coupon() : couponEnCours;
            String code = requireText(codeField, "Le code coupon est obligatoire.").toUpperCase();
            if (!COUPON_CODE_PATTERN.matcher(code).matches()) {
                showWarning("Code coupon invalide (4-30 caracteres: A-Z, 0-9, -).");
                return;
            }

            String type = typeCombo.getValue();
            if (type == null || type.isBlank()) {
                showWarning("Le type du coupon est obligatoire.");
                return;
            }

            double valeur = parsePositiveDouble(valeurField, "La valeur du coupon doit etre superieure a 0.");
            if (Coupon.TYPE_PERCENTAGE.equalsIgnoreCase(type) && valeur > 100) {
                showWarning("Pour un coupon percentage, la valeur doit etre <= 100.");
                return;
            }

            LocalDate expiration = expirationPicker.getValue();
            if (expiration == null) {
                showWarning("La date d'expiration est obligatoire.");
                return;
            }
            if (expiration.isBefore(LocalDate.now())) {
                showWarning("La date d'expiration ne peut pas etre dans le passe.");
                return;
            }

            int usageMax = parsePositiveInt(usageMaxField, "Usage max doit etre un entier > 0.");
            int usageCount = parseNonNegativeInt(usageCountField, "Usage count doit etre un entier >= 0.");
            if (usageCount > usageMax) {
                showWarning("Usage count ne peut pas depasser usage max.");
                return;
            }

            double minimum = parseNonNegativeDouble(minimumField, "Le montant minimum doit etre >= 0.");

            c.setCode(code);
            c.setType(type);
            c.setValeur(valeur);
            c.setDateExpiration(expiration);
            c.setUsageMax(usageMax);
            c.setUsageCount(usageCount);
            c.setActif(actifCheck.isSelected());
            c.setMontantMinimumPanier(minimum);

            if (couponEnCours == null) {
                couponService.add(c);
                showInfo("Coupon ajoute avec succes.");
            } else {
                couponService.update(c);
                showInfo("Coupon modifie avec succes.");
            }

            showForm(false);
            loadCoupons();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (Exception e) {
            showError("Erreur coupon: " + e.getMessage());
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
        codeField.setText(generateCouponCode());
        typeCombo.setValue(Coupon.TYPE_PERCENTAGE);
        valeurField.setText("");
        expirationPicker.setValue(null);
        usageMaxField.setText("1");
        usageCountField.setText("0");
        actifCheck.setSelected(true);
        minimumField.setText("0");
    }

    private void fillForm(Coupon c) {
        codeField.setText(c.getCode());
        typeCombo.setValue(c.getType());
        valeurField.setText(String.valueOf(c.getValeur()));
        expirationPicker.setValue(c.getDateExpiration());
        usageMaxField.setText(String.valueOf(c.getUsageMax()));
        usageCountField.setText(String.valueOf(c.getUsageCount()));
        actifCheck.setSelected(c.isActif());
        minimumField.setText(String.valueOf(c.getMontantMinimumPanier()));
    }

    @FXML
    private void handleGenerateCode() {
        codeField.setText(generateCouponCode());
    }

    private String generateCouponCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CV-" + raw;
    }

    private void showForm(boolean show) {
        modalOverlay.setVisible(show);
        modalOverlay.setManaged(show);
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

    private double parseNonNegativeDouble(TextField field, String message) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        try {
            double value = Double.parseDouble(raw);
            if (value < 0) {
                throw new IllegalArgumentException(message);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private int parsePositiveInt(TextField field, String message) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        try {
            int value = Integer.parseInt(raw);
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
}
