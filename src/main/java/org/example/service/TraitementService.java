package org.example.service;

import org.example.model.Traitement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TraitementService {
    private List<Traitement> traitements = new ArrayList<>();
    private int nextId = 1;

    public void add(Traitement traitement) {
        traitement.setIdTraitement(nextId++);
        traitements.add(traitement);
    }

    public void update(Traitement traitement) {
        for (int i = 0; i < traitements.size(); i++) {
            if (traitements.get(i).getIdTraitement() == traitement.getIdTraitement()) {
                traitements.set(i, traitement);
                break;
            }
        }
    }

    public void delete(int id) {
        traitements.removeIf(t -> t.getIdTraitement() == id);
    }

    public List<Traitement> getAll() {
        return new ArrayList<>(traitements);
    }

    public List<Traitement> search(String query) {
        return traitements.stream()
                .filter(t -> (t.getDosage() != null && t.getDosage().toLowerCase().contains(query.toLowerCase())) ||
                             (t.getStatus() != null && t.getStatus().toLowerCase().contains(query.toLowerCase())) ||
                             (t.getNotes() != null && t.getNotes().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Traitement getById(int id) {
        return traitements.stream().filter(t -> t.getIdTraitement() == id).findFirst().orElse(null);
    }
}
