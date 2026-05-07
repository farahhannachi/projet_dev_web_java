# Résumé des Modifications - Intégration Produit/Commande et Depot/Stock

## Modifications Appliquées ✅

### 1. Nouveau modèle: `LigneCommande` ✅
**Fichier créé:** `src/main/java/org/example/model/LigneCommande.java`

**Purpose:** Représenter une ligne d'une commande avec :
- Produit commandé
- Quantité commandée
- Prix unitaire
- Dépôt/Stock d'origine (optionnel)

**Avantages:**
- Permet de tracker la quantité par produit
- Permet de connaître le prix unitaire au moment de la commande
- Lien vers le Stock/Depot spécifique

### 2. Refonte du modèle `Commande` ✅
**Fichier modifié:** `src/main/java/org/example/model/Commande.java`

**Changements:**
- Remplacement de `List<Produit> produits` par `List<LigneCommande> lignes`
- Ajout du champ `Depot depot` (le dépôt qui prépare la commande)
- Ajout de méthodes utilitaires :
  - `calculerTotal()` - calcule le total à partir des lignes
  - `addLigne(LigneCommande)` - ajoute une ligne
  - `removeLigne(int)` - supprime une ligne
  - `getNombreArticles()` - nombre total d'articles
  - `isValide()` - valide la commande
- Backward compatibility : `getProduits()` et `setProduits()` encore disponibles
- Meilleure gestion des valeurs null avec Objects.requireNonNull

### 3. Amélioration de `CommandeService` ✅
**Fichier modifié:** `src/main/java/org/example/service/CommandeService.java`

**Changements:**
- Intégration de la vérification du stock dans `add(Commande)` :
  - Appel à `StockService.getStocksByProduit()` pour chaque ligne
  - Validation que la quantité demandée est disponible
  - Affectation automatique du Stock si trouvé
  - Lève une exception si stock insuffisant
- Validation stricter de la Commande (null-safe) dans `add()`
- Amélioration du `search()` avec null-safety
- Ajout de méthodes de requête :
  - `getCommandesByClient(int clientId)`
  - `getCommandesByStatut(String statut)`

**Conséquence:** Une commande ne peut pas être créée si le stock n'est pas disponible.

### 4. Extension de `StockService` ✅
**Fichier modifié:** `src/main/java/org/example/service/StockService.java`

**Nouvelles méthodes:**
- `getStocksByProduit(int produitId)` - tous les stocks d'un produit
- `isQuantiteDisponible(int produitId, int depotId, int quantite)` - vérifie disponibilité
- `getQuantiteTotaleProduit(int produitId)` - somme quantités sur tous dépôts
- `decrementerStock(int stockId, int quantite)` - décrémente avec vérification
- `incrementerStock(int stockId, int quantite)` - incrémente (retour/réapprov)

**Remarque:** Ces opérations utilisent directement SQL (sans transaction complète) ; pour un usage production multi-utilisateur, implémenter des transactions complètes avec locks (SELECT ... FOR UPDATE).

---

## Architecture et Relations Maintenant en Place

```
┌─────────────────┐
│     Client      │
└────────┬────────┘
         │ (1..N)
         │
    ┌────▼──────────────────┐
    │     Commande          │
    │ - id                  │
    │ - client (FK)         │
    │ - lignes (List)       │
    │ - depot (optionnel)   │
    │ - statut              │
    │ - total               │
    └────┬──────────────────┘
         │ (1..N)
         │
    ┌────▼──────────────────┐
    │   LigneCommande       │
    │ - produit (FK)        │
    │ - quantite            │
    │ - prixUnitaire        │
    │ - stock (optionnel)   │
    │ - depot (optionnel)   │
    └────┬──────────────────┘
         │
         └───────────────────┬──────────────────┐
                             │                  │
                        ┌────▼─────┐      ┌────▼──────┐
                        │  Produit  │      │   Stock   │
                        │ (modèle)  │      │ (modèle)  │
                        └───────────┘      └────┬──────┘
                                                 │
                                            ┌────▼─────┐
                                            │  Depot   │
                                            │ (modèle) │
                                            └──────────┘
```

---

## Flux de Création de Commande (améliore)

1. **Utilisateur crée LigneCommande** avec produit, quantité, prix unitaire
2. **Utilisateur ajoute lignes à Commande**
3. **Utilisateur appelle CommandeService.add(commande)**
   - Vérification de validité (client, lignes non vides, dates, statut)
   - **Pour chaque ligne:**
     - Appel `StockService.getStocksByProduit(produitId)`
     - Vérifie qu'un stock dispose de la quantité
     - Affecte le Stock trouvé à la ligne
   - **Si tout OK:** commit de la commande
   - **Si stock insuffisant:** RuntimeException levée (transaction annulée)

---

## Points Clés d'Intégration

### ✅ Quantité par Produit
- **Avant:** Commande contenait `List<Produit>` (pas de quantité)
- **Maintenant:** `List<LigneCommande>` chaque ligne a quantité et prix

### ✅ Vérification Stock
- **Avant:** CommandeService créait les commandes sans vérifier stock
- **Maintenant:** Appel à StockService.getStocksByProduit() ; exception si insuffisant

### ✅ Lien Commande ↔ Dépôt
- **Avant:** Commande n'avait aucun lien au Dépôt
- **Maintenant:** Commande.depot indique le dépôt de fulfillment (optionnel)

### ✅ Lien Ligne ↔ Stock
- **Avant:** Aucun lien entre ligne de commande et Stock
- **Maintenant:** LigneCommande.stock référence le Stock spécifique d'où la marchandise provient

---

## TODO et Recommandations Futures 🔮

### 1. Persistance des Commandes
Les commandes sont actuellement en mémoire (CommandeService uses ArrayList).
**À faire:** Ajouter tables `commande` et `ligne_commande` en base, migrer CommandeService vers JDBC.

```sql
CREATE TABLE commande (
  id_commande INT PRIMARY KEY AUTO_INCREMENT,
  client_id INT NOT NULL,
  depot_id INT,
  date_commande DATETIME DEFAULT CURRENT_TIMESTAMP,
  total DECIMAL(10,2),
  statut VARCHAR(50),
  FOREIGN KEY (client_id) REFERENCES client(id_client),
  FOREIGN KEY (depot_id) REFERENCES depot(id_depot)
);

CREATE TABLE ligne_commande (
  id_ligne INT PRIMARY KEY AUTO_INCREMENT,
  commande_id INT NOT NULL,
  stock_id INT,
  produit_id INT NOT NULL,
  quantite INT,
  prix_unitaire DECIMAL(10,2),
  FOREIGN KEY (commande_id) REFERENCES commande(id_commande),
  FOREIGN KEY (stock_id) REFERENCES stock(id_stock),
  FOREIGN KEY (produit_id) REFERENCES produit(id_produit)
);
```

### 2. Transactions Distribuées
Actuellement `CommandeService.add()` vérifie le stock mais ne décrémente pas atomiquement.
**À faire:** Implémenter un bloc transactionnel (Connection.setAutoCommit(false), rollback en cas d'erreur) qui :
- Vérifie stock
- Crée commande
- Crée lignes
- Décrémente stocks
- Commit

### 3. Gestion des Réservations
Si vous supportez le "panier" avant paiement :
**À faire:** Ajouter `reserveStock()` / `releaseReservation()` avec timestamps pour libérer les réservations expirées.

### 4. Retours et Annulations
Gérer les retours de stock post-commande.
**À faire:** Méthode `CommandeService.cancelCommande(int commandeId)` qui :
- Change statut en "Annulée"
- Appelle `StockService.incrementerStock()` pour chaque ligne

### 5. Source de Vérité - Quantités Produit
Actuellement `Produit.quantiteStock` existe mais n'est jamais synchronisé avec la somme des stocks.
**Recommandation:** Supprimer `Produit.quantiteStock` ou le calculer dynamiquement via :
```java
int totalQte = stockService.getQuantiteTotaleProduit(produitId);
```

### 6. Cas Multi-Utilisateur
Ajouter des locks optimistes ou pessimistes (SELECT ... FOR UPDATE) dans `StockService.decrementerStock()`.

---

## Tests Recommandés 🧪

1. **Test création Commande valide** → doit réussir
2. **Test création Commande sans client** → doit lever exception
3. **Test création Commande avec stock insuffisant** → doit lever exception
4. **Test calcul total commande** → doit être égal à somme (quantité × prixUnitaire)
5. **Test search commande par client** → doit retourner commandes du client
6. **Test recherche stocks par produit** → doit retourner tous les dépôts ayant ce produit
7. **Test getNombreArticles** → somme des quantités de toutes les lignes

---

## Migration de Code Existant

Si vous aviez du code qui utilisait :
```java
commande.getProduits()  // Avant (List<Produit>)
```

**Vous pouvez:**
- Utiliser la méthode de backward compatibility `getProduits()` (retourne la liste des Produits)
- Ou migrer progressivement vers `commande.getLignes()` (List<LigneCommande>)

---

## Notes Finales

✅ **Intégration Produit/Commande** → Maintenant les commandes contiennent des quantités et prix par produit
✅ **Intégration Commande/Stock** → Ajout des vérifications de disponibilité automatiques
✅ **Intégration Commande/Depot** → Commande peut référencer son dépôt de fulfillment
✅ **Null-Safety** → Meilleures vérifications dans CommandeService.search()

**Prochain objectif prioritaire:** Persistance des Commandes en BD et transactions atomiques.

