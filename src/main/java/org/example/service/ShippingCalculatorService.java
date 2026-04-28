package org.example.service;

public class ShippingCalculatorService {
    public double calculateForAddress(String region, int itemCount, double subtotal) {
        double base = subtotal >= 120 ? 0.0 : 7.5;

        if (region != null) {
            String r = region.toLowerCase();
            if (r.contains("sud") || r.contains("remote") || r.contains("interieur")) {
                base += 3.0;
            }
        }

        if (itemCount >= 10) {
            base += 2.5;
        }

        return round2(base);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
