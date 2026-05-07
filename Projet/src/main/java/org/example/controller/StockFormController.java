package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.model.Stock;
import org.example.model.Depot;
import org.example.model.Produit;
import org.example.service.StockService;
import org.example.service.DepotService;
import org.example.service.ProduitService;
import org.example.util.NotificationUtil;

/**
 * Contrôleur pour le formulaire d'ajout/modification de stock
 * Affiche le formulaire dans le content pane (pas de nouvelle fenêtre)
 * Architecture SPA - Navigation sans Stage
 */
public class StockFormController {
    @FXML private Label formTitle;
    @FXML private ComboBox<String> produitField;
    @FXML private TextField quantiteField;
    @FXML private TextField seuilMinimumField;
    @FXML private ComboBox<String> depotField;
    @FXML private Label errorLabel;

    private final StockService stockService = StockService.getInstance();
    private final DepotService depotService = DepotService.getInstance();
    private final ProduitService produitService = ProduitService.getInstance();

    private Stock stockToEdit;
    private StockController parentController;

    private ObservableList<Produit> produits = FXCollections.observableArrayList();
    private ObservableList<Depot> depots = FXCollections.observableArrayList();

    public void setStockToEdit(Stock stock) {
        this.stockToEdit = stock;
        if (stock != null) {
            formTitle.setText("Modifier le stock");
            // Remplir les champs avec les données du stock
            if (stock.getProduit() != null) {
                produitField.setValue(stock.getProduit().getNom());
            }
            quantiteField.setText(String.valueOf(stock.getQuantite()));
            seuilMinimumField.setText(String.valueOf(stock.getSeuilMinimum()));
            if (stock.getDepot() != null) {
                depotField.setValue(stock.getDepot().getNom());
            }
        } else {
            formTitle.setText("Ajouter un stock");
        }
    }

    public void setParentController(StockController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void initialize() {
        // Charger les produits depuis le service
        try {
            produits.setAll(produitService.getAll());
            ObservableList<String> produitNames = FXCollections.observableArrayList();
            for (Produit p : produits) {
                produitNames.add(p.getNom());
            }
            produitField.setItems(produitNames);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des produits: " + e.getMessage());
        }

        // Charger les dépôts depuis la base de données
        try {
            depots.setAll(depotService.getAll());
            ObservableList<String> depotNames = FXCollections.observableArrayList();
            for (Depot d : depots) {
                depotNames.add(d.getNom());
            }
            depotField.setItems(depotNames);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des dépôts: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        // Validation
        if ((produitField.getValue() == null || produitField.getValue().isEmpty()) ||
            quantiteField.getText().trim().isEmpty() ||
            seuilMinimumField.getText().trim().isEmpty() ||
            (depotField.getValue() == null || depotField.getValue().isEmpty())) {
            errorLabel.setText("Tous les champs obligatoires doivent être remplis.");
            return;
        }

        try {
            Integer.parseInt(quantiteField.getText());
            Integer.parseInt(seuilMinimumField.getText());
        } catch (NumberFormatException e) {
            errorLabel.setText("Quantité et Seuil doivent être des nombres entiers.");
            return;
        }

        errorLabel.setText("");

        // Trouver le produit et le dépôt sélectionnés
        Produit selectedProduit = produits.stream()
                .filter(p -> p.getNom().equals(produitField.getValue()))
                .findFirst()
                .orElse(null);

        Depot selectedDepot = depots.stream()
                .filter(d -> d.getNom().equals(depotField.getValue()))
                .findFirst()
                .orElse(null);

        if (selectedProduit == null || selectedDepot == null) {
            errorLabel.setText("Erreur: Produit ou Dépôt invalide.");
            return;
        }

        if (stockToEdit == null) {
            // Ajout d'un nouveau stock
            Stock newStock = new Stock();
            newStock.setProduit(selectedProduit);
            newStock.setDepot(selectedDepot);
            newStock.setQuantite(Integer.parseInt(quantiteField.getText()));
            newStock.setSeuilMinimum(Integer.parseInt(seuilMinimumField.getText()));

            stockService.add(newStock);
            NotificationUtil.showSuccess("✅ Stock ajouté avec succès.");
        } else {
            // Modification d'un stock existant
            stockToEdit.setProduit(selectedProduit);
            stockToEdit.setDepot(selectedDepot);
            stockToEdit.setQuantite(Integer.parseInt(quantiteField.getText()));
            stockToEdit.setSeuilMinimum(Integer.parseInt(seuilMinimumField.getText()));

            stockService.update(stockToEdit);
            NotificationUtil.showSuccess("✅ Stock modifié avec succès.");
        }

        // Revenir à la liste des stocks
        handleCancel();
    }

    @FXML
    private void handleCancel() {
        // Revenir à la vue de la liste des stocks
        if (parentController != null) {
            parentController.showTableView();
        }
    }
}
