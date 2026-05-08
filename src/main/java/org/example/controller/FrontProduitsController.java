package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.model.Produit;
import org.example.service.ChatbotService;
import org.example.service.FrontPanierService;
import org.example.service.MailerService;
import org.example.service.ProduitService;
import org.example.service.PromotionService;
import org.example.service.UserService;
import org.example.util.SceneNavigation;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FrontProduitsController {
    @FXML private FlowPane productsFlow;
    @FXML private VBox cartItemsBox;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private ComboBox<String> stockFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;
    @FXML private ListView<String> chatMessagesList;
    @FXML private TextField chatInputField;
    @FXML private VBox medBotPanel;
    @FXML private Button medBotLauncher;
    @FXML private FrontShopNavBarController shopNavController;

    private final ProduitService produitService = ProduitService.getInstance();
    private final PromotionService promotionService = new PromotionService();
    private final FrontPanierService panierService = new FrontPanierService();
    private final UserService userService = new UserService();
    private final ChatbotService chatbotService = new ChatbotService();
    private final MailerService mailerService = new MailerService();

    private ObservableList<Produit> produits;
        private final List<Map<String, List<String>>> symptomRules = List.of(
            Map.of(
                "keys", List.of("tete", "migraine", "mal de tete"),
                "meds", List.of("doliprane", "paracetamol", "efferalgan"),
                "generic", List.of("Paracetamol", "Ibuprofene (si pas de contre-indication)", "Hydratation + repos")
            ),
            Map.of(
                "keys", List.of("fievre", "temperature"),
                "meds", List.of("doliprane", "paracetamol", "efferalgan"),
                "generic", List.of("Paracetamol", "Hydratation", "Surveillance de la temperature")
            ),
            Map.of(
                "keys", List.of("gorge", "angine", "toux"),
                "meds", List.of("sirop", "pastille", "hexaspray"),
                "generic", List.of("Pastilles antiseptiques", "Sirop selon type de toux", "Lavages de nez au serum physiologique")
            ),
            Map.of(
                "keys", List.of("ventre", "estomac", "nausee", "diarrhee"),
                "meds", List.of("smecta", "gaviscon", "spasfon"),
                "generic", List.of("Solution de rehydratation", "Antispasmodique", "Pansement gastrique")
            ),
            Map.of(
                "keys", List.of("allergie", "rhume", "nez", "eternuement"),
                "meds", List.of("cetirizine", "loratadine", "spray"),
                "generic", List.of("Antihistaminique", "Spray nasal saline", "Eviter l allergene si connu")
            ),
            Map.of(
                "keys", List.of("douleur", "muscle", "dos"),
                "meds", List.of("ibuprofene", "diclofenac", "doliprane"),
                "generic", List.of("Paracetamol", "Anti-inflammatoire local", "Repos + glace/chaleur selon douleur")
            )
        );

    @FXML
    public void initialize() {
        if (shopNavController != null) {
            shopNavController.configure(FrontShopNavBarController.ActiveShopPage.PRODUITS, searchField);
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
        initializeChatbot();

        if (medBotPanel != null) {
            medBotPanel.setVisible(false);
            medBotPanel.setManaged(false);
            medBotPanel.setMouseTransparent(true);
        }
    }

    private void initializeChatbot() {
        if (chatMessagesList == null) {
            return;
        }

        chatMessagesList.setItems(FXCollections.observableArrayList());
        chatMessagesList.getItems().add("Assistant: Bonjour, decrivez votre symptome (ex: j'ai mal a la tete).");
    }

    @FXML
    private void handleSendChatMessage() {
        String message = chatInputField.getText() == null ? "" : chatInputField.getText().trim();
        if (message.isBlank()) {
            return;
        }

        chatMessagesList.getItems().add("Vous: " + message);
        chatInputField.clear();

        ChatbotService.ProfanityResult profanityResult = chatbotService.checkProfanity(message);
        String safeMessage = profanityResult.success() ? profanityResult.censored() : message;
        if (profanityResult.success() && profanityResult.hasProfanity()) {
            chatMessagesList.getItems().add("System: Message filtre pour langage inapproprie.");
        }

        String deterministicReply = buildSymptomReply(safeMessage);
        if (deterministicReply != null) {
            chatMessagesList.getItems().add("Assistant: " + deterministicReply);
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);
            return;
        }

        Thread thread = new Thread(() -> {
            ChatbotService.ChatbotResult result = chatbotService.ask(safeMessage);
            Platform.runLater(() -> {
                if (result.success()) {
                    chatMessagesList.getItems().add("Assistant: " + result.reply());
                } else {
                    chatMessagesList.getItems().add("Assistant: Service indisponible. " + result.error());
                }
                chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void toggleMedBotPanel() {
        if (medBotPanel == null) {
            return;
        }

        boolean open = !medBotPanel.isVisible();
        medBotPanel.setVisible(open);
        medBotPanel.setManaged(open);
        medBotPanel.setMouseTransparent(!open);

        if (open && chatMessagesList != null && chatMessagesList.getItems().isEmpty()) {
            chatMessagesList.getItems().add("Assistant: Bonjour, decrivez votre symptome (ex: j'ai mal a la tete).");
        }
        if (open && chatInputField != null) {
            chatInputField.requestFocus();
        }
    }

    @FXML
    private void closeMedBotPanel() {
        if (medBotPanel == null) {
            return;
        }
        medBotPanel.setVisible(false);
        medBotPanel.setManaged(false);
        medBotPanel.setMouseTransparent(true);
    }

    @FXML
    private void handleSendChatTranscript() {
        if (chatMessagesList == null || chatMessagesList.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune conversation a envoyer.").showAndWait();
            return;
        }

        if (!mailerService.canSend()) {
            new Alert(Alert.AlertType.WARNING, "Mailing non configure (SMTP). Configurez SMTP_USER et SMTP_PASS.").showAndWait();
            return;
        }

        String toEmail = userService.getCurrentUser() != null ? userService.getCurrentUser().getEmail() : null;
        String userName = userService.getCurrentUser() != null ? userService.getCurrentUser().getNom() : "Client";

        if (toEmail == null || toEmail.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Email utilisateur introuvable.").showAndWait();
            return;
        }

        try {
            mailerService.sendChatbotTranscriptEmail(toEmail, userName, List.copyOf(chatMessagesList.getItems()));
            new Alert(Alert.AlertType.INFORMATION, "Conversation envoyee par mail.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur envoi mail: " + e.getMessage()).showAndWait();
        }
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
    private void goToCommande() {
        SceneNavigation.replaceScene(productsFlow, "/fxml/FrontCommande.fxml");
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

    private String buildSymptomReply(String question) {
        String q = normalizeText(question);
        if (q.isBlank()) {
            return null;
        }

        for (Map<String, List<String>> rule : symptomRules) {
            List<String> keys = rule.getOrDefault("keys", List.of());
            boolean matches = keys.stream().anyMatch(k -> q.contains(normalizeText(k)));
            if (!matches) {
                continue;
            }

            List<String> medsKeys = rule.getOrDefault("meds", List.of());
            List<String> availableProducts = findMatchingAvailableProducts(medsKeys);
            String productPart = availableProducts.isEmpty()
                    ? "Aucun produit correspondant n est disponible actuellement dans le catalogue. "
                    : "Produits disponibles chez nous: " + String.join(", ", availableProducts.subList(0, Math.min(3, availableProducts.size()))) + ". ";

            List<String> generic = rule.getOrDefault("generic", List.of());
            String genericPart = generic.isEmpty()
                    ? ""
                    : "Suggestions generales: " + String.join(", ", generic) + ". ";

            String safety = "Si symptomes forts, persistants, grossesse, enfant, ou maladie chronique: demandez avis medical.";
            return productPart + genericPart + safety;
        }

        return "Je peux proposer des conseils generaux pour: tete, fievre, gorge/toux, ventre, allergie/rhume, douleur musculaire.";
    }

    private List<String> findMatchingAvailableProducts(List<String> medsKeys) {
        List<String> found = new ArrayList<>();
        if (produits == null || medsKeys == null || medsKeys.isEmpty()) {
            return found;
        }

        for (Produit p : produits) {
            if (p == null || p.getQuantiteStock() <= 0) {
                continue;
            }
            String normalizedName = normalizeText(p.getNom());
            boolean matches = medsKeys.stream().map(this::normalizeText).anyMatch(normalizedName::contains);
            if (matches) {
                found.add(p.getNom());
            }
        }
        return found;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase().trim();
    }

}
