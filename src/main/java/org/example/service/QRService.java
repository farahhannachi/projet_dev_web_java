package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service pour générer des codes QR en Java
 * Utilise ZXing pour créer les QR codes
 */
public class QRService {

    private static QRService instance;
    private static final String QR_CODE_FOLDER = "qr_codes";
    private static final int QR_CODE_SIZE = 300; // pixels

    private QRService() {
        // Créer le dossier s'il n'existe pas
        try {
            Files.createDirectories(Paths.get(QR_CODE_FOLDER));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier QR: " + e.getMessage());
        }
    }

    /**
     * Obtenir l'instance singleton du service
     */
    public static QRService getInstance() {
        if (instance == null) {
            instance = new QRService();
        }
        return instance;
    }

    /**
     * Générer un QR code pour un service
     *
     * @param serviceId ID du service
     * @param serviceData Données pour le QR (ex: "Service#123" ou un lien)
     * @return Chemin du fichier PNG généré
     */
    public String generateServiceQRCode(int serviceId, String serviceData) {
        try {
            // Créer le contenu du QR code
            String qrContent = serviceData != null ? serviceData : "Service#" + serviceId;

            // Générer le QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            // Chemin du fichier
            String fileName = "service_" + serviceId + ".png";
            Path filePath = Paths.get(QR_CODE_FOLDER, fileName);

            // Écrire l'image PNG
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

            System.out.println("✓ QR code généré pour service " + serviceId + ": " + filePath);
            return filePath.toAbsolutePath().toString();

        } catch (WriterException | IOException e) {
            System.err.println("Erreur lors de la génération du QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Générer un QR code avec informations complètes du service
     * Format: id|nom|type|specialite|date
     *
     * @param serviceId ID du service
     * @param nom Nom du service/médecin
     * @param type Type (Médecin, Infirmier, etc)
     * @param specialite Spécialité
     * @return Chemin du fichier PNG généré
     */
    public String generateServiceQRCodeWithInfo(int serviceId, String nom, String type, String specialite) {
        String qrData = String.format("id:%d|nom:%s|type:%s|specialite:%s", serviceId, nom, type, specialite);
        return generateServiceQRCode(serviceId, qrData);
    }

    /**
     * Générer un QR code avec un lien vers le service
     *
     * @param serviceId ID du service
     * @param baseUrl URL de base (ex: "http://localhost:8080")
     * @return Chemin du fichier PNG généré
     */
    public String generateServiceQRCodeWithLink(int serviceId, String baseUrl) {
        String serviceLink = baseUrl + "/service/" + serviceId;
        return generateServiceQRCode(serviceId, serviceLink);
    }

    /**
     * Charger l'image PNG du QR code en tant qu'objet JavaFX Image
     * Pour afficher dans une ImageView
     *
     * @param filePath Chemin absolu du fichier PNG
     * @return Objet Image JavaFX, null si erreur
     */
    public Image loadQRCodeImage(String filePath) {
        try {
            if (filePath != null) {
                return new Image("file:" + filePath);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image QR: " + e.getMessage());
        }
        return null;
    }

    /**
     * Générer un QR code en mémoire (retourne les bytes)
     * Utile pour envoyer par email ou stocker en base de données
     *
     * @param serviceId ID du service
     * @param serviceData Données pour le QR
     * @return Bytes du PNG, null si erreur
     */
    public byte[] generateQRCodeBytes(int serviceId, String serviceData) {
        try {
            String qrContent = serviceData != null ? serviceData : "Service#" + serviceId;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            System.err.println("Erreur lors de la génération du QR code en mémoire: " + e.getMessage());
            return null;
        }
    }

    /**
     * Supprimer le fichier QR code
     *
     * @param serviceId ID du service
     * @return true si suppression réussie
     */
    public boolean deleteServiceQRCode(int serviceId) {
        try {
            String fileName = "service_" + serviceId + ".png";
            Path filePath = Paths.get(QR_CODE_FOLDER, fileName);
            Files.deleteIfExists(filePath);
            System.out.println("✓ QR code supprimé pour service " + serviceId);
            return true;
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression du QR code: " + e.getMessage());
            return false;
        }
    }
}

