package org.example.service;

import org.example.model.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService {
    private List<Client> clients = new ArrayList<>();
    private int nextId = 1;

    public void add(Client client) {
        client.setId(nextId++);
        clients.add(client);
    }

    public void update(Client client) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId() == client.getId()) {
                clients.set(i, client);
                break;
            }
        }
    }

    public void delete(int id) {
        clients.removeIf(c -> c.getId() == id);
    }

    public List<Client> getAll() {
        return new ArrayList<>(clients);
    }

    public List<Client> search(String query) {
        return clients.stream()
                .filter(c -> c.getNom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getPrenom().toLowerCase().contains(query.toLowerCase()) ||
                             c.getEmail().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Client getById(int id) {
        return clients.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }
}
