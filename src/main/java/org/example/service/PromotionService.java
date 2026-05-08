package org.example.service;

import org.example.model.Promotion;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromotionService {
    private String resolvedProduitFkColumn;
    private Boolean hasIdAdminColumn;
    private final ProduitService produitService = ProduitService.getInstance();

    public void add(Promotion promotion) {
        ensureNoDuplicateActivePromotion(promotion.getProduitId(), promotion.getId());
        String produitFk = resolveProduitFkColumn();
        boolean withAdmin = hasIdAdmin();
        String sql;
        if (withAdmin) {
            sql = "INSERT INTO promotion (" + produitFk + ", titre, description, valeur_reduction, date_debut, date_fin, statut, id_admin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO promotion (" + produitFk + ", titre, description, valeur_reduction, date_debut, date_fin, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (promotion.getProduitId() != null) {
                statement.setInt(1, promotion.getProduitId());
            } else {
                statement.setObject(1, null);
            }
            statement.setString(2, promotion.getTitre());
            statement.setString(3, promotion.getDescription());
            statement.setDouble(4, promotion.getValeurReduction());
            statement.setTimestamp(5, Timestamp.valueOf(promotion.getDateDebut()));
            statement.setTimestamp(6, Timestamp.valueOf(promotion.getDateFin()));
            statement.setString(7, promotion.getStatut());
            if (withAdmin) {
                statement.setInt(8, promotion.getIdAdmin());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PromotionService] Add error: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout de la promotion", e);
        }
    }


    public void update(Promotion promotion) {
        ensureNoDuplicateActivePromotion(promotion.getProduitId(), promotion.getId());
        String produitFk = resolveProduitFkColumn();
        boolean withAdmin = hasIdAdmin();
        String sql;
        if (withAdmin) {
            sql = "UPDATE promotion SET " + produitFk + " = ?, titre = ?, description = ?, valeur_reduction = ?, date_debut = ?, date_fin = ?, statut = ?, id_admin = ? WHERE id_promotion = ?";
        } else {
            sql = "UPDATE promotion SET " + produitFk + " = ?, titre = ?, description = ?, valeur_reduction = ?, date_debut = ?, date_fin = ?, statut = ? WHERE id_promotion = ?";
        }
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (promotion.getProduitId() != null) {
                statement.setInt(1, promotion.getProduitId());
            } else {
                statement.setObject(1, null);
            }
            statement.setString(2, promotion.getTitre());
            statement.setString(3, promotion.getDescription());
            statement.setDouble(4, promotion.getValeurReduction());
            statement.setTimestamp(5, Timestamp.valueOf(promotion.getDateDebut()));
            statement.setTimestamp(6, Timestamp.valueOf(promotion.getDateFin()));
            statement.setString(7, promotion.getStatut());
            if (withAdmin) {
                statement.setInt(8, promotion.getIdAdmin());
                statement.setInt(9, promotion.getId());
            } else {
                statement.setInt(8, promotion.getId());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PromotionService] Update error: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la mise a jour de la promotion", e);
        }
    }

    public void delete(int id) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM promotion WHERE id_promotion = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la promotion", e);
        }
    }

    public Promotion getById(int id) {
        String produitFk = resolveProduitFkColumn();
        String adminCol = hasIdAdmin() ? ", id_admin" : "";
        String sql = "SELECT id_promotion, " + produitFk + " AS produit_fk, titre, description, valeur_reduction, date_debut, date_fin, statut" + adminCol + " FROM promotion WHERE id_promotion = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de la promotion", e);
        }
        return null;
    }

    public Promotion getActivePromotionForProduct(int produitId) {
        String produitFk = resolveProduitFkColumn();
        String adminCol = hasIdAdmin() ? ", id_admin" : "";
        String sql = "SELECT id_promotion, " + produitFk + " AS produit_fk, titre, description, valeur_reduction, date_debut, date_fin, statut" + adminCol + " FROM promotion WHERE " + produitFk + " = ? ORDER BY id_promotion DESC";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, produitId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Promotion p = mapRow(rs);
                    if (p.isActive()) {
                        return p;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de la promotion active", e);
        }

        return null;
    }

    public double getPromotionalPrice(int produitId, double originalPrice) {
        Promotion promotion = getActivePromotionForProduct(produitId);
        if (promotion == null) {
            return originalPrice;
        }
        return promotion.applyDiscount(originalPrice);
    }

    public List<Promotion> getAllPromotions() {
        String produitFk = resolveProduitFkColumn();
        String adminCol = hasIdAdmin() ? ", id_admin" : "";
        String sql = "SELECT id_promotion, " + produitFk + " AS produit_fk, titre, description, valeur_reduction, date_debut, date_fin, statut" + adminCol + " FROM promotion ORDER BY id_promotion DESC";
        List<Promotion> promotions = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                promotions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des promotions", e);
        }
        return promotions;
    }

    public boolean toggleStatus(int idPromotion) {
        Promotion promotion = getById(idPromotion);
        if (promotion == null) {
            return false;
        }

        String status = promotion.getStatut() == null ? "inactive" : promotion.getStatut();
        promotion.setStatut("active".equalsIgnoreCase(status) ? "inactive" : "active");
        update(promotion);
        return true;
    }

    public List<Promotion> getFilteredPromotions(
            String search,
            String category,
            String promotionStatus,
            Double priceMin,
            Double priceMax,
            String sort
    ) {
        Map<Integer, org.example.model.Produit> productsById = produitService.getAll().stream()
                .collect(Collectors.toMap(org.example.model.Produit::getId, p -> p, (a, b) -> a));

        String safeSearch = search == null ? "" : search.trim().toLowerCase();

        return getAllPromotions().stream()
                .filter(p -> {
                    org.example.model.Produit product = p.getProduitId() == null ? null : productsById.get(p.getProduitId());

                    if (!safeSearch.isEmpty()) {
                        String title = p.getTitre() == null ? "" : p.getTitre().toLowerCase();
                        String desc = p.getDescription() == null ? "" : p.getDescription().toLowerCase();
                        String productName = product == null || product.getNom() == null ? "" : product.getNom().toLowerCase();
                        if (!title.contains(safeSearch) && !desc.contains(safeSearch) && !productName.contains(safeSearch)) {
                            return false;
                        }
                    }

                    if (category != null && !category.isBlank()) {
                        if (product == null || product.getCategorie() == null || !product.getCategorie().equalsIgnoreCase(category)) {
                            return false;
                        }
                    }

                    if (priceMin != null && product != null && product.getPrix() < priceMin) {
                        return false;
                    }
                    if (priceMax != null && product != null && product.getPrix() > priceMax) {
                        return false;
                    }

                    if (promotionStatus != null && !promotionStatus.isBlank()) {
                        if ("with_promo".equalsIgnoreCase(promotionStatus) && !p.isActive()) {
                            return false;
                        }
                        if ("without_promo".equalsIgnoreCase(promotionStatus) && p.isActive()) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(resolveSort(sort))
                .collect(Collectors.toList());
    }

    private Comparator<Promotion> resolveSort(String sort) {
        if ("price".equalsIgnoreCase(sort) || "Reduction +".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Promotion::getValeurReduction);
        }
        if ("Reduction -".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Promotion::getValeurReduction).reversed();
        }
        return Comparator.comparing(Promotion::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Promotion mapRow(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("date_debut");
        Timestamp endTs = rs.getTimestamp("date_fin");
        int adminId = 0;
        if (hasIdAdmin()) {
            try { adminId = rs.getInt("id_admin"); } catch (SQLException ignored) {}
        }

        return new Promotion(
                rs.getInt("id_promotion"),
                rs.getObject("produit_fk") != null ? rs.getInt("produit_fk") : null,
                rs.getString("titre"),
                rs.getString("description"),
                rs.getDouble("valeur_reduction"),
                startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now(),
                endTs != null ? endTs.toLocalDateTime() : LocalDateTime.now(),
                rs.getString("statut"),
                adminId
        );
    }

    private boolean hasIdAdmin() {
        if (hasIdAdminColumn != null) {
            return hasIdAdminColumn;
        }
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion' AND COLUMN_NAME = 'id_admin'";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            hasIdAdminColumn = rs.next();
        } catch (SQLException e) {
            hasIdAdminColumn = false;
        }
        return hasIdAdminColumn;
    }

    private void ensureNoDuplicateActivePromotion(Integer produitId, int currentPromotionId) {
        if (produitId == null) {
            return;
        }

        String produitFk = resolveProduitFkColumn();
        String sql = "SELECT id_promotion, statut, date_debut, date_fin FROM promotion WHERE " + produitFk + " = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, produitId);
            try (ResultSet rs = statement.executeQuery()) {
                LocalDateTime now = LocalDateTime.now();
                while (rs.next()) {
                    int id = rs.getInt("id_promotion");
                    if (id == currentPromotionId) {
                        continue;
                    }
                    String statut = rs.getString("statut");
                    Timestamp start = rs.getTimestamp("date_debut");
                    Timestamp end = rs.getTimestamp("date_fin");
                    boolean active = "active".equalsIgnoreCase(statut)
                            && start != null && !start.toLocalDateTime().isAfter(now)
                            && end != null && !end.toLocalDateTime().isBefore(now);
                    if (active) {
                        throw new IllegalStateException("Ce produit a deja une promotion active.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la verification des promotions", e);
        }
    }

    private String resolveProduitFkColumn() {
        if (resolvedProduitFkColumn != null) {
            return resolvedProduitFkColumn;
        }

        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion' AND COLUMN_NAME IN ('id_produit_id', 'id_produit')";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String c = rs.getString("COLUMN_NAME");
                if ("id_produit_id".equalsIgnoreCase(c)) {
                    resolvedProduitFkColumn = "id_produit_id";
                    return resolvedProduitFkColumn;
                }
                if ("id_produit".equalsIgnoreCase(c)) {
                    resolvedProduitFkColumn = "id_produit";
                }
            }
        } catch (SQLException e) {
            // fallback
        }

        if (resolvedProduitFkColumn == null) {
            resolvedProduitFkColumn = "id_produit_id";
        }
        return resolvedProduitFkColumn;
    }
}
