package org.example.service;

import org.example.model.Commande;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandeService {

    }

    public void update(Commande commande) {
                break;
            }
        }
    }

    public void delete(int id) {
    }

    public List<Commande> getAll() {
    }

    public List<Commande> search(String query) {
                .filter(c -> String.valueOf(c.getId()).contains(query) ||
                             c.getStatut().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Commande getById(int id) {
    }
}
