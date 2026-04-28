package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Service de signature électronique pour les ordonnances.
 * Utilise les colonnes réelles du schéma :
 *   signature_medecin (varchar 255), signature_date (datetime)
 *   signature_patient (longtext), signature_patient_date (datetime)
 */
public class ElectronicSignatureService {

    private static ElectronicSignatureService instance;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ElectronicSignatureService() {}

    public static ElectronicSignatureService getInstance() {
        if (instance == null) instance = new ElectronicSignatureService();
        return instance;
    }

    public static class SignatureResult {
        public boolean success;
        public String signatureHash;
        public String signedAt;
        public String signataire;
        public String role;
        public String message;

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

    public SignatureResult signer(String numeroOrdonnance, String signataire,
                                   String role, String signatureData) {
        try {
            String timestamp = LocalDateTime.now().format(FMT);
            String payload = "CURAVITA|" + numeroOrdonnance + "|" + signataire
                    + "|" + role + "|" + timestamp + "|" + signatureData;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            return new SignatureResult(true, hash, timestamp, signataire, role,
                    "Signature électronique générée avec succès.");
        } catch (Exception e) {
            return new SignatureResult(false, null, null, signataire, role,
                    "Erreur : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde la signature médecin.
     * Stocke : "NomMedecin|2026-04-28 10:30:00|HASH..." dans signature_medecin
     */
    public boolean sauvegarderSignatureMedecin(int ordonnanceId, SignatureResult sig) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE ordonnance SET signature_medecin = ?, signature_date = NOW() " +
                "WHERE id_ordonnance = ?"
            );
            String valeur = sig.signataire + "|" + sig.signedAt + "|" + sig.signatureHash;
            ps.setString(1, valeur);
            ps.setInt(2, ordonnanceId);
            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("[Signature] Erreur sauvegarde médecin : " + e.getMessage());
            return false;
        }
    }

    /**
     * Sauvegarde la signature patient.
     * Stocke : "NomPatient|2026-04-28 10:30:00|HASH..." dans signature_patient
     */
    public boolean sauvegarderSignaturePatient(int ordonnanceId, SignatureResult sig) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
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

    public boolean[] verifierSignatures(int ordonnanceId) {
        boolean[] result = {false, false};
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT signature_medecin, signature_patient FROM ordonnance WHERE id_ordonnance = ?"
            );
            ps.setInt(1, ordonnanceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result[0] = rs.getString("signature_medecin") != null && !rs.getString("signature_medecin").isBlank();
                result[1] = rs.getString("signature_patient") != null && !rs.getString("signature_patient").isBlank();
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            System.err.println("[Signature] Erreur vérification : " + e.getMessage());
        }
        return result;
    }

    /** Extrait le nom du signataire depuis la valeur stockée "Nom|date|hash" */
    public String extraireNomSignataire(String valeurStockee) {
        if (valeurStockee == null || valeurStockee.isBlank()) return null;
        String[] parts = valeurStockee.split("\\|");
        return parts.length > 0 ? parts[0] : valeurStockee;
    }

    /** Extrait la date depuis la valeur stockée "Nom|date|hash" */
    public String extraireDate(String valeurStockee) {
        if (valeurStockee == null || valeurStockee.isBlank()) return null;
        String[] parts = valeurStockee.split("\\|");
        return parts.length > 1 ? parts[1] : "";
    }

    public void initColonnes() {
        // Les colonnes existent déjà dans le schéma — rien à faire
    }
}
