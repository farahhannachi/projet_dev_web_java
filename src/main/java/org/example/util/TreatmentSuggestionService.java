package org.example.util;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service IA de suggestion de dosage, fréquence, repas et durée
 * basé sur les symptômes/antécédents du patient et l'historique en base.
 */
public class TreatmentSuggestionService {

    private static TreatmentSuggestionService instance;

    private TreatmentSuggestionService() {}

    public static TreatmentSuggestionService getInstance() {
        if (instance == null) instance = new TreatmentSuggestionService();
        return instance;
    }

    public static class Suggestion {
        public String dosage;
        public String frequence;
        public String repas;
        public int dureeJours;
        public String source; // "historique" ou "règles"

        public Suggestion(String dosage, String frequence, String repas, int dureeJours, String source) {
            this.dosage = dosage;
            this.frequence = frequence;
            this.repas = repas;
            this.dureeJours = dureeJours;
            this.source = source;
        }
    }

    /**
     * Suggère dosage/fréquence/repas/durée pour un produit donné
     * en analysant l'historique des traitements similaires en base.
     * @param produitId  ID du produit
     * @param notes      notes/symptômes/antécédents du patient
     */
    public Suggestion suggerer(int produitId, String notes) {
        // 1. Chercher dans l'historique : traitements actifs/terminés pour ce produit
        Suggestion fromHistory = chercherDansHistorique(produitId, notes);
        if (fromHistory != null) return fromHistory;

        // 2. Fallback : règles par défaut selon le produit
        return reglesParDefaut(produitId, notes);
    }

    private Suggestion chercherDansHistorique(int produitId, String notes) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Chercher les traitements les plus fréquents pour ce produit (terminés avec succès)
            PreparedStatement ps = conn.prepareStatement(
                "SELECT dosage, frequence, repas, duree_jours, COUNT(*) AS nb " +
                "FROM traitement " +
                "WHERE id_produit_id = ? AND status IN ('actif', 'terminé') " +
                "AND dosage IS NOT NULL AND dosage != '' " +
                "AND frequence IS NOT NULL AND frequence != '' " +
                "GROUP BY dosage, frequence, repas, duree_jours " +
                "ORDER BY nb DESC LIMIT 1"
            );
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Suggestion s = new Suggestion(
                    rs.getString("dosage"),
                    rs.getString("frequence"),
                    rs.getString("repas") != null ? rs.getString("repas") : "Après le repas",
                    rs.getInt("duree_jours") > 0 ? rs.getInt("duree_jours") : 7,
                    "historique (" + rs.getInt("nb") + " cas similaires)"
                );
                rs.close(); ps.close();
                return s;
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println("[TreatmentSuggestion] Erreur SQL : " + e.getMessage());
        }
        return null;
    }

    private Suggestion reglesParDefaut(int produitId, String notes) {
        String notesLower = notes != null ? notes.toLowerCase() : "";
        String nomProduit = getNomProduit(produitId).toLowerCase();

        // Règles basées sur le nom du produit et les symptômes
        String dosage = "500mg";
        String frequence = "2 fois par jour";
        String repas = "Après le repas";
        int duree = 7;

        // Antalgiques / antipyrétiques
        if (nomProduit.contains("paracetamol") || nomProduit.contains("panadol") || nomProduit.contains("doliprane")) {
            dosage = "500mg";
            frequence = "3 fois par jour";
            repas = "Pendant le repas";
            duree = 5;
        }
        // Anti-inflammatoires
        else if (nomProduit.contains("ibuprofene") || nomProduit.contains("ibuprofen") || nomProduit.contains("advil")) {
            dosage = "400mg";
            frequence = "3 fois par jour";
            repas = "Pendant le repas";
            duree = 5;
            if (notesLower.contains("allergi") || notesLower.contains("gastrite")) {
                repas = "Après le repas";
            }
        }
        // Antibiotiques
        else if (nomProduit.contains("amoxicilline") || nomProduit.contains("augmentin")) {
            dosage = "1g";
            frequence = "2 fois par jour";
            repas = "Pendant le repas";
            duree = 7;
        }
        else if (nomProduit.contains("azithromycine") || nomProduit.contains("zithromax")) {
            dosage = "500mg";
            frequence = "1 fois par jour";
            repas = "En dehors des repas";
            duree = 3;
        }
        // Antihistaminiques
        else if (nomProduit.contains("cetirizine") || nomProduit.contains("loratadine") || nomProduit.contains("zyrtec")) {
            dosage = "10mg";
            frequence = "1 fois par jour";
            repas = "En dehors des repas";
            duree = 14;
        }
        // Antiacides
        else if (nomProduit.contains("omeprazole") || nomProduit.contains("pantoprazole")) {
            dosage = "20mg";
            frequence = "1 fois par jour";
            repas = "Avant le repas";
            duree = 14;
        }
        // Aspirine
        else if (nomProduit.contains("aspirine") || nomProduit.contains("aspegic")) {
            dosage = "500mg";
            frequence = "3 fois par jour";
            repas = "Après le repas";
            duree = 5;
        }

        // Ajustement durée selon symptômes
        if (notesLower.contains("chronique") || notesLower.contains("longue durée")) duree = 30;
        else if (notesLower.contains("aigu") || notesLower.contains("urgent")) duree = 3;

        return new Suggestion(dosage, frequence, repas, duree, "règles médicales par défaut");
    }

    private String getNomProduit(int produitId) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT nom FROM produit WHERE id_produit = ?");
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                rs.close(); ps.close();
                return nom;
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println("[TreatmentSuggestion] Erreur getNom : " + e.getMessage());
        }
        return "";
    }
}
