package org.example.util; // Package "util" — services transversaux

// Bibliothèque Google ZXing (Zebra Crossing) pour la génération de QR codes
import com.google.zxing.BarcodeFormat;              // Format du code-barres (QR_CODE, EAN_13, etc.)
import com.google.zxing.EncodeHintType;             // Options d'encodage (correction d'erreur, marge, charset)
import com.google.zxing.WriterException;            // Exception levée si l'encodage échoue
import com.google.zxing.common.BitMatrix;           // Matrice binaire représentant les pixels du QR code
import com.google.zxing.qrcode.QRCodeWriter;        // Classe principale qui encode le texte en QR code
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel; // Niveau de correction d'erreur

// JavaFX : classes pour créer et manipuler des images
import javafx.scene.image.Image;          // Interface Image JavaFX (type de retour)
import javafx.scene.image.PixelWriter;    // Permet d'écrire pixel par pixel dans une image
import javafx.scene.image.WritableImage;  // Image JavaFX modifiable (on peut écrire dedans)

import java.util.HashMap; // Pour stocker les options d'encodage (clé → valeur)
import java.util.Map;     // Interface Map

/**
 * QRCodeService — Génère des QR codes sous forme d'images JavaFX.
 *
 * Rôle : encoder une URL (ou tout texte) en QR code affichable dans l'interface JavaFX.
 * Le QR code encode l'URL du PDF de l'ordonnance → scannable par téléphone.
 *
 * Bibliothèque utilisée : Google ZXing 3.5.3
 * Processus : texte → BitMatrix (matrice de pixels) → WritableImage JavaFX
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class QRCodeService {

    // Instance unique (Singleton)
    private static QRCodeService instance;

    // Constructeur privé
    private QRCodeService() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static QRCodeService getInstance() {
        if (instance == null) instance = new QRCodeService();
        return instance;
    }

    /**
     * Génère une Image JavaFX contenant le QR code du texte donné.
     *
     * Processus en 3 étapes :
     *   1. Configurer les options d'encodage (correction d'erreur, marge, charset)
     *   2. Encoder le texte en BitMatrix (matrice binaire de pixels)
     *   3. Convertir la BitMatrix en WritableImage JavaFX pixel par pixel
     *
     * @param content Le texte à encoder dans le QR code (ex: URL du PDF)
     * @param size    La taille en pixels du QR code (ex: 300 → image 300×300)
     * @return Une Image JavaFX affichable dans un ImageView
     * @throws WriterException Si l'encodage ZXing échoue
     */
    public Image generateQRImage(String content, int size) throws WriterException {

        // ── Étape 1 : Configurer les options d'encodage ───────────────────
        Map<EncodeHintType, Object> hints = new HashMap<>();

        // Niveau de correction d'erreur M = Medium = 15% de données récupérables
        // Si le QR code est partiellement endommagé/sale, il reste lisible
        // Niveaux disponibles : L(7%), M(15%), Q(25%), H(30%)
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        // Marge autour du QR code (en "modules" = unités de base du QR code)
        // 2 modules de marge blanche autour du code (requis par la norme QR)
        hints.put(EncodeHintType.MARGIN, 2);

        // Charset UTF-8 : permet d'encoder des caractères accentués (é, à, ç...)
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // ── Étape 2 : Encoder le texte en BitMatrix ───────────────────────
        QRCodeWriter writer = new QRCodeWriter(); // Encodeur ZXing

        // encode() : convertit le texte en matrice binaire
        // BarcodeFormat.QR_CODE : on veut un QR code (pas un code-barres 1D)
        // size, size : largeur et hauteur en pixels
        // hints : les options configurées ci-dessus
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        // BitMatrix est une grille de booléens : true = pixel noir (module QR), false = pixel blanc

        // ── Étape 3 : Convertir BitMatrix → WritableImage JavaFX ─────────
        WritableImage image = new WritableImage(size, size); // Créer une image vide size×size
        PixelWriter pw = image.getPixelWriter(); // Obtenir l'outil pour écrire les pixels

        // Parcourir chaque pixel de l'image (ligne par ligne, colonne par colonne)
        for (int y = 0; y < size; y++) {       // y = ligne (de haut en bas)
            for (int x = 0; x < size; x++) {   // x = colonne (de gauche à droite)
                // matrix.get(x, y) : true si ce pixel est un module QR (noir), false sinon (blanc)
                pw.setColor(x, y, matrix.get(x, y)
                        ? javafx.scene.paint.Color.BLACK  // Module QR → pixel noir
                        : javafx.scene.paint.Color.WHITE); // Fond → pixel blanc
            }
        }
        return image; // Retourner l'image JavaFX prête à afficher
    }

    /**
     * Construit le texte lisible encodé dans le QR code pour une ordonnance.
     *
     * Ce texte est affiché quand on scanne le QR avec une app de lecture de QR
     * (pas un navigateur). Il contient toutes les infos du traitement en clair.
     *
     * Note : dans le flux principal, on encode plutôt l'URL du PDF
     * (via QRPdfServerService) pour ouvrir directement le PDF dans le navigateur.
     *
     * @param numeroOrdonnance Le numéro de l'ordonnance (ex: "ORD-2026-1227")
     * @param patient          Le nom du patient
     * @param traitements      La liste des traitements avec leurs détails
     * @return Le texte formaté à encoder dans le QR code
     */
    public String buildQRContent(String numeroOrdonnance, String patient,
                                  java.util.List<TraitementInfo> traitements) {
        StringBuilder sb = new StringBuilder(); // Constructeur de chaîne efficace

        // En-tête de l'ordonnance
        sb.append("=== ORDONNANCE CURAVITA ===\n");
        sb.append("Numéro : ").append(numeroOrdonnance).append("\n");
        sb.append("Patient : ").append(patient).append("\n");
        sb.append("Date scan : ").append(java.time.LocalDate.now()).append("\n"); // Date du jour
        sb.append("\n--- INSTRUCTIONS D'UTILISATION ---\n");

        int i = 1; // Numérotation des médicaments
        for (TraitementInfo t : traitements) {
            sb.append("\n").append(i++).append(". ").append(t.produit).append("\n"); // "1. Doliprane"

            // N'afficher que les champs renseignés (éviter les lignes vides)
            if (t.dosage != null && !t.dosage.isBlank())
                sb.append("   Dosage    : ").append(t.dosage).append("\n");
            if (t.frequence != null && !t.frequence.isBlank())
                sb.append("   Fréquence : ").append(t.frequence).append("\n");
            if (t.repas != null && !t.repas.isBlank())
                sb.append("   Repas     : ").append(t.repas).append("\n");
            if (t.dureeJours > 0) // 0 = non renseigné
                sb.append("   Durée     : ").append(t.dureeJours).append(" jours\n");
            if (t.statut != null && !t.statut.isBlank())
                sb.append("   Statut    : ").append(t.statut).append("\n");
        }

        // Pied de page avec avertissement de sécurité
        sb.append("\n==========================\n");
        sb.append("⚠ IMPORTANT : Respectez toujours le dosage prescrit.\n");
        sb.append("Ne dépassez jamais la dose recommandée.\n");
        sb.append("En cas de doute, consultez votre médecin ou pharmacien.\n");
        sb.append("==========================\n");
        sb.append("Généré par CuraVita");

        return sb.toString(); // Retourner le texte complet
    }

    /**
     * TraitementInfo — DTO (Data Transfer Object) pour transporter les infos d'un traitement.
     *
     * Classe interne statique : accessible via QRCodeService.TraitementInfo
     * Utilisée pour passer les données de traitement au service QR sans dépendance
     * sur le modèle Traitement (découplage des couches).
     *
     * Les champs sont publics pour un accès direct (simplicité).
     */
    public static class TraitementInfo {
        public String produit;   // Nom du médicament (ex: "Doliprane 1000mg")
        public String dosage;    // ex: "500mg"
        public String frequence; // ex: "3 fois par jour"
        public String repas;     // ex: "Après le repas"
        public int dureeJours;   // ex: 7
        public String statut;    // ex: "actif"

        // Constructeur : initialise tous les champs
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
