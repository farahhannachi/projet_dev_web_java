package org.example.util; // Package "util" — services transversaux

import java.nio.charset.StandardCharsets; // Charset UTF-8 pour encoder le texte en bytes
import java.security.MessageDigest;       // Classe Java pour calculer des hachages cryptographiques
import java.sql.Connection;               // Connexion JDBC
import java.sql.PreparedStatement;        // Requête SQL paramétrée
import java.sql.ResultSet;                // Résultat d'une requête SELECT
import java.time.LocalDateTime;           // Date et heure sans fuseau horaire
import java.time.format.DateTimeFormatter; // Formateur de date/heure
import java.util.Base64;                  // Encodage Base64 (binaire → texte ASCII)

/**
 * ElectronicSignatureService — Service de signature électronique pour les ordonnances.
 *
 * Rôle : générer et stocker une signature électronique pour valider une ordonnance.
 * Deux types de signatures : médecin et patient.
 *
 * Algorithme de signature :
 *   1. Construire un "payload" unique : "CURAVITA|numéro|signataire|rôle|timestamp|données"
 *   2. Calculer le hash SHA-256 du payload (empreinte numérique unique)
 *   3. Encoder le hash en Base64 (pour le stocker comme texte en base)
 *   4. Stocker "Nom|date|HASH" dans la colonne signature_medecin ou signature_patient
 *
 * SHA-256 : algorithme de hachage cryptographique → même entrée = même hash,
 * impossible de retrouver l'entrée depuis le hash (sens unique).
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class ElectronicSignatureService {

    // Instance unique (Singleton)
    private static ElectronicSignatureService instance;

    // Formateur de date : "2026-04-28 10:30:00" (format SQL-compatible)
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructeur privé
    private ElectronicSignatureService() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static ElectronicSignatureService getInstance() {
        if (instance == null) instance = new ElectronicSignatureService();
        return instance;
    }

    /**
     * SignatureResult — DTO représentant le résultat d'une opération de signature.
     *
     * Contient le hash généré, la date, le signataire et un message de statut.
     */
    public static class SignatureResult {
        public boolean success;       // true si la signature a été générée avec succès
        public String signatureHash;  // Le hash SHA-256 encodé en Base64
        public String signedAt;       // Horodatage de la signature (ex: "2026-04-28 10:30:00")
        public String signataire;     // Nom du signataire (patient ou médecin)
        public String role;           // "PATIENT" ou "MEDECIN"
        public String message;        // Message de succès ou d'erreur

        // Constructeur : initialise tous les champs
        public SignatureResult(boolean success, String hash, String signedAt,
                               String signataire, String role, String message) {
            this.success = success;
            this.signatureHash = hash;
            this.signedAt = signedAt;
            this.signataire = signataire;
            this.role = role;
            this.message = message;
        }
    }

    /**
     * Génère une signature électronique pour une ordonnance.
     *
     * Processus :
     *   1. Capturer l'horodatage exact (timestamp)
     *   2. Construire le payload : chaîne unique combinant toutes les infos
     *   3. Calculer SHA-256(payload) → tableau de bytes
     *   4. Encoder en Base64 → chaîne de texte stockable
     *
     * @param numeroOrdonnance Le numéro de l'ordonnance (ex: "ORD-2026-1227")
     * @param signataire       Le nom du signataire (ex: "Jean Dupont")
     * @param role             Le rôle : "PATIENT" ou "MEDECIN"
     * @param signatureData    Données supplémentaires (ex: données biométriques, IP...)
     * @return SignatureResult avec le hash et les métadonnées
     */
    public SignatureResult signer(String numeroOrdonnance, String signataire,
                                   String role, String signatureData) {
        try {
            // Capturer l'horodatage exact de la signature
            String timestamp = LocalDateTime.now().format(FMT); // ex: "2026-04-28 10:30:00"

            // Construire le payload : chaîne unique qui identifie cette signature
            // Le séparateur "|" permet de parser facilement les parties plus tard
            String payload = "CURAVITA|" + numeroOrdonnance + "|" + signataire
                    + "|" + role + "|" + timestamp + "|" + signatureData;
            // Exemple : "CURAVITA|ORD-2026-1227|Jean Dupont|PATIENT|2026-04-28 10:30:00|data"

            // Obtenir l'instance de l'algorithme SHA-256
            // MessageDigest est la classe Java standard pour les hachages cryptographiques
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Calculer le hash : convertir le payload en bytes UTF-8, puis hacher
            // digest() retourne un tableau de 32 bytes (256 bits)
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));

            // Encoder les bytes en Base64 pour obtenir une chaîne de texte stockable
            // Base64 : convertit des données binaires en texte ASCII (64 caractères possibles)
            // Ex: [0x3F, 0xA2, ...] → "P6Lm..."
            String hash = Base64.getEncoder().encodeToString(hashBytes);

            // Retourner le résultat de la signature
            return new SignatureResult(true, hash, timestamp, signataire, role,
                    "Signature électronique générée avec succès.");

        } catch (Exception e) {
            // En cas d'erreur (algorithme SHA-256 non disponible, etc.)
            return new SignatureResult(false, null, null, signataire, role,
                    "Erreur : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde la signature du médecin dans la base de données.
     *
     * Format stocké dans la colonne signature_medecin :
     *   "NomMedecin|2026-04-28 10:30:00|HASH_BASE64"
     *
     * Exemple : "Dr. Martin|2026-04-28 10:30:00|P6LmXk9..."
     *
     * @param ordonnanceId L'ID de l'ordonnance à signer
     * @param sig          Le résultat de la signature (contient hash, date, nom)
     * @return true si la mise à jour a réussi
     */
    public boolean sauvegarderSignatureMedecin(int ordonnanceId, SignatureResult sig) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // UPDATE : mettre à jour les colonnes signature_medecin et signature_date
            // NOW() : fonction SQL qui insère la date/heure actuelle du serveur MySQL
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE ordonnance SET signature_medecin = ?, signature_date = NOW() " +
                "WHERE id_ordonnance = ?"
            );

            // Construire la valeur à stocker : "Nom|date|hash"
            String valeur = sig.signataire + "|" + sig.signedAt + "|" + sig.signatureHash;
            ps.setString(1, valeur);       // La valeur de signature
            ps.setInt(2, ordonnanceId);    // L'ID de l'ordonnance à modifier

            int rows = ps.executeUpdate(); // Exécuter l'UPDATE
            ps.close();
            return rows > 0; // true si au moins une ligne a été modifiée

        } catch (Exception e) {
            System.err.println("[Signature] Erreur sauvegarde médecin : " + e.getMessage());
            return false;
        }
    }

    /**
     * Sauvegarde la signature du patient dans la base de données.
     *
     * Format stocké dans la colonne signature_patient :
     *   "NomPatient|2026-04-28 10:30:00|HASH_BASE64"
     *
     * @param ordonnanceId L'ID de l'ordonnance à signer
     * @param sig          Le résultat de la signature
     * @return true si la mise à jour a réussi
     */
    public boolean sauvegarderSignaturePatient(int ordonnanceId, SignatureResult sig) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // UPDATE : mettre à jour signature_patient et signature_patient_date
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE ordonnance SET signature_patient = ?, signature_patient_date = NOW() " +
                "WHERE id_ordonnance = ?"
            );

            String valeur = sig.signataire + "|" + sig.signedAt + "|" + sig.signatureHash;
            ps.setString(1, valeur);
            ps.setInt(2, ordonnanceId);

            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("[Signature] Erreur sauvegarde patient : " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si une ordonnance a été signée (médecin et/ou patient).
     *
     * @param ordonnanceId L'ID de l'ordonnance à vérifier
     * @return boolean[2] : [0] = médecin a signé, [1] = patient a signé
     */
    public boolean[] verifierSignatures(int ordonnanceId) {
        boolean[] result = {false, false}; // Par défaut : aucune signature

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Récupérer les deux colonnes de signature pour cette ordonnance
            PreparedStatement ps = conn.prepareStatement(
                "SELECT signature_medecin, signature_patient FROM ordonnance WHERE id_ordonnance = ?"
            );
            ps.setInt(1, ordonnanceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Une signature est présente si la colonne n'est pas null et pas vide
                result[0] = rs.getString("signature_medecin") != null
                        && !rs.getString("signature_medecin").isBlank(); // Médecin signé ?
                result[1] = rs.getString("signature_patient") != null
                        && !rs.getString("signature_patient").isBlank(); // Patient signé ?
            }
            rs.close(); ps.close();

        } catch (Exception e) {
            System.err.println("[Signature] Erreur vérification : " + e.getMessage());
        }
        return result;
    }

    /**
     * Extrait le nom du signataire depuis la valeur stockée en base.
     *
     * Format stocké : "NomSignataire|date|hash"
     * split("\\|") : découpe la chaîne sur le caractère "|" (échappé car | est spécial en regex)
     * parts[0] : premier élément = le nom
     *
     * @param valeurStockee La valeur brute de la colonne signature_medecin ou signature_patient
     * @return Le nom du signataire, ou null si la valeur est vide
     */
    public String extraireNomSignataire(String valeurStockee) {
        if (valeurStockee == null || valeurStockee.isBlank()) return null;
        String[] parts = valeurStockee.split("\\|"); // Découper sur "|"
        return parts.length > 0 ? parts[0] : valeurStockee; // Retourner le premier élément
    }

    /**
     * Extrait la date de signature depuis la valeur stockée en base.
     *
     * Format stocké : "NomSignataire|date|hash"
     * parts[1] : deuxième élément = la date
     *
     * @param valeurStockee La valeur brute de la colonne de signature
     * @return La date de signature, ou "" si non disponible
     */
    public String extraireDate(String valeurStockee) {
        if (valeurStockee == null || valeurStockee.isBlank()) return null;
        String[] parts = valeurStockee.split("\\|");
        return parts.length > 1 ? parts[1] : ""; // Retourner le deuxième élément
    }

    /**
     * Méthode de compatibilité : les colonnes de signature existent déjà en base.
     * Ne fait rien (conservée pour ne pas casser les appels existants).
     */
    public void initColonnes() {
        // Les colonnes signature_medecin, signature_date, signature_patient,
        // signature_patient_date existent déjà dans le schéma SQL — rien à faire
    }
}
