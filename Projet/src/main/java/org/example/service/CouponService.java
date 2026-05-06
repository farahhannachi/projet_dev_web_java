package org.example.service;

import org.example.model.Coupon;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CouponService {
    private List<Coupon> coupons = new ArrayList<>();
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

    public Coupon getByCode(String code) {
        return coupons.stream().filter(c -> c.getCode().equalsIgnoreCase(code) && c.isValide()).findFirst().orElse(null);
    }
}
