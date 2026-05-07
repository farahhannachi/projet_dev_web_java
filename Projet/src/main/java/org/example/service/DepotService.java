package org.example.service;

import org.example.model.Depot;
import org.example.util.DatabaseUtil;
import org.example.util.DepotValidator;
import org.example.util.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DepotService {
    private static final String INSERT_SQL = """
        INSERT INTO depot (
            nom_depot,
            adresse_depot,
            ville,
            capacite_depot,
            responsable_depot,
            responsable_telephone,
            date_creation,
            latitude,
            longitude,
            location_name
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE depot
        SET nom_depot=?,
            adresse_depot=?,
            ville=?,
            capacite_depot=?,
            responsable_depot=?,
            responsable_telephone=?,
            latitude=?,
            longitude=?,
            location_name=?
        WHERE id_depot=?
        """;

    private static DepotService instance;
    private String validationErrors = "";

    private DepotService() {
    }

    public static DepotService getInstance() {
        if (instance == null) {
            instance = new DepotService();
        }
        return instance;
    }

    private boolean validateDepot(Depot depot) {
        DepotValidator validator = new DepotValidator();
        boolean isValid = validator.validate(
                depot.getNom(),
                depot.getAdresse(),
                depot.getVille(),
                String.valueOf(depot.getCapaciteDepot()),
                depot.getResponsableDepot(),
                depot.getResponsableTelephone(),
                String.valueOf(depot.getLatitude()),
                String.valueOf(depot.getLongitude()),
                depot.getLocationName()
        );

        if (!isValid) {
            validationErrors = validator.getErrorMessage();
        }

        return isValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public boolean add(Depot depot) throws ValidationException {
        if (!validateDepot(depot)) {
            throw new ValidationException("Validation echouee: " + validationErrors);
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, depot.getNom());
            stmt.setString(2, depot.getAdresse());
            stmt.setString(3, depot.getVille());
            stmt.setInt(4, depot.getCapaciteDepot());
            stmt.setString(5, depot.getResponsableDepot());
            stmt.setString(6, depot.getResponsableTelephone());
            stmt.setTimestamp(7, Timestamp.valueOf(
                    depot.getDateCreation() != null ? depot.getDateCreation() : LocalDateTime.now()
            ));
            stmt.setDouble(8, depot.getLatitude());
            stmt.setDouble(9, depot.getLongitude());
            stmt.setString(10, depot.getLocationName());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    depot.setId(generatedKeys.getInt(1));
                }
            }
            return true;
        } catch (SQLException exception) {
            throw new RuntimeException("Erreur lors de l'ajout du depot", exception);
        }
    }

    public boolean update(Depot depot) throws ValidationException {
        if (!validateDepot(depot)) {
            throw new ValidationException("Validation echouee: " + validationErrors);
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, depot.getNom());
            stmt.setString(2, depot.getAdresse());
            stmt.setString(3, depot.getVille());
            stmt.setInt(4, depot.getCapaciteDepot());
            stmt.setString(5, depot.getResponsableDepot());
            stmt.setString(6, depot.getResponsableTelephone());
            stmt.setDouble(7, depot.getLatitude());
            stmt.setDouble(8, depot.getLongitude());
            stmt.setString(9, depot.getLocationName());
            stmt.setInt(10, depot.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new RuntimeException("Erreur lors de la modification du depot", exception);
        }
    }

    public void delete(int id) {
        if (hasAssociatedStocks(id)) {
            throw new RuntimeException("Cannot delete depot: it has associated stocks. Please remove all stocks from this depot first.");
        }

        String sql = "DELETE FROM depot WHERE id_depot=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Error deleting depot", exception);
        }
    }

    private boolean hasAssociatedStocks(int depotId) {
        String sql = "SELECT COUNT(*) FROM stock WHERE depot_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, depotId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Error checking associated stocks", exception);
        }
    }

    public List<Depot> getAll() {
        List<Depot> depots = new ArrayList<>();
        String sql = "SELECT * FROM depot ORDER BY nom_depot";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                depots.add(mapDepot(rs));
            }
        } catch (SQLException exception) {
            System.err.println("Erreur lors de la recuperation des depots: " + exception.getMessage());
            return new ArrayList<>();
        }
        return depots;
    }

    public List<Depot> search(String query) {
        String searchTerm = query == null ? "" : query.toLowerCase();
        return getAll().stream()
                .filter(depot -> (depot.getNom() != null && depot.getNom().toLowerCase().contains(searchTerm))
                        || (depot.getAdresse() != null && depot.getAdresse().toLowerCase().contains(searchTerm))
                        || (depot.getLocationName() != null && depot.getLocationName().toLowerCase().contains(searchTerm)))
                .collect(Collectors.toList());
    }

    public Depot getById(int id) {
        String sql = "SELECT * FROM depot WHERE id_depot=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDepot(rs);
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Error getting depot by id", exception);
        }
        return null;
    }

    private Depot mapDepot(ResultSet rs) throws SQLException {
        Depot depot = new Depot();
        depot.setId(rs.getInt("id_depot"));
        depot.setNom(rs.getString("nom_depot"));
        depot.setAdresse(rs.getString("adresse_depot"));
        depot.setVille(rs.getString("ville"));
        depot.setCapaciteDepot(rs.getInt("capacite_depot"));
        depot.setResponsableDepot(rs.getString("responsable_depot"));
        depot.setResponsableTelephone(rs.getString("responsable_telephone"));

        Timestamp createdAt = rs.getTimestamp("date_creation");
        if (createdAt != null) {
            depot.setDateCreation(createdAt.toLocalDateTime());
        }

        depot.setLatitude(readDouble(rs, "latitude"));
        depot.setLongitude(readDouble(rs, "longitude"));
        depot.setLocationName(readOptionalString(rs, "location_name", defaultLocationName(depot)));
        return depot;
    }

    private double readDouble(ResultSet rs, String columnName) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return 0d;
        }
        return rs.getDouble(columnName);
    }

    private String readOptionalString(ResultSet rs, String columnName, String fallback) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return fallback;
        }

        String value = rs.getString(columnName);
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String label = metaData.getColumnLabel(index);
            String name = metaData.getColumnName(index);
            if (columnName.equalsIgnoreCase(label) || columnName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private String defaultLocationName(Depot depot) {
        if (depot.getVille() != null && !depot.getVille().isBlank()) {
            return depot.getVille();
        }
        if (depot.getAdresse() != null && !depot.getAdresse().isBlank()) {
            return depot.getAdresse();
        }
        return "";
    }
}
