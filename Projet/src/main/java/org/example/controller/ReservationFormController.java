package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.model.Reservation;
import org.example.model.Service;
import org.example.service.ReservationService;
import org.example.util.NotificationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationFormController {

    @FXML private Label serviceName;
    @FXML private Label serviceType;
    @FXML private Label serviceSpecialite;
    @FXML private TextField nomClientField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> heureCombo;
    @FXML private TextArea motifArea;
    @FXML private Label errorLabel;
    @FXML private VBox formContainer;

    private ReservationService reservationService = ReservationService.getInstance();
    private Service selectedService;
    private Runnable onReservationComplete;

    public void setService(Service service, Runnable onComplete) {
        this.selectedService = service;
        this.onReservationComplete = onComplete;

        // Afficher les infos du service
        serviceName.setText(service.getNom());
        serviceType.setText(service.getType());
        serviceSpecialite.setText("Spécialité: " + service.getSpecialite());

        // Initialiser les heures
        initializeHours();

        // Initialiser la date min à aujourd'hui
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
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
    private void handleReserve() {
        // Validation
        String validationError = validateForm();
        if (validationError != null) {
            errorLabel.setText(validationError);
            return;
        }
        errorLabel.setText("");

        try {
            // Créer la réservation
            LocalDate date = datePicker.getValue();
            String[] timeParts = heureCombo.getValue().split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            LocalDateTime dateRendezVous = LocalDateTime.of(date, time);

            Reservation reservation = new Reservation(
                selectedService.getId(),
                nomClientField.getText().trim(),
                emailField.getText().trim(),
                telephoneField.getText().trim(),
                LocalDateTime.now(),
                motifArea.getText().trim()
            );
            reservation.setDateRendezVous(dateRendezVous);

            // Enregistrer en base
            if (reservationService.add(reservation)) {
                NotificationUtil.showSuccess("✓ Réservation confirmée!\n\nUn email de notification a été envoyé à l'administrateur.");
                clearForm();
                if (onReservationComplete != null) {
                    onReservationComplete.run();
                }
            } else {
                errorLabel.setText("Erreur lors de l'enregistrement de la réservation");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
        if (formContainer != null && formContainer.getParent() != null) {
            formContainer.getParent().getChildrenUnmodifiable();
        }
    }

    private String validateForm() {
        if (nomClientField.getText().trim().isEmpty()) {
            return "Le nom complet est requis";
        }
        if (emailField.getText().trim().isEmpty()) {
            return "L'email est requis";
        }
        if (!emailField.getText().contains("@")) {
            return "L'email n'est pas valide";
        }
        if (telephoneField.getText().trim().isEmpty()) {
            return "Le téléphone est requis";
        }
        if (datePicker.getValue() == null) {
            return "La date du rendez-vous est requise";
        }
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            return "La date doit être dans le futur";
        }
        if (heureCombo.getValue() == null) {
            return "L'heure du rendez-vous est requise";
        }
        if (motifArea.getText().trim().isEmpty()) {
            return "Le motif de la consultation est requis";
        }
        return null;
    }

    private void clearForm() {
        nomClientField.clear();
        emailField.clear();
        telephoneField.clear();
        datePicker.setValue(null);
        motifArea.clear();
        errorLabel.setText("");
    }
}
