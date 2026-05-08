package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.example.model.Service;
import org.example.service.ServiceService;
import org.example.util.NotificationUtil;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ServiceController implements Initializable {
    @FXML private TableView<Service> serviceTable;
    @FXML private TableColumn<Service, String> colNom;
    @FXML private TableColumn<Service, String> colType;
    @FXML private TableColumn<Service, String> colSpecialite;
    @FXML private TableColumn<Service, String> colTelephone;
    @FXML private TableColumn<Service, String> colEmail;
    @FXML private TableColumn<Service, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Pagination pagination;
    @FXML private VBox listView; // La vue tableau
    @FXML private Label totalServicesLabel;
    @FXML private Label consommationTotaleLabel;
    @FXML private Label servicePlusActifLabel;

    private final ServiceService serviceService = ServiceService.getInstance();
    private ObservableList<Service> services = FXCollections.observableArrayList();
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Initialisation des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Actions CRUD dans la table
        colActions.setCellFactory(param -> new TableCell<Service, Void>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                editBtn.setOnAction(e -> openEditServiceModal(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDeleteService(getTableView().getItems().get(getIndex())));
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
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Initialisation des types
        typeFilter.setItems(FXCollections.observableArrayList("", "Médecin", "Infirmier"));
        typeFilter.setValue("");

        // Chargement initial
        try {
            refreshTable();
            loadStats();
        } catch (Exception e) {
            NotificationUtil.showError("Erreur lors du chargement des services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void openAddServiceModal() {
        showFormView(null);
    }

    public void openEditServiceModal(Service service) {
        showFormView(service);
    }

    /**
     * Afficher le formulaire dans le content pane (pas de nouvelle fenêtre)
     * Architecture SPA - le formulaire remplace la liste dans le même espace
     */
    private void showFormView(Service serviceToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServiceForm.fxml"));
            Parent formView = loader.load();
            ServiceFormController formController = loader.getController();
            formController.setServiceToEdit(serviceToEdit);
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
     * Revenir à la vue tableau des services
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

    private void confirmDeleteService(Service service) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le service");
        alert.setContentText("Voulez-vous vraiment supprimer le service '" + service.getNom() + "' ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceService.delete(service.getId());
            NotificationUtil.showSuccess("Service supprimé avec succès.");
            refreshTable();
        }
    }

    private void refreshTable() {
        List<Service> allServices = serviceService.getAll();
        services.setAll(allServices);
        applyFilters();
        loadStats();
    }

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String type = typeFilter.getValue() != null ? typeFilter.getValue() : "";
        List<Service> filtered = services.stream()
                .filter(s -> (s.getNom() != null && s.getNom().toLowerCase().contains(search)) ||
                             (s.getSpecialite() != null && s.getSpecialite().toLowerCase().contains(search)))
                .filter(s -> type.isEmpty() || (s.getType() != null && s.getType().equalsIgnoreCase(type)))
                .collect(Collectors.toList());
        updatePagination(filtered);
    }

    private void updatePagination(List<Service> filtered) {
        int pageCount = (int) Math.ceil((double) filtered.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filtered.size());
            serviceTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
            return serviceTable;
        });
        if (filtered.isEmpty()) serviceTable.setItems(FXCollections.observableArrayList());
    }

    private void loadStats() {
        List<Service> allServices = serviceService.getAll();
        totalServicesLabel.setText(String.valueOf(allServices.size()));
        double consommationTotale = allServices.stream().mapToDouble(Service::getConsommation).sum();
        consommationTotaleLabel.setText(String.valueOf(consommationTotale));
        Service servicePlusActif = allServices.stream().max((s1, s2) -> Double.compare(s1.getConsommation(), s2.getConsommation())).orElse(null);
        servicePlusActifLabel.setText(servicePlusActif != null ? servicePlusActif.getNom() : "N/A");
    }

    private double parseDoubleSafe(String value) {
        try { return Double.parseDouble(value); } catch (Exception e) { return 0.0; }
    }
}
