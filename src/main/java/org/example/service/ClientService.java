package org.example.service;

import org.example.model.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService {
    public void add(Client client) {
    }

    public void update(Client client) {
    }

    public void delete(int id) {
    }

    public List<Client> getAll() {
    }

    public List<Client> search(String query) {
                .filter(c -> c.getNom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getPrenom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getEmail().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Client getById(int id) {
    }
}
