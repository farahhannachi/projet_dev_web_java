package org.example.service;

import org.example.model.StockMovement;
import org.example.model.Stock;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service métier pour la gestion des mouvements de stock
 * Gère la consommation de stock par les services (médecins/infirmiers)
 *
 * Responsabilités :
 * - Enregistrer la consommation de stock
 * - Décrémenter automatiquement la quantité
 * - Générer une traçabilité complète
 * - Vérifier les seuils d'alerte
 */
public class ServiceConsommationService {
    private static ServiceConsommationService instance;
    private final StockService stockService = StockService.getInstance();

    private ServiceConsommationService() {}

    public static ServiceConsommationService getInstance() {
        if (instance == null) {
            instance = new ServiceConsommationService();
        }
        return instance;
    }

    /**
     * Enregistre une consommation de stock par un service
     *
     * @param idService ID du service (médecin/infirmier)
     * @param idStock ID du stock consommé
     * @param quantiteConsommee Quantité consommée
     * @param motif Raison de la consommation
     * @param referenceDocument Numéro d'ordonnance/document (optionnel)
     * @return ID du mouvement créé
     */
    public int enregistrerConsommation(int idService, int idStock, int quantiteConsommee,
                                       String motif, String referenceDocument) {

        // Vérifications préalables
        if (quantiteConsommee <= 0) {
            throw new RuntimeException("La quantité consommée doit être positive");
        }

        // Récupérer le stock actuel
        Stock stock = stockService.getById(idStock);
        if (stock == null) {
            throw new RuntimeException("Stock non trouvé avec l'ID: " + idStock);
        }

        // Vérifier la disponibilité
        if (stock.getQuantite() < quantiteConsommee) {
            throw new RuntimeException(
                "Stock insuffisant. Disponible: " + stock.getQuantite() +
                ", Demandé: " + quantiteConsommee
            );
        }

        int quantiteAvant = stock.getQuantite();
        int quantiteApres = quantiteAvant - quantiteConsommee;

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Enregistrer le mouvement de stock
                int idMouvement = enregistrerMouvement(conn, idService, idStock,
                    quantiteConsommee, quantiteAvant, quantiteApres, motif, referenceDocument);

                // 2. Mettre à jour la quantité en stock
                stock.setQuantite(quantiteApres);
                stock.setDerniereSortie(LocalDateTime.now());
                mettreAJourStock(conn, stock);

                // 3. Valider la transaction
                conn.commit();

                System.out.println("✅ Consommation enregistrée: " + quantiteConsommee +
                                 " unités, Mouvement ID: " + idMouvement);
                return idMouvement;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            Logger logger = Logger.getLogger(ServiceConsommationService.class.getName());
            logger.log(Level.SEVERE, "Erreur lors de l'enregistrement de la consommation", e);
            throw new RuntimeException("Erreur lors de l'enregistrement de la consommation", e);
        }
    }

    /**
     * Enregistre un mouvement de stock en base de données
     */
    private int enregistrerMouvement(Connection conn, int idService, int idStock,
                                      int quantite, int quantiteAvant, int quantiteApres,
                                      String motif, String referenceDocument) throws SQLException {

        String sql = "INSERT INTO stock_movement " +
            "(id_stock, id_service, type, type_consommation, quantite, quantite_before, " +
            "quantite_after, status, motif, reference_document, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idStock);
            stmt.setInt(2, idService);
            stmt.setString(3, "SORTIE");
            stmt.setString(4, "CONSOMMATION_SERVICE");
            stmt.setInt(5, quantite);
            stmt.setInt(6, quantiteAvant);
            stmt.setInt(7, quantiteApres);
            stmt.setString(8, "APPROUVEE");
            stmt.setString(9, motif);
            stmt.setString(10, referenceDocument);
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Met à jour le stock en base de données
     */
    private void mettreAJourStock(Connection conn, Stock stock) throws SQLException {
        String sql = "UPDATE stock SET quantite = ?, date_derniere_mise_a_jour = ?, " +
                     "derniere_sortie = ?, total_sorties = total_sorties + 1 WHERE id_stock = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, stock.getQuantite());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(3, Timestamp.valueOf(stock.getDerniereSortie()));
            stmt.setInt(4, stock.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Récupère l'historique de consommation d'un service
     */
    public List<StockMovement> getHistoriqueService(int idService) {
        List<StockMovement> mouvements = new ArrayList<>();
        String sql = "SELECT m.*, p.nom as produit_nom, d.nom_depot, s.nom_service " +
                    "FROM stock_movement m " +
                    "LEFT JOIN stock st ON m.id_stock = st.id_stock " +
                    "LEFT JOIN produit p ON st.produit_id = p.id_produit " +
                    "LEFT JOIN depot d ON st.depot_id = d.id_depot " +
                    "LEFT JOIN service s ON m.id_service = s.id_service " +
                    "WHERE m.id_service = ? " +
                    "ORDER BY m.created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idService);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovement mouvement = mapResultSetToMovement(rs);
                    mouvements.add(mouvement);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
        }

        return mouvements;
    }

    /**
     * Récupère l'historique de consommation d'un stock
     */
    public List<StockMovement> getHistoriqueStock(int idStock) {
        List<StockMovement> mouvements = new ArrayList<>();
        String sql = "SELECT m.*, s.nom_service FROM stock_movement m " +
                    "LEFT JOIN service s ON m.id_service = s.id_service " +
                    "WHERE m.id_stock = ? ORDER BY m.created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idStock);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovement mouvement = mapResultSetToMovement(rs);
                    mouvements.add(mouvement);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
        }

        return mouvements;
    }

    /**
     * Récupère les mouvements récents (derniers 30 jours)
     */
    public List<StockMovement> getMouvementsRecents() {
        List<StockMovement> mouvements = new ArrayList<>();
        String sql = "SELECT m.*, p.nom as produit_nom, d.nom_depot, s.nom_service " +
                    "FROM stock_movement m " +
                    "LEFT JOIN stock st ON m.id_stock = st.id_stock " +
                    "LEFT JOIN produit p ON st.produit_id = p.id_produit " +
                    "LEFT JOIN depot d ON st.depot_id = d.id_depot " +
                    "LEFT JOIN service s ON m.id_service = s.id_service " +
                    "WHERE m.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "ORDER BY m.created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                StockMovement mouvement = mapResultSetToMovement(rs);
                mouvements.add(mouvement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des mouvements: " + e.getMessage());
        }

        return mouvements;
    }

    /**
     * Mappe un ResultSet à un objet StockMovement
     */
    private StockMovement mapResultSetToMovement(ResultSet rs) throws SQLException {
        StockMovement mouvement = new StockMovement();
        mouvement.setId(rs.getInt("id"));
        mouvement.setIdStock(rs.getInt("id_stock"));

        if (rs.getObject("id_service") != null) {
            mouvement.setIdService(rs.getInt("id_service"));
        }

        mouvement.setType(rs.getString("type"));

        if (rs.getObject("type_consommation") != null) {
            mouvement.setTypeConsommation(rs.getString("type_consommation"));
        }

        mouvement.setQuantite(rs.getInt("quantite"));
        mouvement.setQuantiteAvant(rs.getInt("quantite_before"));
        mouvement.setQuantiteApres(rs.getInt("quantite_after"));
        mouvement.setStatus(rs.getString("status"));
        mouvement.setMotif(rs.getString("motif"));

        if (rs.getObject("reference_document") != null) {
            mouvement.setReferenceDocument(rs.getString("reference_document"));
        }

        mouvement.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return mouvement;
    }
}
