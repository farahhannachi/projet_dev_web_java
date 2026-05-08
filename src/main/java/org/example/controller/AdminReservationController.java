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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Reservation;
import org.example.model.Service;
import org.example.service.ReservationService;
import org.example.service.ServiceService;
import org.example.util.NotificationUtil;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminReservationController implements Initializable {
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, String> colClient;
    @FXML private TableColumn<Reservation, String> colService;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Pagination pagination;
    @FXML private VBox listView;
    @FXML private Label totalReservationsLabel;

    private final ReservationService reservationService = ReservationService.getInstance();
    private final ServiceService serviceService = ServiceService.getInstance();
    private ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize columns
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNomClient()));
        colService.setCellValueFactory(cellData -> {
            Service service = serviceService.getById(cellData.getValue().getServiceId());
            return new javafx.beans.property.SimpleStringProperty(service != null ? service.getNom() : "Unknown");
        });
        colDate.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDateRendezVous().format(formatter));
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Status column with color
        colStatus.setCellFactory(column -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "CONFIRMED":
                            setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                            break;
                        case "REJECTED":
                            setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                            break;
                        case "PENDING":
                            setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        // Actions column
        colActions.setCellFactory(param -> new TableCell<Reservation, Void>() {
            private final Button confirmBtn = new Button("✓");
            private final Button rejectBtn = new Button("✗");
            private final HBox pane = new HBox(5, confirmBtn, rejectBtn);

            {
                confirmBtn.getStyleClass().add("btn-table-confirm");
                rejectBtn.getStyleClass().add("btn-table-reject");
                confirmBtn.setOnAction(e -> confirmReservation(getTableView().getItems().get(getIndex())));
                rejectBtn.setOnAction(e -> rejectReservation(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    // Only show buttons for PENDING reservations
                    if ("PENDING".equals(reservation.getStatut())) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Search and filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Status filter
        statusFilter.setItems(FXCollections.observableArrayList("", "PENDING", "CONFIRMED", "REJECTED"));
        statusFilter.setValue("");

        // Load data
        refreshTable();
    }

    @FXML
    private void openAddReservationModal() {
        showFormView(null);
    }

    private void showFormView(Reservation reservationToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationFormAdmin.fxml"));
            Parent formView = loader.load();
            ReservationFormAdminController formController = loader.getController();
            formController.setReservationToEdit(reservationToEdit);
            formController.setParentController(this);

            // Show in modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(reservationToEdit == null ? "Add Reservation" : "Edit Reservation");
            stage.setScene(new Scene(formView));
            stage.showAndWait();

        } catch (IOException e) {
            NotificationUtil.showError("Error opening form: " + e.getMessage());
        }
    }

    private void confirmReservation(Reservation reservation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Reservation");
        alert.setHeaderText("Confirm reservation #" + reservation.getId());
        alert.setContentText("Are you sure you want to confirm this reservation?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (reservationService.updateStatus(reservation.getId(), "CONFIRMED")) {
                NotificationUtil.showSuccess("Reservation confirmed successfully!");
                refreshTable();
            } else {
                NotificationUtil.showError("Failed to confirm reservation");
            }
        }
    }

    private void rejectReservation(Reservation reservation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reject Reservation");
        alert.setHeaderText("Reject reservation #" + reservation.getId());
        alert.setContentText("Are you sure you want to reject this reservation?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (reservationService.updateStatus(reservation.getId(), "REJECTED")) {
                NotificationUtil.showSuccess("Reservation rejected successfully!");
                refreshTable();
            } else {
                NotificationUtil.showError("Failed to reject reservation");
            }
        }
    }

    private void refreshTable() {
        List<Reservation> allReservations = reservationService.getAll();
        reservations.setAll(allReservations);
        applyFilters();
        totalReservationsLabel.setText(String.valueOf(allReservations.size()));
    }

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String status = statusFilter.getValue() != null ? statusFilter.getValue() : "";

        List<Reservation> filtered = reservations.stream()
                .filter(r -> (r.getNomClient() != null && r.getNomClient().toLowerCase().contains(search)) ||
                             (r.getEmailClient() != null && r.getEmailClient().toLowerCase().contains(search)))
                .filter(r -> status.isEmpty() || (r.getStatut() != null && r.getStatut().equalsIgnoreCase(status)))
                .collect(Collectors.toList());

        updatePagination(filtered);
    }

    private void updatePagination(List<Reservation> filtered) {
        int pageCount = (int) Math.ceil((double) filtered.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filtered.size());
            reservationTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
            return reservationTable;
        });
        if (filtered.isEmpty()) {
            reservationTable.setItems(FXCollections.observableArrayList());
        }
    }

    public void showTableView() {
        refreshTable();
    }
}
