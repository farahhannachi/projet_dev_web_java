package org.example.service;

import org.example.model.Produit;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les Produits avec base de données
 */
public class ProduitService {
    private static ProduitService instance;

    private ProduitService() {}

    public static ProduitService getInstance() {
        if (instance == null) {
            instance = new ProduitService();
        }
        return instance;
    }

    public void add(Produit produit) {
        String sql = "INSERT INTO produit (nom, description, prix, quantite_stock, date_expiration, categorie, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getQuantiteStock());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().plusDays(365))); // Expiration dans 1 an
            stmt.setString(6, produit.getCategorie());
            stmt.setString(7, produit.isDisponible() ? "disponible" : "indisponible");

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produit.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du produit", e);
        }
    }

    public void update(Produit produit) {
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, quantite_stock=?, categorie=? WHERE id_produit=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getQuantiteStock());
            stmt.setString(5, produit.getCategorie());
            stmt.setInt(6, produit.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du produit", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM produit WHERE id_produit=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }

    public List<Produit> getAll() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit ORDER BY nom";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produit produit = new Produit();
                produit.setId(rs.getInt("id_produit"));
                produit.setNom(rs.getString("nom"));
                produit.setDescription(rs.getString("description"));
                produit.setPrix(rs.getDouble("prix"));
                produit.setQuantiteStock(rs.getInt("quantite_stock"));
                produit.setCategorie(rs.getString("categorie"));
                String statut = rs.getString("statut");
                produit.setDisponible(statut != null && statut.equals("disponible"));
                produits.add(produit);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des produits: " + e.getMessage());
            // Retourner liste vide en cas d'erreur DB
            return new ArrayList<>();
        }
        return produits;
    }

    public List<Produit> search(String query) {
        return getAll().stream()
                .filter(p -> p.getNom().toLowerCase().contains(query.toLowerCase()) ||
                             p.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                             p.getCategorie().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Produit getById(int id) {
        String sql = "SELECT * FROM produit WHERE id_produit=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Produit produit = new Produit();
                    produit.setId(rs.getInt("id_produit"));
                    produit.setNom(rs.getString("nom"));
                    produit.setDescription(rs.getString("description"));
                    produit.setPrix(rs.getDouble("prix"));
                    produit.setQuantiteStock(rs.getInt("quantite_stock"));
                    produit.setCategorie(rs.getString("categorie"));
                    String statut = rs.getString("statut");
                    produit.setDisponible(statut != null && statut.equals("disponible"));
                    return produit;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du produit", e);
        }
        return null;
    }
}
