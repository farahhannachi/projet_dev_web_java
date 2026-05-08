package org.example.service;

import org.example.model.Commande;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandeService {

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Insère la commande en base et retourne l'id généré (id_commande).
     */
    public int add(Commande commande) {
        String sql = """
                INSERT INTO commande
                  (id_utilisateur_id, date_commande, statut, total,
                   mode_paiement, adresse_livraison, telephone, nom, email,
                   message, produits_ids, coupon_code, coupon_discount,
                   estimated_delivery_date, fraud_score, base_shipping_cost)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // id_utilisateur_id (nullable)
            if (commande.getUtilisateurId() != null) {
                ps.setInt(1, commande.getUtilisateurId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            // date_commande
            LocalDateTime dt = commande.getDateCommandeDateTime();
            ps.setTimestamp(2, dt != null ? Timestamp.valueOf(dt) : Timestamp.valueOf(LocalDateTime.now()));

            ps.setString(3, commande.getStatut() != null ? commande.getStatut() : "en_attente");
            ps.setDouble(4, commande.getTotal());
            ps.setString(5, commande.getModePaiement());
            ps.setString(6, commande.getAdresseLivraison());
            ps.setString(7, commande.getTelephone());
            ps.setString(8, commande.getNom());
            ps.setString(9, commande.getEmail());
            ps.setString(10, commande.getMessage());
            ps.setString(11, commande.getProduitsIds());
            ps.setString(12, commande.getCouponCode());
            ps.setDouble(13, commande.getCouponDiscount());

            // estimated_delivery_date (nullable)
            LocalDateTime edd = commande.getEstimatedDeliveryDate();
            if (edd != null) {
                ps.setTimestamp(14, Timestamp.valueOf(edd));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }

            ps.setInt(15, commande.getFraudScore());
            ps.setDouble(16, commande.getBaseShippingCost());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    commande.setId(generatedId);
                    return generatedId;
                }
            }

        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur add(): " + e.getMessage());
            throw new RuntimeException("Impossible d'enregistrer la commande : " + e.getMessage(), e);
        }

        return -1;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public void update(Commande commande) {
        String sql = """
                UPDATE commande SET
                  id_utilisateur_id = ?, date_commande = ?, statut = ?, total = ?,
                  mode_paiement = ?, adresse_livraison = ?, telephone = ?, nom = ?,
                  email = ?, message = ?, produits_ids = ?, coupon_code = ?,
                  coupon_discount = ?, estimated_delivery_date = ?,
                  fraud_score = ?, base_shipping_cost = ?
                WHERE id_commande = ?
                """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (commande.getUtilisateurId() != null) {
                ps.setInt(1, commande.getUtilisateurId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            LocalDateTime dt = commande.getDateCommandeDateTime();
            ps.setTimestamp(2, dt != null ? Timestamp.valueOf(dt) : Timestamp.valueOf(LocalDateTime.now()));

            ps.setString(3, commande.getStatut());
            ps.setDouble(4, commande.getTotal());
            ps.setString(5, commande.getModePaiement());
            ps.setString(6, commande.getAdresseLivraison());
            ps.setString(7, commande.getTelephone());
            ps.setString(8, commande.getNom());
            ps.setString(9, commande.getEmail());
            ps.setString(10, commande.getMessage());
            ps.setString(11, commande.getProduitsIds());
            ps.setString(12, commande.getCouponCode());
            ps.setDouble(13, commande.getCouponDiscount());

            LocalDateTime edd = commande.getEstimatedDeliveryDate();
            if (edd != null) {
                ps.setTimestamp(14, Timestamp.valueOf(edd));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }

            ps.setInt(15, commande.getFraudScore());
            ps.setDouble(16, commande.getBaseShippingCost());
            ps.setInt(17, commande.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur update(): " + e.getMessage());
            throw new RuntimeException("Impossible de mettre à jour la commande : " + e.getMessage(), e);
        }
    }

    public boolean updateStatusWithBusinessRules(int id, String newStatus) {
        String sql = "UPDATE commande SET statut = ? WHERE id_commande = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur updateStatus(): " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public void delete(int id) {
        String sql = "DELETE FROM commande WHERE id_commande = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur delete(): " + e.getMessage());
            throw new RuntimeException("Impossible de supprimer la commande : " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<Commande> getAll() {
        String sql = "SELECT * FROM commande ORDER BY date_commande DESC";
        List<Commande> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur getAll(): " + e.getMessage());
        }
        return list;
    }

    public Commande getById(int id) {
        String sql = "SELECT * FROM commande WHERE id_commande = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur getById(): " + e.getMessage());
        }
        return null;
    }

    public List<Commande> search(String query) {
        return getAll().stream()
                .filter(c -> String.valueOf(c.getId()).contains(query) ||
                        (c.getNom() != null && c.getNom().toLowerCase().contains(query.toLowerCase())) ||
                        (c.getStatut() != null && c.getStatut().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Mapping ResultSet → Commande
    // -------------------------------------------------------------------------

    private Commande map(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id_commande"));

        int uid = rs.getInt("id_utilisateur_id");
        c.setUtilisateurId(rs.wasNull() ? null : uid);

        Timestamp dt = rs.getTimestamp("date_commande");
        if (dt != null) {
            c.setDateCommandeDateTime(dt.toLocalDateTime());
            c.setDateCommande(dt.toLocalDateTime().toLocalDate());
        }

        c.setStatut(rs.getString("statut"));
        c.setTotal(rs.getDouble("total"));
        c.setModePaiement(rs.getString("mode_paiement"));
        c.setAdresseLivraison(rs.getString("adresse_livraison"));
        c.setTelephone(rs.getString("telephone"));
        c.setNom(rs.getString("nom"));
        c.setEmail(rs.getString("email"));
        c.setMessage(rs.getString("message"));
        c.setProduitsIds(rs.getString("produits_ids"));
        c.setCouponCode(rs.getString("coupon_code"));
        c.setCouponDiscount(rs.getDouble("coupon_discount"));

        Timestamp edd = rs.getTimestamp("estimated_delivery_date");
        if (edd != null) {
            c.setEstimatedDeliveryDate(edd.toLocalDateTime());
        }

        c.setFraudScore(rs.getInt("fraud_score"));
        c.setBaseShippingCost(rs.getDouble("base_shipping_cost"));

        return c;
    }
}
