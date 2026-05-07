package org.example.service;

import org.example.model.Service;
import org.example.util.DatabaseUtil;
import org.example.util.ServiceValidator;
import org.example.util.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceService {
    private static ServiceService instance;
    private String validationErrors = "";
    private boolean tableChecked = false;

    private ServiceService() {}

    public static ServiceService getInstance() {
        if (instance == null) {
            instance = new ServiceService();
        }
        return instance;
    }

    private boolean validateService(Service service) {
        ServiceValidator validator = new ServiceValidator();
        boolean isValid = validator.validate(
                service.getNom(),
                service.getType(),
                service.getSpecialite(),
                service.getTelephone(),
                service.getEmail(),
                service.getAdresse()
        );

        if (!isValid) {
            validationErrors = validator.getErrorMessage();
        }

        return isValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public boolean add(Service service) throws ValidationException {
        if (!validateService(service)) {
            throw new ValidationException("Validation echouee: " + validationErrors);
        }

        String sql = "INSERT INTO service (nom_service, type_service, specialite, telephone, email, adresse, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ensureTableExists(conn);

            stmt.setString(1, service.getNom());
            stmt.setString(2, service.getType());
            stmt.setString(3, service.getSpecialite());
            stmt.setString(4, service.getTelephone());
            stmt.setString(5, service.getEmail());
            stmt.setString(6, service.getAdresse());
            stmt.setTimestamp(7, Timestamp.valueOf(service.getDateCreation() != null ? service.getDateCreation() : LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    service.setId(rs.getInt(1));
                    generateQrCode(service);
                }
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du service: " + e.getMessage(), e);
        }
    }

    public boolean update(Service service) throws ValidationException {
        if (!validateService(service)) {
            throw new ValidationException("Validation echouee: " + validationErrors);
        }

        String sql = "UPDATE service SET nom_service=?, type_service=?, specialite=?, telephone=?, email=?, adresse=? WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setString(1, service.getNom());
            stmt.setString(2, service.getType());
            stmt.setString(3, service.getSpecialite());
            stmt.setString(4, service.getTelephone());
            stmt.setString(5, service.getEmail());
            stmt.setString(6, service.getAdresse());
            stmt.setInt(7, service.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du service: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM service WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du service", e);
        }
    }

    public List<Service> getAll() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT * FROM service ORDER BY nom_service";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            ensureTableExists(conn);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    services.add(mapService(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des services: " + e.getMessage());
            return new ArrayList<>();
        }
        return services;
    }

    public List<Service> search(String query) {
        String safeQuery = query == null ? "" : query.toLowerCase();
        return getAll().stream()
                .filter(s -> contains(s.getNom(), safeQuery)
                        || contains(s.getType(), safeQuery)
                        || contains(s.getSpecialite(), safeQuery))
                .collect(Collectors.toList());
    }

    public Service getById(int id) {
        String sql = "SELECT * FROM service WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureTableExists(conn);

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapService(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du service par ID", e);
        }
        return null;
    }

    private void ensureTableExists(Connection conn) throws SQLException {
        if (tableChecked) {
            return;
        }

        String sql = """
            CREATE TABLE IF NOT EXISTS service (
              id_service INT NOT NULL AUTO_INCREMENT,
              nom_service VARCHAR(255) NOT NULL,
              type_service VARCHAR(50) NOT NULL,
              specialite VARCHAR(255) NOT NULL,
              telephone VARCHAR(50) NOT NULL,
              email VARCHAR(255) NOT NULL,
              adresse VARCHAR(255) NOT NULL,
              date_creation DATETIME NOT NULL,
              PRIMARY KEY (id_service)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
            tableChecked = true;
        }
    }

    private Service mapService(ResultSet rs) throws SQLException {
        Service service = new Service();
        service.setId(rs.getInt("id_service"));
        service.setNom(rs.getString("nom_service"));
        service.setType(rs.getString("type_service"));
        service.setSpecialite(rs.getString("specialite"));
        service.setTelephone(rs.getString("telephone"));
        service.setEmail(rs.getString("email"));
        service.setAdresse(rs.getString("adresse"));

        Timestamp createdAt = rs.getTimestamp("date_creation");
        if (createdAt != null) {
            service.setDateCreation(createdAt.toLocalDateTime());
        }
        return service;
    }

    private void generateQrCode(Service service) {
        try {
            Class<?> qrClass = Class.forName("org.example.service.QRService");
            Object qrService = qrClass.getMethod("getInstance").invoke(null);
            qrClass.getMethod("generateServiceQRCodeWithInfo", int.class, String.class, String.class, String.class)
                    .invoke(qrService, service.getId(), service.getNom(), service.getType(), service.getSpecialite());
        } catch (Exception e) {
            System.err.println("QR code non genere, mais service cree: " + e.getMessage());
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
