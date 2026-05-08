package org.example.util; // Package "util" — services transversaux

import java.sql.*;                     // Classes JDBC
import java.time.LocalDateTime;        // Date et heure sans fuseau horaire
import java.time.format.DateTimeFormatter; // Formateur de date/heure
import java.util.ArrayList;            // Liste dynamique
import java.util.List;                 // Interface List

/**
 * AuditService — Service de traçabilité complète des actions CRUD.
 *
 * Rôle : enregistrer chaque création, modification et suppression dans la table
 * "audit_log" pour garder un historique complet de toutes les actions effectuées.
 *
 * Pourquoi l'audit ?
 *   - Traçabilité réglementaire (qui a fait quoi, quand ?)
 *   - Débogage (retrouver l'origine d'une erreur)
 *   - Sécurité (détecter des actions suspectes)
 *   - Conformité (RGPD, normes médicales)
 *
 * Table audit_log (existante en base) :
 *   - entity_type : "ordonnance" ou "traitement"
 *   - entity_id : ID de l'enregistrement concerné
 *   - action : "CRÉATION", "MODIFICATION", "SUPPRESSION"
 *   - changed_fields : champ modifié (JSON string)
 *   - old_values / new_values : valeurs avant/après (JSON string)
 *   - user_name : nom de l'admin/utilisateur
 *   - created_at : horodatage
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class AuditService {

    // Instance unique (Singleton)
    private static AuditService instance;

    // Formateur de date : "2026-04-28 10:30:00" (format SQL-compatible)
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructeur privé
    private AuditService() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static AuditService getInstance() {
        if (instance == null) instance = new AuditService();
        return instance;
    }

    /**
     * AuditEntry — DTO représentant une entrée dans le journal d'audit.
     *
     * Contient toutes les informations d'une action : qui, quoi, quand, avant, après.
     */
    public static class AuditEntry {
        public int id;                // ID auto-incrémenté de l'entrée d'audit
        public String entite;         // "ordonnance" ou "traitement"
        public String entiteId;       // ID de l'enregistrement concerné (ex: "42")
        public String action;         // "CRÉATION", "MODIFICATION", "SUPPRESSION"
        public String champ;          // Champ modifié (ex: "statut"), null si création/suppression
        public String ancienneValeur; // Valeur avant modification (null si création)
        public String nouvelleValeur; // Valeur après modification (null si suppression)
        public String modifiePar;     // Nom de l'utilisateur qui a fait l'action
        public String modifieAt;      // Horodatage de l'action (ex: "2026-04-28 10:30:00")

        // Constructeur : initialise tous les champs
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
     * Vérifie que la table audit_log est accessible.
     * Appelé au démarrage pour s'assurer que la table existe.
     */
    public void initTable() {
        try {
            // Requête de test : SELECT 1 retourne juste "1" si la table existe
            // Si la table n'existe pas → SQLException → log de l'erreur
            DatabaseUtil.getInstance().getConnection()
                .createStatement().execute("SELECT 1 FROM audit_log LIMIT 1");
        } catch (Exception e) {
            System.err.println("[Audit] Table audit_log inaccessible : " + e.getMessage());
        }
    }

    /**
     * Enregistre une action d'audit dans la table audit_log.
     *
     * Méthode centrale utilisée par toutes les autres méthodes de log.
     * Les valeurs sont stockées au format JSON string (avec guillemets).
     *
     * @param entite         Type d'entité : "ordonnance" ou "traitement"
     * @param entiteId       ID de l'enregistrement (ex: "42")
     * @param action         Type d'action : "CRÉATION", "MODIFICATION", "SUPPRESSION"
     * @param champ          Champ modifié (null si création/suppression)
     * @param ancienneValeur Valeur avant (null si création)
     * @param nouvelleValeur Valeur après (null si suppression)
     * @param modifiePar     Nom de l'utilisateur qui a fait l'action
     */
    public void log(String entite, String entiteId, String action,
                    String champ, String ancienneValeur, String nouvelleValeur,
                    String modifiePar) {
        try {
            // Capturer l'horodatage exact de l'action
            String at = LocalDateTime.now().format(FMT);

            // Convertir entiteId (String) en int pour la colonne entity_id (INT en base)
            // Si entiteId n'est pas un nombre (ex: "abc") → utiliser 0 par défaut
            int entityIdInt = 0;
            try { entityIdInt = Integer.parseInt(entiteId); } catch (Exception ignored) {}

            // Préparer l'INSERT dans audit_log
            PreparedStatement ps = DatabaseUtil.getInstance().getConnection().prepareStatement(
                "INSERT INTO audit_log (entity_type, entity_id, action, user_name, " +
                "old_values, new_values, changed_fields, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );

            ps.setString(1, entite);                                    // ex: "ordonnance"
            ps.setInt(2, entityIdInt);                                  // ex: 42
            ps.setString(3, action);                                    // ex: "MODIFICATION"
            ps.setString(4, modifiePar != null ? modifiePar : "Admin"); // Nom ou "Admin" par défaut

            // Stocker les valeurs au format JSON string : "valeur" (avec guillemets)
            // replace("\"", "'") : remplacer les guillemets doubles par simples pour éviter les conflits JSON
            ps.setString(5, ancienneValeur != null
                    ? "\"" + ancienneValeur.replace("\"", "'") + "\"" : null);
            ps.setString(6, nouvelleValeur != null
                    ? "\"" + nouvelleValeur.replace("\"", "'") + "\"" : null);
            ps.setString(7, champ != null ? "\"" + champ + "\"" : null); // ex: "\"statut\""
            ps.setString(8, at); // Horodatage

            ps.executeUpdate(); // Insérer l'entrée d'audit
            ps.close();

        } catch (Exception e) {
            // Ne pas planter l'application si l'audit échoue
            System.err.println("[Audit] Erreur log : " + e.getMessage());
        }
    }

    /**
     * Raccourci pour loguer une CRÉATION.
     * Appelle log() avec action="CRÉATION", ancienneValeur=null.
     *
     * @param entite     Type d'entité
     * @param entiteId   ID de l'enregistrement créé
     * @param resume     Résumé de ce qui a été créé (ex: "Ordonnance ORD-2026-1227")
     * @param modifiePar Nom de l'utilisateur
     */
    public void logCreation(String entite, String entiteId, String resume, String modifiePar) {
        // champ=null (pas de champ spécifique), ancienneValeur=null (rien avant), nouvelleValeur=resume
        log(entite, entiteId, "CRÉATION", null, null, resume, modifiePar);
    }

    /**
     * Raccourci pour loguer une SUPPRESSION.
     * Appelle log() avec action="SUPPRESSION", nouvelleValeur=null.
     *
     * @param entite     Type d'entité
     * @param entiteId   ID de l'enregistrement supprimé
     * @param resume     Résumé de ce qui a été supprimé
     * @param modifiePar Nom de l'utilisateur
     */
    public void logSuppression(String entite, String entiteId, String resume, String modifiePar) {
        // ancienneValeur=resume (ce qui existait avant), nouvelleValeur=null (plus rien après)
        log(entite, entiteId, "SUPPRESSION", null, resume, null, modifiePar);
    }

    /**
     * Logue une MODIFICATION seulement si la valeur a réellement changé.
     * Évite de créer des entrées d'audit inutiles si rien n'a changé.
     *
     * @param entite     Type d'entité
     * @param entiteId   ID de l'enregistrement modifié
     * @param champ      Nom du champ modifié (ex: "statut")
     * @param avant      Valeur avant modification
     * @param apres      Valeur après modification
     * @param modifiePar Nom de l'utilisateur
     */
    public void logSiModifie(String entite, String entiteId, String champ,
                              String avant, String apres, String modifiePar) {
        // Traiter null comme chaîne vide pour la comparaison
        if (avant == null) avant = "";
        if (apres == null) apres = "";

        // Ne loguer que si les valeurs sont différentes
        if (!avant.equals(apres)) {
            log(entite, entiteId, "MODIFICATION", champ, avant, apres, modifiePar);
        }
        // Si avant == apres → rien à loguer (pas de changement réel)
    }

    /**
     * Récupère l'historique complet d'un enregistrement depuis audit_log.
     * Trié du plus récent au plus ancien (ORDER BY id DESC).
     *
     * @param entite   Type d'entité (ex: "ordonnance")
     * @param entiteId ID de l'enregistrement (ex: "42")
     * @return Liste des entrées d'audit pour cet enregistrement
     */
    public List<AuditEntry> getHistorique(String entite, String entiteId) {
        List<AuditEntry> entries = new ArrayList<>();

        try {
            // Convertir entiteId en int
            int entityIdInt = 0;
            try { entityIdInt = Integer.parseInt(entiteId); } catch (Exception ignored) {}

            // Récupérer toutes les entrées pour cette entité et cet ID
            // ORDER BY id DESC : les plus récentes en premier
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
                // Nettoyer les guillemets JSON autour des valeurs stockées
                // Les valeurs sont stockées comme "\"valeur\"" → on retire les guillemets
                String champ  = clean(rs.getString("changed_fields")); // ex: "statut" → statut
                String avant  = clean(rs.getString("old_values"));     // ex: "\"en_attente\"" → en_attente
                String apres  = clean(rs.getString("new_values"));     // ex: "\"validée\"" → validée
                String modPar = rs.getString("user_name");
                String modAt  = rs.getString("created_at");

                // Créer l'objet AuditEntry et l'ajouter à la liste
                entries.add(new AuditEntry(
                    rs.getInt("id"),
                    rs.getString("entity_type"),
                    String.valueOf(rs.getInt("entity_id")),
                    rs.getString("action"),
                    champ, avant, apres,
                    modPar != null ? modPar : "Admin", // Valeur par défaut si null
                    modAt != null ? modAt : ""
                ));
            }
            rs.close(); ps.close();

        } catch (Exception e) {
            System.err.println("[Audit] Erreur getHistorique : " + e.getMessage());
        }
        return entries;
    }

    /**
     * Retire les guillemets JSON autour d'une valeur stockée.
     *
     * Les valeurs sont stockées comme : "\"valeur\""
     * Cette méthode les nettoie pour retourner juste : "valeur"
     *
     * Exemple : "\"en_attente\"" → "en_attente"
     *
     * @param val La valeur brute de la base de données
     * @return La valeur nettoyée, ou null si vide
     */
    private String clean(String val) {
        if (val == null) return null;
        val = val.trim(); // Supprimer les espaces en début/fin

        // Si la valeur commence ET finit par un guillemet → retirer les guillemets
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1)
            val = val.substring(1, val.length() - 1); // Retirer premier et dernier caractère

        return val.isBlank() ? null : val; // Retourner null si la valeur est vide après nettoyage
    }
}
