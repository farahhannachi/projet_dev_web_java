package org.example.service;

import org.example.model.Client;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService {
    public void add(Client client) {
        throw new UnsupportedOperationException("Creation client non implementee dans ce module JavaFX");
    }

    public void update(Client client) {
        throw new UnsupportedOperationException("Mise a jour client non implementee dans ce module JavaFX");
    }

    public void delete(int id) {
        throw new UnsupportedOperationException("Suppression client non implementee dans ce module JavaFX");
    }

    public List<Client> getAll() {
        String sql = "SELECT id_utilisateur, nom, prenom, email, telephone, date_naissance FROM utilisateur ORDER BY id_utilisateur DESC";
        List<Client> clients = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                java.sql.Date dob = resultSet.getDate("date_naissance");
                LocalDate dateNaissance = dob != null ? dob.toLocalDate() : null;

                clients.add(new Client(
                        resultSet.getInt("id_utilisateur"),
                        resultSet.getString("nom"),
                        resultSet.getString("prenom"),
                        resultSet.getString("email"),
                        resultSet.getString("telephone"),
                        dateNaissance,
                        ""
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des clients", e);
        }

        return clients;
    }

    public List<Client> search(String query) {
        return getAll().stream()
                .filter(c -> c.getNom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getPrenom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getEmail().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Client getById(int id) {
        return getAll().stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }
}
