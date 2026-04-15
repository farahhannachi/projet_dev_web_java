package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Commande;
import org.example.model.Coupon;
import org.example.model.User;
import org.example.service.CommandeService;
import org.example.service.CouponService;
import org.example.service.FrontPanierService;
import org.example.service.ProduitService;
import org.example.service.PromotionService;
import org.example.service.UserService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FrontCommandeController {
    @FXML private TableView<FrontPanierService.CartItem> cartTable;
    @FXML private TableColumn<FrontPanierService.CartItem, String> produitCol;
    @FXML private TableColumn<FrontPanierService.CartItem, Integer> quantiteCol;
    @FXML private TableColumn<FrontPanierService.CartItem, Double> prixCol;
    @FXML private TableColumn<FrontPanierService.CartItem, Double> totalCol;
    @FXML private TableColumn<FrontPanierService.CartItem, Void> actionsCol;

    @FXML private TextField couponField;
    @FXML private Label subTotalLabel;
    @FXML private Label couponDiscountLabel;
    @FXML private Label shippingLabel;
    @FXML private Label totalFinalLabel;
    @FXML private Label cartCountLabel;

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextField adresseField;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;

    private final FrontPanierService panierService = new FrontPanierService();
    private final ProduitService produitService = new ProduitService();
    private final PromotionService promotionService = new PromotionService();
    private final CouponService couponService = new CouponService();
    private final CommandeService commandeService = new CommandeService();
    private final UserService userService = new UserService();

    private double currentSubTotal;
    private double currentCouponDiscount;
    private double currentShipping;
    private Coupon currentCoupon;

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        produitCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        quantiteCol.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalLigne"));
        initActionsColumn();

        paiementCombo.setItems(FXCollections.observableArrayList("en_ligne", "espece", "cheque"));
        paiementCombo.setValue("en_ligne");

        User current = userService.getCurrentUser();
        if (current != null) {
            nomField.setText(current.getNom());
            emailField.setText(current.getEmail());
        }

        refreshPanier();
    }

    private void initActionsColumn() {
        if (actionsCol == null) {
            return;
        }
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button minusBtn = new Button("-");
            private final Button removeBtn = new Button("Suppr");
            private final HBox box = new HBox(6, minusBtn, removeBtn);

            {
                minusBtn.getStyleClass().add("btn-admin-outline");
                removeBtn.getStyleClass().add("btn-admin-danger");

                minusBtn.setOnAction(e -> {
                    FrontPanierService.CartItem item = getTableView().getItems().get(getIndex());
                    panierService.setQuantite(item.getProduitId(), item.getQuantite() - 1);
                    refreshPanier();
                });

                removeBtn.setOnAction(e -> {
                    FrontPanierService.CartItem item = getTableView().getItems().get(getIndex());
                    panierService.removeProduit(item.getProduitId());
                    refreshPanier();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void refreshPanier() {
        cartTable.setItems(FXCollections.observableArrayList(panierService.getItems(produitService, promotionService)));
        if (cartCountLabel != null) {
            cartCountLabel.setText(String.valueOf(panierService.getNombreArticles()));
        }
        currentSubTotal = panierService.getTotal(produitService, promotionService);
        currentCouponDiscount = 0;
        currentCoupon = null;
        currentShipping = currentSubTotal >= 120 ? 0 : 7.5;
        renderTotals();
    }

    @FXML
    private void handleApplyCoupon() {
        String code = couponField.getText() == null ? "" : couponField.getText().trim();
        if (code.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Saisissez un code coupon.").showAndWait();
            return;
        }

        CouponService.CouponValidationResult validation = couponService.validateCoupon(code, currentSubTotal);
        if (!validation.valid) {
            currentCoupon = null;
            currentCouponDiscount = 0;
            renderTotals();
            new Alert(Alert.AlertType.WARNING, validation.message).showAndWait();
            return;
        }

        CouponService.CouponApplyResult applied = couponService.applyCoupon(currentSubTotal, validation.coupon);
        currentCoupon = validation.coupon;
        currentCouponDiscount = applied.discount;
        renderTotals();
    }

    @FXML
    private void handlePasserCommande() {
        if (cartTable.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Votre panier est vide.").showAndWait();
            return;
        }

        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String telephone = telephoneField.getText() == null ? "" : telephoneField.getText().trim();
        String adresse = adresseField.getText() == null ? "" : adresseField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || adresse.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Nom, email et adresse sont obligatoires.").showAndWait();
            return;
        }

        User current = userService.getCurrentUser();

        Commande commande = new Commande();
        commande.setUtilisateurId(current != null ? current.getId() : null);
        commande.setDateCommande(LocalDate.now());
        commande.setDateCommandeDateTime(LocalDateTime.now());
        commande.setStatut("en_attente");
        commande.setModePaiement(paiementCombo.getValue());
        commande.setAdresseLivraison(adresse);
        commande.setTelephone(telephone);
        commande.setNom(nom);
        commande.setEmail(email);
        commande.setMessage("Commande front-office JavaFX");
        commande.setProduitsIds(panierService.getProduitsIdsJson());
        commande.setCouponCode(currentCoupon != null ? currentCoupon.getCode() : null);
        commande.setCouponDiscount(currentCouponDiscount);
        commande.setFraudScore(0);
        commande.setBaseShippingCost(currentShipping);
        commande.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(2));

        double total = Math.max(0, currentSubTotal - currentCouponDiscount + currentShipping);
        total = Math.round(total * 100.0) / 100.0;
        commande.setTotalHt(Math.max(0, currentSubTotal - currentCouponDiscount));
        commande.setTotalTtc(total);

        try {
            commandeService.add(commande);
            if (currentCoupon != null) {
                couponService.incrementUsage(currentCoupon.getId());
            }
            panierService.clear();
            refreshPanier();
            new Alert(Alert.AlertType.INFORMATION, "Commande confirmee avec succes.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur creation commande: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void goToProduits() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontProduits.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToTracking() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontMesCommandes.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goBackHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goHome() throws IOException {
        goBackHome();
    }

    @FXML
    private void showFrontProduits() throws IOException {
        goToProduits();
    }

    @FXML
    private void showFrontPanier() {
        // already on panier page
    }

    @FXML
    private void handleClearCart() {
        panierService.clear();
        refreshPanier();
    }

    @FXML
    private void handleRemoveSelected() {
        FrontPanierService.CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Selectionnez un produit dans le panier.").showAndWait();
            return;
        }
        panierService.removeProduit(selected.getProduitId());
        refreshPanier();
    }

    @FXML
    private void showFrontTracking() throws IOException {
        goToTracking();
    }

    @FXML
    private void handleSearch() {
        couponField.requestFocus();
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void showProfile() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void logout() throws IOException {
        userService.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(scene);
    }

    private void renderTotals() {
        double totalFinal = Math.max(0, currentSubTotal - currentCouponDiscount + currentShipping);
        subTotalLabel.setText(String.format("%.2f DT", currentSubTotal));
        couponDiscountLabel.setText(String.format("-%.2f DT", currentCouponDiscount));
        shippingLabel.setText(String.format("%.2f DT", currentShipping));
        totalFinalLabel.setText(String.format("%.2f DT", totalFinal));
    }
}
