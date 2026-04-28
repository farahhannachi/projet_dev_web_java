package org.example.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.HashMap;
import java.util.Map;

/**
 * Génère un QR code JavaFX Image à partir d'un texte.
 */
public class QRCodeService {

    private static QRCodeService instance;

    private QRCodeService() {}

    public static QRCodeService getInstance() {
        if (instance == null) instance = new QRCodeService();
        return instance;
    }

    /**
     * Génère une Image JavaFX contenant le QR code du texte donné.
     * @param content texte à encoder
     * @param size    taille en pixels (ex: 300)
     */
    public Image generateQRImage(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pw.setColor(x, y, matrix.get(x, y)
                        ? javafx.scene.paint.Color.BLACK
                        : javafx.scene.paint.Color.WHITE);
            }
        }
        return image;
    }

    /**
     * Construit le texte encodé dans le QR code pour une ordonnance.
     * Format lisible par n'importe quel scanner.
     */
    public String buildQRContent(String numeroOrdonnance, String patient,
                                  java.util.List<TraitementInfo> traitements) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ORDONNANCE CURAVITA ===\n");
        sb.append("Numéro : ").append(numeroOrdonnance).append("\n");
        sb.append("Patient : ").append(patient).append("\n");
        sb.append("Date scan : ").append(java.time.LocalDate.now()).append("\n");
        sb.append("\n--- INSTRUCTIONS D'UTILISATION ---\n");

        int i = 1;
        for (TraitementInfo t : traitements) {
            sb.append("\n").append(i++).append(". ").append(t.produit).append("\n");
            if (t.dosage != null && !t.dosage.isBlank())
                sb.append("   Dosage    : ").append(t.dosage).append("\n");
            if (t.frequence != null && !t.frequence.isBlank())
                sb.append("   Fréquence : ").append(t.frequence).append("\n");
            if (t.repas != null && !t.repas.isBlank())
                sb.append("   Repas     : ").append(t.repas).append("\n");
            if (t.dureeJours > 0)
                sb.append("   Durée     : ").append(t.dureeJours).append(" jours\n");
            if (t.statut != null && !t.statut.isBlank())
                sb.append("   Statut    : ").append(t.statut).append("\n");
        }

        sb.append("\n==========================\n");
        sb.append("⚠ IMPORTANT : Respectez toujours le dosage prescrit.\n");
        sb.append("Ne dépassez jamais la dose recommandée.\n");
        sb.append("En cas de doute, consultez votre médecin ou pharmacien.\n");
        sb.append("==========================\n");
        sb.append("Généré par CuraVita");
        return sb.toString();
    }

    /** DTO simple pour les infos d'un traitement */
    public static class TraitementInfo {
        public String produit;
        public String dosage;
        public String frequence;
        public String repas;
        public int dureeJours;
        public String statut;

        public TraitementInfo(String produit, String dosage, String frequence,
                               String repas, int dureeJours, String statut) {
            this.produit = produit;
            this.dosage = dosage;
            this.frequence = frequence;
            this.repas = repas;
            this.dureeJours = dureeJours;
            this.statut = statut;
        }
    }
}
