package org.example.service;

import org.example.model.Address;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AddressService {
    private String resolvedUserIdColumn;

    public List<Address> getByUserId(int userId) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "SELECT id, " + userIdColumn + " AS id_utilisateur_id, full_name, line1, line2, city, region, postal_code, country, phone "
                + "FROM address WHERE " + userIdColumn + " = ? ORDER BY id DESC";
        List<Address> addresses = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addresses.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des adresses", e);
        }

        return addresses;
    }

    public Address getByIdForUser(int addressId, int userId) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "SELECT id, " + userIdColumn + " AS id_utilisateur_id, full_name, line1, line2, city, region, postal_code, country, phone "
            + "FROM address WHERE id = ? AND " + userIdColumn + " = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, addressId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de l'adresse", e);
        }

        return null;
    }

    public int add(Address address) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "INSERT INTO address (" + userIdColumn + ", full_name, line1, line2, city, region, postal_code, country, phone) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAddress(statement, address);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de l'adresse", e);
        }
    }

    public void update(Address address) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "UPDATE address SET full_name = ?, line1 = ?, line2 = ?, city = ?, region = ?, postal_code = ?, country = ?, phone = ? "
            + "WHERE id = ? AND " + userIdColumn + " = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trim(address.getFullName()));
            statement.setString(2, trim(address.getLine1()));
            if (address.getLine2() == null || address.getLine2().isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, trim(address.getLine2()));
            }
            statement.setString(4, trim(address.getCity()));
            statement.setString(5, trim(address.getRegion()));
            statement.setString(6, trim(address.getPostalCode()));
            statement.setString(7, trim(address.getCountry()));
            if (address.getPhone() == null || address.getPhone().isBlank()) {
                statement.setNull(8, Types.VARCHAR);
            } else {
                statement.setString(8, trim(address.getPhone()));
            }
            statement.setInt(9, address.getId());
            if (address.getUserId() == null) {
                statement.setNull(10, Types.INTEGER);
            } else {
                statement.setInt(10, address.getUserId());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour de l'adresse", e);
        }
    }

    public void deleteForUser(int addressId, int userId) {
        String userIdColumn = resolveUserIdColumn();
        String sql = "DELETE FROM address WHERE id = ? AND " + userIdColumn + " = ?";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, addressId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'adresse", e);
        }
    }

    private void bindAddress(PreparedStatement statement, Address address) throws SQLException {
        if (address.getUserId() == null) {
            statement.setNull(1, Types.INTEGER);
        } else {
            statement.setInt(1, address.getUserId());
        }
        statement.setString(2, trim(address.getFullName()));
        statement.setString(3, trim(address.getLine1()));
        if (address.getLine2() == null || address.getLine2().isBlank()) {
            statement.setNull(4, Types.VARCHAR);
        } else {
            statement.setString(4, trim(address.getLine2()));
        }
        statement.setString(5, trim(address.getCity()));
        statement.setString(6, trim(address.getRegion()));
        statement.setString(7, trim(address.getPostalCode()));
        statement.setString(8, trim(address.getCountry()));
        if (address.getPhone() == null || address.getPhone().isBlank()) {
            statement.setNull(9, Types.VARCHAR);
        } else {
            statement.setString(9, trim(address.getPhone()));
        }
    }

    private Address mapRow(ResultSet resultSet) throws SQLException {
        Address address = new Address();
        address.setId(resultSet.getInt("id"));
        int userId = resultSet.getInt("id_utilisateur_id");
        address.setUserId(resultSet.wasNull() ? null : userId);
        address.setFullName(resultSet.getString("full_name"));
        address.setLine1(resultSet.getString("line1"));
        address.setLine2(resultSet.getString("line2"));
        address.setCity(resultSet.getString("city"));
        address.setRegion(resultSet.getString("region"));
        address.setPostalCode(resultSet.getString("postal_code"));
        address.setCountry(resultSet.getString("country"));
        address.setPhone(resultSet.getString("phone"));
        return address;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveUserIdColumn() {
        if (resolvedUserIdColumn != null) {
            return resolvedUserIdColumn;
        }

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'address' AND COLUMN_NAME IN ('id_utilisateur_id', 'id_utilisateur')");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if ("id_utilisateur_id".equalsIgnoreCase(columnName)) {
                    resolvedUserIdColumn = "id_utilisateur_id";
                    return resolvedUserIdColumn;
                }
                if ("id_utilisateur".equalsIgnoreCase(columnName)) {
                    resolvedUserIdColumn = "id_utilisateur";
                }
            }
        } catch (SQLException e) {
            // fallback to current Symfony schema naming
        }

        if (resolvedUserIdColumn == null) {
            resolvedUserIdColumn = "id_utilisateur";
        }
        return resolvedUserIdColumn;
    }
}
