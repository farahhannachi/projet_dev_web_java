package org.example.util; // Package "util" — services transversaux (IA, API, outils)

import java.sql.*;        // Classes JDBC pour interroger la base de données
import java.util.HashMap; // Non utilisé directement ici mais importé
import java.util.Map;     // Non utilisé directement ici mais importé

/**
 * TreatmentSuggestionService — Service IA de suggestion de traitement.
 *
 * Rôle : suggérer automatiquement le dosage, la fréquence, le moment de prise
 * et la durée d'un médicament, en se basant sur :
 *   1. L'historique des traitements en base (apprentissage par les données réelles)
 *   2. Des règles médicales codées en dur (knowledge base) si pas d'historique
 *
 * C'est une forme simplifiée de Machine Learning : on apprend des prescriptions passées.
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class TreatmentSuggestionService {

    // Instance unique (Singleton)
    private static TreatmentSuggestionService instance;

    // Constructeur privé : interdit "new TreatmentSuggestionService()" depuis l'extérieur
    private TreatmentSuggestionService() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static TreatmentSuggestionService getInstance() {
        if (instance == null) instance = new TreatmentSuggestionService();
        return instance;
    }

    /**
     * Suggestion — Classe interne (DTO) qui représente une suggestion de traitement.
     *
     * DTO = Data Transfer Object : objet simple qui transporte des données
     * entre le service et le controller, sans logique métier.
     *
     * Les champs sont publics pour un accès direct (pas de getters/setters nécessaires).
     */
    public static class Suggestion {
        public String dosage;      // ex: "500mg", "1g", "400mg"
        public String frequence;   // ex: "2 fois par jour", "1 fois par jour"
        public String repas;       // ex: "Avant le repas", "Après le repas", "Pendant le repas"
        public int dureeJours;     // Nombre de jours de traitement (ex: 7, 14, 30)
        public String source;      // D'où vient la suggestion : "historique (N cas)" ou "règles médicales"

        // Constructeur : initialise tous les champs en une seule ligne
        public Suggestion(String dosage, String frequence, String repas, int dureeJours, String source) {
            this.dosage = dosage;
            this.frequence = frequence;
            this.repas = repas;
            this.dureeJours = dureeJours;
            this.source = source;
        }
    }

    /**
     * Point d'entrée principal : suggère un traitement pour un produit donné.
     *
     * Algorithme en 2 étapes :
     *   Étape 1 : chercher dans l'historique des traitements en base
     *   Étape 2 : si rien trouvé → appliquer les règles médicales par défaut
     *
     * @param produitId  ID du produit (médicament) dans la table "produit"
     * @param notes      Symptômes/antécédents saisis par le patient (texte libre)
     * @return Une Suggestion avec dosage, fréquence, repas, durée et source
     */
    public Suggestion suggerer(int produitId, String notes) {
        // Étape 1 : essayer de trouver une suggestion basée sur l'historique réel
        Suggestion fromHistory = chercherDansHistorique(produitId, notes);
        if (fromHistory != null) return fromHistory; // Si trouvé → retourner directement

        // Étape 2 : fallback sur les règles médicales codées en dur
        return reglesParDefaut(produitId, notes);
    }

    /**
     * Cherche la combinaison dosage/fréquence/repas/durée la plus prescrite
     * pour ce médicament dans l'historique des traitements en base.
     *
     * Requête SQL : GROUP BY pour regrouper les combinaisons identiques,
     * COUNT(*) pour compter combien de fois chaque combinaison a été prescrite,
     * ORDER BY nb DESC LIMIT 1 pour prendre la plus fréquente.
     *
     * C'est du "collaborative filtering" simplifié : on recommande ce qui a
     * le plus souvent été prescrit pour ce médicament.
     *
     * @param produitId ID du médicament
     * @param notes     Symptômes (non utilisés ici, réservé pour évolution future)
     * @return Une Suggestion basée sur l'historique, ou null si aucun historique
     */
    private Suggestion chercherDansHistorique(int produitId, String notes) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection(); // Obtenir la connexion JDBC

            // Requête d'agrégation :
            // - WHERE id_produit_id = ? : seulement pour ce médicament
            // - AND status IN ('actif', 'terminé') : seulement les traitements qui ont abouti
            // - AND dosage IS NOT NULL AND dosage != '' : ignorer les lignes sans dosage
            // - GROUP BY dosage, frequence, repas, duree_jours : regrouper les combinaisons identiques
            // - ORDER BY nb DESC LIMIT 1 : prendre la combinaison la plus fréquente
            PreparedStatement ps = conn.prepareStatement(
                "SELECT dosage, frequence, repas, duree_jours, COUNT(*) AS nb " +
                "FROM traitement " +
                "WHERE id_produit_id = ? AND status IN ('actif', 'terminé') " +
                "AND dosage IS NOT NULL AND dosage != '' " +
                "AND frequence IS NOT NULL AND frequence != '' " +
                "GROUP BY dosage, frequence, repas, duree_jours " +
                "ORDER BY nb DESC LIMIT 1"
            );
            ps.setInt(1, produitId); // Remplacer le "?" par l'ID du produit
            ResultSet rs = ps.executeQuery(); // Exécuter la requête

            if (rs.next()) { // Si au moins une combinaison a été trouvée
                Suggestion s = new Suggestion(
                    rs.getString("dosage"),    // Dosage le plus prescrit pour ce médicament
                    rs.getString("frequence"), // Fréquence la plus prescrite

                    // Si repas est null en base → valeur par défaut "Après le repas"
                    rs.getString("repas") != null ? rs.getString("repas") : "Après le repas",

                    // Si durée est 0 ou null → valeur par défaut 7 jours
                    rs.getInt("duree_jours") > 0 ? rs.getInt("duree_jours") : 7,

                    // Source : indique combien de cas similaires ont été trouvés
                    "historique (" + rs.getInt("nb") + " cas similaires)"
                );
                rs.close(); ps.close(); // Fermer les ressources JDBC
                return s;
            }
            rs.close(); ps.close();

        } catch (SQLException e) {
            // Log l'erreur mais ne plante pas l'application
            System.err.println("[TreatmentSuggestion] Erreur SQL : " + e.getMessage());
        }
        return null; // Aucun historique trouvé pour ce médicament
    }

    /**
     * Applique des règles médicales codées en dur pour suggérer un traitement.
     * Utilisé quand il n'y a pas d'historique en base.
     *
     * Les règles sont basées sur :
     *   - Le nom du médicament (famille thérapeutique)
     *   - Les symptômes du patient (ajustements contextuels)
     *
     * @param produitId ID du médicament (pour récupérer son nom)
     * @param notes     Symptômes/antécédents du patient
     * @return Une Suggestion basée sur les règles médicales
     */
    private Suggestion reglesParDefaut(int produitId, String notes) {
        // Mettre en minuscules pour les comparaisons insensibles à la casse
        String notesLower = notes != null ? notes.toLowerCase() : "";

        // Récupérer le nom du médicament depuis la base pour identifier sa famille
        String nomProduit = getNomProduit(produitId).toLowerCase();

        // Valeurs par défaut génériques (si aucune règle ne correspond)
        String dosage = "500mg";
        String frequence = "2 fois par jour";
        String repas = "Après le repas";
        int duree = 7;

        // ── Règles par famille de médicament ──────────────────────────────

        // Antalgiques / antipyrétiques (contre la douleur et la fièvre)
        if (nomProduit.contains("paracetamol") || nomProduit.contains("panadol") || nomProduit.contains("doliprane")) {
            dosage = "500mg";
            frequence = "3 fois par jour"; // Toutes les 8h maximum
            repas = "Pendant le repas";    // Protège l'estomac
            duree = 5;                     // Traitement court pour douleur aiguë
        }
        // Anti-inflammatoires non stéroïdiens (AINS)
        else if (nomProduit.contains("ibuprofene") || nomProduit.contains("ibuprofen") || nomProduit.contains("advil")) {
            dosage = "400mg";
            frequence = "3 fois par jour";
            repas = "Pendant le repas"; // Obligatoire pour protéger l'estomac
            duree = 5;
            // Ajustement contextuel : si le patient a des problèmes gastriques → après le repas
            if (notesLower.contains("allergi") || notesLower.contains("gastrite")) {
                repas = "Après le repas"; // Plus de protection gastrique
            }
        }
        // Antibiotiques — amoxicilline (pénicilline)
        else if (nomProduit.contains("amoxicilline") || nomProduit.contains("augmentin")) {
            dosage = "1g";              // Dose adulte standard
            frequence = "2 fois par jour"; // Toutes les 12h pour maintenir le taux sanguin
            repas = "Pendant le repas"; // Réduit les effets digestifs
            duree = 7;                  // Durée minimale pour éviter les résistances
        }
        // Antibiotiques — azithromycine (macrolide, traitement court)
        else if (nomProduit.contains("azithromycine") || nomProduit.contains("zithromax")) {
            dosage = "500mg";
            frequence = "1 fois par jour"; // Demi-vie longue → 1 prise/jour suffit
            repas = "En dehors des repas"; // L'alimentation réduit l'absorption
            duree = 3;                     // Traitement très court (3 jours suffisent)
        }
        // Antihistaminiques (contre les allergies)
        else if (nomProduit.contains("cetirizine") || nomProduit.contains("loratadine") || nomProduit.contains("zyrtec")) {
            dosage = "10mg";
            frequence = "1 fois par jour"; // Effet longue durée (24h)
            repas = "En dehors des repas"; // Absorption non affectée par les repas
            duree = 14;                    // Traitement plus long pour les allergies
        }
        // Inhibiteurs de la pompe à protons (contre les ulcères/reflux)
        else if (nomProduit.contains("omeprazole") || nomProduit.contains("pantoprazole")) {
            dosage = "20mg";
            frequence = "1 fois par jour";
            repas = "Avant le repas"; // Doit être pris 30min avant le repas pour être efficace
            duree = 14;               // Traitement de 2 semaines minimum
        }
        // Aspirine (antalgique, antipyrétique, antiagrégant plaquettaire)
        else if (nomProduit.contains("aspirine") || nomProduit.contains("aspegic")) {
            dosage = "500mg";
            frequence = "3 fois par jour";
            repas = "Après le repas"; // Obligatoire : l'aspirine irrite l'estomac
            duree = 5;
        }

        // ── Ajustements selon les symptômes du patient ────────────────────

        // Si le patient mentionne une maladie chronique → traitement long
        if (notesLower.contains("chronique") || notesLower.contains("longue durée")) duree = 30;
        // Si le patient décrit une urgence → traitement court et intensif
        else if (notesLower.contains("aigu") || notesLower.contains("urgent")) duree = 3;

        // Retourner la suggestion avec la source "règles médicales par défaut"
        return new Suggestion(dosage, frequence, repas, duree, "règles médicales par défaut");
    }

    /**
     * Récupère le nom d'un produit depuis la base de données par son ID.
     * Méthode utilitaire privée utilisée par reglesParDefaut().
     *
     * @param produitId L'ID du produit dans la table "produit"
     * @return Le nom du produit (ex: "Doliprane 1000mg"), ou "" si non trouvé
     */
    private String getNomProduit(int produitId) {
        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            // Requête simple : SELECT nom FROM produit WHERE id_produit = ?
            PreparedStatement ps = conn.prepareStatement("SELECT nom FROM produit WHERE id_produit = ?");
            ps.setInt(1, produitId); // Paramètre : l'ID du produit
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String nom = rs.getString("nom"); // Lire le nom
                rs.close(); ps.close();
                return nom; // Retourner le nom trouvé
            }
            rs.close(); ps.close();

        } catch (SQLException e) {
            System.err.println("[TreatmentSuggestion] Erreur getNom : " + e.getMessage());
        }
        return ""; // Retourner chaîne vide si non trouvé ou erreur
    }
}
