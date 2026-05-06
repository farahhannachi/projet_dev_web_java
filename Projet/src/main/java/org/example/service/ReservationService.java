package org.example.service;

import org.example.model.Reservation;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReservationService {
    private static ReservationService instance;

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
                        // Send email notifications asynchronously after successful reservation
                        CompletableFuture.runAsync(() -> {
                            try {
                                EmailService.getInstance().sendReservationEmail(reservation);
                            } catch (Exception e) {
                                System.err.println("Erreur lors de l'envoi de l'email admin: " + e.getMessage());
                            }
                        });
                        CompletableFuture.runAsync(() -> {
                            try {
                                EmailService.getInstance().sendPatientConfirmation(reservation);
                            } catch (Exception e) {
                                System.err.println("Erreur lors de l'envoi de l'email patient: " + e.getMessage());
                            }
                        });
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la réservation: " + e.getMessage());
            return false;
        }
    }

    public List<Reservation> getByServiceId(int serviceId) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE service_id = ? ORDER BY date_rendez_vous DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serviceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réservations: " + e.getMessage());
        }
        return reservations;
    }

    public List<Reservation> getAll() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation ORDER BY date_rendez_vous DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapRowToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réservations: " + e.getMessage());
        }
        return reservations;
    }

    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReservation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la réservation: " + e.getMessage());
        }
        return null;
    }

    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservation SET statut = ?, date_rendez_vous = ?, motif = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatut());
            stmt.setTimestamp(2, Timestamp.valueOf(reservation.getDateRendezVous()));
            stmt.setString(3, reservation.getMotif());
            stmt.setInt(4, reservation.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la réservation: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM reservation WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la réservation: " + e.getMessage());
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
            // Send status update email asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    EmailService.getInstance().sendStatusEmail(reservation);
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi de l'email de statut: " + e.getMessage());
                }
            });
        }
        return updated;
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
}
