package org.example.util; // Package "util" — services transversaux

import java.sql.*;        // Classes JDBC
import java.util.ArrayList; // Liste dynamique
import java.util.List;      // Interface List

/**
 * AdherencePredictor — Modèle de prédiction de non-adhérence au traitement.
 *
 * "Adhérence" = le fait qu'un patient suive correctement son traitement
 * (prend ses médicaments aux bonnes doses, aux bons moments, jusqu'au bout).
 *
 * Ce service analyse l'historique de chaque patient et calcule un score de risque
 * de non-adhérence (0.0 = aucun risque, 1.0 = risque maximal).
 *
 * Modèle pondéré (weighted scoring model) :
 *   - Taux d'abandon des traitements : 45% du score
 *   - Ordonnances expirées sans complétion : max 30% du score
 *   - Traitements en attente depuis +30 jours : max 25% du score
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class AdherencePredictor {

    // Instance unique (Singleton)
    private static AdherencePredictor instance;

    // Constructeur privé
    private AdherencePredictor() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static AdherencePredictor getInstance() {
        if (instance == null) instance = new AdherencePredictor();
        return instance;
    }

    /**
     * PatientRisk — Classe interne (DTO) représentant le profil de risque d'un patient.
     *
     * Contient toutes les métriques calculées pour un patient :
     * compteurs bruts, score final, niveau de risque et liste des facteurs.
     */
    public static class PatientRisk {
        public int userId;                      // ID de l'utilisateur dans la base
        public String nomPatient;               // Prénom + Nom du patient
        public int totalTraitements;            // Nombre total de traitements prescrits
        public int traitementsAbandonnés;       // Traitements avec status = 'annulé'
        public int ordonnancesExpirees;         // Ordonnances expirées sans traitement terminé
        public int traitementsEnAttenteTropLongs; // Traitements en attente depuis > 30 jours
        public double scoreRisque;              // Score calculé entre 0.0 et 1.0
        public String niveau;                   // "Faible", "Modéré" ou "Élevé"
        public List<String> facteurs = new ArrayList<>(); // Explications textuelles du score

        // Constructeur : initialise avec l'ID et le nom du patient
        public PatientRisk(int userId, String nom) {
            this.userId = userId;
            this.nomPatient = nom;
        }
    }

    /**
     * Calcule le risque de non-adhérence pour TOUS les patients ayant des traitements.
     *
     * Étapes :
     *   1. Récupérer tous les patients distincts ayant au moins un traitement
     *   2. Pour chaque patient, calculer son score de risque
     *   3. Retourner la liste complète des profils de risque
     *
     * @return Liste de PatientRisk pour tous les patients (triée par nom)
     */
    public List<PatientRisk> predireTousLesRisques() {
        List<PatientRisk> resultats = new ArrayList<>();

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Requête : récupérer tous les patients DISTINCTS ayant au moins un traitement
            // DISTINCT : éviter les doublons si un patient a plusieurs traitements
            // CONCAT(prenom, ' ', nom) : construire le nom complet en SQL
            // JOIN traitement : s'assurer que le patient a au moins un traitement
            // ORDER BY u.nom : trier alphabétiquement
            PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT u.id_utilisateur, CONCAT(u.prenom, ' ', u.nom) AS nom_complet " +
                "FROM utilisateur u " +
                "JOIN traitement t ON t.id_utilisateur_id = u.id_utilisateur " +
                "ORDER BY u.nom"
            );

            ResultSet rs = ps.executeQuery();

            // Pour chaque patient trouvé, calculer son score de risque
            while (rs.next()) {
                PatientRisk risk = calculerRisque(
                    rs.getInt("id_utilisateur"),  // ID du patient
                    rs.getString("nom_complet"),  // Nom complet
                    conn                          // Réutiliser la même connexion (performance)
                );
                if (risk != null) resultats.add(risk); // Ajouter si le calcul a réussi
            }
            rs.close(); ps.close();

        } catch (SQLException e) {
            System.err.println("[AdherencePredictor] Erreur : " + e.getMessage());
        }
        return resultats;
    }

    /**
     * Calcule le score de risque de non-adhérence pour UN patient spécifique.
     *
     * Exécute 4 requêtes SQL distinctes pour collecter les métriques,
     * puis applique le modèle de scoring pondéré.
     *
     * @param userId ID du patient
     * @param nom    Nom complet du patient
     * @param conn   Connexion JDBC (réutilisée pour performance)
     * @return PatientRisk avec le score calculé, ou null si aucun traitement
     */
    private PatientRisk calculerRisque(int userId, String nom, Connection conn) throws SQLException {
        PatientRisk risk = new PatientRisk(userId, nom); // Créer le profil de risque

        // ── Métrique 1 : Total des traitements ────────────────────────────
        // COUNT(*) : compter toutes les lignes pour cet utilisateur
        PreparedStatement ps1 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ?");
        ps1.setInt(1, userId);
        ResultSet rs1 = ps1.executeQuery();
        if (rs1.next()) risk.totalTraitements = rs1.getInt(1); // getInt(1) = première colonne
        rs1.close(); ps1.close();

        // Si le patient n'a aucun traitement → pas de calcul possible
        if (risk.totalTraitements == 0) return null;

        // ── Métrique 2 : Traitements abandonnés ──────────────────────────
        // status = 'annulé' : le patient a arrêté son traitement avant la fin
        PreparedStatement ps2 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ? AND status = 'annulé'");
        ps2.setInt(1, userId);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next()) risk.traitementsAbandonnés = rs2.getInt(1);
        rs2.close(); ps2.close();

        // ── Métrique 3 : Ordonnances expirées sans traitement complété ────
        // Sous-requête EXISTS : vérifie qu'il existe au moins un traitement non terminé
        // pour cette ordonnance expirée → le patient n'a pas fini son traitement
        PreparedStatement ps3 = conn.prepareStatement(
            "SELECT COUNT(*) FROM ordonnance o " +
            "WHERE o.id_utilisateur_id = ? AND o.statut = 'expirée' " +
            "AND EXISTS (" +
            "  SELECT 1 FROM traitement t " +
            "  WHERE t.id_ordonnance_id = o.id_ordonnance AND t.status != 'terminé'" +
            ")");
        ps3.setInt(1, userId);
        ResultSet rs3 = ps3.executeQuery();
        if (rs3.next()) risk.ordonnancesExpirees = rs3.getInt(1);
        rs3.close(); ps3.close();

        // ── Métrique 4 : Traitements en attente depuis plus de 30 jours ──
        // DATEDIFF(NOW(), date_debut) : nombre de jours entre aujourd'hui et la date de début
        // > 30 : le traitement aurait dû commencer depuis longtemps
        PreparedStatement ps4 = conn.prepareStatement(
            "SELECT COUNT(*) FROM traitement WHERE id_utilisateur_id = ? " +
            "AND status = 'en_attente' AND DATEDIFF(NOW(), date_debut) > 30");
        ps4.setInt(1, userId);
        ResultSet rs4 = ps4.executeQuery();
        if (rs4.next()) risk.traitementsEnAttenteTropLongs = rs4.getInt(1);
        rs4.close(); ps4.close();

        // ── Calcul du score de risque (modèle pondéré) ───────────────────
        double score = 0.0; // Score initial à 0

        if (risk.totalTraitements > 0) {

            // Facteur 1 : Taux d'abandon (poids 45%)
            // Ex: 3 abandons sur 10 traitements → taux = 0.3 → contribution = 0.3 * 0.45 = 0.135
            double tauxAbandon = (double) risk.traitementsAbandonnés / risk.totalTraitements;
            score += tauxAbandon * 0.45; // 45% du score total

            // Facteur 2 : Ordonnances expirées (poids max 30%)
            if (risk.ordonnancesExpirees > 0) {
                // Math.min() : plafonner à 0.30 pour ne pas dépasser 30% du score
                // Ex: 2 ordonnances expirées → 2 * 0.15 = 0.30 (plafonné)
                score += Math.min(risk.ordonnancesExpirees * 0.15, 0.30);
                // Ajouter une explication textuelle au profil
                risk.facteurs.add(risk.ordonnancesExpirees + " ordonnance(s) expirée(s) sans complétion");
            }

            // Facteur 3 : Traitements en attente trop longs (poids max 25%)
            if (risk.traitementsEnAttenteTropLongs > 0) {
                // Math.min() : plafonner à 0.25
                score += Math.min(risk.traitementsEnAttenteTropLongs * 0.10, 0.25);
                risk.facteurs.add(risk.traitementsEnAttenteTropLongs + " traitement(s) en attente depuis +30 jours");
            }

            // Ajouter l'explication du taux d'abandon si > 0
            if (tauxAbandon > 0) {
                risk.facteurs.add(risk.traitementsAbandonnés + "/" + risk.totalTraitements + " traitement(s) abandonné(s)");
            }
        }

        // Math.min(score, 1.0) : s'assurer que le score ne dépasse jamais 1.0 (100%)
        risk.scoreRisque = Math.min(score, 1.0);

        // ── Déterminer le niveau de risque ────────────────────────────────
        // Seuils : >= 0.6 = Élevé, >= 0.3 = Modéré, < 0.3 = Faible
        if (risk.scoreRisque >= 0.6) risk.niveau = "Élevé";
        else if (risk.scoreRisque >= 0.3) risk.niveau = "Modéré";
        else risk.niveau = "Faible";

        return risk; // Retourner le profil de risque complet
    }
}
