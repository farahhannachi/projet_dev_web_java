package org.example.service;

import org.example.model.Reservation;
import org.example.util.DatabaseUtil;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReservationService {
    private static ReservationService instance;
    private boolean tableChecked = false;

    private ReservationService() {}

    public static ReservationService getInstance() {
        if (instance == null) {
            instance = new ReservationService();
        }
        return instance;
    }

    public boolean add(Reservation reservation) {
        String sql = "INSERT INTO reservation (service_id, nom_client, email_client, telephone_client, " +
                "date_reservation, date_rendez_vous, motif, statut, date_creation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ensureTableExists(conn);

            stmt.setInt(1, reservation.getServiceId());
            stmt.setString(2, reservation.getNomClient());
            stmt.setString(3, reservation.getEmailClient());
            stmt.setString(4, reservation.getTelephoneClient());
            stmt.setTimestamp(5, Timestamp.valueOf(reservation.getDateReservation()));
            stmt.setTimestamp(6, Timestamp.valueOf(reservation.getDateRendezVous()));
            stmt.setString(7, reservation.getMotif());
            stmt.setString(8, reservation.getStatut());
            stmt.setTimestamp(9, Timestamp.valueOf(reservation.getDateCreation()));

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservation.setId(rs.getInt(1));
                        sendOptionalEmail("sendReservationEmail", reservation);
                        sendOptionalEmail("sendPatientConfirmation", reservation);
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la reservation: " + e.getMessage());
            return false;
        }
    }

    public List<Reservation> getByServiceId(int serviceId) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE service_id = ? ORDER BY date_rendez_vous DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des reservations: " + e.getMessage());
        }
        return reservations;
    }

    public List<Reservation> getAll() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation ORDER BY date_rendez_vous DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ensureTableExists(conn);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des reservations: " + e.getMessage());
        }
        return reservations;
    }

    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReservation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation de la reservation: " + e.getMessage());
        }
        return null;
    }

    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservation SET statut = ?, date_rendez_vous = ?, motif = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setString(1, reservation.getStatut());
            stmt.setTimestamp(2, Timestamp.valueOf(reservation.getDateRendezVous()));
            stmt.setString(3, reservation.getMotif());
            stmt.setInt(4, reservation.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise a jour de la reservation: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM reservation WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la reservation: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int reservationId, String newStatus) {
        Reservation reservation = getById(reservationId);
        if (reservation == null) {
            return false;
        }
        reservation.setStatut(newStatus);
        boolean updated = update(reservation);
        if (updated) {
            sendOptionalEmail("sendStatusEmail", reservation);
        }
        return updated;
    }

    private void ensureTableExists(Connection conn) throws SQLException {
        if (tableChecked) {
            return;
        }

        String sql = """
            CREATE TABLE IF NOT EXISTS reservation (
              id INT NOT NULL AUTO_INCREMENT,
              service_id INT NOT NULL,
              nom_client VARCHAR(255) NOT NULL,
              email_client VARCHAR(255) NOT NULL,
              telephone_client VARCHAR(50) NOT NULL,
              date_reservation DATETIME NOT NULL,
              date_rendez_vous DATETIME NOT NULL,
              motif TEXT,
              statut VARCHAR(50) NOT NULL DEFAULT 'PENDING',
              date_creation DATETIME NOT NULL,
              PRIMARY KEY (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
            tableChecked = true;
        }
    }

    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("service_id"),
                rs.getString("nom_client"),
                rs.getString("email_client"),
                rs.getString("telephone_client"),
                rs.getTimestamp("date_reservation").toLocalDateTime(),
                rs.getTimestamp("date_rendez_vous").toLocalDateTime(),
                rs.getString("motif"),
                rs.getString("statut"),
                rs.getTimestamp("date_creation").toLocalDateTime()
        );
    }

    private void sendOptionalEmail(String methodName, Reservation reservation) {
        CompletableFuture.runAsync(() -> {
            try {
                Class<?> emailClass = Class.forName("org.example.service.EmailService");
                Object emailService = emailClass.getMethod("getInstance").invoke(null);
                Method method = emailClass.getMethod(methodName, Reservation.class);
                method.invoke(emailService, reservation);
            } catch (Exception e) {
                System.err.println("Email optionnel ignore: " + e.getMessage());
            }
        });
    }
}
