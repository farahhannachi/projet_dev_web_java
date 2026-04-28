package org.example.service;

import org.example.model.Commande;
import org.example.model.LigneCommande;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des commandes avec calcul de totaux,
 * remises par palier, et workflow de statuts
 */
public class CommandeService {
    private final StockService stockService = new StockService();
    private final FraudDetectionService fraudDetectionService = new FraudDetectionService();
    private final LoyaltyService loyaltyService = new LoyaltyService();
    private String resolvedUserIdColumn;

    public int add(Commande commande) {
        calculerTotal(commande);

        // Symfony parity: calculate fraud score and apply decision before persist.
        int fraudScore = fraudDetectionService.calculateFraudScore(commande, 0);
        fraudDetectionService.applyFraudDecision(commande, fraudScore);

        String userIdColumn = resolveUserIdColumn();
        String sql = "INSERT INTO commande (" + userIdColumn + ", date_commande, statut, total, mode_paiement, adresse_livraison, " +
                "telephone, nom, email, message, produits_ids, coupon_code, coupon_discount, estimated_delivery_date, fraud_score, base_shipping_cost) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (commande.getUtilisateurId() != null) {
                statement.setInt(1, commande.getUtilisateurId());
            } else {
                statement.setNull(1, Types.INTEGER);
            }
            statement.setTimestamp(2, toTimestamp(commande.getDateCommandeDateTime() != null ? commande.getDateCommandeDateTime() : LocalDateTime.now()));
            statement.setString(3, commande.getStatut() != null ? commande.getStatut() : "en_attente");
            statement.setDouble(4, commande.getTotal());
            statement.setString(5, commande.getModePaiement() != null ? commande.getModePaiement() : "en_ligne");
            statement.setString(6, safeText(commande.getAdresseLivraison()));
            statement.setString(7, safeText(commande.getTelephone()));
            statement.setString(8, safeText(commande.getNom()));
            statement.setString(9, safeText(commande.getEmail()));
            statement.setString(10, commande.getMessage());
            statement.setString(11, commande.getProduitsIds());
            statement.setString(12, commande.getCouponCode());
            statement.setDouble(13, commande.getCouponDiscount());
            statement.setTimestamp(14, toTimestamp(commande.getEstimatedDeliveryDate()));
            statement.setInt(15, commande.getFraudScore());
            statement.setDouble(16, commande.getBaseShippingCost());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    commande.setId(generatedId);
                    return generatedId;
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la commande", e);
        }
    }

    public void update(Commande commande) {
        if (!commande.peutEtreModifiee()) {
            throw new IllegalStateException("Impossible de modifier une commande expédiée ou annulée");
        }

        calculerTotal(commande);

        String sql = "UPDATE commande SET statut = ?, total = ?, mode_paiement = ?, adresse_livraison = ?, telephone = ?, " +
                "nom = ?, email = ?, message = ?, produits_ids = ?, coupon_code = ?, coupon_discount = ?, estimated_delivery_date = ?, " +
                "fraud_score = ?, base_shipping_cost = ? WHERE id_commande = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, commande.getStatut());
            statement.setDouble(2, commande.getTotal());
            statement.setString(3, commande.getModePaiement());
            statement.setString(4, safeText(commande.getAdresseLivraison()));
            statement.setString(5, safeText(commande.getTelephone()));
            statement.setString(6, safeText(commande.getNom()));
            statement.setString(7, safeText(commande.getEmail()));
            statement.setString(8, commande.getMessage());
            statement.setString(9, commande.getProduitsIds());
            statement.setString(10, commande.getCouponCode());
            statement.setDouble(11, commande.getCouponDiscount());
            statement.setTimestamp(12, toTimestamp(commande.getEstimatedDeliveryDate()));
            statement.setInt(13, commande.getFraudScore());
            statement.setDouble(14, commande.getBaseShippingCost());
            statement.setInt(15, commande.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour de la commande", e);
        }
    }

    public boolean updateStatusWithBusinessRules(int commandeId, String newStatus) {
        Commande commande = getById(commandeId);
        if (commande == null) {
            return false;
        }

        String[] allowed = {"en_attente", "confirmee", "annulee", "livree", "review", "bloquee"};
        boolean isAllowed = false;
        for (String s : allowed) {
            if (s.equalsIgnoreCase(newStatus)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            return false;
        }

        String oldStatus = commande.getStatut();
        commande.setStatut(newStatus);
        update(commande);
        maybeGrantLoyaltyPoints(commande, oldStatus);
        return true;
    }

    public String exportCommandesCsv() {
        List<Commande> commandes = getAll();
        List<String> rows = new ArrayList<>();
        rows.add("ID;Date;Nom;Email;Telephone;Adresse;ModePaiement;Statut;Coupon;Remise;FraudScore;LivraisonEstimee;Total");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Commande commande : commandes) {
            String dateCommande = commande.getDateCommandeDateTime() != null ? commande.getDateCommandeDateTime().format(formatter) : "";
            String estimatedDelivery = commande.getEstimatedDeliveryDate() != null ? commande.getEstimatedDeliveryDate().format(formatter) : "";
            String adresse = commande.getAdresseLivraison() == null ? "" : commande.getAdresseLivraison().replace(';', ',');

            rows.add(String.join(";",
                    String.valueOf(commande.getId()),
                    dateCommande,
                    safeText(commande.getNom()),
                    safeText(commande.getEmail()),
                    safeText(commande.getTelephone()),
                    adresse,
                    safeText(commande.getModePaiement()),
                    safeText(commande.getStatut()),
                    commande.getCouponCode() == null ? "" : commande.getCouponCode(),
                    String.format("%.2f", commande.getCouponDiscount()),
                    String.valueOf(commande.getFraudScore()),
                    estimatedDelivery,
                    String.format("%.2f", commande.getTotal())
            ));
        }

        return String.join("\n", rows);
    }

    public void delete(int id) {
        String sql = "DELETE FROM commande WHERE id_commande = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la commande", e);
        }
    }

    public List<Commande> getAll() {
        String userIdColumn = resolveUserIdColumn();
        String sql = "SELECT id_commande, " + userIdColumn + " AS user_id, date_commande, statut, total, mode_paiement, adresse_livraison, telephone, " +
                "nom, email, message, produits_ids, coupon_code, coupon_discount, estimated_delivery_date, fraud_score, base_shipping_cost " +
                "FROM commande ORDER BY date_commande DESC";

        List<Commande> commandes = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                commandes.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des commandes", e);
        }

        return commandes;
    }

    /**
     * Calcule le total HT, applique les remises par palier et calcule le TTC
     */
    public void calculerTotal(Commande commande) {
        if (commande.getLignes() == null || commande.getLignes().isEmpty()) {
            double totalNet = Math.max(0, commande.getTotal() - commande.getCouponDiscount() + commande.getBaseShippingCost());
            commande.setTotalHt(totalNet);
            commande.setTotalTtc(totalNet);
            return;
        }

        // Calculer total HT
        double totalHt = commande.getLignes().stream()
                .mapToDouble(LigneCommande::getTotalLigne)
                .sum();

        // Appliquer remises par palier
        double remisePalier = 0;
        int nbArticles = commande.getNbArticles();
        
        if (nbArticles > 10) {
            remisePalier = totalHt * 0.10; // 10%
        } else if (nbArticles > 5) {
            remisePalier = totalHt * 0.05; // 5%
        }

        totalHt = totalHt - remisePalier;

        double totalTtc = totalHt;

        commande.setTotalHt(totalHt);
        commande.setTotalTtc(totalTtc);
    }

    /**
     * Change le statut de la commande avec vérification du workflow
     */
    public boolean changerStatut(int commandeId, String nouveauStatut) {
        Commande commande = getById(commandeId);
        if (commande == null) {
            return false;
        }

        String statutActuel = commande.getStatut();

        // Vérifier la validité de la transition
        if (statutActuel.equals(Commande.Statut.EXPEDIEE.getValeur()) ||
            statutActuel.equals(Commande.Statut.ANNULEE.getValeur())) {
            return false; // Impossible de modifier
        }

        // Autoriser les transitions valides
        commande.setStatut(nouveauStatut);
        update(commande);
        return true;
    }

    /**
     * Valide une commande et réserve le stock
     */
    public boolean validerCommande(int commandeId) {
        Commande commande = getById(commandeId);
        if (commande == null) {
            return false;
        }

        // Vérifier que la commande est en brouillon
        if (!commande.getStatut().equals(Commande.Statut.BROUILLON.getValeur())) {
            return false;
        }

        // Vérifier et réserver le stock pour chaque ligne
        for (LigneCommande ligne : commande.getLignes()) {
            if (!stockService.verifierDisponibilite(ligne.getProduitId(), ligne.getQuantite())) {
                return false;
            }
        }

        // Réserver le stock et confirmer la commande
        for (LigneCommande ligne : commande.getLignes()) {
            stockService.reserverStock(ligne.getProduitId(), ligne.getQuantite());
        }

        commande.setStatut(Commande.Statut.CONFIRMEE.getValeur());
        update(commande);
        return true;
    }

    /**
     * Annule une commande et libère le stock
     */
    public boolean annulerCommande(int commandeId) {
        Commande commande = getById(commandeId);
        if (commande == null) {
            return false;
        }

        if (commande.estExpediee()) {
            return false; // Impossible d'annuler si expédiée
        }

        // Libérer le stock si la commande était confirmée
        if (commande.estConfirmee()) {
            for (LigneCommande ligne : commande.getLignes()) {
                stockService.libererStock(ligne.getProduitId(), ligne.getQuantite());
            }
        }

        commande.setStatut(Commande.Statut.ANNULEE.getValeur());
        update(commande);
        return true;
    }

    /**
     * Recherche les commandes par statut
     */
    public List<Commande> rechercherParStatut(String statut) {
        return getAll().stream()
                .filter(c -> c.getStatut().equalsIgnoreCase(statut))
                .collect(Collectors.toList());
    }

    /**
     * Recherche les commandes par client
     */
    public List<Commande> rechercherParClient(int clientId) {
        return getAll().stream()
                .filter(c -> c.getUtilisateurId() != null && c.getUtilisateurId() == clientId)
                .collect(Collectors.toList());
    }

    public List<Commande> search(String query) {
        return getAll().stream()
                .filter(c -> String.valueOf(c.getId()).contains(query) ||
                             c.getClientNom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getStatut().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Commande getById(int id) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "SELECT id_commande, " + userIdColumn + " AS user_id, date_commande, statut, total, mode_paiement, adresse_livraison, telephone, " +
                "nom, email, message, produits_ids, coupon_code, coupon_discount, estimated_delivery_date, fraud_score, base_shipping_cost " +
                "FROM commande WHERE id_commande = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de la commande", e);
        }

        return null;
    }

    /**
     * Retourne les commandes impayées d'un client
     */
    public List<Commande> getCommandesImpayees(int clientId) {
        return getAll().stream()
                .filter(c -> c.getUtilisateurId() != null && c.getUtilisateurId() == clientId &&
                        c.estConfirmee() && !c.estExpediee())
                .collect(Collectors.toList());
    }

    private Commande mapRow(ResultSet resultSet) throws SQLException {
        Timestamp dateCommandeTs = resultSet.getTimestamp("date_commande");
        Timestamp estimatedTs = resultSet.getTimestamp("estimated_delivery_date");
        Integer userId = resultSet.getObject("user_id") != null ? resultSet.getInt("user_id") : null;

        return new Commande(
                resultSet.getInt("id_commande"),
                userId,
                dateCommandeTs != null ? dateCommandeTs.toLocalDateTime() : null,
                resultSet.getString("statut"),
                resultSet.getDouble("total"),
                resultSet.getString("mode_paiement"),
                resultSet.getString("adresse_livraison"),
                resultSet.getString("telephone"),
                resultSet.getString("nom"),
                resultSet.getString("email"),
                resultSet.getString("message"),
                resultSet.getString("produits_ids"),
                resultSet.getString("coupon_code"),
                resultSet.getDouble("coupon_discount"),
                estimatedTs != null ? estimatedTs.toLocalDateTime() : null,
                resultSet.getInt("fraud_score"),
                resultSet.getDouble("base_shipping_cost")
        );
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void maybeGrantLoyaltyPoints(Commande commande, String oldStatus) {
        if (commande == null || commande.getUtilisateurId() == null) {
            return;
        }

        String statut = commande.getStatut() == null ? "" : commande.getStatut().toLowerCase();
        boolean eligibleNow = "confirmee".equals(statut) || "livree".equals(statut);
        if (!eligibleNow) {
            return;
        }

        if (oldStatus != null) {
            String old = oldStatus.toLowerCase();
            if ("confirmee".equals(old) || "livree".equals(old)) {
                return;
            }
        }

        loyaltyService.addPoints(commande.getUtilisateurId(), commande);
    }

    private String resolveUserIdColumn() {
        if (resolvedUserIdColumn != null) {
            return resolvedUserIdColumn;
        }

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'commande' AND COLUMN_NAME IN ('id_utilisateur_id', 'id_utilisateur')");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if ("id_utilisateur_id".equalsIgnoreCase(columnName)) {
                    resolvedUserIdColumn = "id_utilisateur_id";
                    return resolvedUserIdColumn;
                }
                if ("id_utilisateur".equalsIgnoreCase(columnName)) {
                    resolvedUserIdColumn = "id_utilisateur";
                }
            }
        } catch (SQLException e) {
            // fallback
        }

        if (resolvedUserIdColumn == null) {
            resolvedUserIdColumn = "id_utilisateur";
        }
        return resolvedUserIdColumn;
    }
}
