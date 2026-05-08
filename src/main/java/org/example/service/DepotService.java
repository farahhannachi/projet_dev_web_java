package org.example.service;

import org.example.model.Depot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DepotService {
    private static DepotService instance;

    private final List<Depot> depots = new ArrayList<>();
    private int nextId = 1;

    private DepotService() {}

    public static synchronized DepotService getInstance() {
        if (instance == null) {
            instance = new DepotService();
        }
        return instance;
    }

    public void add(Depot depot) {
        depot.setId(nextId++);
        depots.add(depot);
    }

    public void update(Depot depot) {
        for (int i = 0; i < depots.size(); i++) {
            if (depots.get(i).getId() == depot.getId()) {
                depots.set(i, depot);
                break;
            }
        }
    }

    public void delete(int id) {
        depots.removeIf(d -> d.getId() == id);
    }

    public List<Depot> getAll() {
        return new ArrayList<>(depots);
    }

    public List<Depot> search(String query) {
        return depots.stream()
                .filter(d -> d.getNom().toLowerCase().contains(query.toLowerCase())
                        || d.getAdresse().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Depot getById(int id) {
        return depots.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }
}
