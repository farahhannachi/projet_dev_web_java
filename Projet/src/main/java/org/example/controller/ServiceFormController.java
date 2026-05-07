package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.model.Service;
import org.example.service.QRService;
import org.example.service.ServiceService;
import org.example.util.NotificationUtil;
import org.example.util.ServiceValidator;
import org.example.util.ValidationException;

import java.time.LocalDateTime;

/**
 * Contrôleur pour le formulaire d'ajout/modification de service
 * Affiche le formulaire dans le content pane (pas de nouvelle fenêtre)
 * Architecture SPA - Navigation sans Stage
 *
 * ⚠️ VALIDATION CÔTÉ SERVEUR (BACKEND) :
 * - Toutes les validations sont faites côté serveur (ServiceValidator)
 * - Pas de validation JavaScript
 * - Les erreurs sont affichées après validation serveur
 */
public class ServiceFormController {
    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField specialiteField;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private TextField adresseField;
    @FXML private Label errorLabel;
    @FXML private ImageView qrCodeView;

    private final ServiceService serviceService = ServiceService.getInstance();
    private final QRService qrService = QRService.getInstance();
    private Service serviceToEdit;
    private ServiceController parentController; // Référence au contrôleur parent

    @FXML
    public void initialize() {
        // Initialiser le ComboBox avec les types
        typeField.setItems(javafx.collections.FXCollections.observableArrayList("Médecin", "Infirmier"));
    }

    public void setServiceToEdit(Service service) {
        this.serviceToEdit = service;
        if (service != null) {
            formTitle.setText("Modifier le service");
            nomField.setText(service.getNom());
            typeField.setValue(service.getType());
            specialiteField.setText(service.getSpecialite());
            telephoneField.setText(service.getTelephone());
            emailField.setText(service.getEmail());
            adresseField.setText(service.getAdresse());
        } else {
            formTitle.setText("Ajouter un service");
        }
    }

    public void setParentController(ServiceController parentController) {
        this.parentController = parentController;
    }

    private void displayQRCode(Service service) {
        try {
            String qrPath = qrService.generateServiceQRCodeWithInfo(
                service.getId(),
                service.getNom(),
                service.getType(),
                service.getSpecialite()
            );
            Image qrImage = new Image("file:" + qrPath);
            qrCodeView.setImage(qrImage);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage du QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        // Récupérer les valeurs du formulaire
        String nom = nomField.getText();
        String type = typeField.getValue();
        String specialite = specialiteField.getText();
        String telephone = telephoneField.getText();
        String email = emailField.getText();
        String adresse = adresseField.getText();

        // ⚠️ VALIDATION CÔTÉ SERVEUR (BACKEND)
        // Créer un validateur pour vérifier tous les champs
        ServiceValidator validator = new ServiceValidator();
        if (!validator.validate(nom, type, specialite, telephone, email, adresse)) {
            // Afficher les erreurs de validation
            String errorMessage = validator.getErrorMessage();
            errorLabel.setText(errorMessage);
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            return;
        }

        // Effacer les erreurs si validation OK
        errorLabel.setText("");

        try {
            if (serviceToEdit == null) {
                // Ajout d'un nouveau service
                Service newService = new Service();
                newService.setNom(nom.trim());
                newService.setType(type);
                newService.setSpecialite(specialite.trim());
                newService.setTelephone(telephone.trim());
                newService.setEmail(email.trim());
                newService.setAdresse(adresse.trim());
                newService.setDateCreation(LocalDateTime.now());

                // Validation + insertion côté serveur
                serviceService.add(newService);
                NotificationUtil.showSuccess("✅ Service ajouté avec succès!");

                // Afficher le QR code
                displayQRCode(newService);
            } else {
                // Modification d'un service existant
                serviceToEdit.setNom(nom.trim());
                serviceToEdit.setType(type);
                serviceToEdit.setSpecialite(specialite.trim());
                serviceToEdit.setTelephone(telephone.trim());
                serviceToEdit.setEmail(email.trim());
                serviceToEdit.setAdresse(adresse.trim());

                // Validation + update côté serveur
                serviceService.update(serviceToEdit);
                NotificationUtil.showSuccess("✅ Service modifié avec succès!");

                // Afficher le QR code
                displayQRCode(serviceToEdit);
            }

            // Revenir à la liste des services
            handleCancel();
        } catch (ValidationException e) {
            // Erreur de validation côté serveur
            errorLabel.setText("❌ " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
        } catch (Exception e) {
            // Erreur d'insertion/modification
            errorLabel.setText("❌ Erreur: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-wrap-text: true;");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        // Revenir à la vue de la liste des services
        if (parentController != null) {
            parentController.showTableView();
        }
    }
}
