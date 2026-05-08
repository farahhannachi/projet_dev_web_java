package org.example.service;

import org.example.model.Coupon;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CouponService {

    public static final class CouponValidationResult {
        public final boolean valid;
        public final String message;
        public final Coupon coupon;

        public CouponValidationResult(boolean valid, String message, Coupon coupon) {
            this.valid = valid;
            this.message = message;
            this.coupon = coupon;
        }
    }

    public static final class CouponApplyResult {
        public final double discount;
        public final double finalTotal;

        public CouponApplyResult(double discount, double finalTotal) {
            this.discount = discount;
            this.finalTotal = finalTotal;
        }
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public void add(Coupon coupon) {
        String sql = """
                INSERT INTO coupon (code, type, valeur, date_expiration, usage_max, usage_count, actif, montant_minimum_panier)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, coupon.getCode());
            ps.setString(2, coupon.getType());
            ps.setDouble(3, coupon.getReduction());

            LocalDate exp = coupon.getDateExpiration();
            if (exp != null) {
                ps.setTimestamp(4, Timestamp.valueOf(exp.atStartOfDay()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }

            ps.setInt(5, coupon.getUsageMax());
            ps.setInt(6, coupon.getUsageCount());
            ps.setBoolean(7, coupon.isActif());
            ps.setDouble(8, coupon.getMontantMinimumPanier());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    coupon.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur add(): " + e.getMessage());
            throw new RuntimeException("Impossible d'enregistrer le coupon : " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public void update(Coupon coupon) {
        String sql = """
                UPDATE coupon SET code = ?, type = ?, valeur = ?, date_expiration = ?,
                  usage_max = ?, usage_count = ?, actif = ?, montant_minimum_panier = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, coupon.getCode());
            ps.setString(2, coupon.getType());
            ps.setDouble(3, coupon.getReduction());

            LocalDate exp = coupon.getDateExpiration();
            if (exp != null) {
                ps.setTimestamp(4, Timestamp.valueOf(exp.atStartOfDay()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }

            ps.setInt(5, coupon.getUsageMax());
            ps.setInt(6, coupon.getUsageCount());
            ps.setBoolean(7, coupon.isActif());
            ps.setDouble(8, coupon.getMontantMinimumPanier());
            ps.setInt(9, coupon.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur update(): " + e.getMessage());
            throw new RuntimeException("Impossible de mettre à jour le coupon : " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public void delete(int id) {
        String sql = "DELETE FROM coupon WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur delete(): " + e.getMessage());
            throw new RuntimeException("Impossible de supprimer le coupon : " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<Coupon> getAll() {
        String sql = "SELECT * FROM coupon ORDER BY id DESC";
        List<Coupon> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur getAll(): " + e.getMessage());
        }
        return list;
    }

    public Coupon getById(int id) {
        String sql = "SELECT * FROM coupon WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur getById(): " + e.getMessage());
        }
        return null;
    }

    public List<Coupon> search(String query) {
        return getAll().stream()
                .filter(c -> c.getCode().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Coupon findByCodeAnyState(String code) {
        if (code == null || code.isBlank()) return null;
        String sql = "SELECT * FROM coupon WHERE LOWER(code) = LOWER(?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur findByCode(): " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // BUSINESS LOGIC
    // -------------------------------------------------------------------------

    public CouponValidationResult validateCoupon(String code, double subtotal) {
        if (code == null || code.isBlank()) {
            return new CouponValidationResult(false, "Code coupon vide.", null);
        }
        if (subtotal <= 0) {
            return new CouponValidationResult(false, "Montant invalide pour appliquer un coupon.", null);
        }
        Coupon coupon = findByCodeAnyState(code.trim());
        if (coupon == null) {
            return new CouponValidationResult(false, "Code coupon introuvable.", null);
        }
        if (!coupon.isActif()) {
            return new CouponValidationResult(false, "Ce coupon est inactif.", coupon);
        }
        if (!coupon.isValide()) {
            return new CouponValidationResult(false, "Ce coupon n'est plus valide (expiration ou usages).", coupon);
        }
        if (subtotal < coupon.getMontantMinimumPanier()) {
            return new CouponValidationResult(false,
                    "Montant minimum panier: " + String.format("%.2f DT", coupon.getMontantMinimumPanier()),
                    coupon);
        }
        return new CouponValidationResult(true, "", coupon);
    }

    public CouponApplyResult applyCoupon(double subtotal, Coupon coupon) {
        if (coupon == null || subtotal <= 0) {
            return new CouponApplyResult(0, Math.max(0, subtotal));
        }
        double discount;
        if (coupon.isPercentage()) {
            discount = Math.round(subtotal * (coupon.getReduction() / 100.0) * 100.0) / 100.0;
        } else {
            discount = Math.min(subtotal, Math.max(0, coupon.getReduction()));
        }
        double finalTotal = Math.max(0, subtotal - discount);
        finalTotal = Math.round(finalTotal * 100.0) / 100.0;
        discount = Math.round(discount * 100.0) / 100.0;
        return new CouponApplyResult(discount, finalTotal);
    }

    public void incrementUsage(int couponId) {
        String sql = "UPDATE coupon SET usage_count = usage_count + 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CouponService] Erreur incrementUsage(): " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Mapping ResultSet → Coupon
    // -------------------------------------------------------------------------

    private Coupon map(ResultSet rs) throws SQLException {
        Coupon c = new Coupon();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setType(rs.getString("type"));
        c.setValeur(rs.getDouble("valeur"));

        Timestamp exp = rs.getTimestamp("date_expiration");
        if (exp != null) {
            c.setDateExpiration(exp.toLocalDateTime().toLocalDate());
        }

        c.setUsageMax(rs.getInt("usage_max"));
        c.setUsageCount(rs.getInt("usage_count"));
        c.setActif(rs.getBoolean("actif"));
        c.setMontantMinimumPanier(rs.getDouble("montant_minimum_panier"));
        return c;
    }
}
