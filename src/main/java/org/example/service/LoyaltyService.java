package org.example.service;

import org.example.model.Commande;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoyaltyService {
    public int addPoints(int userId, Commande commande) {
        if (userId <= 0 || commande == null) {
            return 0;
        }

        String statut = commande.getStatut() == null ? "" : commande.getStatut().toLowerCase();
        if (!"confirmee".equals(statut) && !"livree".equals(statut)) {
            return 0;
        }

        int points = (int) Math.floor(commande.getTotal() / 10.0);
        if (commande.getTotal() >= 300) {
            points += 20;
        }

        int currentPoints = getCurrentPoints(userId);
        int newPoints = currentPoints + points;
        String level = calculateLevel(newPoints);

        String sql = "UPDATE utilisateur SET loyalty_points = ?, loyalty_level = ?, last_activity_at = NOW() WHERE id_utilisateur = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newPoints);
            statement.setString(2, level);
            statement.setInt(3, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise a jour fidelite", e);
        }

        return points;
    }

    public String calculateLevel(int points) {
        if (points >= 2000) {
            return "PLATINUM";
        }
        if (points >= 1000) {
            return "GOLD";
        }
        if (points >= 400) {
            return "SILVER";
        }
        return "BRONZE";
    }

    public double getDiscountByLevel(String level) {
        if ("PLATINUM".equalsIgnoreCase(level)) {
            return 12.0;
        }
        if ("GOLD".equalsIgnoreCase(level)) {
            return 8.0;
        }
        if ("SILVER".equalsIgnoreCase(level)) {
            return 4.0;
        }
        return 0.0;
    }

    private int getCurrentPoints(int userId) {
        String sql = "SELECT loyalty_points FROM utilisateur WHERE id_utilisateur = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("loyalty_points");
                }
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }
}
