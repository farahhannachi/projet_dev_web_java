package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.model.Service;
import org.example.service.QRService;

/**
 * 🎯 EXEMPLE SIMPLE - Contrôleur pour afficher QR code d'un service
 * Utilise QRService pour générer et afficher les QR codes
 */
public class ServiceQRController {

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private Label serviceInfoLabel;

    private QRService qrService = QRService.getInstance();

    /**
     * 🔧 Afficher le QR code d'un service
     * Appeler cette méthode quand vous voulez afficher un QR code
     *
     * @param service Le service dont afficher le QR code
     */
    public void displayServiceQRCode(Service service) {
        if (service == null) {
            serviceInfoLabel.setText("❌ Aucun service sélectionné");
            return;
        }

        try {
            // Afficher les infos du service
            serviceInfoLabel.setText(String.format("Service: %s (%s - %s)",
                service.getNom(), service.getType(), service.getSpecialite()));

            // Générer le QR code avec les informations du service
            String qrFilePath = qrService.generateServiceQRCodeWithInfo(
                service.getId(),
                service.getNom(),
                service.getType(),
                service.getSpecialite()
            );

            if (qrFilePath != null) {
                // Charger l'image PNG et l'afficher
                Image qrImage = qrService.loadQRCodeImage(qrFilePath);
                if (qrImage != null) {
                    qrCodeImageView.setImage(qrImage);
                    System.out.println("✅ QR code affiché: " + qrFilePath);
                } else {
                    serviceInfoLabel.setText("❌ Impossible de charger l'image QR");
                }
            } else {
                serviceInfoLabel.setText("❌ Erreur lors de la génération du QR code");
            }

        } catch (Exception e) {
            serviceInfoLabel.setText("❌ Erreur: " + e.getMessage());
            System.err.println("Erreur lors de l'affichage du QR code: " + e.getMessage());
        }
    }

    /**
     * 🔧 Générer QR code avec lien URL
     * Alternative: QR code contenant un lien vers le service
     */
    public void displayServiceQRCodeWithLink(Service service, String baseUrl) {
        if (service == null) return;

        try {
            // Générer QR avec lien: http://localhost:8080/service/123
            String qrFilePath = qrService.generateServiceQRCodeWithLink(
                service.getId(),
                baseUrl
            );

            if (qrFilePath != null) {
                Image qrImage = qrService.loadQRCodeImage(qrFilePath);
                qrCodeImageView.setImage(qrImage);
                serviceInfoLabel.setText("QR code avec lien généré");
            }

        } catch (Exception e) {
            System.err.println("Erreur QR avec lien: " + e.getMessage());
        }
    }

    /**
     * 🔧 Générer QR code simple (ID seulement)
     * Alternative: QR code minimal
     */
    public void displaySimpleQRCode(Service service) {
        if (service == null) return;

        try {
            // QR simple: "Service#123"
            String qrFilePath = qrService.generateServiceQRCode(
                service.getId(),
                "Service#" + service.getId()
            );

            if (qrFilePath != null) {
                Image qrImage = qrService.loadQRCodeImage(qrFilePath);
                qrCodeImageView.setImage(qrImage);
                serviceInfoLabel.setText("QR code simple généré");
            }

        } catch (Exception e) {
            System.err.println("Erreur QR simple: " + e.getMessage());
        }
    }
}
