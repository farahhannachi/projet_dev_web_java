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

/**
 * Contrôleur pour la consommation de stock par les services
 * Permet à un service (médecin/infirmier) de consommer du stock
 */
public class ServiceConsommationController {
    @FXML private ComboBox<Service> serviceCombo;
    @FXML private ComboBox<Stock> stockCombo;
    @FXML private Spinner<Integer> quantiteSpinner;
    @FXML private TextField motifField;
    @FXML private TextField referenceDocumentField;
    @FXML private Label stockDisponibleLabel;
    @FXML private Label avertissementLabel;
    @FXML private Button consommerButton;
    @FXML private Button annulerButton;

    // Tableau d'historique
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
        // Initialiser le spinner
        historiqueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SpinnerValueFactory<Integer> valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1);
        quantiteSpinner.setValueFactory(valueFactory);

        // Initialiser les combos
        chargerServices();
        chargerStocks();

        // Initialiser le tableau
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

        // Listener pour mettre à jour le stock disponible
        stockCombo.setOnAction(e -> mettreAJourStockDisponible());
        quantiteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> verifierDisponibilite());
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

    private void mettreAJourStockDisponible() {
        Stock selectedStock = stockCombo.getValue();
        if (selectedStock != null) {
            stockDisponibleLabel.setText("Disponible: " + selectedStock.getQuantite() + " unités");
            verifierDisponibilite();
        } else {
            stockDisponibleLabel.setText("Sélectionnez un stock");
        }
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
            consommerButton.setDisable(true);
        } else if (quantiteDemandee > selectedStock.getSeuilMinimum()) {
            avertissementLabel.setText("⚠️ Attention: stock faible après cette opération");
            avertissementLabel.setStyle("-fx-text-fill: #F39C12;");
            consommerButton.setDisable(false);
        } else {
            avertissementLabel.setText("✅ Stock suffisant");
            avertissementLabel.setStyle("-fx-text-fill: #27AE60;");
            consommerButton.setDisable(false);
        }
    }

    @FXML
    private void handleConsommer() {
        Service selectedService = serviceCombo.getValue();
        Stock selectedStock = stockCombo.getValue();
        Integer quantite = quantiteSpinner.getValue();
        String motif = motifField.getText().trim();
        String referenceDocument = referenceDocumentField.getText().trim();

        // Validations
        if (selectedService == null || selectedStock == null || quantite == null) {
            NotificationUtil.showError("Veuillez sélectionner un service et un stock");
            return;
        }

        if (quantite <= 0) {
            NotificationUtil.showError("La quantité doit être positive");
            return;
        }

        if (motif.isEmpty()) {
            NotificationUtil.showError("Veuillez indiquer le motif de la consommation");
            return;
        }

        try {
            // Enregistrer la consommation
            consommationService.enregistrerConsommation(
                selectedService.getId(),
                selectedStock.getId(),
                quantite,
                motif,
                referenceDocument.isEmpty() ? null : referenceDocument
            );

            NotificationUtil.showSuccess("✅ Consommation enregistrée avec succès!");

            // Réinitialiser le formulaire
            motifField.clear();
            referenceDocumentField.clear();
            quantiteSpinner.getValueFactory().setValue(1);
            chargerStocks(); // Recharger pour avoir les quantités à jour
            mettreAJourStockDisponible();
            rafraichirHistorique();

        } catch (RuntimeException e) {
            NotificationUtil.showError("❌ Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        motifField.clear();
        referenceDocumentField.clear();
        quantiteSpinner.getValueFactory().setValue(1);
        avertissementLabel.setText("");
    }

    private void rafraichirHistorique() {
        mouvements.clear();
        List<StockMovement> recents = consommationService.getMouvementsRecents();
        mouvements.addAll(recents);
    }

    public void chargerHistoriqueService(int idService) {
        mouvements.clear();
        List<StockMovement> historique = consommationService.getHistoriqueService(idService);
        mouvements.addAll(historique);
    }

    public void chargerHistoriqueStock(int idStock) {
        mouvements.clear();
        List<StockMovement> historique = consommationService.getHistoriqueStock(idStock);
        mouvements.addAll(historique);
    }
}
