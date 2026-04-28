package org.example.service;

import org.example.model.Coupon;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CouponService {
    public static class CouponValidationResult {
        public final boolean valid;
        public final String message;
        public final Coupon coupon;

        public CouponValidationResult(boolean valid, String message, Coupon coupon) {
            this.valid = valid;
            this.message = message;
            this.coupon = coupon;
        }
    }

    public static class CouponApplyResult {
        public final double discount;
        public final double finalTotal;

        public CouponApplyResult(double discount, double finalTotal) {
            this.discount = discount;
            this.finalTotal = finalTotal;
        }
    }

    public void add(Coupon coupon) {
        String sql = "INSERT INTO coupon (code, type, valeur, date_expiration, usage_max, usage_count, actif, montant_minimum_panier) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, coupon.getCode());
            statement.setString(2, coupon.getType());
            statement.setDouble(3, coupon.getValeur());
            if (coupon.getDateExpiration() != null) {
                statement.setTimestamp(4, Timestamp.valueOf(coupon.getDateExpiration().atStartOfDay()));
            } else {
                statement.setTimestamp(4, null);
            }
            statement.setInt(5, coupon.getUsageMax());
            statement.setInt(6, coupon.getUsageCount());
            statement.setBoolean(7, coupon.isActif());
            statement.setDouble(8, coupon.getMontantMinimumPanier());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du coupon", e);
        }
    }

    public void update(Coupon coupon) {
        String sql = "UPDATE coupon SET code = ?, type = ?, valeur = ?, date_expiration = ?, usage_max = ?, usage_count = ?, actif = ?, montant_minimum_panier = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, coupon.getCode());
            statement.setString(2, coupon.getType());
            statement.setDouble(3, coupon.getValeur());
            if (coupon.getDateExpiration() != null) {
                statement.setTimestamp(4, Timestamp.valueOf(coupon.getDateExpiration().atStartOfDay()));
            } else {
                statement.setTimestamp(4, null);
            }
            statement.setInt(5, coupon.getUsageMax());
            statement.setInt(6, coupon.getUsageCount());
            statement.setBoolean(7, coupon.isActif());
            statement.setDouble(8, coupon.getMontantMinimumPanier());
            statement.setInt(9, coupon.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du coupon", e);
        }
    }

    public void delete(int id) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM coupon WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du coupon", e);
        }
    }

    public List<Coupon> getAll() {
        List<Coupon> coupons = new ArrayList<>();
        String sql = "SELECT id, code, type, valeur, date_expiration, usage_max, usage_count, actif, montant_minimum_panier FROM coupon ORDER BY id DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                coupons.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des coupons", e);
        }
        return coupons;
    }

    public List<Coupon> search(String query) {
        return getAll().stream()
                .filter(c -> c.getCode().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Coupon getById(int id) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, code, type, valeur, date_expiration, usage_max, usage_count, actif, montant_minimum_panier FROM coupon WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du coupon", e);
        }
        return null;
    }

    public Coupon getByCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, code, type, valeur, date_expiration, usage_max, usage_count, actif, montant_minimum_panier FROM coupon WHERE code = ?")) {
            statement.setString(1, normalized);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du coupon", e);
        }
        return null;
    }

    public CouponValidationResult validateCoupon(String code, double cartTotal) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return new CouponValidationResult(false, "Code promo vide.", null);
        }

        Coupon coupon = getByCode(normalized);
        if (coupon == null) {
            return new CouponValidationResult(false, "Coupon introuvable.", null);
        }

        if (!coupon.isActif()) {
            return new CouponValidationResult(false, "Coupon inactif.", null);
        }

        if (coupon.getDateExpiration() != null && coupon.getDateExpiration().isBefore(LocalDate.now())) {
            return new CouponValidationResult(false, "Coupon expire.", null);
        }

        if (coupon.getUsageCount() >= coupon.getUsageMax()) {
            return new CouponValidationResult(false, "Coupon epuise.", null);
        }

        if (cartTotal < coupon.getMontantMinimumPanier()) {
            return new CouponValidationResult(false,
                    String.format("Montant minimum requis: %.2f DT.", coupon.getMontantMinimumPanier()), null);
        }

        return new CouponValidationResult(true, "Coupon valide.", coupon);
    }

    public CouponApplyResult applyCoupon(double cartTotal, Coupon coupon) {
        double discount;
        if (Coupon.TYPE_PERCENTAGE.equalsIgnoreCase(coupon.getType())) {
            discount = cartTotal * (coupon.getValeur() / 100.0);
        } else {
            discount = coupon.getValeur();
        }

        discount = Math.min(cartTotal, Math.max(0.0, discount));
        double finalTotal = Math.max(0.0, cartTotal - discount);
        return new CouponApplyResult(round2(discount), round2(finalTotal));
    }

    public void incrementUsage(int couponId) {
        String sql = "UPDATE coupon SET usage_count = usage_count + 1 WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, couponId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'incrementation du coupon", e);
        }
    }

    public List<Coupon> getFilteredCoupons(String search, String active, String sort) {
        String safeSearch = search == null ? "" : search.trim().toUpperCase();
        String safeActive = active == null ? "all" : active;
        Comparator<Coupon> comparator = resolveSort(sort);

        return getAll().stream()
                .filter(c -> safeSearch.isEmpty() || (c.getCode() != null && c.getCode().toUpperCase().contains(safeSearch)))
                .filter(c -> {
                    if ("all".equalsIgnoreCase(safeActive)) {
                        return true;
                    }
                    if ("1".equals(safeActive)) {
                        return c.isActif();
                    }
                    if ("0".equals(safeActive)) {
                        return !c.isActif();
                    }
                    return true;
                })
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Comparator<Coupon> resolveSort(String sort) {
        if ("code_asc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(c -> c.getCode() == null ? "" : c.getCode());
        }
        if ("usage_desc".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(Coupon::getUsageCount).reversed();
        }
        if ("value_desc".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(Coupon::getValeur).reversed();
        }
        if ("date_asc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Coupon::getDateExpiration, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing(Coupon::getDateExpiration, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Coupon mapRow(ResultSet rs) throws SQLException {
        Timestamp exp = rs.getTimestamp("date_expiration");
        return new Coupon(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("type"),
                rs.getDouble("valeur"),
                exp != null ? exp.toLocalDateTime().toLocalDate() : null,
                rs.getInt("usage_max"),
                rs.getInt("usage_count"),
                rs.getBoolean("actif"),
                rs.getDouble("montant_minimum_panier")
        );
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
