package org.example.service;

import org.example.model.Stock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockService {
    private List<Stock> stocks = new ArrayList<>();
    private int nextId = 1;

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
                .filter(s -> s.getProduit().getNom().toLowerCase().contains(query.toLowerCase()) ||
                             s.getDepot().getNom().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Stock getById(int id) {
        return stocks.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    public List<Stock> getStocksFaibles() {
        return stocks.stream().filter(Stock::isStockFaible).collect(Collectors.toList());
    }
}
