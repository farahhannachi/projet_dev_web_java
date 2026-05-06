package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Client;
import org.example.model.Commande;
import org.example.model.Depot;
import org.example.model.Produit;
import org.example.model.Stock;
import org.example.service.ClientService;
import org.example.service.ProduitService;
import org.example.service.CommandeService;
import org.example.service.StockService;
import org.example.service.DepotService;
import org.example.service.ServiceService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController {

    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar; // Vérifier l'utilisation dans le fichier FXML
    @FXML private Label totalClientsLabel;
    @FXML private Label totalProduitsLabel;
    @FXML private Label totalCommandesLabel;
    @FXML private Label alertesStockLabel;
    @FXML private Button nouveauClientBtn; // Vérifier l'utilisation dans le fichier FXML
    @FXML private Button ajouterProduitBtn; // Vérifier l'utilisation dans le fichier FXML
    @FXML private Button nouvelleCommandeBtn; // Vérifier l'utilisation dans le fichier FXML
    @FXML private Button frontOfficeBtn;
    @FXML private Button consommationBtn; // Vérifier l'utilisation dans le fichier FXML
    @FXML private Pane stocksChart;
    @FXML private Pane depotsChart;
    @FXML private Pane servicesChart;
    @FXML private Pane movementsChart;

    private final ClientService clientService = new ClientService();
    private final ProduitService produitService = ProduitService.getInstance();
    private final CommandeService commandeService = new CommandeService();
    private final StockService stockService = StockService.getInstance();
    private final DepotService depotService = DepotService.getInstance();
    private final ServiceService serviceService = ServiceService.getInstance();
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @FXML
    public void initialize() {
        // Only load stats and charts - DO NOT reseed data on every initialization
        // This prevents data duplication and ensures UI reflects actual DB state
        loadStats();
        initializeCharts();
    }

    private void addSampleData() {
        try {
            // Sample products
            produitService.add(new Produit(0, "Paracétamol", "Antalgique et antipyrétique", 5.50, 100, "Analgésiques", true));
            produitService.add(new Produit(0, "Ibuprofène", "Anti-inflammatoire non stéroïdien", 4.20, 80, "Anti-inflammatoires", true));
            produitService.add(new Produit(0, "Aspirine", "Antalgique et antiagrégant", 3.80, 120, "Analgésiques", true));
            produitService.add(new Produit(0, "Vitamine C", "Supplément vitaminique", 8.90, 50, "Vitamines", true));

            // Sample clients
            clientService.add(new Client(0, "Dupont", "Jean", "jean.dupont@email.com", "0123456789", LocalDate.of(1980, 5, 15), "123 Rue de la Paix, Paris"));
            clientService.add(new Client(0, "Martin", "Marie", "marie.martin@email.com", "0987654321", LocalDate.of(1990, 3, 22), "456 Avenue des Champs, Lyon"));

            // Sample depots
            Depot depot1 = new Depot(0, "Dépôt Central", "10 Rue du Stock, Paris", "Paris", 1000, "Jean Dupont", "0145678901", null, 48.8566, 2.3522);
            Depot depot2 = new Depot(0, "Dépôt Régional", "20 Boulevard Commercial, Lyon", "Lyon", 800, "Marie Martin", "0276543210", null, 45.7640, 4.8357);

            depotService.add(depot1);
            depotService.add(depot2);

            // Sample stocks - maintenant que les produits et dépôts existent en DB
            stockService.add(new Stock(0, produitService.getAll().get(0), 50, 10, depot1));
            stockService.add(new Stock(0, produitService.getAll().get(1), 5, 10, depot2)); // Low stock

            // Sample orders
            commandeService.add(new Commande(0, clientService.getAll().stream().findFirst().orElse(null),
                java.util.Collections.singletonList(produitService.getAll().stream().findFirst().orElse(null)),
                LocalDate.now(), 5.50, "Confirmée"));

            // Sample services
            serviceService.add(new org.example.model.Service(0, "Dupont", "Médecin", "Médecine générale", "0123456789", "dupont@medecin.fr", "123 Rue de la Santé, Paris", java.time.LocalDateTime.now()));
            serviceService.add(new org.example.model.Service(0, "Martin", "Médecin", "Cardiologie", "0987654321", "martin@medecin.fr", "456 Avenue du Cœur, Lyon", java.time.LocalDateTime.now()));
            serviceService.add(new org.example.model.Service(0, "Dubois", "Infirmier", "Soins infirmiers", "0145678901", "dubois@infirmier.fr", "789 Boulevard des Soins, Marseille", java.time.LocalDateTime.now()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des données d'exemple", e);
        }
    }

    private void loadStats() {
        totalClientsLabel.setText(String.valueOf(clientService.getAll().size()));
        totalProduitsLabel.setText(String.valueOf(produitService.getAll().size()));
        totalCommandesLabel.setText(String.valueOf(commandeService.getAll().size()));
        alertesStockLabel.setText(String.valueOf(stockService.getStocksFaibles().size()));
    }

    private void initializeCharts() {
        // Stocks Chart
        javafx.scene.chart.BarChart<String, Number> stocksBarChart = new javafx.scene.chart.BarChart<>(new javafx.scene.chart.CategoryAxis(), new javafx.scene.chart.NumberAxis());
        javafx.scene.chart.XYChart.Series<String, Number> stockSeries = new javafx.scene.chart.XYChart.Series<>();
        stockSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Total", stockService.getAll().size()));
        stockSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Faible", stockService.getStocksFaibles().size()));
        stockSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Critique", stockService.getStocksCritiques().size()));
        stocksBarChart.getData().add(stockSeries);
        stocksChart.getChildren().add(stocksBarChart);

        // Depots Chart
        javafx.scene.chart.PieChart depotsPieChart = new javafx.scene.chart.PieChart();
        depotService.getAll().forEach(depot -> {
            depotsPieChart.getData().add(new javafx.scene.chart.PieChart.Data(depot.getNom(), depot.getCapaciteDepot()));
        });
        depotsChart.getChildren().add(depotsPieChart);

        // Services Chart
        javafx.scene.chart.BarChart<String, Number> servicesBarChart = new javafx.scene.chart.BarChart<>(new javafx.scene.chart.CategoryAxis(), new javafx.scene.chart.NumberAxis());
        javafx.scene.chart.XYChart.Series<String, Number> serviceSeries = new javafx.scene.chart.XYChart.Series<>();
        serviceService.getAll().forEach(service -> {
            serviceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(service.getNom(), service.getConsommation()));
        });
        servicesBarChart.getData().add(serviceSeries);
        servicesChart.getChildren().add(servicesBarChart);

        // Movements Chart
        javafx.scene.chart.BarChart<String, Number> movementsBarChart = new javafx.scene.chart.BarChart<>(new javafx.scene.chart.CategoryAxis(), new javafx.scene.chart.NumberAxis());
        javafx.scene.chart.XYChart.Series<String, Number> movementSeries = new javafx.scene.chart.XYChart.Series<>();
        movementSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Entrées", stockService.getMouvementsEntree().size()));
        movementSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Sorties", stockService.getMouvementsSortie().size()));
        movementsBarChart.getData().add(movementSeries);
        movementsChart.getChildren().add(movementsBarChart);
    }

    @FXML
    private void handleNouveauClient() {
        // Open new client form
        System.out.println("Nouveau Client clicked");
    }

    @FXML
    private void handleAjouterProduit() {
        // Open add product form
        System.out.println("Ajouter Produit clicked");
    }

    @FXML
    private void handleNouvelleCommande() {
        // Open new order form
        System.out.println("Nouvelle Commande clicked");
    }

    @FXML
    private void goToFrontOffice() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        if (getClass().getResource("/css/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        }
        Stage stage = (Stage) frontOfficeBtn.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void showClients() {
        loadViewInCenter("/fxml/Clients.fxml");
    }

    @FXML
    private void showProduits() {
        loadViewInCenter("/fxml/Produits.fxml");
    }

    @FXML
    private void showCommandes() {
        loadViewInCenter("/fxml/Commandes.fxml");
    }

    @FXML
    private void showPromotions() {
        loadViewInCenter("/fxml/Promotions.fxml");
    }

    @FXML
    private void showCoupons() {
        loadViewInCenter("/fxml/Coupons.fxml");
    }

    @FXML
    private void showDepots() {
        loadViewInCenter("/fxml/Depots.fxml");
    }

    @FXML
    private void showStocks() {
        loadViewInCenter("/fxml/Stocks.fxml");
    }

    @FXML
    private void showServices() {
        loadViewInCenter("/fxml/Services.fxml");
    }

    @FXML
    private void showConsommation() {
        loadViewInCenter("/fxml/Consommation.fxml");
    }

    @FXML
    private void showReservations() {
        loadViewInCenter("/fxml/Reservations.fxml");
    }

    private void loadViewInCenter(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainPane.setCenter(view);

            // Log success
            System.out.println("Vue chargée avec succès: " + fxmlPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de " + fxmlPath, e);

            // Afficher une boîte de dialogue d'erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger la vue");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void logout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        if (getClass().getResource("/css/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        }
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
