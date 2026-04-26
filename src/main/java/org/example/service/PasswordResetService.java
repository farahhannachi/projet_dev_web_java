package org.example.service;

import org.example.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing password reset tokens and operations
 * Uses existing reset_token and reset_token_expires_at columns in utilisateur table
 */
public class PasswordResetService {
    
    /**
     * Generate and save password reset token
     * @param email User email
     * @return Reset token (UUID) or null if email not found
     */
    public static String generateResetToken(String email) {
        try {
            // Generate UUID token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            
            // Update utilisateur table with token
            String updateSql = "UPDATE utilisateur SET reset_token = ?, reset_token_expires_at = ? WHERE email = ?";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, token);
                stmt.setTimestamp(2, Timestamp.valueOf(expiresAt));
                stmt.setString(3, email);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("[RESET] Token generated for user: " + email);
                    return token;
                } else {
                    System.out.println("[RESET] Email not found: " + email);
                    return null;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[RESET ERROR] Failed to generate token: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Verify if reset token is valid
     * @param token The reset token
     * @return Email associated with token, or null if invalid/expired
     */
    public static String verifyResetToken(String token) {
        try {
            String sql = "SELECT email, reset_token_expires_at FROM utilisateur " +
                        "WHERE reset_token = ? AND reset_token_expires_at > NOW()";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, token);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String email = rs.getString("email");
                    System.out.println("[RESET] Token verified for: " + email);
                    return email;
                }
                
                System.out.println("[RESET] Invalid or expired token");
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("[RESET ERROR] Failed to verify token: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Reset password using token
     * @param token The reset token
     * @param newPassword New password (plain text)
     * @return true if successful
     */
    public static boolean resetPassword(String token, String newPassword) {
        try {
            // Verify token and get email
            String email = verifyResetToken(token);
            if (email == null) {
                System.out.println("[RESET] Token verification failed");
                return false;
            }
            
            // Hash new password
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            
            // Update password and clear token
            String updateSql = "UPDATE utilisateur SET mot_de_passe = ?, reset_token = NULL, reset_token_expires_at = NULL WHERE email = ?";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, hashedPassword);
                stmt.setString(2, email);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("[RESET] Password updated for: " + email);
                    return true;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[RESET ERROR] Failed to reset password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Delete used reset token
     * @param email User email
     */
    public static void deleteResetToken(String email) {
        try {
            String sql = "UPDATE utilisateur SET reset_token = NULL, reset_token_expires_at = NULL WHERE email = ?";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.executeUpdate();
                System.out.println("[RESET] Token deleted");
            }
        } catch (SQLException e) {
            System.err.println("[RESET ERROR] Failed to delete token: " + e.getMessage());
        }
    }
    
    /**
     * Clean up expired tokens (optional maintenance)
     */
    public static void cleanExpiredTokens() {
        try {
            String sql = "UPDATE utilisateur SET reset_token = NULL, reset_token_expires_at = NULL " +
                        "WHERE reset_token_expires_at < NOW() AND reset_token IS NOT NULL";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 Statement stmt = conn.createStatement()) {
                int rowsUpdated = stmt.executeUpdate(sql);
                System.out.println("[RESET] Cleaned up " + rowsUpdated + " expired tokens");
            }
        } catch (SQLException e) {
            System.err.println("[RESET ERROR] Failed to clean tokens: " + e.getMessage());
        }
    }
}

