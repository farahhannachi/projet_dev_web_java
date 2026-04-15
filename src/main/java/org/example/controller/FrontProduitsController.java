package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Produit;
import org.example.service.FrontPanierService;
import org.example.service.ProduitService;
import org.example.service.PromotionService;
import org.example.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class FrontProduitsController {
    @FXML private FlowPane productsFlow;
    @FXML private VBox cartItemsBox;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private ComboBox<String> stockFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;

    private final ProduitService produitService = new ProduitService();
    private final PromotionService promotionService = new PromotionService();
    private final FrontPanierService panierService = new FrontPanierService();
    private final UserService userService = new UserService();

    private ObservableList<Produit> produits;

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        stockFilter.setItems(FXCollections.observableArrayList("Tous", "Disponible", "Stock faible", "Rupture"));
        stockFilter.setValue("Tous");
        sortFilter.setItems(FXCollections.observableArrayList("Nom", "Prix +", "Prix -"));
        sortFilter.setValue("Nom");

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        categorieFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        stockFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        loadProduits();
        refreshCartSummary();
    }

    private void loadProduits() {
        List<Produit> list = produitService.getAll().stream()
                .filter(p -> "disponible".equalsIgnoreCase(p.getStatut()) || "stock_critique".equalsIgnoreCase(p.getStatut()))
                .toList();
        produits = FXCollections.observableArrayList(list);

        ObservableList<String> categories = FXCollections.observableArrayList("Toutes categories");
        list.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .forEach(categories::add);
        categorieFilter.setItems(categories);
        categorieFilter.setValue("Toutes categories");

        applyFilters();
    }

    @FXML
    private void applyFilters() {
        if (produits == null) {
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categorieFilter.getValue();
        String stock = stockFilter.getValue();
        String sort = sortFilter.getValue();

        Comparator<Produit> comparator;
        if ("Prix +".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparingDouble(p -> promotionService.getPromotionalPrice(p.getId(), p.getPrix()));
        } else if ("Prix -".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparingDouble((Produit p) -> promotionService.getPromotionalPrice(p.getId(), p.getPrix())).reversed();
        } else {
            comparator = Comparator.comparing(p -> p.getNom() == null ? "" : p.getNom().toLowerCase());
        }

        List<Produit> filtered = produits.stream()
                .filter(p -> {
                    if (!search.isEmpty()) {
                        String nom = p.getNom() == null ? "" : p.getNom().toLowerCase();
                        String desc = p.getDescription() == null ? "" : p.getDescription().toLowerCase();
                        if (!nom.contains(search) && !desc.contains(search)) {
                            return false;
                        }
                    }
                    if (category != null && !"Toutes categories".equalsIgnoreCase(category)) {
                        if (p.getCategorie() == null || !p.getCategorie().equalsIgnoreCase(category)) {
                            return false;
                        }
                    }
                    int qty = p.getQuantiteStock();
                    if ("Disponible".equalsIgnoreCase(stock) && qty <= 0) {
                        return false;
                    }
                    if ("Stock faible".equalsIgnoreCase(stock) && !(qty > 0 && qty <= 5)) {
                        return false;
                    }
                    if ("Rupture".equalsIgnoreCase(stock) && qty > 0) {
                        return false;
                    }
                    return true;
                })
                .sorted(comparator)
                .toList();

        renderProductCards(filtered);
    }

    private void renderProductCards(List<Produit> source) {
        productsFlow.getChildren().clear();

        for (Produit p : source) {
            VBox card = new VBox(8);
            card.getStyleClass().add("front-product-card");
            card.setPrefWidth(270);
            card.setPadding(new Insets(10));

            ImageView imageView = new ImageView(new Image(resolveProductImageUrl(p), true));
            imageView.setFitWidth(248);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("front-product-image");

            Label name = new Label(p.getNom());
            name.getStyleClass().add("front-product-name");

            Label desc = new Label(shortDescription(p.getDescription()));
            desc.getStyleClass().add("front-product-desc");
            desc.setWrapText(true);

            double promoPrice = promotionService.getPromotionalPrice(p.getId(), p.getPrix());
            Label price = new Label(String.format("%.2f DT", promoPrice));
            price.getStyleClass().add("front-product-price");

            Label stock = new Label(buildStockText(p.getQuantiteStock()));
            stock.getStyleClass().add(p.getQuantiteStock() <= 0 ? "stock-out" : (p.getQuantiteStock() <= 5 ? "stock-low" : "stock-ok"));

            Button addBtn = new Button("Ajouter au panier");
            addBtn.getStyleClass().add("btn-admin-primary");
            addBtn.setMaxWidth(Double.MAX_VALUE);
            addBtn.setDisable(p.getQuantiteStock() <= 0);
            addBtn.setOnAction(e -> {
                panierService.addProduit(p.getId(), 1);
                refreshCartSummary();
            });

            VBox.setVgrow(desc, Priority.ALWAYS);
            card.getChildren().addAll(imageView, name, desc, price, stock, addBtn);
            productsFlow.getChildren().add(card);
        }

        if (source.isEmpty()) {
            Label noResults = new Label("Aucun produit ne correspond aux filtres.");
            noResults.getStyleClass().add("admin-subtitle");
            productsFlow.getChildren().add(noResults);
        }
    }

    @FXML
    private void handleAddToCart() {
        new Alert(Alert.AlertType.INFORMATION, "Utilisez le bouton 'Ajouter au panier' directement sur la carte produit.").showAndWait();
    }

    @FXML
    private void goToCommande() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontCommande.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) productsFlow.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goToTracking() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontMesCommandes.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) productsFlow.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goBackHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) productsFlow.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goHome() throws IOException {
        goBackHome();
    }

    @FXML
    private void showFrontProduits() {
        // already on products page
    }

    @FXML
    private void showFrontPanier() throws IOException {
        goToCommande();
    }

    @FXML
    private void showFrontTracking() throws IOException {
        goToTracking();
    }

    @FXML
    private void handleSearch() {
        searchField.requestFocus();
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
        Stage stage = (Stage) productsFlow.getScene().getWindow();
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
        Stage stage = (Stage) productsFlow.getScene().getWindow();
        stage.setScene(scene);
    }

    private void refreshCartSummary() {
        cartCountLabel.setText(String.valueOf(panierService.getNombreArticles()));
        cartTotalLabel.setText(String.format("%.2f DT", panierService.getTotal(produitService, promotionService)));

        cartItemsBox.getChildren().clear();
        for (FrontPanierService.CartItem item : panierService.getItems(produitService, promotionService)) {
            HBox row = new HBox(8);
            row.getStyleClass().add("front-cart-item");

            Label label = new Label(item.getNom() + " x" + item.getQuantite());
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);

            Button minus = new Button("-");
            minus.getStyleClass().add("btn-admin-outline");
            minus.setOnAction(e -> {
                panierService.setQuantite(item.getProduitId(), item.getQuantite() - 1);
                refreshCartSummary();
            });

            Button plus = new Button("+");
            plus.getStyleClass().add("btn-admin-outline");
            plus.setOnAction(e -> {
                panierService.setQuantite(item.getProduitId(), item.getQuantite() + 1);
                refreshCartSummary();
            });

            Label lineTotal = new Label(String.format("%.2f", item.getTotalLigne()));
            row.getChildren().addAll(label, minus, plus, lineTotal);
            cartItemsBox.getChildren().add(row);
        }

        if (cartItemsBox.getChildren().isEmpty()) {
            cartItemsBox.getChildren().add(new Label("Panier vide"));
        }
    }

    @FXML
    private void handleClearCart() {
        panierService.clear();
        refreshCartSummary();
    }

    private String shortDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Description indisponible";
        }
        if (description.length() <= 90) {
            return description;
        }
        return description.substring(0, 90) + "...";
    }

    private String buildStockText(int qty) {
        if (qty <= 0) {
            return "Rupture";
        }
        if (qty <= 5) {
            return "Stock faible: " + qty;
        }
        return "Stock: " + qty;
    }

    private String resolveProductImageUrl(Produit produit) {
        if (produit.getImage() != null && !produit.getImage().isBlank()) {
            String image = produit.getImage().trim();
            if (image.startsWith("http://") || image.startsWith("https://")) {
                return image;
            }

            File file = new File(System.getProperty("user.dir"), image.replace("/", File.separator));
            if (file.exists()) {
                return file.toURI().toString();
            }

            if (!image.contains("/")) {
                File uploads = new File(System.getProperty("user.dir"), "uploads" + File.separator + "produits" + File.separator + image);
                if (uploads.exists()) {
                    return uploads.toURI().toString();
                }
            }
        }

        File fallback = new File(System.getProperty("user.dir"), "projet_dev_web_java-update-produits-commandes" + File.separator + "public" + File.separator + "assets" + File.separator + "img" + File.separator + "default-product.png");
        if (fallback.exists()) {
            return fallback.toURI().toString();
        }
        return "https://via.placeholder.com/248x150?text=Produit";
    }
}
