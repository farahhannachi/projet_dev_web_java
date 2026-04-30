package org.example.service; // Package "service" — couche d'accès aux données pour les traitements

import org.example.model.Traitement;  // Modèle Traitement (POJO)
import org.example.util.DatabaseUtil; // Singleton de connexion JDBC

import java.sql.*;                    // Classes JDBC : Connection, PreparedStatement, ResultSet...
import java.util.ArrayList;           // Liste dynamique
import java.util.List;                // Interface List
import java.util.stream.Collectors;   // Pour .collect() dans search()

/**
 * TraitementService — Couche Service (CRUD) pour la table "traitement".
 *
 * Un traitement représente la prescription d'un médicament (produit) à un patient,
 * dans le cadre d'une ordonnance. Il contient : dosage, fréquence, durée, repas, statut.
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class TraitementService {

    // Instance unique (Singleton)
    private static TraitementService instance;

    // Constructeur privé : interdit "new TraitementService()" depuis l'extérieur
    private TraitementService() {
    }

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static TraitementService getInstance() {
        if (instance == null) {
            instance = new TraitementService();
        }
        return instance;
    }

    /**
     * Insère un nouveau traitement en base de données.
     * Après l'insertion, récupère l'ID auto-généré et le stocke dans l'objet.
     *
     * @param traitement L'objet Traitement à insérer
     * @return true si l'insertion a réussi
     */
    public boolean add(Traitement traitement) {
        // 11 colonnes à insérer → 11 paramètres "?"
        String sql = "INSERT INTO traitement (id_utilisateur_id, dosage, frequence, duree_jours, " +
                     "date_debut, date_fin, status, notes, id_ordonnance_id, id_produit_id, repas) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             // RETURN_GENERATED_KEYS : récupérer l'id_traitement auto-incrémenté après INSERT
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, traitement.getIdUtilisateurId());  // Clé étrangère → utilisateur (patient)
            stmt.setString(2, traitement.getDosage());         // ex: "500mg", "1g"
            stmt.setString(3, traitement.getFrequence());      // ex: "2 fois par jour"
            stmt.setInt(4, traitement.getDureeJours());        // ex: 7 (jours)

            // Conversion LocalDateTime → Timestamp SQL (null-safe)
            stmt.setTimestamp(5, traitement.getDateDebut() != null
                    ? Timestamp.valueOf(traitement.getDateDebut()) : null);
            stmt.setTimestamp(6, traitement.getDateFin() != null
                    ? Timestamp.valueOf(traitement.getDateFin()) : null);

            stmt.setString(7, traitement.getStatus());         // "actif", "terminé", "annulé", "en_attente"
            stmt.setString(8, traitement.getNotes());          // Notes libres du médecin/pharmacien
            stmt.setInt(9, traitement.getIdOrdonnanceId());    // Clé étrangère → ordonnance parente
            stmt.setInt(10, traitement.getIdProduitId());      // Clé étrangère → produit (médicament)
            stmt.setString(11, traitement.getRepas());         // "Avant le repas", "Après le repas"...

            stmt.executeUpdate(); // Exécuter l'INSERT

            // Récupérer l'ID auto-généré par MySQL
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    traitement.setIdTraitement(rs.getInt(1)); // Stocker l'ID dans l'objet Java
                }
            }
            return true;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du traitement", e);
        }
    }

    /**
     * Met à jour un traitement existant.
     * Cible la ligne via id_traitement (position 12 dans le WHERE).
     *
     * @param traitement L'objet avec les nouvelles valeurs (doit avoir un id valide)
     * @return true si au moins une ligne a été modifiée
     */
    public boolean update(Traitement traitement) {
        // UPDATE : 11 champs à modifier + 1 condition WHERE (id_traitement)
        String sql = "UPDATE traitement SET id_utilisateur_id=?, dosage=?, frequence=?, duree_jours=?, " +
                     "date_debut=?, date_fin=?, status=?, notes=?, id_ordonnance_id=?, id_produit_id=?, repas=? " +
                     "WHERE id_traitement=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Positions 1 à 11 : nouvelles valeurs
            stmt.setInt(1, traitement.getIdUtilisateurId());
            stmt.setString(2, traitement.getDosage());
            stmt.setString(3, traitement.getFrequence());
            stmt.setInt(4, traitement.getDureeJours());
            stmt.setTimestamp(5, traitement.getDateDebut() != null
                    ? Timestamp.valueOf(traitement.getDateDebut()) : null);
            stmt.setTimestamp(6, traitement.getDateFin() != null
                    ? Timestamp.valueOf(traitement.getDateFin()) : null);
            stmt.setString(7, traitement.getStatus());
            stmt.setString(8, traitement.getNotes());
            stmt.setInt(9, traitement.getIdOrdonnanceId());
            stmt.setInt(10, traitement.getIdProduitId());
            stmt.setString(11, traitement.getRepas());

            // Position 12 : condition WHERE — l'ID du traitement à modifier
            stmt.setInt(12, traitement.getIdTraitement());

            return stmt.executeUpdate() > 0; // true si une ligne a été modifiée

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du traitement", e);
        }
    }

    /**
     * Supprime un traitement par son ID.
     *
     * @param id L'identifiant du traitement à supprimer
     */
    public void delete(int id) {
        String sql = "DELETE FROM traitement WHERE id_traitement=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);   // Cibler la ligne avec cet ID
            stmt.executeUpdate(); // Exécuter la suppression

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du traitement", e);
        }
    }

    /**
     * Récupère tous les traitements, triés du plus récent au plus ancien (par ID DESC).
     *
     * @return Liste de tous les traitements (vide si aucun ou erreur)
     */
    public List<Traitement> getAll() {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement ORDER BY id_traitement DESC"; // Plus récent en premier

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                traitements.add(mapResultSet(rs)); // Convertir chaque ligne en objet Java
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des traitements: " + e.getMessage());
            return new ArrayList<>(); // Retourner liste vide en cas d'erreur (pas de crash)
        }
        return traitements;
    }

    /**
     * Recherche des traitements par mot-clé (dosage, statut ou notes).
     * Filtre en mémoire sur la liste complète.
     *
     * @param query Le texte à rechercher (insensible à la casse)
     * @return Liste filtrée
     */
    public List<Traitement> search(String query) {
        return getAll().stream()
                .filter(t ->
                    // Chercher dans le dosage (ex: "500mg")
                    (t.getDosage() != null && t.getDosage().toLowerCase().contains(query.toLowerCase())) ||
                    // OU dans le statut (ex: "actif")
                    (t.getStatus() != null && t.getStatus().toLowerCase().contains(query.toLowerCase())) ||
                    // OU dans les notes libres
                    (t.getNotes() != null && t.getNotes().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    /**
     * Récupère un traitement par son ID.
     *
     * @param id L'identifiant du traitement
     * @return L'objet Traitement, ou null si non trouvé
     */
    public Traitement getById(int id) {
        String sql = "SELECT * FROM traitement WHERE id_traitement=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs); // Convertir et retourner
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du traitement", e);
        }
        return null; // Non trouvé
    }

    /**
     * Récupère tous les traitements liés à une ordonnance spécifique.
     * Utilisé pour afficher les médicaments d'une ordonnance dans la vue "Mes Ordonnances".
     *
     * @param ordonnanceId L'ID de l'ordonnance parente
     * @return Liste des traitements de cette ordonnance
     */
    public List<Traitement> getByOrdonnanceId(int ordonnanceId) {
        List<Traitement> traitements = new ArrayList<>();
        // Filtre par clé étrangère id_ordonnance_id
        String sql = "SELECT * FROM traitement WHERE id_ordonnance_id=?";

        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ordonnanceId); // L'ID de l'ordonnance parente

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    traitements.add(mapResultSet(rs)); // Ajouter chaque traitement à la liste
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des traitements par ordonnance", e);
        }
        return traitements;
    }

    /**
     * Convertit une ligne du ResultSet SQL en objet Java Traitement.
     * Méthode privée réutilisée par getAll(), getById(), getByOrdonnanceId().
     *
     * @param rs Le curseur positionné sur la ligne à lire
     * @return Un objet Traitement rempli
     */
    private Traitement mapResultSet(ResultSet rs) throws SQLException {
        Traitement t = new Traitement(); // Objet vide à remplir

        t.setIdTraitement(rs.getInt("id_traitement"));         // Clé primaire
        t.setIdUtilisateurId(rs.getInt("id_utilisateur_id"));  // Clé étrangère → patient
        t.setDosage(rs.getString("dosage"));                   // ex: "500mg"
        t.setFrequence(rs.getString("frequence"));             // ex: "3 fois par jour"
        t.setDureeJours(rs.getInt("duree_jours"));             // ex: 7

        // Conversion Timestamp SQL → LocalDateTime Java (null-safe)
        t.setDateDebut(rs.getTimestamp("date_debut") != null
                ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
        t.setDateFin(rs.getTimestamp("date_fin") != null
                ? rs.getTimestamp("date_fin").toLocalDateTime() : null);

        t.setStatus(rs.getString("status"));                   // "actif", "terminé", "annulé"...
        t.setNotes(rs.getString("notes"));                     // Notes libres
        t.setIdOrdonnanceId(rs.getInt("id_ordonnance_id"));    // Clé étrangère → ordonnance
        t.setIdProduitId(rs.getInt("id_produit_id"));          // Clé étrangère → produit/médicament
        t.setRepas(rs.getString("repas"));                     // "Avant le repas", "Après le repas"...

        return t;
    }
}
