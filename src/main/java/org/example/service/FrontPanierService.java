package org.example.service;

import org.example.model.Produit;
import org.example.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontPanierService {
    public static class CartItem {
        private final int produitId;
        private final String nom;
        private final String image;
        private final double prixUnitaire;
        private final int quantite;
        private final double totalLigne;

        public CartItem(int produitId, String nom, String image, double prixUnitaire, int quantite) {
            this.produitId = produitId;
            this.nom = nom;
            this.image = image;
            this.prixUnitaire = prixUnitaire;
            this.quantite = quantite;
            this.totalLigne = Math.round(prixUnitaire * quantite * 100.0) / 100.0;
        }

        public int getProduitId() { return produitId; }
        public String getNom() { return nom; }
        public String getImage() { return image; }
        public double getPrixUnitaire() { return prixUnitaire; }
        public int getQuantite() { return quantite; }
        public double getTotalLigne() { return totalLigne; }
    }

    private static final Map<String, Map<Integer, Integer>> CARTS = new HashMap<>();

    private final UserService userService = new UserService();

    public void addProduit(int produitId, int quantite) {
        if (produitId <= 0 || quantite <= 0) {
            return;
        }
        Map<Integer, Integer> cart = getCurrentCartMap();
        cart.put(produitId, cart.getOrDefault(produitId, 0) + quantite);
    }

    public void setQuantite(int produitId, int quantite) {
        Map<Integer, Integer> cart = getCurrentCartMap();
        if (quantite <= 0) {
            cart.remove(produitId);
        } else {
            cart.put(produitId, quantite);
        }
    }

    public void removeProduit(int produitId) {
        getCurrentCartMap().remove(produitId);
    }

    public void clear() {
        getCurrentCartMap().clear();
    }

    public int getNombreArticles() {
        return getCurrentCartMap().values().stream().mapToInt(Integer::intValue).sum();
    }

    public List<CartItem> getItems(ProduitService produitService, PromotionService promotionService) {
        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : getCurrentCartMap().entrySet()) {
            Produit produit = produitService.getById(entry.getKey());
            if (produit == null) {
                continue;
            }
            double prixPromo = promotionService.getPromotionalPrice(produit.getId(), produit.getPrix());
            items.add(new CartItem(
                    produit.getId(),
                    produit.getNom(),
                    produit.getImage(),
                    prixPromo,
                    entry.getValue()
            ));
        }
        return items;
    }

    public double getTotal(ProduitService produitService, PromotionService promotionService) {
        return Math.round(getItems(produitService, promotionService)
                .stream()
                .mapToDouble(CartItem::getTotalLigne)
                .sum() * 100.0) / 100.0;
    }

    public String getProduitsIdsJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : getCurrentCartMap().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(entry.getKey());
                first = false;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private Map<Integer, Integer> getCurrentCartMap() {
        String key = resolveCurrentUserKey();
        return CARTS.computeIfAbsent(key, k -> new HashMap<>());
    }

    private String resolveCurrentUserKey() {
        User current = userService.getCurrentUser();
        if (current == null) {
            return "guest";
        }
        return "user-" + current.getId();
    }
}
