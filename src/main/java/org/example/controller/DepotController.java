package org.example.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Depot;
import org.example.service.DepotService;
import org.example.util.NotificationUtil;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DepotController implements Initializable {
    @FXML private TableView<Depot> depotTable;
    @FXML private TableColumn<Depot, String> colNom;
    @FXML private TableColumn<Depot, String> colAdresse;
    @FXML private TableColumn<Depot, String> colVille;
    @FXML private TableColumn<Depot, String> colLocation;
    @FXML private TableColumn<Depot, Integer> colCapacite;
    @FXML private TableColumn<Depot, String> colResponsable;
    @FXML private TableColumn<Depot, String> colTelephone;
    @FXML private TableColumn<Depot, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> villeFilter;
    @FXML private Pagination pagination;
    @FXML private VBox listView; // La vue tableau
    @FXML private Label totalDepotsLabel;
    @FXML private Label capaciteTotaleLabel;
    @FXML private Label depotPlusChargeLabel;

    private final DepotService depotService = DepotService.getInstance();
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[DEBUG] DepotController.initialize() - START");
        // Initialisation des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("locationName"));
        colLocation.setCellFactory(column -> new TableCell<Depot, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("Non défini");
                    setStyle("-fx-text-fill: gray;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: blue; -fx-underline: true; -fx-cursor: hand;");
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            showLocationMap(getTableView().getItems().get(getIndex()));
                        }
                    });
                }
            }
        });
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capaciteDepot"));
        colResponsable.setCellValueFactory(new PropertyValueFactory<>("responsableDepot"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("responsableTelephone"));

        // Actions CRUD dans la table
        colActions.setCellFactory(param -> new TableCell<Depot, Void>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                editBtn.setOnAction(e -> openEditDepotModal(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDeleteDepot(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(pane);
            }
        });

        // Recherche dynamique - TOUJOURS requêter la DB fraîchement
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTable());
        villeFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTable());

        // Initialisation des villes (exemple)
        villeFilter.setItems(FXCollections.observableArrayList("", "Tunis", "Sfax", "Sousse"));
        villeFilter.setValue("");

        // Chargement initial
        System.out.println("[DEBUG] DepotController - Calling refreshTable()");
        try {
            refreshTable();
            System.out.println("[DEBUG] DepotController - Calling loadStats()");
            loadStats();
            System.out.println("[DEBUG] DepotController.initialize() - SUCCESS");
        } catch (Exception e) {
            System.err.println("[ERROR] DepotController.initialize() FAILED: " + e.getMessage());
            e.printStackTrace();
            NotificationUtil.showError("Erreur lors du chargement des dépôts: " + e.getMessage());
        }
    }

    @FXML
    public void openAddDepotModal() {
        showFormView(null);
    }

    public void openEditDepotModal(Depot depot) {
        showFormView(depot);
    }

    /**
     * Afficher le formulaire dans le content pane (pas de nouvelle fenêtre)
     * Architecture SPA - le formulaire remplace la liste dans le même espace
     */
    private void showFormView(Depot depotToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DepotForm.fxml"));
            Parent formView = loader.load();
            DepotFormController formController = loader.getController();
            formController.setDepotToEdit(depotToEdit);
            formController.setParentController(this); // Passer la référence du parent

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
        // Le parent du listView est le StackPane
        // On remplace le contenu du StackPane par le formulaire
        if (listView != null && listView.getParent() instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) listView.getParent();
            stackPane.getChildren().clear();
            stackPane.getChildren().add(formView);
        }
    }

    /**
     * Revenir à la vue tableau des dépôts
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

    private void confirmDeleteDepot(Depot depot) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le dépôt");
        alert.setContentText("Voulez-vous vraiment supprimer le dépôt '" + depot.getNom() + "' ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                depotService.delete(depot.getId());
                NotificationUtil.showSuccess("Dépôt supprimé avec succès.");
                refreshTable();
            } catch (RuntimeException e) {
                NotificationUtil.showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    /**
     * Rafraîchit la table en rechargeant TOUTES les données depuis la base de données
     * Aucune donnée en cache n'est conservée - chaque appel fait une requête SQL fraîche
     */
    private void refreshTable() {
        // Récupération FRAÎCHE depuis la base de données - pas de cache
        List<Depot> allDepots = depotService.getAll();
        applyFilters(allDepots);
        loadStats();
    }

    /**
     * Applique les filtres sur une liste de dépôts fournie (requête fraîche depuis DB)
     * @param allDepots Liste fraîche de dépôts depuis la base de données
     */
    private void applyFilters(List<Depot> allDepots) {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String ville = villeFilter.getValue() != null ? villeFilter.getValue() : "";
        List<Depot> filtered = allDepots.stream()
                .filter(d -> (d.getNom() != null && d.getNom().toLowerCase().contains(search)) ||
                             (d.getAdresse() != null && d.getAdresse().toLowerCase().contains(search)))
                .filter(d -> ville.isEmpty() || (d.getVille() != null && d.getVille().equalsIgnoreCase(ville)))
                .collect(Collectors.toList());
        updatePagination(filtered);
    }

    private void updatePagination(List<Depot> filtered) {
        int pageCount = (int) Math.ceil((double) filtered.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filtered.size());
            depotTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
            return depotTable;
        });
        if (filtered.isEmpty()) depotTable.setItems(FXCollections.observableArrayList());
    }

    private void loadStats() {
        // TOUJOURS requêter la base de données pour des stats à jour
        List<Depot> allDepots = depotService.getAll();
        totalDepotsLabel.setText(String.valueOf(allDepots.size()));
        int capaciteTotale = allDepots.stream().mapToInt(Depot::getCapaciteDepot).sum();
        capaciteTotaleLabel.setText(String.valueOf(capaciteTotale));
        Depot depotPlusCharge = allDepots.stream().max((d1, d2) -> Integer.compare(d1.getCapaciteDepot(), d2.getCapaciteDepot())).orElse(null);
        depotPlusChargeLabel.setText(depotPlusCharge != null ? depotPlusCharge.getNom() : "N/A");
    }

    private double parseDoubleSafe(String value) {
        try { return Double.parseDouble(value); } catch (Exception e) { return 0.0; }
    }

    private void showLocationMap(Depot depot) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Emplacement: " + depot.getNom());
        popupStage.setWidth(300);
        popupStage.setHeight(200);

        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("Emplacement du dépôt");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label nameLabel = new Label("Nom: " + depot.getNom());
        Label latLabel = new Label("Latitude: " + depot.getLatitude());
        Label lngLabel = new Label("Longitude: " + depot.getLongitude());

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(e -> popupStage.close());

        vbox.getChildren().addAll(titleLabel, nameLabel, latLabel, lngLabel, closeButton);

        Scene scene = new Scene(vbox);
        popupStage.setScene(scene);
        popupStage.show();
    }
}