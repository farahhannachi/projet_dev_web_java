package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.model.Reservation;
import org.example.model.Service;
import org.example.service.ReservationService;
import org.example.service.ServiceService;
import org.example.util.NotificationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javafx.stage.Stage;

public class ReservationFormAdminController {

    @FXML private ComboBox<Service> serviceCombo;
    @FXML private TextField nomClientField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> heureCombo;
    @FXML private TextArea motifArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private ReservationService reservationService = ReservationService.getInstance();
    private ServiceService serviceService = ServiceService.getInstance();
    private AdminReservationController parentController;
    private Reservation reservationToEdit;

    @FXML
    public void initialize() {
        // Load services
        List<Service> services = serviceService.getAll();
        serviceCombo.setItems(javafx.collections.FXCollections.observableArrayList(services));
        if (!services.isEmpty()) {
            serviceCombo.setValue(services.get(0));
        }

        // Initialize hours
        initializeHours();

        // Initialize date picker
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    public void setParentController(AdminReservationController parentController) {
        this.parentController = parentController;
    }

    public void setReservationToEdit(Reservation reservation) {
        this.reservationToEdit = reservation;
        if (reservation != null) {
            // Load existing data
            Service service = serviceService.getById(reservation.getServiceId());
            serviceCombo.setValue(service);
            nomClientField.setText(reservation.getNomClient());
            emailField.setText(reservation.getEmailClient());
            telephoneField.setText(reservation.getTelephoneClient());
            datePicker.setValue(reservation.getDateRendezVous().toLocalDate());
            String time = reservation.getDateRendezVous().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            heureCombo.setValue(time);
            motifArea.setText(reservation.getMotif());
            saveButton.setText("Update");
        }
    }

    private void initializeHours() {
        heureCombo.getItems().clear();
        for (int h = 8; h < 18; h++) {
            for (int m = 0; m < 60; m += 30) {
                String time = String.format("%02d:%02d", h, m);
                heureCombo.getItems().add(time);
            }
        }
        if (!heureCombo.getItems().isEmpty()) {
            heureCombo.setValue(heureCombo.getItems().get(0));
        }
    }

    @FXML
    private void handleSave() {
        // Validation
        String validationError = validateForm();
        if (validationError != null) {
            errorLabel.setText(validationError);
            return;
        }
        errorLabel.setText("");

        try {
            // Create or update reservation
            LocalDate date = datePicker.getValue();
            String[] timeParts = heureCombo.getValue().split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            LocalDateTime dateRendezVous = LocalDateTime.of(date, time);

            Reservation reservation;
            if (reservationToEdit != null) {
                reservation = reservationToEdit;
                reservation.setServiceId(serviceCombo.getValue().getId());
                reservation.setNomClient(nomClientField.getText().trim());
                reservation.setEmailClient(emailField.getText().trim());
                reservation.setTelephoneClient(telephoneField.getText().trim());
                reservation.setDateRendezVous(dateRendezVous);
                reservation.setMotif(motifArea.getText().trim());
                // For edit, we might not change status, but for new, set to CONFIRMED or PENDING
                if (reservation.getStatut() == null) {
                    reservation.setStatut("CONFIRMED"); // Admin created, assume confirmed
                }
                if (reservationService.update(reservation)) {
                    NotificationUtil.showSuccess("Reservation updated successfully!");
                } else {
                    errorLabel.setText("Failed to update reservation");
                    return;
                }
            } else {
                reservation = new Reservation(
                    serviceCombo.getValue().getId(),
                    nomClientField.getText().trim(),
                    emailField.getText().trim(),
                    telephoneField.getText().trim(),
                    LocalDateTime.now(),
                    motifArea.getText().trim()
                );
                reservation.setDateRendezVous(dateRendezVous);
                reservation.setStatut("CONFIRMED"); // Admin created reservations are confirmed by default

                if (reservationService.add(reservation)) {
                    NotificationUtil.showSuccess("Reservation created successfully!");
                } else {
                    errorLabel.setText("Failed to create reservation");
                    return;
                }
            }

            // Close modal and refresh parent
            if (parentController != null) {
                parentController.showTableView();
            }
            closeModal();

        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private String validateForm() {
        if (serviceCombo.getValue() == null) {
            return "Service is required";
        }
        if (nomClientField.getText().trim().isEmpty()) {
            return "Client name is required";
        }
        if (emailField.getText().trim().isEmpty()) {
            return "Email is required";
        }
        if (!emailField.getText().contains("@")) {
            return "Email is not valid";
        }
        if (telephoneField.getText().trim().isEmpty()) {
            return "Phone is required";
        }
        if (datePicker.getValue() == null) {
            return "Date is required";
        }
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            return "Date must be in the future";
        }
        if (heureCombo.getValue() == null) {
            return "Time is required";
        }
        if (motifArea.getText().trim().isEmpty()) {
            return "Motif is required";
        }
        return null;
    }
}
