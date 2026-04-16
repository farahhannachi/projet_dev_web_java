package org.example.service;

import org.example.model.Ordonnance;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrdonnanceService {
    private static OrdonnanceService instance;

    private OrdonnanceService() {
    }

    public static OrdonnanceService getInstance() {
        if (instance == null) {
            instance = new OrdonnanceService();
        }
        return instance;
    }

    public boolean add(Ordonnance ordonnance) {
        String sql = "INSERT INTO ordonnance (numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, id_utilisateur_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, ordonnance.getNumeroOrdonnance());
            stmt.setTimestamp(2, ordonnance.getDateOrdonnance() != null ? Timestamp.valueOf(ordonnance.getDateOrdonnance()) : null);
            stmt.setTimestamp(3, ordonnance.getDateExpiration() != null ? Timestamp.valueOf(ordonnance.getDateExpiration()) : null);
            stmt.setString(4, ordonnance.getStatut());
            stmt.setString(5, ordonnance.getNoteMedical());
            stmt.setInt(6, ordonnance.getIdUtilisateurId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ordonnance.setIdOrdonnance(rs.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de l'ordonnance", e);
        }
    }

    public boolean update(Ordonnance ordonnance) {
        String sql = "UPDATE ordonnance SET numero_ordonnance=?, date_ordonnance=?, date_expiration=?, statut=?, note_medical=?, id_utilisateur_id=? WHERE id_ordonnance=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ordonnance.getNumeroOrdonnance());
            stmt.setTimestamp(2, ordonnance.getDateOrdonnance() != null ? Timestamp.valueOf(ordonnance.getDateOrdonnance()) : null);
            stmt.setTimestamp(3, ordonnance.getDateExpiration() != null ? Timestamp.valueOf(ordonnance.getDateExpiration()) : null);
            stmt.setString(4, ordonnance.getStatut());
            stmt.setString(5, ordonnance.getNoteMedical());
            stmt.setInt(6, ordonnance.getIdUtilisateurId());
            stmt.setInt(7, ordonnance.getIdOrdonnance());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de l'ordonnance", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM ordonnance WHERE id_ordonnance=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'ordonnance", e);
        }
    }

    public List<Ordonnance> getAll() {
        List<Ordonnance> ordonnances = new ArrayList<>();
        String sql = "SELECT * FROM ordonnance ORDER BY date_ordonnance DESC";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ordonnances.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des ordonnances: " + e.getMessage());
            return new ArrayList<>();
        }
        return ordonnances;
    }

    public List<Ordonnance> search(String query) {
        return getAll().stream()
                .filter(o -> (o.getNumeroOrdonnance() != null && o.getNumeroOrdonnance().toLowerCase().contains(query.toLowerCase())) ||
                             (o.getStatut() != null && o.getStatut().toLowerCase().contains(query.toLowerCase())) ||
                             (o.getNoteMedical() != null && o.getNoteMedical().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Ordonnance getById(int id) {
        String sql = "SELECT * FROM ordonnance WHERE id_ordonnance=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de l'ordonnance", e);
        }
        return null;
    }

    private Ordonnance mapResultSet(ResultSet rs) throws SQLException {
        Ordonnance o = new Ordonnance();
        o.setIdOrdonnance(rs.getInt("id_ordonnance"));
        o.setNumeroOrdonnance(rs.getString("numero_ordonnance"));
        o.setDateOrdonnance(rs.getTimestamp("date_ordonnance") != null ? rs.getTimestamp("date_ordonnance").toLocalDateTime() : null);
        o.setDateExpiration(rs.getTimestamp("date_expiration") != null ? rs.getTimestamp("date_expiration").toLocalDateTime() : null);
        o.setStatut(rs.getString("statut"));
        o.setNoteMedical(rs.getString("note_medical"));
        o.setIdUtilisateurId(rs.getInt("id_utilisateur_id"));
        return o;
    }
}
