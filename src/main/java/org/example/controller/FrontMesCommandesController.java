package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.model.Commande;
import org.example.model.User;
import org.example.service.CommandeService;
import org.example.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FrontMesCommandesController {
    @FXML private VBox ordersContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private Label totalCommandesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label confirmeesLabel;
    @FXML private Label livreesLabel;
    @FXML private Label montantTotalLabel;
    @FXML private FrontShopNavBarController shopNavController;

    private final CommandeService commandeService = new CommandeService();
    private final UserService userService = new UserService();
    private List<Commande> userOrders = List.of();

    @FXML
    public void initialize() {
        if (shopNavController != null) {
            shopNavController.configure(FrontShopNavBarController.ActiveShopPage.COMMANDES, searchField);
        }

        statusFilter.setItems(FXCollections.observableArrayList("Tous statuts", "En attente", "Confirmee", "Livree", "Annulee", "Bloquee"));
        statusFilter.setValue("Tous statuts");

        sortFilter.setItems(FXCollections.observableArrayList("Date recente", "Date ancienne", "Total croissant", "Total decroissant"));
        sortFilter.setValue("Date recente");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        sortFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        loadMyOrders();
    }

    private void loadMyOrders() {
        User current = userService.getCurrentUser();
        if (current == null) {
            ordersContainer.getChildren().clear();
            return;
        }

        List<Commande> all = commandeService.getAll();
        userOrders = all.stream()
                .filter(c -> {
                    if (c.getUtilisateurId() != null) {
                        return c.getUtilisateurId() == current.getId();
                    }
                    return c.getEmail() != null && c.getEmail().equalsIgnoreCase(current.getEmail());
                })
                .toList();

        long enAttente = userOrders.stream().filter(c -> "en_attente".equalsIgnoreCase(c.getStatut())).count();
        long confirmees = userOrders.stream().filter(c -> "confirmee".equalsIgnoreCase(c.getStatut())).count();
        long livrees = userOrders.stream().filter(c -> "livree".equalsIgnoreCase(c.getStatut())).count();
        double totalMontant = userOrders.stream().mapToDouble(Commande::getTotal).sum();

        totalCommandesLabel.setText(String.valueOf(userOrders.size()));
        enAttenteLabel.setText(String.valueOf(enAttente));
        if (confirmeesLabel != null) {
            confirmeesLabel.setText(String.valueOf(confirmees));
        }
        livreesLabel.setText(String.valueOf(livrees));
        if (montantTotalLabel != null) {
            montantTotalLabel.setText(String.format("%.2f DT", totalMontant));
        }

        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue() == null ? "Tous statuts" : statusFilter.getValue();
        String sort = sortFilter.getValue() == null ? "Date recente" : sortFilter.getValue();

        Comparator<Commande> comparator;
        switch (sort) {
            case "Date ancienne" -> comparator = Comparator.comparing(Commande::getDateCommandeDateTime, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Total croissant" -> comparator = Comparator.comparingDouble(Commande::getTotal);
            case "Total decroissant" -> comparator = Comparator.comparingDouble(Commande::getTotal).reversed();
            default -> comparator = Comparator.comparing(Commande::getDateCommandeDateTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        List<Commande> filtered = userOrders.stream()
                .filter(c -> matchesQuery(c, query))
                .filter(c -> matchesStatus(c, status))
                .sorted(comparator)
                .toList();

        renderOrders(filtered);
    }

    private boolean matchesQuery(Commande c, String query) {
        if (query.isBlank()) {
            return true;
        }
        String nom = c.getNom() == null ? "" : c.getNom().toLowerCase(Locale.ROOT);
        String email = c.getEmail() == null ? "" : c.getEmail().toLowerCase(Locale.ROOT);
        String adresse = c.getAdresseLivraison() == null ? "" : c.getAdresseLivraison().toLowerCase(Locale.ROOT);
        return nom.contains(query) || email.contains(query) || adresse.contains(query);
    }

    private boolean matchesStatus(Commande c, String status) {
        if ("Tous statuts".equalsIgnoreCase(status)) {
            return true;
        }
        String normalized = c.getStatut() == null ? "" : c.getStatut().toLowerCase(Locale.ROOT);
        return switch (status) {
            case "En attente" -> "en_attente".equals(normalized);
            case "Confirmee" -> "confirmee".equals(normalized);
            case "Livree" -> "livree".equals(normalized);
            case "Annulee" -> "annulee".equals(normalized);
            case "Bloquee" -> "bloquee".equals(normalized);
            default -> true;
        };
    }

    private void renderOrders(List<Commande> orders) {
        ordersContainer.getChildren().clear();
        if (orders.isEmpty()) {
            Label empty = new Label("Aucune commande pour les filtres selectionnes.");
            empty.getStyleClass().add("admin-subtitle");
            ordersContainer.getChildren().add(empty);
            return;
        }

        for (Commande commande : orders) {
            ordersContainer.getChildren().add(buildOrderCard(commande));
        }
    }

    private VBox buildOrderCard(Commande commande) {
        VBox card = new VBox(8);
        card.getStyleClass().add("commande-card");
        card.setPadding(new Insets(12));

        HBox head = new HBox(10);
        Label title = new Label("Commande #" + commande.getId());
        title.getStyleClass().add("commande-title");
        Label badge = new Label(formatStatut(commande.getStatut()));
        badge.getStyleClass().addAll("commande-badge", badgeClass(commande.getStatut()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(title, spacer, badge);

        String dateTxt = commande.getDateCommandeDateTime() == null
                ? "Date inconnue"
                : commande.getDateCommandeDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label meta = new Label(dateTxt + " | " + safe(commande.getModePaiement()));
        meta.getStyleClass().add("commande-meta");

        HBox row1 = new HBox(10);
        Label addr = new Label(safe(commande.getAdresseLivraison()));
        addr.getStyleClass().add("commande-line");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label total = new Label(String.format("%.2f DT", commande.getTotal()));
        total.getStyleClass().add("commande-total");
        row1.getChildren().addAll(addr, spacer2, total);

        card.getChildren().addAll(head, meta, row1);
        return card;
    }

    private String formatStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return "Inconnu";
        }
        return switch (statut.toLowerCase(Locale.ROOT)) {
            case "en_attente" -> "En attente";
            case "confirmee" -> "Confirmee";
            case "livree" -> "Livree";
            case "annulee" -> "Annulee";
            case "bloquee" -> "Bloquee";
            default -> statut;
        };
    }

    private String badgeClass(String statut) {
        if (statut == null) {
            return "commande-badge-neutral";
        }
        return switch (statut.toLowerCase(Locale.ROOT)) {
            case "en_attente" -> "commande-badge-warn";
            case "confirmee", "livree" -> "commande-badge-ok";
            case "annulee", "bloquee" -> "commande-badge-danger";
            default -> "commande-badge-neutral";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
