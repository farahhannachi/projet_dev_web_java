package org.example.service;

import org.example.model.Commande;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FraudDetectionService {
    public int calculateFraudScore(Commande order, int paymentFailures) {
        int score = 0;

        int recentOrders = countRecentOrdersByEmail(order.getEmail(), 20);
        if (recentOrders >= 4) {
            score += 35;
        } else if (recentOrders >= 2) {
            score += 20;
        }

        double amount = order.getTotal();
        if (amount >= 1200) {
            score += 35;
        } else if (amount >= 600) {
            score += 20;
        }

        String address = order.getAdresseLivraison() == null ? "" : order.getAdresseLivraison().toLowerCase();
        String[] suspiciousTerms = {"boite postale", "p.o. box", "unknown", "test"};
        for (String term : suspiciousTerms) {
            if (address.contains(term)) {
                score += 20;
                break;
            }
        }

        if (paymentFailures >= 3) {
            score += 30;
        } else if (paymentFailures >= 1) {
            score += 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    public void applyFraudDecision(Commande order, int score) {
        order.setFraudScore(score);

        if (score > 90) {
            order.setStatut("bloquee");
            return;
        }

        if (score > 70) {
            order.setStatut("review");
        }
    }

    private int countRecentOrdersByEmail(String email, int days) {
        if (email == null || email.isBlank()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM commande WHERE email = ? AND date_commande >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            statement.setInt(2, days);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            return 0;
        }

        return 0;
    }
}
