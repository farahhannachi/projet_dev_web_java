package org.example.service;

import org.example.model.Service;
import org.example.util.DatabaseUtil;
import org.example.util.ServiceValidator;
import org.example.util.ValidationException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceService {
    private static ServiceService instance;
    private String validationErrors = "";

    private ServiceService() {}

    public static ServiceService getInstance() {
        if (instance == null) {
            instance = new ServiceService();
        }
        return instance;
    }

    /**
     * Valide un service avant insertion en base
     */
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
            this.validationErrors = validator.getErrorMessage();
        }

        return isValid;
    }

    /**
     * Retourne les erreurs de validation
     */
    public String getValidationErrors() {
        return validationErrors;
    }

    public boolean add(Service service) throws ValidationException {
        // Validation côté serveur (backend) - OBLIGATOIRE avant insertion
        if (!validateService(service)) {
            throw new ValidationException("Validation échouée: " + validationErrors);
        }

        String sql = "INSERT INTO service (nom_service, type_service, specialite, telephone, email, adresse, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

                    // ✨ GÉNÉRER LE QR CODE AUTOMATIQUEMENT APRÈS AJOUT
                    try {
                        QRService qrService = QRService.getInstance();
                        String qrPath = qrService.generateServiceQRCodeWithInfo(
                            service.getId(),
                            service.getNom(),
                            service.getType(),
                            service.getSpecialite()
                        );
                        if (qrPath != null) {
                            System.out.println("✅ QR code généré pour service: " + service.getNom() + " → " + qrPath);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ QR code non généré (mais service créé): " + e.getMessage());
                        // Ne pas échouer l'ajout du service si QR échoue
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout du service: " + e.getMessage(), e);
        }
    }

    public boolean update(Service service) throws ValidationException {
        // Validation côté serveur (backend) - OBLIGATOIRE avant update
        if (!validateService(service)) {
            throw new ValidationException("Validation échouée: " + validationErrors);
        }

        String sql = "UPDATE service SET nom_service=?, type_service=?, specialite=?, telephone=?, email=?, adresse=? WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, service.getNom());
            stmt.setString(2, service.getType());
            stmt.setString(3, service.getSpecialite());
            stmt.setString(4, service.getTelephone());
            stmt.setString(5, service.getEmail());
            stmt.setString(6, service.getAdresse());
            stmt.setInt(7, service.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la modification du service: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM service WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Service service = new Service();
                service.setId(rs.getInt("id_service"));
                service.setNom(rs.getString("nom_service"));
                service.setType(rs.getString("type_service"));
                service.setSpecialite(rs.getString("specialite"));
                service.setTelephone(rs.getString("telephone"));
                service.setEmail(rs.getString("email"));
                service.setAdresse(rs.getString("adresse"));
                service.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                services.add(service);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des services: " + e.getMessage());
            // Return empty list if DB fails
            return new ArrayList<>();
        }
        return services;
    }

    public List<Service> search(String query) {
        return getAll().stream()
                .filter(s -> s.getNom().toLowerCase().contains(query.toLowerCase()) ||
                             s.getType().toLowerCase().contains(query.toLowerCase()) ||
                             s.getSpecialite().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Service getById(int id) {
        String sql = "SELECT * FROM service WHERE id_service=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Service service = new Service();
                    service.setId(rs.getInt("id_service"));
                    service.setNom(rs.getString("nom_service"));
                    service.setType(rs.getString("type_service"));
                    service.setSpecialite(rs.getString("specialite"));
                    service.setTelephone(rs.getString("telephone"));
                    service.setEmail(rs.getString("email"));
                    service.setAdresse(rs.getString("adresse"));
                    service.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                    return service;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du service par ID", e);
        }
        return null;
    }
}
