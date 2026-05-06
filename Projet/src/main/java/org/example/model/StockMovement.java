package org.example.model;

import java.time.LocalDateTime;

/**
 * Modèle pour les mouvements de stock
 * Enregistre chaque consommation/ajout de stock par les services
 *
 * Relations :
 * - Stock : produit en stock dans un dépôt
 * - Service : médecin/infirmier qui consomme
 * - Depot : d'où provient la consommation
 */
public class StockMovement {
    private int id;
    private int idStock;
    private Integer idService; // Clé étrangère vers Service
    private String type; // ENTREE, SORTIE, etc.
    private String typeConsommation; // CONSOMMATION_SERVICE, RETOUR, etc.
    private int quantite;
    private int quantiteAvant;
    private int quantiteApres;
    private String status; // APPROUVEE, EN_ATTENTE, REJETEE
    private String motif;
    private String referenceDocument; // Numéro de document/ordonnance
    private LocalDateTime createdAt;

    // Relations pour plus de contexte
    private Stock stock;
    private Service service;
    private Depot depot;

    // Constructeurs
    public StockMovement() {}

    public StockMovement(int idStock, Integer idService, String type, String typeConsommation,
                         int quantite, int quantiteAvant, int quantiteApres, String status,
                         String motif, String referenceDocument) {
        this.idStock = idStock;
        this.idService = idService;
        this.type = type;
        this.typeConsommation = typeConsommation;
        this.quantite = quantite;
        this.quantiteAvant = quantiteAvant;
        this.quantiteApres = quantiteApres;
        this.status = status;
        this.motif = motif;
        this.referenceDocument = referenceDocument;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdStock() { return idStock; }
    public void setIdStock(int idStock) { this.idStock = idStock; }

    public Integer getIdService() { return idService; }
    public void setIdService(Integer idService) { this.idService = idService; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTypeConsommation() { return typeConsommation; }
    public void setTypeConsommation(String typeConsommation) { this.typeConsommation = typeConsommation; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public int getQuantiteAvant() { return quantiteAvant; }
    public void setQuantiteAvant(int quantiteAvant) { this.quantiteAvant = quantiteAvant; }

    public int getQuantiteApres() { return quantiteApres; }
    public void setQuantiteApres(int quantiteApres) { this.quantiteApres = quantiteApres; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Relations
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }

    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    @Override
    public String toString() {
        return "Mouvement: " + typeConsommation + " - " + quantite + " unités" +
               (service != null ? " par " + service.getNom() : "") +
               " le " + createdAt;
    }
}

