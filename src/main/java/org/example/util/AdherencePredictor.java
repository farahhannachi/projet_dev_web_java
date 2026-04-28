package org.example.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de prédiction de non-adhérence au traitement.
 * Basé sur l'historique du patient : traitements abandonnés, durées non respectées,
 * ordonnances expirées sans complétion.
 */
public class AdherencePredictor {

    private static AdherencePredictor instance;

    private AdherencePredictor() {}

    public static AdherencePredictor getInstance() {
        if (instance == null) instance = new AdherencePredictor();
        return instance;
    }

    public static class PatientRisk {
        public int userId;
        public String nomPatient;
        public int totalTraitements;
        public int traitementsAbandonnés;
        public int ordonnancesExpirees;
        public int traitementsEnAttenteTropLongs; // en attente depuis > 30 jours
        public double scoreRisque; // 0.0 à 1.0
        public String niveau; // "Faible", "Modéré", "Élevé"
        public List<String> facteurs = new ArrayList<>();

        public PatientRisk(int userId, String nom) {
            this.userId = userId;
            this.nomPatient = nom;
        }
    }

    /**
     * Calcule le risque de non-adhérence pour tous les patients ayant des traitements actifs
     */
    public List<PatientRisk> predireTousLesRisques() {
        List<PatientRisk> resultats = new ArrayList<>();
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Récupérer tous les patients avec au moins un traitement
            PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT u.id_utilisateur, CONCAT(u.prenom, ' ', u.nom) AS nom_complet " +
                "FROM utilisateur u " +
                "JOIN traitement t ON t.id_utilisateur_id = u.id_utilisateur " +
                "ORDER BY u.nom"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PatientRisk risk = calculerRisque(rs.getInt("id_utilisateur"), rs.getString("nom_complet"), conn);
                if (risk != null) resultats.add(risk);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println("[AdherencePredictor] Erreur : " + e.getMessage());
        }
        return resultats;
    }

    private PatientRisk calculerRisque(int userId, String nom, Connection conn) throws SQLException {
        PatientRisk risk = new PatientRisk(userId, nom);

        // 1. Total traitements
        PreparedStatement ps1 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ?");
        ps1.setInt(1, userId);
        ResultSet rs1 = ps1.executeQuery();
        if (rs1.next()) risk.totalTraitements = rs1.getInt(1);
        rs1.close(); ps1.close();

        if (risk.totalTraitements == 0) return null;

        // 2. Traitements "annulé" ou terminés avant la date de fin prévue
        PreparedStatement ps2 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ? AND status = 'annulé'");
        ps2.setInt(1, userId);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next()) risk.traitementsAbandonnés = rs2.getInt(1);
        rs2.close(); ps2.close();

        // 3. Ordonnances expirées sans traitement complété
        PreparedStatement ps3 = conn.prepareStatement(
            "SELECT COUNT(*) FROM ordonnance o " +
            "WHERE o.id_utilisateur_id = ? AND o.statut = 'expirée' " +
            "AND EXISTS (SELECT 1 FROM traitement t WHERE t.id_ordonnance_id = o.id_ordonnance AND t.status != 'terminé')");
        ps3.setInt(1, userId);
        ResultSet rs3 = ps3.executeQuery();
        if (rs3.next()) risk.ordonnancesExpirees = rs3.getInt(1);
        rs3.close(); ps3.close();

        // 4. Traitements en attente depuis plus de 30 jours
        PreparedStatement ps4 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ? " +
            "AND status = 'en_attente' AND DATEDIFF(NOW(), date_debut) > 30");
        ps4.setInt(1, userId);
        ResultSet rs4 = ps4.executeQuery();
        if (rs4.next()) risk.traitementsEnAttenteTropLongs = rs4.getInt(1);
        rs4.close(); ps4.close();

        // Calcul du score (modèle simple pondéré)
        double score = 0.0;
        if (risk.totalTraitements > 0) {
            double tauxAbandon = (double) risk.traitementsAbandonnés / risk.totalTraitements;
            score += tauxAbandon * 0.45; // 45% du poids
            if (risk.ordonnancesExpirees > 0) {
                score += Math.min(risk.ordonnancesExpirees * 0.15, 0.30); // max 30%
                risk.facteurs.add(risk.ordonnancesExpirees + " ordonnance(s) expirée(s) sans complétion");
            }
            if (risk.traitementsEnAttenteTropLongs > 0) {
                score += Math.min(risk.traitementsEnAttenteTropLongs * 0.10, 0.25); // max 25%
                risk.facteurs.add(risk.traitementsEnAttenteTropLongs + " traitement(s) en attente depuis +30 jours");
            }
            if (tauxAbandon > 0) {
                risk.facteurs.add(risk.traitementsAbandonnés + "/" + risk.totalTraitements + " traitement(s) abandonné(s)");
            }
        }

        risk.scoreRisque = Math.min(score, 1.0);

        if (risk.scoreRisque >= 0.6) risk.niveau = "Élevé";
        else if (risk.scoreRisque >= 0.3) risk.niveau = "Modéré";
        else risk.niveau = "Faible";

        return risk;
    }
}
