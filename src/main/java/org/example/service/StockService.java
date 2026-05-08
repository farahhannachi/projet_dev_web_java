package org.example.service;

import org.example.model.Stock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockService {
    private static StockService instance;

    private final List<Stock> stocks = new ArrayList<>();
    private int nextId = 1;

    private StockService() {}

    public static synchronized StockService getInstance() {
        if (instance == null) {
            instance = new StockService();
        }
        return instance;
    }

    public void add(Stock stock) {
        stock.setId(nextId++);
        stocks.add(stock);
    }

    public void update(Stock stock) {
        for (int i = 0; i < stocks.size(); i++) {
            if (stocks.get(i).getId() == stock.getId()) {
                stocks.set(i, stock);
                break;
            }
        }
    }

    public void delete(int id) {
        stocks.removeIf(s -> s.getId() == id);
    }

    public List<Stock> getAll() {
        return new ArrayList<>(stocks);
    }

    public List<Stock> search(String query) {
        return stocks.stream()
                .filter(s -> {
                    boolean produitOk = s.getProduit() != null && s.getProduit().getNom() != null;
                    boolean depotOk = s.getDepot() != null && s.getDepot().getNom() != null;
                    String q = query.toLowerCase();
                    return produitOk && s.getProduit().getNom().toLowerCase().contains(q)
                            || depotOk && s.getDepot().getNom().toLowerCase().contains(q);
                })
                .collect(Collectors.toList());
    }

    public Stock getById(int id) {
        return stocks.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    public List<Stock> getStocksFaibles() {
        return stocks.stream().filter(Stock::isStockFaible).collect(Collectors.toList());
    }

    /** Stock critique : rupture ou quantité nulle */
    public List<Stock> getStocksCritiques() {
        return stocks.stream().filter(s -> s.getQuantiteDisponible() <= 0).collect(Collectors.toList());
    }
}
