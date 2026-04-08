package org.example.services;

import org.example.entities.Traitement;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TraitementService {

    private final Connection connection;

    public TraitementService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public void insert(Traitement t) throws SQLException {
        String sql = "INSERT INTO traitement (id_ordonnance, id_utilisateur, dosage, frequence, duree_jours, date_debut, date_fin, status, notes, id_produit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, t.getIdOrdonnance());
        ps.setInt(2, t.getIdUtilisateur());
        ps.setString(3, t.getDosage());
        ps.setString(4, t.getFrequence());
        ps.setInt(5, t.getDureeJours());
        ps.setString(6, t.getDateDebut());
        ps.setString(7, t.getDateFin());
        ps.setString(8, t.getStatus());
        ps.setString(9, t.getNotes());
        ps.setInt(10, t.getIdProduit());
        ps.executeUpdate();
    }

    public List<Traitement> getAll() throws SQLException {
        List<Traitement> list = new ArrayList<>();
        String sql = "SELECT * FROM traitement";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Traitement t = new Traitement();
            t.setIdTraitement(rs.getInt("id_traitement"));
            t.setIdOrdonnance(rs.getInt("id_ordonnance"));
            t.setIdUtilisateur(rs.getInt("id_utilisateur"));
            t.setDosage(rs.getString("dosage"));
            t.setFrequence(rs.getString("frequence"));
            t.setDureeJours(rs.getInt("duree_jours"));
            t.setDateDebut(rs.getString("date_debut"));
            t.setDateFin(rs.getString("date_fin"));
            t.setStatus(rs.getString("status"));
            t.setNotes(rs.getString("notes"));
            t.setIdProduit(rs.getInt("id_produit"));
            list.add(t);
        }
        return list;
    }

    public Traitement getById(int id) throws SQLException {
        String sql = "SELECT * FROM traitement WHERE id_traitement = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Traitement t = new Traitement();
            t.setIdTraitement(rs.getInt("id_traitement"));
            t.setIdOrdonnance(rs.getInt("id_ordonnance"));
            t.setIdUtilisateur(rs.getInt("id_utilisateur"));
            t.setDosage(rs.getString("dosage"));
            t.setFrequence(rs.getString("frequence"));
            t.setDureeJours(rs.getInt("duree_jours"));
            t.setDateDebut(rs.getString("date_debut"));
            t.setDateFin(rs.getString("date_fin"));
            t.setStatus(rs.getString("status"));
            t.setNotes(rs.getString("notes"));
            t.setIdProduit(rs.getInt("id_produit"));
            return t;
        }
        return null;
    }

    public void update(Traitement t) throws SQLException {
        String sql = "UPDATE traitement SET id_ordonnance=?, id_utilisateur=?, dosage=?, frequence=?, duree_jours=?, date_debut=?, date_fin=?, status=?, notes=?, id_produit=? WHERE id_traitement=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, t.getIdOrdonnance());
        ps.setInt(2, t.getIdUtilisateur());
        ps.setString(3, t.getDosage());
        ps.setString(4, t.getFrequence());
        ps.setInt(5, t.getDureeJours());
        ps.setString(6, t.getDateDebut());
        ps.setString(7, t.getDateFin());
        ps.setString(8, t.getStatus());
        ps.setString(9, t.getNotes());
        ps.setInt(10, t.getIdProduit());
        ps.setInt(11, t.getIdTraitement());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM traitement WHERE id_traitement = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
