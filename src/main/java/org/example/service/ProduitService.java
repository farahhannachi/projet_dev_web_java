package org.example.service;

import org.example.model.Produit;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des produits avec recherche avancée et archivage
 */
public class ProduitService {
    public void add(Produit produit) {
        String sql = "INSERT INTO produit (nom, description, prix, quantite_stock, date_expiration, categorie, image, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, produit.getNom());
            statement.setString(2, produit.getDescription());
            statement.setDouble(3, produit.getPrix());
            statement.setInt(4, produit.getQuantiteStock());

            if (produit.getDateExpiration() != null) {
                statement.setTimestamp(5, Timestamp.valueOf(produit.getDateExpiration().atStartOfDay()));
            } else {
                statement.setTimestamp(5, null);
            }

            statement.setString(6, produit.getCategorie());
            statement.setString(7, produit.getImage());
            statement.setString(8, normalizeStatut(produit));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du produit", e);
        }
    }

    public void update(Produit produit) {
        String sql = "UPDATE produit SET nom = ?, description = ?, prix = ?, quantite_stock = ?, date_expiration = ?, " +
                "categorie = ?, image = ?, statut = ? WHERE id_produit = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, produit.getNom());
            statement.setString(2, produit.getDescription());
            statement.setDouble(3, produit.getPrix());
            statement.setInt(4, produit.getQuantiteStock());

            if (produit.getDateExpiration() != null) {
                statement.setTimestamp(5, Timestamp.valueOf(produit.getDateExpiration().atStartOfDay()));
            } else {
                statement.setTimestamp(5, null);
            }

            statement.setString(6, produit.getCategorie());
            statement.setString(7, produit.getImage());
            statement.setString(8, normalizeStatut(produit));
            statement.setInt(9, produit.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du produit", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM produit WHERE id_produit = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }

    /**
     * Archive un produit (soft delete)
     */
    public void archiver(int id) {
        Produit produit = getById(id);
        if (produit != null) {
            produit.setStatut("indisponible");
            update(produit);
        }
    }

    public List<Produit> getAll() {
        String sql = "SELECT id_produit, nom, description, prix, quantite_stock, date_expiration, categorie, image, statut FROM produit ORDER BY nom ASC";
        List<Produit> produits = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                produits.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des produits", e);
        }

        return produits;
    }

    /**
     * Retourne seulement les produits actifs (non archivés)
     */
    public List<Produit> getProduitActifs() {
        return getAll().stream()
                .filter(p -> p.isActif() && !p.isArchive())
                .collect(Collectors.toList());
    }

    /**
     * Recherche par catégorie
     */
    public List<Produit> rechercherParCategorie(String categorie) {
        return getAll().stream()
                .filter(p -> !p.isArchive() && p.getCategorie().equalsIgnoreCase(categorie))
                .collect(Collectors.toList());
    }

    /**
     * Recherche par plage de prix
     */
    public List<Produit> rechercherParPrix(double min, double max) {
        return getAll().stream()
                .filter(p -> !p.isArchive() && p.getPrixUnitaire() >= min && p.getPrixUnitaire() <= max)
                .collect(Collectors.toList());
    }

    /**
     * Recherche générale (texte)
     */
    public List<Produit> search(String query) {
        return getAll().stream()
                .filter(p -> !p.isArchive() && (
                        p.getNom().toLowerCase().contains(query.toLowerCase()) ||
                        p.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                        p.getCategorie().toLowerCase().contains(query.toLowerCase()) ||
                        p.getCodeSku().toLowerCase().contains(query.toLowerCase())
                ))
                .collect(Collectors.toList());
    }

    public Produit getById(int id) {
        String sql = "SELECT id_produit, nom, description, prix, quantite_stock, date_expiration, categorie, image, statut " +
                "FROM produit WHERE id_produit = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du produit", e);
        }

        return null;
    }

    /**
     * Retourne les produits avec stock faible
     */
    public List<Produit> getProduitStockFaible() {
        return getAll().stream()
                .filter(p -> !p.isArchive() && p.estStockFaible())
                .collect(Collectors.toList());
    }

    /**
     * Retourne toutes les catégories uniques
     */
    public List<String> getToutesCategories() {
        return getAll().stream()
                .filter(p -> !p.isArchive())
                .map(Produit::getCategorie)
                .distinct()
                .collect(Collectors.toList());
    }

    private Produit mapRow(ResultSet resultSet) throws SQLException {
        Timestamp dateExpirationTs = resultSet.getTimestamp("date_expiration");
        LocalDate expiration = dateExpirationTs != null ? dateExpirationTs.toLocalDateTime().toLocalDate() : null;
        return new Produit(
                resultSet.getInt("id_produit"),
                resultSet.getString("nom"),
                resultSet.getString("description"),
                resultSet.getDouble("prix"),
                resultSet.getInt("quantite_stock"),
                expiration,
                resultSet.getString("categorie"),
                resultSet.getString("image"),
                resultSet.getString("statut")
        );
    }

    private String normalizeStatut(Produit produit) {
        if (produit.getStatut() != null && !produit.getStatut().isBlank()) {
            return produit.getStatut();
        }
        return produit.isActif() ? "disponible" : "indisponible";
    }
}
