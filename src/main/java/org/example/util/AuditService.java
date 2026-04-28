package org.example.util;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'audit : enregistre chaque création/modification/suppression
 * dans la table audit_log pour traçabilité complète.
 */
public class AuditService {

    private static AuditService instance;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditService() {}

    public static AuditService getInstance() {
        if (instance == null) instance = new AuditService();
        return instance;
    }

    /** DTO représentant une entrée d'audit */
    public static class AuditEntry {
        public int id;
        public String entite;       // "ordonnance" ou "traitement"
        public String entiteId;     // ID de l'enregistrement concerné
        public String action;       // "CREATION", "MODIFICATION", "SUPPRESSION"
        public String champ;        // champ modifié (null si création/suppression)
        public String ancienneValeur;
        public String nouvelleValeur;
        public String modifiePar;   // nom de l'admin
        public String modifieAt;    // horodatage

        public AuditEntry(int id, String entite, String entiteId, String action,
                          String champ, String ancienneValeur, String nouvelleValeur,
                          String modifiePar, String modifieAt) {
            this.id = id;
            this.entite = entite;
            this.entiteId = entiteId;
            this.action = action;
            this.champ = champ;
            this.ancienneValeur = ancienneValeur;
            this.nouvelleValeur = nouvelleValeur;
            this.modifiePar = modifiePar;
            this.modifieAt = modifieAt;
        }
    }

    /**
     * Initialise la table audit_log si elle n'existe pas.
     * Compatible avec la structure existante en base.
     */
    public void initTable() {
        // La table existe déjà — on vérifie juste qu'elle est accessible
        try {
            DatabaseUtil.getInstance().getConnection()
                .createStatement().execute("SELECT 1 FROM audit_log LIMIT 1");
        } catch (Exception e) {
            System.err.println("[Audit] Table audit_log inaccessible : " + e.getMessage());
        }
    }

    /**
     * Enregistre une action d'audit.
     */
    public void log(String entite, String entiteId, String action,
                    String champ, String ancienneValeur, String nouvelleValeur,
                    String modifiePar) {
        try {
            String at = LocalDateTime.now().format(FMT);
            // Convertir entiteId en int (0 si non numérique)
            int entityIdInt = 0;
            try { entityIdInt = Integer.parseInt(entiteId); } catch (Exception ignored) {}

            PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(
                "INSERT INTO audit_log (entity_type, entity_id, action, user_name, " +
                "old_values, new_values, changed_fields, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, entite);
            ps.setInt(2, entityIdInt);
            ps.setString(3, action);
            ps.setString(4, modifiePar != null ? modifiePar : "Admin");
            ps.setString(5, ancienneValeur != null ? "\"" + ancienneValeur.replace("\"", "'") + "\"" : null);
            ps.setString(6, nouvelleValeur != null ? "\"" + nouvelleValeur.replace("\"", "'") + "\"" : null);
            ps.setString(7, champ != null ? "\"" + champ + "\"" : null);
            ps.setString(8, at);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            System.err.println("[Audit] Erreur log : " + e.getMessage());
        }
    }

    /**
     * Raccourci pour loguer une création complète.
     */
    public void logCreation(String entite, String entiteId, String resume, String modifiePar) {
        log(entite, entiteId, "CRÉATION", null, null, resume, modifiePar);
    }

    /**
     * Raccourci pour loguer une suppression.
     */
    public void logSuppression(String entite, String entiteId, String resume, String modifiePar) {
        log(entite, entiteId, "SUPPRESSION", null, resume, null, modifiePar);
    }

    /**
     * Compare deux valeurs et logue si elles diffèrent.
     */
    public void logSiModifie(String entite, String entiteId, String champ,
                              String avant, String apres, String modifiePar) {
        if (avant == null) avant = "";
        if (apres == null) apres = "";
        if (!avant.equals(apres)) {
            log(entite, entiteId, "MODIFICATION", champ, avant, apres, modifiePar);
        }
    }

    /**
     * Récupère l'historique d'un enregistrement.
     */
    public List<AuditEntry> getHistorique(String entite, String entiteId) {
        List<AuditEntry> entries = new ArrayList<>();
        try {
            int entityIdInt = 0;
            try { entityIdInt = Integer.parseInt(entiteId); } catch (Exception ignored) {}

            PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(
                "SELECT id, entity_type, entity_id, action, changed_fields, " +
                "old_values, new_values, user_name, created_at " +
                "FROM audit_log WHERE entity_type = ? AND entity_id = ? " +
                "ORDER BY id DESC"
            );
            ps.setString(1, entite);
            ps.setInt(2, entityIdInt);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Nettoyer les guillemets JSON autour des valeurs
                String champ    = clean(rs.getString("changed_fields"));
                String avant    = clean(rs.getString("old_values"));
                String apres    = clean(rs.getString("new_values"));
                String modPar   = rs.getString("user_name");
                String modAt    = rs.getString("created_at");

                entries.add(new AuditEntry(
                    rs.getInt("id"),
                    rs.getString("entity_type"),
                    String.valueOf(rs.getInt("entity_id")),
                    rs.getString("action"),
                    champ, avant, apres,
                    modPar != null ? modPar : "Admin",
                    modAt != null ? modAt : ""
                ));
            }
            rs.close(); ps.close();
        } catch (Exception e) {
            System.err.println("[Audit] Erreur getHistorique : " + e.getMessage());
        }
        return entries;
    }

    /** Retire les guillemets JSON autour d'une valeur */
    private String clean(String val) {
        if (val == null) return null;
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1)
            val = val.substring(1, val.length() - 1);
        return val.isBlank() ? null : val;
    }
}
