package org.example.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the database with required tables if they don't exist.
 */
public class DatabaseInitializer {

    /**
     * Initialize the database by creating missing tables
     */
    public static void initializeDatabase() {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create reservation table if it doesn't exist
            String createReservationTable = "CREATE TABLE IF NOT EXISTS reservation (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "service_id INT NOT NULL," +
                    "nom_client VARCHAR(100) NOT NULL," +
                    "email_client VARCHAR(100) NOT NULL," +
                    "telephone_client VARCHAR(20) NOT NULL," +
                    "date_reservation DATETIME NOT NULL," +
                    "date_rendez_vous DATETIME NOT NULL," +
                    "motif LONGTEXT NOT NULL," +
                    "statut VARCHAR(50) DEFAULT 'En attente'," +
                    "date_creation DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (service_id) REFERENCES service(id_service) ON DELETE CASCADE," +
                    "INDEX idx_service (service_id)," +
                    "INDEX idx_date (date_rendez_vous)," +
                    "INDEX idx_statut (statut)" +
                    ")";

            stmt.executeUpdate(createReservationTable);
            System.out.println("✓ Table 'reservation' created or already exists");

            // Fix AUTO_INCREMENT if missing (for existing tables created without it)
            try {
                // Check if id column has AUTO_INCREMENT
                var rs = stmt.executeQuery("SHOW COLUMNS FROM reservation WHERE Field = 'id'");
                if (rs.next()) {
                    String extra = rs.getString("Extra");
                    if (!extra.contains("auto_increment")) {
                        System.out.println("🔧 Fixing missing AUTO_INCREMENT on reservation.id");
                        stmt.executeUpdate("ALTER TABLE reservation MODIFY id INT NOT NULL AUTO_INCREMENT");
                        System.out.println("✓ AUTO_INCREMENT fixed on reservation.id");
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Could not check/fix AUTO_INCREMENT: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
            // Do not throw, just log and continue
        }
    }

    /**
     * Check if reservation table exists
     */
    public static boolean reservationTableExists() {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeQuery("SELECT 1 FROM reservation LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
