# Guide d'Utilisation - Intégration Produit/Commande/Stock/Dépôt

## Vue d'ensemble de l'architecture intégrée

Les quatre modules (Produit, Commande, Stock, Dépôt) fonctionnent maintenant de manière cohérente :

```
Client
  └─ Commande (1 par client possible)
      ├─ LigneCommande 1 (Produit A, Quantité 2, Prix 10€)
      ├─ LigneCommande 2 (Produit B, Quantité 1, Prix 5€)
      ├─ Dépôt: Dépôt Central
      └─ Statut: Confirmée

Stock (relations)
  ├─ Stock 1: Produit A → Dépôt Central (100 unités)
  ├─ Stock 2: Produit A → Dépôt Régional (50 unités)
  └─ Stock 3: Produit B → Dépôt Central (30 unités)
```

---

## Exemples d'Utilisation

### Exemple 1 : Créer une commande simple

```java
// 1. Récupérer les services
ProduitService produitService = ProduitService.getInstance();
CommandeService commandeService = new CommandeService();
ClientService clientService = new ClientService();

// 2. Récupérer les données
List<Produit> produits = produitService.getAll();
Client client = clientService.getAll().stream().findFirst().orElse(null);

// 3. Créer une commande avec lignes
Commande commande = new Commande();
commande.setClient(client);
commande.setDateCommande(LocalDate.now());
commande.setStatut("En attente");

// 4. Ajouter des lignes (avec quantité et prix)
for (Produit p : produits) {
    LigneCommande ligne = new LigneCommande(p, 2, p.getPrix());
    commande.addLigne(ligne);
}

// 5. Ajouter la commande (cela VÉRIFIE le stock automatiquement)
try {
    commandeService.add(commande);
    System.out.println("Commande créée avec succès : " + commande.getId());
} catch (RuntimeException e) {
    System.out.println("Erreur : " + e.getMessage());
    // Stock insuffisant ou autre erreur
}
```

### Exemple 2 : Créer une commande avec lien au dépôt

```java
Commande commande = new Commande();
commande.setClient(client);
commande.setDateCommande(LocalDate.now());
commande.setStatut("Confirmée");
commande.setDepot(depot); // Indiquer le dépôt de fulfillment

LigneCommande ligne = new LigneCommande(produit, 5, produit.getPrix(), depot);
commande.addLigne(ligne);

commandeService.add(commande);
```

### Exemple 3 : Vérifier la disponibilité de stock avant création

```java
StockService stockService = StockService.getInstance();

// Vérifier la disponibilité globale d'un produit
int quantiteTotale = stockService.getQuantiteTotaleProduit(produitId);
System.out.println("Disponibilité totale : " + quantiteTotale);

// Vérifier dans un dépôt spécifique
boolean disponible = stockService.isQuantiteDisponible(produitId, depotId, 10);
if (disponible) {
    // Créer la commande
}

// Récupérer les stocks par produit (pour choisir le dépôt)
List<Stock> stocks = stockService.getStocksByProduit(produitId);
for (Stock s : stocks) {
    System.out.println(s.getDepot().getNom() + ": " + s.getQuantiteDisponible());
}
```

### Exemple 4 : Gérer les mouvements de stock

```java
StockService stockService = StockService.getInstance();

// Décrémenter un stock (après une vente/sortie)
boolean success = stockService.decrementerStock(stockId, 5);

// Incrémenter un stock (après un retour/réapprov)
stockService.incrementerStock(stockId, 10);

// Récupérer les stocks faibles
List<Stock> faibles = stockService.getStocksFaibles();

// Récupérer les stocks critiques
List<Stock> critiques = stockService.getStocksCritiques();

// Récupérer les mouvements récents
List<Stock> entrees = stockService.getMouvementsEntree(); // Ajouts récents
List<Stock> sorties = stockService.getMouvementsSortie();  // Réductions récentes
```

### Exemple 5 : Rechercher et filtrer les commandes

```java
CommandeService commandeService = new CommandeService();

// Toutes les commandes
List<Commande> toutes = commandeService.getAll();

// Par ID
Commande cmd = commandeService.getById(1);

// Recherche textuelle
List<Commande> resultat = commandeService.search("Dupont");

// Par client
List<Commande> cmdClient = commandeService.getCommandesByClient(clientId);

// Par statut
List<Commande> confirmees = commandeService.getCommandesByStatut("Confirmée");
```

### Exemple 6 : Calculer et modifier un total de commande

```java
Commande commande = commandeService.getById(1);

// Ajouter une ligne
LigneCommande nouvelle = new LigneCommande(produit, 3, 25.00);
commande.addLigne(nouvelle);

// Recalculer le total automatiquement
double total = commande.calculerTotal();
System.out.println("Nouveau total : " + total + "€");

// Ou accéder au total
System.out.println("Total stocké : " + commande.getTotal() + "€");
```

---

## Flux d'Intégration - Cas d'Usage Courants

### Cas 1 : Passation d'une commande depuis le front-office

1. Client choisit des produits et quantités (UI)
2. Pour chaque produit sélectionné :
   - Vérifier `StockService.getStocksByProduit(produitId)` pour afficher disponibilité par dépôt
   - Créer une `LigneCommande(produit, quantité, prixUnitaire)`
3. Client valide le panier → création `Commande`
4. Appel `CommandeService.add(commande)` :
   - ✅ Si tous les stocks OK → commande créée, afficher confirmation
   - ❌ Si stock insuffisant → afficher erreur, proposer réduction de quantité
5. Mettre à jour l'affichage du stock en temps réel (relancer `getStocksByProduit`)

### Cas 2 : Gestion des retours

1. Client demande retour pour commande existante
2. Récupérer `Commande` via `getById()`
3. Pour chaque ligne du retour :
   - `StockService.incrementerStock(stockId, quantiteRetour)`
4. Mettre à jour statut commande (ex : "Partiellement retournée")

### Cas 3 : Alertes de rupture de stock

1. Afficher `StockService.getStocksFaibles()` dans le tableau de bord
2. Pour chaque stock faible, consulter `Stock.isStockFaible()`
3. Déclencher alerte ou demande de réapprovisionment automatique

### Cas 4 : Optimisation de fulfillment

1. Lors de la création d'une commande, chercher le dépôt optimal :
   ```java
   List<Stock> disponibles = stockService.getStocksByProduit(produitId)
       .stream()
       .filter(s -> s.getQuantiteDisponible() >= quantiteDemandee)
       .collect(Collectors.toList());
   
   // Choisir le plus proche ou le moins chargé
   Stock optimal = disponibles.get(0);
   commande.setDepot(optimal.getDepot());
   ```

---

## Méthodes Utiles - Référence Rapide

### Commande
| Méthode | Description | Exemple |
|---------|-------------|---------|
| `addLigne(LigneCommande)` | Ajoute une ligne | `cmd.addLigne(ligne)` |
| `removeLigne(index)` | Supprime une ligne | `cmd.removeLigne(0)` |
| `calculerTotal()` | Recalcule le total | `cmd.calculerTotal()` |
| `getNombreArticles()` | Nombre total d'articles | `int n = cmd.getNombreArticles()` |
| `isValide()` | Vérifie que la commande est valide | `if (cmd.isValide())` |
| `getLignes()` | Récupère les lignes | `List<LigneCommande> l = cmd.getLignes()` |

### LigneCommande
| Méthode | Description | Exemple |
|---------|-------------|---------|
| `getMontantTotal()` | Prix × Quantité | `double m = ligne.getMontantTotal()` |
| `getProduit()` | Produit de la ligne | `Produit p = ligne.getProduit()` |
| `getQuantite()` | Quantité commandée | `int q = ligne.getQuantite()` |
| `getPrixUnitaire()` | Prix unitaire | `double prix = ligne.getPrixUnitaire()` |
| `getStock()` | Stock d'où provient l'article (optionnel) | `Stock s = ligne.getStock()` |
| `getDepot()` | Dépôt d'où provient l'article (optionnel) | `Depot d = ligne.getDepot()` |

### StockService
| Méthode | Description | Exemple |
|---------|-------------|---------|
| `getStocksByProduit(id)` | Tous les stocks d'un produit | `List<Stock> s = srv.getStocksByProduit(1)` |
| `getStocksByDepot(id)` | Tous les stocks d'un dépôt | `List<Stock> s = srv.getStocksByDepot(1)` |
| `getQuantiteTotaleProduit(id)` | Quantité totale d'un produit | `int q = srv.getQuantiteTotaleProduit(1)` |
| `isQuantiteDisponible(pId, dId, q)` | Vérifier disponibilité | `if (srv.isQuantiteDisponible(1, 1, 10))` |
| `decrementerStock(id, q)` | Réduire un stock | `srv.decrementerStock(1, 5)` |
| `incrementerStock(id, q)` | Augmenter un stock | `srv.incrementerStock(1, 10)` |
| `getStocksFaibles()` | Stocks en-dessous du seuil | `List<Stock> s = srv.getStocksFaibles()` |
| `getStocksCritiques()` | Stocks critiques | `List<Stock> s = srv.getStocksCritiques()` |

### CommandeService
| Méthode | Description | Exemple |
|---------|-------------|---------|
| `add(cmd)` | Ajouter une commande (vérifie stock) | `srv.add(commande)` |
| `update(cmd)` | Modifier une commande | `srv.update(commande)` |
| `delete(id)` | Supprimer une commande | `srv.delete(1)` |
| `getAll()` | Toutes les commandes | `List<Commande> c = srv.getAll()` |
| `getById(id)` | Commande par ID | `Commande c = srv.getById(1)` |
| `search(query)` | Recherche textuelle | `List<Commande> c = srv.search("Dupont")` |
| `getCommandesByClient(id)` | Commandes d'un client | `List<Commande> c = srv.getCommandesByClient(1)` |
| `getCommandesByStatut(statut)` | Commandes par statut | `List<Commande> c = srv.getCommandesByStatut("Confirmée")` |

---

## Backward Compatibility (Code Ancien)

Si vous aviez du code qui utilisait l'ancien format de `Commande` :

```java
// ANCIEN (ne marche plus directement)
List<Produit> produits = new ArrayList<>();
produits.add(p1);
produits.add(p2);
Commande cmd = new Commande(0, client, produits, LocalDate.now(), 100, "Confirmée");
// ❌ Erreur de compilation : expect List<LigneCommande>
```

**Solution 1 : Utiliser la méthode de compatibilité**
```java
Commande cmd = new Commande();
cmd.setClient(client);
cmd.setProduits(produits); // Convertit automatiquement en LigneCommande
cmd.setDateCommande(LocalDate.now());
cmd.setStatut("Confirmée");
cmd.calculerTotal();
```

**Solution 2 : Utiliser les lignes directement** (recommandé)
```java
Commande cmd = new Commande();
cmd.setClient(client);
cmd.addLigne(new LigneCommande(p1, 2, p1.getPrix()));
cmd.addLigne(new LigneCommande(p2, 1, p2.getPrix()));
cmd.setDateCommande(LocalDate.now());
cmd.setStatut("Confirmée");
cmd.calculerTotal();
```

---

## Tests Recommandés

### Test 1 : Création de commande valide
```java
@Test
public void testCreerCommandeValide() {
    // Arrange
    Commande cmd = new Commande();
    cmd.setClient(client);
    cmd.addLigne(new LigneCommande(produit, 2, 10.0));
    cmd.setDateCommande(LocalDate.now());
    cmd.setStatut("En attente");
    
    // Act
    commandeService.add(cmd);
    
    // Assert
    assertTrue(cmd.getId() > 0);
    assertEquals(1, commandeService.getAll().size());
    assertEquals(20.0, cmd.getTotal(), 0.01);
}
```

### Test 2 : Rejet de commande sans stock
```java
@Test
public void testCreerCommandeSansStock() {
    // Arrange : produit avec stock 0
    Produit p = new Produit(0, "Test", "Desc", 10.0, 0, "Test", true);
    Commande cmd = new Commande();
    cmd.setClient(client);
    cmd.addLigne(new LigneCommande(p, 10, 10.0)); // Demander 10, mais pas de stock
    cmd.setDateCommande(LocalDate.now());
    cmd.setStatut("En attente");
    
    // Act & Assert
    assertThrows(RuntimeException.class, () -> commandeService.add(cmd));
}
```

### Test 3 : Calcul du total
```java
@Test
public void testCalculerTotal() {
    // Arrange
    Commande cmd = new Commande();
    cmd.addLigne(new LigneCommande(p1, 2, 10.0)); // 20€
    cmd.addLigne(new LigneCommande(p2, 3, 5.0));  // 15€
    
    // Act
    double total = cmd.calculerTotal();
    
    // Assert
    assertEquals(35.0, total, 0.01);
    assertEquals(5, cmd.getNombreArticles());
}
```

### Test 4 : Recherche par client
```java
@Test
public void testRechercheParClient() {
    // Arrange
    Commande cmd1 = createCommande(client1);
    Commande cmd2 = createCommande(client1);
    Commande cmd3 = createCommande(client2);
    commandeService.add(cmd1);
    commandeService.add(cmd2);
    commandeService.add(cmd3);
    
    // Act
    List<Commande> resultat = commandeService.getCommandesByClient(client1.getId());
    
    // Assert
    assertEquals(2, resultat.size());
}
```

---

## Points d'Attention

⚠️ **Stock en mémoire vs Base de données**
- `CommandeService` est actuellement en mémoire (ArrayList)
- `StockService` est persistant (BD)
- Les vérifications de stock fonctionnent, mais les commandes ne survivent pas aux redémarrages
- **Action future:** Persister les commandes en BD

⚠️ **Transactions distribuées**
- La vérification du stock et la création de la commande ne sont pas dans la même transaction
- Dans un contexte multi-utilisateur, deux commandes pourraient être créées simultanément en dépassant le stock
- **Action future:** Ajouter des transactions BD atomiques avec locks (SELECT ... FOR UPDATE)

⚠️ **Null Checks**
- Les méthodes font des vérifications null, mais vérifiez que les objets passés ne sont jamais null
- Ex : `Commande.getClient()` peut retourner null après `search()`

✅ **Points forts de l'intégration**
- ✅ Quantités par produit (LigneCommande)
- ✅ Vérification automatique du stock
- ✅ Lien commande ↔ dépôt
- ✅ Lien ligne ↔ stock/dépôt
- ✅ Calcul automatique du total
- ✅ Recherche par client/statut/produit
- ✅ Gestion des mouvements de stock
- ✅ Backward compatibility partielle (getProduits/setProduits)

---

## Prochaines Étapes Recommandées

1. **Persistance des Commandes** : Ajouter tables `commande` et `ligne_commande`
2. **Transactions atomiques** : Implémenter `CommandeService` en JDBC avec transactions
3. **Gestion des réservations** : Supporter les paniers en attente de paiement
4. **Annulation/Retour** : Ajouter logique d'annulation de commande avec libération du stock
5. **Rapports** : Génération de rapports de ventes par produit/dépôt
6. **API REST** : Exposer l'API pour un frontend déterminé

