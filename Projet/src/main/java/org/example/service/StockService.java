package org.example.service;

import org.example.model.Stock;
import org.example.model.Produit;
import org.example.model.Depot;
import org.example.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les Stocks avec jointure Depot
 * Utilise la base de données avec jointure SQL pour récupérer les informations du dépôt
 */
public class StockService {
    private static StockService instance;

    private StockService() {}

    public static StockService getInstance() {
        if (instance == null) {
            instance = new StockService();
        }
        return instance;
    }

    /**
     * Ajoute un stock en base de données
     */
    public void add(Stock stock) {
        String sql = "INSERT INTO stock (produit_id, depot_id, quantite, quantite_initiale, seuil_alerte, seuil_critique, date_entree, etat_stock, date_derniere_mise_a_jour) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, stock.getProduit().getId());
            stmt.setInt(2, stock.getDepot().getId());
            stmt.setInt(3, stock.getQuantiteDisponible());
            stmt.setInt(4, stock.getQuantiteDisponible()); // quantite_initiale = quantite
            stmt.setInt(5, stock.getSeuilMinimum());
            stmt.setInt(6, stock.getSeuilMinimum()); // seuil_critique = seuil_minimum
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(8, "actif");
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    stock.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du stock", e);
        }
    }

    /**
     * Met à jour un stock en base de données
     */
    public void update(Stock stock) {
        String sql = "UPDATE stock SET produit_id=?, depot_id=?, quantite=?, seuil_alerte=?, seuil_critique=?, date_derniere_mise_a_jour=? WHERE id_stock=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stock.getProduit().getId());
            stmt.setInt(2, stock.getDepot().getId());
            stmt.setInt(3, stock.getQuantiteDisponible());
            stmt.setInt(4, stock.getSeuilMinimum());
            stmt.setInt(5, stock.getSeuilMinimum());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, stock.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du stock", e);
        }
    }

    /**
     * Supprime un stock de la base de données
     */
    public void delete(int id) {
        String sql = "DELETE FROM stock WHERE id_stock=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du stock", e);
        }
    }

    /**
     * Récupère tous les stocks avec jointure complète Produit + Depot
     * Utilise LEFT JOIN pour récupérer les informations du produit et du dépôt
     */
    public List<Stock> getAll() {
        List<Stock> stocks = new ArrayList<>();
        String sql = """
            SELECT s.id_stock, s.quantite, s.seuil_alerte, s.seuil_critique,
                   p.id_produit, p.nom as produit_nom, p.description as produit_description, p.prix as produit_prix,
                   d.id_depot, d.nom_depot, d.adresse_depot, d.ville, d.capacite_depot, d.responsable_depot, d.responsable_telephone
            FROM stock s
            LEFT JOIN produit p ON s.produit_id = p.id_produit
            LEFT JOIN depot d ON s.depot_id = d.id_depot
            ORDER BY s.id_stock
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Stock stock = new Stock();
                stock.setId(rs.getInt("id_stock"));
                stock.setQuantiteDisponible(rs.getInt("quantite"));
                stock.setSeuilMinimum(rs.getInt("seuil_alerte"));

                // Créer et remplir le produit
                Produit produit = new Produit();
                produit.setId(rs.getInt("id_produit"));
                produit.setNom(rs.getString("produit_nom"));
                produit.setDescription(rs.getString("produit_description"));
                produit.setPrix(rs.getDouble("produit_prix"));
                stock.setProduit(produit);

                // Créer et remplir le dépôt
                Depot depot = new Depot();
                depot.setId(rs.getInt("id_depot"));
                depot.setNom(rs.getString("nom_depot"));
                depot.setAdresse(rs.getString("adresse_depot"));
                depot.setVille(rs.getString("ville"));
                depot.setCapaciteDepot(rs.getInt("capacite_depot"));
                depot.setResponsableDepot(rs.getString("responsable_depot"));
                depot.setResponsableTelephone(rs.getString("responsable_telephone"));
                stock.setDepot(depot);

                stocks.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des stocks avec jointure: " + e.getMessage());
            // Retourner liste vide en cas d'erreur DB
            return new ArrayList<>();
        }
        return stocks;
    }

    /**
     * Recherche des stocks avec jointure
     */
    public List<Stock> search(String query) {
        return getAll().stream()
                .filter(s -> (s.getProduit() != null && s.getProduit().getNom() != null &&
                             s.getProduit().getNom().toLowerCase().contains(query.toLowerCase())) ||
                            (s.getDepot() != null && s.getDepot().getNom() != null &&
                             s.getDepot().getNom().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    /**
     * Récupère un stock par ID avec jointure
     */
    public Stock getById(int id) {
        String sql = """
            SELECT s.*, 
                   d.nom_depot, d.adresse_depot, d.ville, d.capacite_depot, d.responsable_depot, d.responsable_telephone,
                   p.nom as produit_nom, p.description as produit_description, p.prix as produit_prix
            FROM stock s
            LEFT JOIN depot d ON s.depot_id = d.id_depot
            LEFT JOIN produit p ON s.produit_id = p.id_produit
            WHERE s.id_stock = ?
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Stock stock = new Stock();
                    stock.setId(rs.getInt("id_stock"));
                    stock.setQuantiteDisponible(rs.getInt("quantite"));
                    stock.setSeuilMinimum(rs.getInt("seuil_alerte"));

                    // Produit
                    Produit produit = new Produit();
                    produit.setId(rs.getInt("produit_id"));
                    produit.setNom(rs.getString("produit_nom"));
                    produit.setDescription(rs.getString("produit_description"));
                    produit.setPrix(rs.getDouble("produit_prix"));
                    stock.setProduit(produit);

                    // Dépôt
                    Depot depot = new Depot();
                    depot.setId(rs.getInt("depot_id"));
                    depot.setNom(rs.getString("nom_depot"));
                    depot.setAdresse(rs.getString("adresse_depot"));
                    depot.setVille(rs.getString("ville"));
                    depot.setCapaciteDepot(rs.getInt("capacite_depot"));
                    depot.setResponsableDepot(rs.getString("responsable_depot"));
                    depot.setResponsableTelephone(rs.getString("responsable_telephone"));
                    stock.setDepot(depot);

                    return stock;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du stock", e);
        }
        return null;
    }

    /**
     * Récupère les stocks faibles avec jointure
     */
    public List<Stock> getStocksFaibles() {
        return getAll().stream().filter(Stock::isStockFaible).collect(Collectors.toList());
    }

    /**
     * Récupère les stocks par dépôt avec jointure
     */
    public List<Stock> getStocksByDepot(int depotId) {
        return getAll().stream()
                .filter(s -> s.getDepot() != null && s.getDepot().getId() == depotId)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les stocks critiques (quantité inférieure au seuil critique)
     */
    public List<Stock> getStocksCritiques() {
        return getAll().stream()
                .filter(stock -> stock.getQuantiteDisponible() < stock.getSeuilMinimum())
                .collect(Collectors.toList());
    }

    /**
     * Récupère les mouvements d'entrée de stock
     */
    public List<Stock> getMouvementsEntree() {
        // Exemple : Filtrer les stocks ajoutés récemment
        return getAll().stream()
                .filter(stock -> stock.getDateDerniereMiseAJour() != null && stock.getDateDerniereMiseAJour().isAfter(LocalDateTime.now().minusDays(7)))
                .collect(Collectors.toList());
    }

    /**
     * Récupère les mouvements de sortie de stock
     */
    public List<Stock> getMouvementsSortie() {
        // Exemple : Filtrer les stocks ayant une réduction récente
        return getAll().stream()
                .filter(stock -> stock.getQuantiteDisponible() < stock.getQuantiteInitiale())
                .collect(Collectors.toList());
    }
}
