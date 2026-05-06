package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.model.Service;
import org.example.service.QRService;
import org.example.service.ServiceService;

/**
 * 🎯 Exemple d'intégration QRService dans un contrôleur JavaFX
 * Montre comment générer et afficher un QR code pour un service
 */
public class ServiceQRDisplayController {

    @FXML
    private ImageView qrCodeView;

    @FXML
    private Label serviceNameLabel;

    @FXML
    private Label qrStatusLabel;

    private QRService qrService = QRService.getInstance();
    private ServiceService serviceService = ServiceService.getInstance();

    /**
     * 🔧 Afficher le QR code d'un service
     * Appeler cette méthode après sélectionner un service
     *
     * @param service Service dont afficher le QR code
     */
    public void displayServiceQRCode(Service service) {
        if (service == null) {
            qrStatusLabel.setText("❌ Service invalide");
            return;
        }

        // Mettre à jour l'affichage du nom
        serviceNameLabel.setText("Service: " + service.getNom());

        try {
            // 🎨 OPTION 1: QR code simple avec ID
            String qrFilePath = qrService.generateServiceQRCode(
                service.getId(),
                "Service#" + service.getId()
            );

            // 🎨 OPTION 2: QR code avec informations (décommentez pour utiliser)
            // String qrFilePath = qrService.generateServiceQRCodeWithInfo(
            //     service.getId(),
            //     service.getNom(),
            //     service.getType(),
            //     service.getSpecialite()
            // );

            // 🎨 OPTION 3: QR code avec lien (décommentez pour utiliser)
            // String qrFilePath = qrService.generateServiceQRCodeWithLink(
            //     service.getId(),
            //     "http://localhost:8080"
            // );

            if (qrFilePath != null) {
                // Charger l'image PNG
                Image qrImage = qrService.loadQRCodeImage(qrFilePath);

                if (qrImage != null) {
                    qrCodeView.setImage(qrImage);
                    qrStatusLabel.setText("✅ QR code généré avec succès!");
                    System.out.println("✓ QR code affiché pour service: " + service.getNom());
                } else {
                    qrStatusLabel.setText("❌ Impossible de charger l'image");
                }
            } else {
                qrStatusLabel.setText("❌ Erreur lors de la génération du QR code");
            }

        } catch (Exception e) {
            qrStatusLabel.setText("❌ Erreur: " + e.getMessage());
            System.err.println("Erreur lors de l'affichage du QR code: " + e.getMessage());
        }
    }

    /**
     * 🔧 Générer QR codes pour tous les services
     * Utile pour une première initialisation
     */
    public void generateQRCodesForAllServices() {
        new Thread(() -> {
            try {
                var services = serviceService.getAll();
                System.out.println("🔄 Génération des QR codes pour " + services.size() + " services...");

                for (Service service : services) {
                    qrService.generateServiceQRCodeWithInfo(
                        service.getId(),
                        service.getNom(),
                        service.getType(),
                        service.getSpecialite()
                    );
                }

                System.out.println("✅ Tous les QR codes ont été générés!");
                qrStatusLabel.setText("✅ " + services.size() + " QR codes générés");

            } catch (Exception e) {
                System.err.println("❌ Erreur: " + e.getMessage());
                qrStatusLabel.setText("❌ Erreur lors de la génération");
            }
        }).start(); // Exécuter dans un thread séparé pour ne pas bloquer l'UI
    }

    /**
     * 🔧 Supprimer le QR code d'un service
     */
    public void deleteServiceQRCode(int serviceId) {
        boolean deleted = qrService.deleteServiceQRCode(serviceId);
        if (deleted) {
            qrStatusLabel.setText("✅ QR code supprimé");
        } else {
            qrStatusLabel.setText("❌ Erreur lors de la suppression");
        }
    }
}

