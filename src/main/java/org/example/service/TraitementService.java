package org.example.service;

import org.example.model.Traitement;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TraitementService {
    private static TraitementService instance;

    private TraitementService() {
    }

    public static TraitementService getInstance() {
        if (instance == null) {
            instance = new TraitementService();
        }
        return instance;
    }

    public boolean add(Traitement traitement) {
        String sql = "INSERT INTO traitement (id_utilisateur_id, dosage, frequence, duree_jours, date_debut, date_fin, status, notes, id_ordonnance_id, id_produit_id, repas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, traitement.getIdUtilisateurId());
            stmt.setString(2, traitement.getDosage());
            stmt.setString(3, traitement.getFrequence());
            stmt.setInt(4, traitement.getDureeJours());
            stmt.setTimestamp(5, traitement.getDateDebut() != null ? Timestamp.valueOf(traitement.getDateDebut()) : null);
            stmt.setTimestamp(6, traitement.getDateFin() != null ? Timestamp.valueOf(traitement.getDateFin()) : null);
            stmt.setString(7, traitement.getStatus());
            stmt.setString(8, traitement.getNotes());
            stmt.setInt(9, traitement.getIdOrdonnanceId());
            stmt.setInt(10, traitement.getIdProduitId());
            stmt.setString(11, traitement.getRepas());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    traitement.setIdTraitement(rs.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du traitement", e);
        }
    }

    public boolean update(Traitement traitement) {
        String sql = "UPDATE traitement SET id_utilisateur_id=?, dosage=?, frequence=?, duree_jours=?, date_debut=?, date_fin=?, status=?, notes=?, id_ordonnance_id=?, id_produit_id=?, repas=? WHERE id_traitement=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, traitement.getIdUtilisateurId());
            stmt.setString(2, traitement.getDosage());
            stmt.setString(3, traitement.getFrequence());
            stmt.setInt(4, traitement.getDureeJours());
            stmt.setTimestamp(5, traitement.getDateDebut() != null ? Timestamp.valueOf(traitement.getDateDebut()) : null);
            stmt.setTimestamp(6, traitement.getDateFin() != null ? Timestamp.valueOf(traitement.getDateFin()) : null);
            stmt.setString(7, traitement.getStatus());
            stmt.setString(8, traitement.getNotes());
            stmt.setInt(9, traitement.getIdOrdonnanceId());
            stmt.setInt(10, traitement.getIdProduitId());
            stmt.setString(11, traitement.getRepas());
            stmt.setInt(12, traitement.getIdTraitement());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du traitement", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM traitement WHERE id_traitement=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du traitement", e);
        }
    }

    public List<Traitement> getAll() {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement ORDER BY id_traitement DESC";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                traitements.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des traitements: " + e.getMessage());
            return new ArrayList<>();
        }
        return traitements;
    }

    public List<Traitement> search(String query) {
        return getAll().stream()
                .filter(t -> (t.getDosage() != null && t.getDosage().toLowerCase().contains(query.toLowerCase())) ||
                             (t.getStatus() != null && t.getStatus().toLowerCase().contains(query.toLowerCase())) ||
                             (t.getNotes() != null && t.getNotes().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Traitement getById(int id) {
        String sql = "SELECT * FROM traitement WHERE id_traitement=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du traitement", e);
        }
        return null;
    }

    public List<Traitement> getByOrdonnanceId(int ordonnanceId) {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement WHERE id_ordonnance_id=?";
        try (Connection conn = DatabaseUtil.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ordonnanceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    traitements.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des traitements par ordonnance", e);
        }
        return traitements;
    }

    private Traitement mapResultSet(ResultSet rs) throws SQLException {
        Traitement t = new Traitement();
        t.setIdTraitement(rs.getInt("id_traitement"));
        t.setIdUtilisateurId(rs.getInt("id_utilisateur_id"));
        t.setDosage(rs.getString("dosage"));
        t.setFrequence(rs.getString("frequence"));
        t.setDureeJours(rs.getInt("duree_jours"));
        t.setDateDebut(rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
        t.setDateFin(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
        t.setStatus(rs.getString("status"));
        t.setNotes(rs.getString("notes"));
        t.setIdOrdonnanceId(rs.getInt("id_ordonnance_id"));
        t.setIdProduitId(rs.getInt("id_produit_id"));
        t.setRepas(rs.getString("repas"));
        return t;
    }
}
