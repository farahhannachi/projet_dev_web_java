package org.example.controller;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.model.Commande;
import org.example.model.Coupon;
import org.example.model.User;
import org.example.model.Address;
import org.example.service.AddressService;
import org.example.service.CommandeService;
import org.example.service.CouponService;
import org.example.service.DeliveryEstimatorService;
import org.example.service.FrontPanierService;
import org.example.service.MailerService;
import org.example.service.MapsApiService;
import org.example.service.ProduitService;
import org.example.service.PromotionService;
import org.example.service.UserService;
import org.example.service.ChatbotService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Duration;
import javafx.scene.control.ToggleGroup;

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
    @FXML private ComboBox<Address> savedAddressCombo;
    @FXML private VBox mapSection;
    @FXML private TextField mapSearchField;
    @FXML private ListView<String> mapResultsList;
    @FXML private ImageView mapPreviewImage;
    @FXML private Label mapStatusLabel;
    @FXML private Label selectedLocationLabel;
    @FXML private TextArea messageField;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private RadioButton paiementLivraisonRadio;
    @FXML private RadioButton paiementEnLigneRadio;
    @FXML private FrontShopNavBarController shopNavController;

    private final FrontPanierService panierService = new FrontPanierService();
    private final ProduitService produitService = ProduitService.getInstance();
    private final PromotionService promotionService = new PromotionService();
    private final CouponService couponService = new CouponService();
    private final CommandeService commandeService = new CommandeService();
    private final UserService userService = new UserService();
    private final AddressService addressService = new AddressService();
    private final MailerService mailerService = new MailerService();
    private final DeliveryEstimatorService deliveryEstimatorService = new DeliveryEstimatorService();
    private final MapsApiService mapsApiService = new MapsApiService();
    private final ChatbotService chatbotService = new ChatbotService();

    private double currentSubTotal;
    private double currentCouponDiscount;
    private double currentShipping;
    private Coupon currentCoupon;
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedDisplayAddress;
    private List<MapsApiService.GeocodeResult> currentAddressResults = new ArrayList<>();
    private final PauseTransition mapSearchDebounce = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        if (shopNavController != null) {
            shopNavController.configure(FrontShopNavBarController.ActiveShopPage.PANIER, nomField::requestFocus);
        }

        produitCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        quantiteCol.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalLigne"));
        initActionsColumn();

        paiementCombo.setItems(FXCollections.observableArrayList("en_ligne", "espece", "cheque"));
        paiementCombo.setValue("en_ligne");

        ToggleGroup paymentGroup = new ToggleGroup();
        if (paiementLivraisonRadio != null) {
            paiementLivraisonRadio.setToggleGroup(paymentGroup);
        }
        if (paiementEnLigneRadio != null) {
            paiementEnLigneRadio.setToggleGroup(paymentGroup);
            paiementEnLigneRadio.setSelected(true);
        }
        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (paiementCombo == null) {
                return;
            }
            if (newV == paiementLivraisonRadio) {
                paiementCombo.setValue("espece");
            } else {
                paiementCombo.setValue("en_ligne");
            }
        });

        initializeMapSelection();

        User current = userService.getCurrentUser();
        if (current != null) {
            nomField.setText(current.getNom());
            emailField.setText(current.getEmail());
            loadSavedAddresses(current.getId());
        }

        refreshPanier();
    }

    private void loadSavedAddresses(int userId) {
        if (savedAddressCombo == null) {
            return;
        }

        List<Address> addresses = addressService.getByUserId(userId);
        savedAddressCombo.setItems(FXCollections.observableArrayList(addresses));
        savedAddressCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) {
                return;
            }
            selectedDisplayAddress = selected.toSingleLine();
            adresseField.setText(selectedDisplayAddress);
            selectedLocationLabel.setText("Adresse choisie: " + selectedDisplayAddress);
            mapSearchField.setText(selected.toSingleLine());
            searchAddressOnMap(false);
        });
    }

    private void initializeMapSelection() {
        if (mapResultsList == null) {
            return;
        }

        mapResultsList.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV == null ? -1 : newV.intValue();
            if (idx < 0 || idx >= currentAddressResults.size()) {
                return;
            }

            MapsApiService.GeocodeResult selected = currentAddressResults.get(idx);
            selectedLatitude = selected.latitude();
            selectedLongitude = selected.longitude();
            selectedDisplayAddress = selected.displayName();

            adresseField.setText(selectedDisplayAddress);
            selectedLocationLabel.setText("Adresse choisie: " + selectedDisplayAddress);
            updateMapPreview(selected.latitude(), selected.longitude());
        });

        mapSearchDebounce.setOnFinished(e -> searchAddressOnMap(false));
        if (mapSearchField != null) {
            mapSearchField.textProperty().addListener((obs, oldV, newV) -> {
                String q = newV == null ? "" : newV.trim();
                if (q.length() < 3) {
                    if (mapResultsList != null) {
                        mapResultsList.getItems().clear();
                    }
                    return;
                }
                mapSearchDebounce.playFromStart();
            });
        }

        selectedLocationLabel.setText("Aucune localisation selectionnee");
        updateMapPreview(36.8065, 10.1815);
    }

        @FXML
        private void toggleMapSection() {
                boolean show = !mapSection.isVisible();
                mapSection.setVisible(show);
                mapSection.setManaged(show);
        }

        @FXML
        private void handleSearchAddressOnMap() {
            searchAddressOnMap(true);
        }

        private void searchAddressOnMap(boolean alertOnEmpty) {
            String query = mapSearchField.getText() == null ? "" : mapSearchField.getText().trim();
            if (query.isEmpty()) {
                if (alertOnEmpty) {
                    new Alert(Alert.AlertType.WARNING, "Saisissez une adresse a rechercher.").showAndWait();
                }
                return;
            }

            if (mapStatusLabel != null) {
                mapStatusLabel.setText("Recherche en cours...");
            }

            Thread thread = new Thread(() -> {
                List<MapsApiService.GeocodeResult> results = mapsApiService.searchAddresses(query, 5);
                if (results.isEmpty()) {
                    MapsApiService.GeocodeResult one = mapsApiService.geocodeAddress(query);
                    if (one.found()) {
                        results = List.of(one);
                    }
                }

                final List<MapsApiService.GeocodeResult> finalResults = results;
                javafx.application.Platform.runLater(() -> {
                    currentAddressResults = finalResults;
                    if (currentAddressResults.isEmpty()) {
                        if (alertOnEmpty) {
                            new Alert(Alert.AlertType.WARNING, "Adresse introuvable.").showAndWait();
                        }
                        if (mapStatusLabel != null) {
                            mapStatusLabel.setText("Aucun resultat pour cette adresse.");
                        }
                        return;
                    }

                    List<String> labels = currentAddressResults.stream().map(MapsApiService.GeocodeResult::displayName).toList();
                    mapResultsList.setItems(FXCollections.observableArrayList(labels));
                    mapResultsList.getSelectionModel().select(0);
                    if (mapStatusLabel != null) {
                        mapStatusLabel.setText(currentAddressResults.size() + " resultat(s) trouve(s).");
                    }
                });
            }, "map-search");
            thread.setDaemon(true);
            thread.start();
        }

        @FXML
        private void handleUseMyLocation() {
            if (mapStatusLabel != null) {
                mapStatusLabel.setText("Recherche de votre localisation...");
            }

            Thread thread = new Thread(() -> {
                MapsApiService.LocationResult location = mapsApiService.approximateCurrentLocation();
                javafx.application.Platform.runLater(() -> {
                    if (!location.found()) {
                        if (mapStatusLabel != null) {
                            mapStatusLabel.setText("Localisation indisponible: " + location.error());
                        }
                        return;
                    }

                    selectedLatitude = location.latitude();
                    selectedLongitude = location.longitude();
                    selectedDisplayAddress = mapsApiService.reverseGeocode(location.latitude(), location.longitude());
                    if (selectedDisplayAddress == null || selectedDisplayAddress.isBlank()) {
                        selectedDisplayAddress = String.format("Lat %.5f, Lng %.5f", selectedLatitude, selectedLongitude);
                    }

                    adresseField.setText(selectedDisplayAddress);
                    selectedLocationLabel.setText("Adresse choisie: " + selectedDisplayAddress);
                    updateMapPreview(selectedLatitude, selectedLongitude);
                    if (mapStatusLabel != null) {
                        mapStatusLabel.setText("Position detectee: " + location.cityLabel());
                    }
                });
            });
            thread.setDaemon(true);
            thread.start();
        }

        private void updateMapPreview(double lat, double lon) {
            if (mapPreviewImage == null) {
                return;
            }

            String staticUrl = mapsApiService.buildStaticMapUrl(lat, lon, 14, 640, 280);

            if (mapStatusLabel != null) {
                mapStatusLabel.setText("Chargement de la carte...");
            }

            Image image = new Image(staticUrl, true);
            image.errorProperty().addListener((obs, oldErr, isErr) -> {
                if (Boolean.TRUE.equals(isErr)) {
                    loadMapPreviewFallback(lat, lon);
                }
            });
            mapPreviewImage.setImage(image);

            if (!image.isBackgroundLoading()) {
                if (image.isError()) {
                    loadMapPreviewFallback(lat, lon);
                } else if (mapStatusLabel != null) {
                    mapStatusLabel.setText("Carte chargee.");
                }
            } else {
                image.progressProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null && newV.doubleValue() >= 1.0 && mapStatusLabel != null && !image.isError()) {
                        mapStatusLabel.setText("Carte chargee.");
                    }
                });
            }
        }

        private void loadMapPreviewFallback(double lat, double lon) {
            String fallbackUrl = mapsApiService.buildStaticMapFallbackUrl(lat, lon, 14, 640, 280);
            Image fallbackImage = new Image(fallbackUrl, true);
            fallbackImage.errorProperty().addListener((obs, oldErr, isErr) -> {
                if (Boolean.TRUE.equals(isErr)) {
                    if (mapStatusLabel != null) {
                        mapStatusLabel.setText("Impossible de charger la carte.");
                    }
                }
            });
            mapPreviewImage.setImage(fallbackImage);

            if (mapStatusLabel != null && !fallbackImage.isError()) {
                mapStatusLabel.setText("Carte chargee (fallback).");
            }
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

        List<FrontPanierService.CartItem> orderItemsSnapshot = List.copyOf(cartTable.getItems());

        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String telephone = telephoneField.getText() == null ? "" : telephoneField.getText().trim();
        String adresse = adresseField.getText() == null ? "" : adresseField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || adresse.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Nom, email et adresse sont obligatoires.").showAndWait();
            return;
        }

        if (selectedDisplayAddress == null || selectedDisplayAddress.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez choisir votre adresse sur la map avant de confirmer.").showAndWait();
            return;
        }

        User current = userService.getCurrentUser();

        Commande commande = new Commande();
        commande.setUtilisateurId(current != null ? current.getId() : null);
        commande.setDateCommande(LocalDate.now());
        commande.setDateCommandeDateTime(LocalDateTime.now());
        commande.setStatut("en_attente");
        commande.setModePaiement(paiementCombo.getValue());
        commande.setAdresseLivraison(selectedDisplayAddress);
        commande.setTelephone(telephone);
        commande.setNom(nom);
        commande.setEmail(email);
        String orderMessage = messageField != null && messageField.getText() != null && !messageField.getText().isBlank()
            ? messageField.getText().trim()
            : "Commande front-office JavaFX";
        ChatbotService.ProfanityResult profanity = chatbotService.checkProfanity(orderMessage);
        commande.setMessage(profanity.success() ? profanity.censored() : orderMessage);
        commande.setProduitsIds(panierService.getProduitsIdsJson());
        commande.setCouponCode(currentCoupon != null ? currentCoupon.getCode() : null);
        commande.setCouponDiscount(currentCouponDiscount);
        commande.setFraudScore(0);
        commande.setBaseShippingCost(currentShipping);
        commande.setEstimatedDeliveryDate(deliveryEstimatorService.estimateDeliveryDateWithMaps(commande, mapsApiService));

        double total = Math.max(0, currentSubTotal - currentCouponDiscount + currentShipping);
        total = Math.round(total * 100.0) / 100.0;
        commande.setTotalHt(Math.max(0, currentSubTotal - currentCouponDiscount));
        commande.setTotalTtc(total);

        try {
            int createdId = commandeService.add(commande);
            commande.setId(createdId);
            if (currentCoupon != null) {
                couponService.incrementUsage(currentCoupon.getId());
            }

            if (mailerService.canSend()) {
                mailerService.clearLastError();
                mailerService.sendCommandeInvoiceEmail(commande, orderItemsSnapshot);
                mailerService.sendAdminCommandeNotification(commande, orderItemsSnapshot);
                if (mailerService.getLastError() != null && !mailerService.getLastError().isBlank()) {
                    System.err.println("[MAILER] Email non envoye: " + mailerService.getLastError());
                }
            } else {
                System.out.println("[MAILER] SMTP non configure, email de confirmation ignore.");
            }

            panierService.clear();
            refreshPanier();
            new Alert(Alert.AlertType.INFORMATION, "Commande confirmee avec succes.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur creation commande: " + e.getMessage()).showAndWait();
        }
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

    private void renderTotals() {
        double totalFinal = Math.max(0, currentSubTotal - currentCouponDiscount + currentShipping);
        subTotalLabel.setText(String.format("%.2f DT", currentSubTotal));
        couponDiscountLabel.setText(String.format("-%.2f DT", currentCouponDiscount));
        shippingLabel.setText(String.format("%.2f DT", currentShipping));
        totalFinalLabel.setText(String.format("%.2f DT", totalFinal));
    }

}
