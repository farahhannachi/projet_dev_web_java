package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.model.Stock;
import org.example.model.Depot;
import org.example.service.StockService;
import org.example.service.DepotService;
import org.example.util.NotificationUtil;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des Stocks
 * Architecture SPA - pas de nouvelles fenêtres, formulaire dans le même espace
 * Copie fonctionnelle du DepotController adapté pour les Stocks
 */
public class StockController implements Initializable {
    @FXML private TableView<Stock> stockTable;
    @FXML private TableColumn<Stock, String> colProduit;
    @FXML private TableColumn<Stock, Integer> colQuantite;
    @FXML private TableColumn<Stock, Integer> colSeuilMinimum;
    @FXML private TableColumn<Stock, String> colDepot;
    @FXML private TableColumn<Stock, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> depotFilter;
    @FXML private Pagination pagination;
    @FXML private VBox listView;
    @FXML private Label totalQuantiteLabel;
    @FXML private Label stockFaibleLabel;
    @FXML private Label stockCritiqueLabel;

    private final StockService stockService = StockService.getInstance();
    private final DepotService depotService = DepotService.getInstance();
    private ObservableList<Stock> stocks = FXCollections.observableArrayList();
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[DEBUG] StockController.initialize() - START");
        // Initialisation des colonnes avec les propriétés des objets liés
        colProduit.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getProduit() != null ?
                cellData.getValue().getProduit().getNom() : "N/A"));

        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantiteDisponible"));
        colSeuilMinimum.setCellValueFactory(new PropertyValueFactory<>("seuilMinimum"));

        colDepot.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDepot() != null ?
                cellData.getValue().getDepot().getNom() : "N/A"));

        // Actions CRUD dans la table
        colActions.setCellFactory(param -> new TableCell<Stock, Void>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                editBtn.setOnAction(e -> openEditStockModal(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDeleteStock(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(pane);
            }
        });

        // Recherche dynamique
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        depotFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Charger les dépôts depuis la base de données
        try {
            List<Depot> depots = depotService.getAll();
            ObservableList<String> depotNames = FXCollections.observableArrayList("");
            for (Depot depot : depots) {
                depotNames.add(depot.getNom());
            }
            depotFilter.setItems(depotNames);
            depotFilter.setValue("");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des dépôts: " + e.getMessage());
            depotFilter.setItems(FXCollections.observableArrayList(""));
        }

        // Chargement initial
        try {
            System.out.println("[DEBUG] StockController - Calling refreshTable()");
            refreshTable();
            System.out.println("[DEBUG] StockController - Calling loadStats()");
            loadStats();
            System.out.println("[DEBUG] StockController.initialize() - SUCCESS");
        } catch (Exception e) {
            System.err.println("[ERROR] StockController.initialize() FAILED: " + e.getMessage());
            e.printStackTrace();
            NotificationUtil.showError("Erreur lors du chargement des stocks: " + e.getMessage());
        }
    }

    @FXML
    public void openAddStockModal() {
        showFormView(null);
    }

    public void openEditStockModal(Stock stock) {
        showFormView(stock);
    }

    /**
     * Afficher le formulaire dans le content pane (pas de nouvelle fenêtre)
     * Architecture SPA - le formulaire remplace la liste dans le même espace
     */
    private void showFormView(Stock stockToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StockForm.fxml"));
            Parent formView = loader.load();
            StockFormController formController = loader.getController();
            formController.setStockToEdit(stockToEdit);
            formController.setParentController(this);

            // Afficher le formulaire dans le content pane
            showForm(formView);
        } catch (IOException e) {
            NotificationUtil.showError("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        }
    }

    /**
     * Afficher le formulaire dans le content pane (remplace le tableau)
     */
    private void showForm(Parent formView) {
        if (listView != null && listView.getParent() instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) listView.getParent();
            stackPane.getChildren().clear();
            stackPane.getChildren().add(formView);
        }
    }

    /**
     * Revenir à la vue tableau des stocks
     */
    public void showTableView() {
        refreshTable();
        // Restaurer le listView dans le StackPane
        if (listView != null && listView.getParent() instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) listView.getParent();
            stackPane.getChildren().clear();
            stackPane.getChildren().add(listView);
        }
    }

    private void confirmDeleteStock(Stock stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le stock");
        alert.setContentText("Voulez-vous vraiment supprimer ce stock ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            stockService.delete(stock.getId());
            NotificationUtil.showSuccess("Stock supprimé avec succès.");
            refreshTable();
        }
    }

    private void refreshTable() {
        List<Stock> allStocks = stockService.getAll();
        stocks.setAll(allStocks);
        applyFilters();
        loadStats();
    }

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String depot = depotFilter.getValue() != null ? depotFilter.getValue() : "";
        List<Stock> filtered = stocks.stream()
                .filter(s -> (s.getProduit() != null && s.getProduit().getNom().toLowerCase().contains(search)))
                .filter(s -> depot.isEmpty() || (s.getDepot() != null && s.getDepot().getNom().equalsIgnoreCase(depot)))
                .collect(Collectors.toList());
        updatePagination(filtered);
    }

    private void updatePagination(List<Stock> filtered) {
        int pageCount = (int) Math.ceil((double) filtered.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filtered.size());
            stockTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
            return stockTable;
        });
        if (filtered.isEmpty()) stockTable.setItems(FXCollections.observableArrayList());
    }

    private void loadStats() {
        int totalQuantite = stockService.getAll().stream().mapToInt(Stock::getQuantiteDisponible).sum();
        int stockFaible = stockService.getStocksFaibles().size();
        int stockCritique = stockService.getStocksCritiques().size();

        totalQuantiteLabel.setText(String.valueOf(totalQuantite));
        stockFaibleLabel.setText(String.valueOf(stockFaible));
        stockCritiqueLabel.setText(String.valueOf(stockCritique));
    }
}
