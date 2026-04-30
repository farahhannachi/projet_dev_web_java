package org.example.service; // Ce fichier appartient au package "service" — couche d'accès aux données

import org.example.model.Ordonnance;   // Modèle Ordonnance (POJO avec getters/setters)
import org.example.util.DatabaseUtil;  // Utilitaire Singleton pour obtenir la connexion JDBC

import java.sql.*;                     // Toutes les classes JDBC : Connection, PreparedStatement, ResultSet...
import java.util.ArrayList;            // Liste dynamique pour stocker les résultats
import java.util.List;                 // Interface List (type de retour)
import java.util.stream.Collectors;    // Utilisé pour .collect() dans la méthode search()

/**
 * OrdonnanceService — Couche Service (CRUD) pour la table "ordonnance".
 *
 * Rôle : faire le lien entre les controllers (UI) et la base de données.
 * Chaque méthode correspond à une opération SQL : INSERT, UPDATE, DELETE, SELECT.
 *
 * Pattern Singleton : une seule instance partagée dans toute l'application.
 * Pourquoi ? Éviter de créer plusieurs objets inutiles et centraliser l'accès aux données.
 */
public class OrdonnanceService {

    // Instance unique de la classe (pattern Singleton)
    private static OrdonnanceService instance;

    // Constructeur privé : empêche d'écrire "new OrdonnanceService()" depuis l'extérieur
    private OrdonnanceService() {
    }

    /**
     * Retourne l'instance unique du service.
     * Si elle n'existe pas encore, on la crée (lazy initialization = création à la demande).
     */
    public static OrdonnanceService getInstance() {
        if (instance == null) {
            instance = new OrdonnanceService(); // Première utilisation : on crée l'instance
        }
        return instance; // Toujours retourner la même instance
    }

    /**
     * Ajoute une nouvelle ordonnance en base de données.
     * Après l'insertion, récupère l'ID auto-généré et le stocke dans l'objet.
     *
     * @param ordonnance L'objet Ordonnance à insérer
     * @return true si l'insertion a réussi
     */
    public boolean add(Ordonnance ordonnance) {
        // Requête SQL paramétrée avec des "?" pour éviter les injections SQL
        String sql = "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) VALUES (?, ?, ?, ?, ?, ?)";

        // try-with-resources : ferme automatiquement conn et stmt même en cas d'erreur
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             // RETURN_GENERATED_KEYS : demande à JDBC de retourner l'ID auto-incrémenté après INSERT
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Remplir les "?" dans l'ordre : position 1 = premier ?, position 2 = deuxième ?, etc.
            stmt.setString(1, ordonnance.getNumeroOrdonnance());  // ex: "ORD-2026-1227"

            // Timestamp.valueOf() convertit LocalDateTime Java → Timestamp SQL
            // Si la date est null, on insère NULL en base (pas de crash)
            stmt.setTimestamp(2, ordonnance.getDateOrdonnance() != null
                    ? Timestamp.valueOf(ordonnance.getDateOrdonnance()) : null);
            stmt.setTimestamp(3, ordonnance.getDateExpiration() != null
                    ? Timestamp.valueOf(ordonnance.getDateExpiration()) : null);

            stmt.setString(4, ordonnance.getStatut());       // ex: "en_attente", "validée"
            stmt.setString(5, ordonnance.getNoteMedical());  // message optionnel pour le pharmacien
            stmt.setInt(6, ordonnance.getIdUtilisateurId()); // clé étrangère vers la table utilisateur

            stmt.executeUpdate(); // Exécute le INSERT en base

            // Récupérer l'ID auto-généré par MySQL (AUTO_INCREMENT)
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ordonnance.setIdOrdonnance(rs.getInt(1)); // Stocker l'ID dans l'objet Java
                }
            }
            return true; // Insertion réussie

        } catch (SQLException e) {
            // On relance l'exception en RuntimeException pour ne pas forcer les appelants à la gérer
            throw new RuntimeException("Erreur lors de l'ajout de l'ordonnance", e);
        }
    }

    /**
     * Met à jour une ordonnance existante en base.
     * Identifie la ligne à modifier via son id_ordonnance (WHERE id_ordonnance=?).
     *
     * @param ordonnance L'objet avec les nouvelles valeurs (doit avoir un id valide)
     * @return true si au moins une ligne a été modifiée
     */
    public boolean update(Ordonnance ordonnance) {
        // UPDATE : on met à jour tous les champs, on cible la ligne par son ID
        String sql = "UPDATE ordonnance SET numero_ordonnance=?, date_ordonnance=?, date_expiration=?, statut=?, note_medical=?, id_utilisateur_id=? WHERE id_ordonnance=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Remplir les nouveaux champs (positions 1 à 6)
            stmt.setString(1, ordonnance.getNumeroOrdonnance());
            stmt.setTimestamp(2, ordonnance.getDateOrdonnance() != null
                    ? Timestamp.valueOf(ordonnance.getDateOrdonnance()) : null);
            stmt.setTimestamp(3, ordonnance.getDateExpiration() != null
                    ? Timestamp.valueOf(ordonnance.getDateExpiration()) : null);
            stmt.setString(4, ordonnance.getStatut());
            stmt.setString(5, ordonnance.getNoteMedical());
            stmt.setInt(6, ordonnance.getIdUtilisateurId());

            // Position 7 = la condition WHERE : l'ID de l'ordonnance à modifier
            stmt.setInt(7, ordonnance.getIdOrdonnance());

            // executeUpdate() retourne le nombre de lignes affectées
            // > 0 signifie qu'au moins une ligne a été modifiée
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de l'ordonnance", e);
        }
    }

    /**
     * Supprime une ordonnance par son ID.
     *
     * @param id L'identifiant de l'ordonnance à supprimer
     */
    public void delete(int id) {
        String sql = "DELETE FROM ordonnance WHERE id_ordonnance=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id); // Cibler la ligne avec cet ID
            stmt.executeUpdate(); // Exécuter la suppression

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'ordonnance", e);
        }
    }

    /**
     * Récupère toutes les ordonnances, triées de la plus récente à la plus ancienne.
     *
     * @return Liste de toutes les ordonnances (vide si aucune ou erreur)
     */
    public List<Ordonnance> getAll() {
        List<Ordonnance> ordonnances = new ArrayList<>(); // Liste qui va accumuler les résultats

        // ORDER BY date_ordonnance DESC : les plus récentes en premier
        String sql = "SELECT * FROM ordonnance ORDER BY date_ordonnance DESC";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             Statement stmt = conn.createStatement();       // Statement simple (pas de paramètres)
             ResultSet rs = stmt.executeQuery(sql)) {       // Exécute le SELECT, retourne un curseur

            // rs.next() avance le curseur ligne par ligne
            while (rs.next()) {
                ordonnances.add(mapResultSet(rs)); // Convertir chaque ligne SQL en objet Java
            }

        } catch (SQLException e) {
            // On log l'erreur mais on retourne une liste vide (pas de crash de l'UI)
            System.err.println("Erreur lors de la récupération des ordonnances: " + e.getMessage());
            return new ArrayList<>();
        }
        return ordonnances;
    }

    /**
     * Recherche des ordonnances par mot-clé (numéro, statut ou note médicale).
     * Filtre en mémoire sur la liste complète (pas de requête SQL avec LIKE).
     *
     * @param query Le texte à rechercher (insensible à la casse)
     * @return Liste filtrée des ordonnances correspondantes
     */
    public List<Ordonnance> search(String query) {
        return getAll().stream() // Récupère toutes les ordonnances et crée un flux (Stream)
                .filter(o ->
                    // Vérifie si le numéro contient le mot-clé
                    (o.getNumeroOrdonnance() != null && o.getNumeroOrdonnance().toLowerCase().contains(query.toLowerCase())) ||
                    // OU si le statut contient le mot-clé
                    (o.getStatut() != null && o.getStatut().toLowerCase().contains(query.toLowerCase())) ||
                    // OU si la note médicale contient le mot-clé
                    (o.getNoteMedical() != null && o.getNoteMedical().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(Collectors.toList()); // Convertit le Stream filtré en List
    }

    /**
     * Récupère une ordonnance par son ID.
     *
     * @param id L'identifiant de l'ordonnance
     * @return L'objet Ordonnance, ou null si non trouvé
     */
    public Ordonnance getById(int id) {
        String sql = "SELECT * FROM ordonnance WHERE id_ordonnance=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id); // Paramètre : l'ID recherché

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {          // Si une ligne est trouvée
                    return mapResultSet(rs); // La convertir en objet Java et retourner
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de l'ordonnance", e);
        }
        return null; // Aucune ordonnance trouvée avec cet ID
    }

    /**
     * Convertit une ligne du ResultSet SQL en objet Java Ordonnance.
     * Méthode privée utilisée par getAll() et getById().
     *
     * rs.getTimestamp() retourne null si la colonne est NULL en base.
     * .toLocalDateTime() convertit Timestamp SQL → LocalDateTime Java.
     *
     * @param rs Le curseur positionné sur la ligne à lire
     * @return Un objet Ordonnance rempli avec les données de la ligne
     */
    private Ordonnance mapResultSet(ResultSet rs) throws SQLException {
        Ordonnance o = new Ordonnance(); // Créer un objet vide

        o.setIdOrdonnance(rs.getInt("id_ordonnance"));           // Clé primaire
        o.setNumeroOrdonnance(rs.getString("numero_ordonnance")); // ex: "ORD-2026-1227"

        // Conversion Timestamp → LocalDateTime avec protection null
        o.setDateOrdonnance(rs.getTimestamp("date_ordonnance") != null
                ? rs.getTimestamp("date_ordonnance").toLocalDateTime() : null);
        o.setDateExpiration(rs.getTimestamp("date_expiration") != null
                ? rs.getTimestamp("date_expiration").toLocalDateTime() : null);

        o.setStatut(rs.getString("statut"));           // "en_attente", "validée", "expirée"...
        o.setNoteMedical(rs.getString("note_medical")); // Message optionnel du patient
        o.setIdUtilisateurId(rs.getInt("id_utilisateur_id")); // Clé étrangère → utilisateur

        return o; // Retourner l'objet rempli
    }
}
