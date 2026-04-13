package org.example.service;

import org.example.model.Ordonnance;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrdonnanceService {
    private List<Ordonnance> ordonnances = new ArrayList<>();
    private int nextId = 1;

    public void add(Ordonnance ordonnance) {
        ordonnance.setIdOrdonnance(nextId++);
        ordonnances.add(ordonnance);
    }

    public void update(Ordonnance ordonnance) {
        for (int i = 0; i < ordonnances.size(); i++) {
            if (ordonnances.get(i).getIdOrdonnance() == ordonnance.getIdOrdonnance()) {
                ordonnances.set(i, ordonnance);
                break;
            }
        }
    }

    public void delete(int id) {
        ordonnances.removeIf(o -> o.getIdOrdonnance() == id);
    }

    public List<Ordonnance> getAll() {
        return new ArrayList<>(ordonnances);
    }

    public List<Ordonnance> search(String query) {
        return ordonnances.stream()
                .filter(o -> (o.getNumeroOrdonnance() != null && o.getNumeroOrdonnance().toLowerCase().contains(query.toLowerCase())) ||
                             (o.getStatut() != null && o.getStatut().toLowerCase().contains(query.toLowerCase())) ||
                             (o.getNoteMedical() != null && o.getNoteMedical().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Ordonnance getById(int id) {
        return ordonnances.stream().filter(o -> o.getIdOrdonnance() == id).findFirst().orElse(null);
    }
}
