package org.example.service;

import org.example.model.Coupon;

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

    private final List<Coupon> coupons = new ArrayList<>();
    private int nextId = 1;

    public void add(Coupon coupon) {
        coupon.setId(nextId++);
        coupons.add(coupon);
    }

    public void update(Coupon coupon) {
        for (int i = 0; i < coupons.size(); i++) {
            if (coupons.get(i).getId() == coupon.getId()) {
                coupons.set(i, coupon);
                break;
            }
        }
    }

    public void delete(int id) {
        coupons.removeIf(c -> c.getId() == id);
    }

    public List<Coupon> getAll() {
        return new ArrayList<>(coupons);
    }

    public List<Coupon> search(String query) {
        return coupons.stream()
                .filter(c -> c.getCode().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Coupon getById(int id) {
        return coupons.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public Coupon findByCodeAnyState(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        return coupons.stream()
                .filter(x -> x.getCode().equalsIgnoreCase(c))
                .findFirst()
                .orElse(null);
    }

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
        Coupon c = getById(couponId);
        if (c != null) {
            c.setUsageCount(c.getUsageCount() + 1);
        }
    }
}
