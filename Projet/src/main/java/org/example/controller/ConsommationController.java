package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.Service;
import org.example.model.Stock;
import org.example.model.StockMovement;
import org.example.service.ServiceService;
import org.example.service.StockService;
import org.example.service.ServiceConsommationService;
import org.example.util.NotificationUtil;

import java.time.LocalDateTime;
import java.util.List;

public class ConsommationController {
    @FXML private ComboBox<Service> serviceCombo;
    @FXML private ComboBox<Stock> stockCombo;
    @FXML private Spinner<Integer> quantiteSpinner;
    @FXML private Label avertissementLabel;
    @FXML private TableView<StockMovement> historiqueTable;
    @FXML private TableColumn<StockMovement, LocalDateTime> colDate;
    @FXML private TableColumn<StockMovement, String> colService;
    @FXML private TableColumn<StockMovement, String> colProduit;
    @FXML private TableColumn<StockMovement, Integer> colQuantite;
    @FXML private TableColumn<StockMovement, String> colMotif;
    @FXML private TableColumn<StockMovement, String> colDepot;

    private final ServiceService serviceService = ServiceService.getInstance();
    private final StockService stockService = StockService.getInstance();
    private final ServiceConsommationService consommationService = ServiceConsommationService.getInstance();
    private ObservableList<StockMovement> mouvements = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1);
        quantiteSpinner.setValueFactory(valueFactory);
        chargerServices();
        chargerStocks();
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colService.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getService() != null ?
                cellData.getValue().getService().getNom() : "N/A"
            )
        );
        colProduit.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStock() != null && cellData.getValue().getStock().getProduit() != null
                    ? cellData.getValue().getStock().getProduit().getNom()
                    : "N/A"
            )
        );
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colDepot.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDepot() != null ?
                cellData.getValue().getDepot().getNom() : "N/A"
            )
        );
        historiqueTable.setItems(mouvements);
        stockCombo.setOnAction(e -> verifierDisponibilite());
        quantiteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> verifierDisponibilite());
        rafraichirHistorique();
    }

    private void chargerServices() {
        List<Service> services = serviceService.getAll();
        ObservableList<Service> serviceList = FXCollections.observableArrayList(services);
        serviceCombo.setItems(serviceList);
    }

    private void chargerStocks() {
        List<Stock> stocks = stockService.getAll();
        ObservableList<Stock> stockList = FXCollections.observableArrayList(stocks);
        stockCombo.setItems(stockList);
    }

    private void verifierDisponibilite() {
        Stock selectedStock = stockCombo.getValue();
        Integer quantiteDemandee = quantiteSpinner.getValue();
        if (selectedStock == null || quantiteDemandee == null) {
            avertissementLabel.setText("");
            return;
        }
        if (quantiteDemandee > selectedStock.getQuantite()) {
            avertissementLabel.setText("⚠️ Stock insuffisant!");
            avertissementLabel.setStyle("-fx-text-fill: #E74C3C;");
        } else if (quantiteDemandee > selectedStock.getSeuilMinimum()) {
            avertissementLabel.setText("⚠️ Attention: stock faible après cette opération");
            avertissementLabel.setStyle("-fx-text-fill: #F39C12;");
        } else {
            avertissementLabel.setText("✅ Stock suffisant");
            avertissementLabel.setStyle("-fx-text-fill: #27AE60;");
        }
    }

    @FXML
    private void handleConsommer() {
        Service selectedService = serviceCombo.getValue();
        Stock selectedStock = stockCombo.getValue();
        Integer quantite = quantiteSpinner.getValue();
        if (selectedService == null || selectedStock == null || quantite == null) {
            NotificationUtil.showError("Veuillez sélectionner un service et un stock");
            return;
        }
        if (quantite <= 0) {
            NotificationUtil.showError("La quantité doit être positive");
            return;
        }
        try {
            consommationService.enregistrerConsommation(
                selectedService.getId(),
                selectedStock.getId(),
                quantite,
                "Consommation manuelle",
                null
            );
            NotificationUtil.showSuccess("✅ Consommation enregistrée avec succès!");
            quantiteSpinner.getValueFactory().setValue(1);
            chargerStocks();
            verifierDisponibilite();
            rafraichirHistorique();
        } catch (RuntimeException e) {
            // Afficher le message d'erreur complet pour le debug
            StringBuilder sb = new StringBuilder();
            sb.append("❌ Erreur: ").append(e.getMessage());
            if (e.getCause() != null) {
                sb.append("\nCause: ").append(e.getCause().toString());
            }
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append("\n    at ").append(ste.toString());
            }
            NotificationUtil.showError(sb.toString());
        }
    }

    private void rafraichirHistorique() {
        mouvements.clear();
        List<StockMovement> recents = consommationService.getMouvementsRecents();
        mouvements.addAll(recents);
    }
}
