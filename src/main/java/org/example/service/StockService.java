package org.example.service;

import org.example.model.Produit;
import org.example.model.Stock;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des stocks avec règles métier avancées
 * - Réservation et libération de stock
 * - Vérification de disponibilité
 * - Alertes stock faible
 * - Gestion des stocks en dépôts
 */
public class StockService {
    private final ProduitService produitService = new ProduitService();
    private List<Stock> stocks = new ArrayList<>();
    private int nextId = 1;

    /**
     * Ajoute un stock (pour dépôt)
     */
    public void add(Stock stock) {
        stock.setId(nextId++);
        stocks.add(stock);
    }

    /**
     * Récupère tous les stocks
     */
    public List<Stock> getAll() {
        return new ArrayList<>(stocks);
    }

    /**
     * Retourne les stocks avec quantité faible
     */
    public List<Stock> getStocksFaibles() {
        String sql = "SELECT id_stock, quantite, seuil_alerte FROM stock WHERE is_actif = 1 AND quantite <= seuil_alerte";
        List<Stock> faibles = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Stock stock = new Stock();
                stock.setId(resultSet.getInt("id_stock"));
                stock.setQuantiteDisponible(resultSet.getInt("quantite"));
                stock.setSeuilMinimum(resultSet.getInt("seuil_alerte"));
                faibles.add(stock);
            }
            return faibles;
        } catch (SQLException e) {
            // fallback en memoire si la base est indisponible
        }

        return stocks.stream()
                .filter(Stock::isStockFaible)
                .collect(Collectors.toList());
    }

    /**
     * Réserve du stock pour une commande
     */
    public boolean reserverStock(int produitId, int quantite) {
        Produit produit = produitService.getById(produitId);
        if (produit == null) {
            throw new IllegalArgumentException("Produit non trouvé : " + produitId);
        }

        if (!verifierDisponibilite(produitId, quantite)) {
            return false;
        }

        produit.setQuantiteStock(produit.getQuantiteStock() - quantite);
        produitService.update(produit);
        return true;
    }

    /**
     * Libère du stock en cas d'annulation
     */
    public void libererStock(int produitId, int quantite) {
        Produit produit = produitService.getById(produitId);
        if (produit == null) {
            throw new IllegalArgumentException("Produit non trouvé : " + produitId);
        }
        
        produit.setQuantiteStock(produit.getQuantiteStock() + quantite);
        produitService.update(produit);
    }

    /**
     * Vérifie la disponibilité d'un produit
     */
    public boolean verifierDisponibilite(int produitId, int quantite) {
        String sql = "SELECT quantite_stock, statut FROM produit WHERE id_produit = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, produitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int qty = resultSet.getInt("quantite_stock");
                    String statut = resultSet.getString("statut");
                    return qty >= quantite && !"indisponible".equalsIgnoreCase(statut) && !"rupture".equalsIgnoreCase(statut);
                }
            }
        } catch (SQLException e) {
            // fallback en memoire
        }

        Produit produit = produitService.getById(produitId);
        if (produit == null) {
            return false;
        }
        return produit.estDisponible() && produit.getQuantiteStock() >= quantite;
    }

    /**
     * Retourne les produits avec stock faible dans les services
     */
    public List<Produit> alerteStockFaible() {
        List<Produit> tous = produitService.getAll();
        return tous.stream()
                .filter(p -> !p.isArchive() && p.estStockFaible())
                .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre de produits avec stock faible
     */
    public int compterStockFaible() {
        return alerteStockFaible().size();
    }
}
