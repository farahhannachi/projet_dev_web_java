package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.model.Produit;
import org.example.service.ProduitService;
import java.util.List;

/**
 * Contrôleur pour la gestion des Produits
 */
public class ProduitController {
    @FXML private VBox listView;
    @FXML private Label totalProduitsLabel;
    @FXML private Label valeurTotaleLabel;
    @FXML private Label produitPlusChierLabel;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private ComboBox<String> statusFilter;
    
    @FXML private TableView<Produit> productTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colQuantite;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, Boolean> colDisponible;
    @FXML private TableColumn<Produit, Void> colActions;
    
    @FXML private Pagination pagination;
    
    private ProduitService produitService = ProduitService.getInstance();
    private List<Produit> allProduits;

    @FXML
    public void initialize() {
        loadData();
        setupTableColumns();
        setupListeners();
        loadStatistics();
    }

    private void loadData() {
        allProduits = produitService.getAll();
        productTable.getItems().addAll(allProduits);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(param -> new javafx.beans.property.SimpleIntegerProperty(param.getValue().getId()).asObject());
        colNom.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getNom()));
        colDescription.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getDescription()));
        colPrix.setCellValueFactory(param -> new javafx.beans.property.SimpleDoubleProperty(param.getValue().getPrix()).asObject());
        colQuantite.setCellValueFactory(param -> new javafx.beans.property.SimpleIntegerProperty(param.getValue().getQuantiteStock()).asObject());
        colCategorie.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getCategorie()));
        colDisponible.setCellValueFactory(param -> new javafx.beans.property.SimpleBooleanProperty(param.getValue().isDisponible()).asObject());
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterData());
        categorieFilter.setOnAction(e -> filterData());
        statusFilter.setOnAction(e -> filterData());
    }

    private void loadStatistics() {
        int total = allProduits.size();
        double valeurTotale = allProduits.stream()
                .mapToDouble(p -> p.getPrix() * p.getQuantiteStock())
                .sum();
        
        Produit plusCher = allProduits.stream()
                .max((p1, p2) -> Double.compare(p1.getPrix(), p2.getPrix()))
                .orElse(null);

        totalProduitsLabel.setText(String.valueOf(total));
        valeurTotaleLabel.setText(String.format("%.2f€", valeurTotale));
        produitPlusChierLabel.setText(plusCher != null ? plusCher.getNom() : "N/A");
    }

    private void filterData() {
        String searchTerm = searchField.getText().toLowerCase();
        String categorie = categorieFilter.getValue();
        String status = statusFilter.getValue();

        var filtered = allProduits.stream()
                .filter(p -> p.getNom().toLowerCase().contains(searchTerm) || 
                           p.getDescription().toLowerCase().contains(searchTerm))
                .filter(p -> categorie == null || p.getCategorie().equals(categorie))
                .filter(p -> status == null || (status.equals("Disponible") && p.isDisponible()) || 
                           (status.equals("Indisponible") && !p.isDisponible()))
                .toList();

        productTable.getItems().clear();
        productTable.getItems().addAll(filtered);
    }

    @FXML
    private void openAddProductModal() {
        System.out.println("Ouvrir modal d'ajout produit");
        // TODO: Implémenter l'ajout de produit
    }
}


