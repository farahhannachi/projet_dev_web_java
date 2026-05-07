package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.model.Commande;
import org.example.model.LigneCommande;
import org.example.service.CommandeService;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur pour la gestion des Commandes
 */
public class CommandeController {
    @FXML private VBox listView;
    @FXML private Label totalCommandesLabel;
    @FXML private Label montantTotalLabel;
    @FXML private Label commandesAttentLabel;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker dateFilter;
    
    @FXML private TableView<Commande> commandeTable;
    @FXML private TableColumn<Commande, Integer> colId;
    @FXML private TableColumn<Commande, String> colClient;
    @FXML private TableColumn<Commande, LocalDate> colDate;
    @FXML private TableColumn<Commande, Integer> colArticles;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, String> colDepot;
    @FXML private TableColumn<Commande, Void> colActions;
    
    @FXML private Pagination pagination;
    
    private CommandeService commandeService = new CommandeService();
    private List<Commande> allCommandes;

    @FXML
    public void initialize() {
        loadData();
        setupTableColumns();
        setupListeners();
        loadStatistics();
    }

    private void loadData() {
        allCommandes = commandeService.getAll();
        commandeTable.getItems().addAll(allCommandes);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(param -> new javafx.beans.property.SimpleIntegerProperty(param.getValue().getId()).asObject());
        colClient.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue().getClient() != null ? param.getValue().getClient().getNom() + " " + param.getValue().getClient().getPrenom() : "N/A"));
        colDate.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getDateCommande()));
        colArticles.setCellValueFactory(param -> new javafx.beans.property.SimpleIntegerProperty(param.getValue().getNombreArticles()).asObject());
        colTotal.setCellValueFactory(param -> new javafx.beans.property.SimpleDoubleProperty(param.getValue().getTotal()).asObject());
        colStatut.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getStatut()));
        colDepot.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue().getDepot() != null ? param.getValue().getDepot().getNom() : "N/A"));
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterData());
        statusFilter.setOnAction(e -> filterData());
        dateFilter.setOnAction(e -> filterData());
    }

    private void loadStatistics() {
        int total = allCommandes.size();
        double montantTotal = allCommandes.stream()
                .mapToDouble(Commande::getTotal)
                .sum();
        long enAttente = allCommandes.stream()
                .filter(c -> "En attente".equals(c.getStatut()))
                .count();

        totalCommandesLabel.setText(String.valueOf(total));
        montantTotalLabel.setText(String.format("%.2f€", montantTotal));
        commandesAttentLabel.setText(String.valueOf(enAttente));
    }

    private void filterData() {
        String searchTerm = searchField.getText().toLowerCase();
        String statut = statusFilter.getValue();
        LocalDate date = dateFilter.getValue();

        var filtered = allCommandes.stream()
                .filter(c -> String.valueOf(c.getId()).contains(searchTerm) || 
                           (c.getClient() != null && (c.getClient().getNom().toLowerCase().contains(searchTerm) || 
                                                      c.getClient().getPrenom().toLowerCase().contains(searchTerm))))
                .filter(c -> statut == null || c.getStatut().equals(statut))
                .filter(c -> date == null || c.getDateCommande().equals(date))
                .toList();

        commandeTable.getItems().clear();
        commandeTable.getItems().addAll(filtered);
    }

    @FXML
    private void openAddCommandeModal() {
        System.out.println("Ouvrir modal d'ajout commande");
        // TODO: Implémenter l'ajout de commande
    }
}


