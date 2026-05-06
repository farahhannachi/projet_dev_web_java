# 🏗️ Architecture Front-Office CuraVita

## 📐 Vue Globale de l'Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CuraVita Application                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Login      │  │   Accueil    │  │  Dashboard   │  │
│  │ (Existing)   │  │ (Modified)   │  │ (Admin Only) │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         │                  │                │           │
│         └──────────────────┼────────────────┘           │
│                            │                            │
│         ┌──────────────────┼──────────────────┐         │
│         │                  │                  │         │
│  ┌─────────────┐    ┌─────────────┐   ┌─────────────┐ │
│  │  FRONT      │    │   FRONT     │   │   FRONT     │ │
│  │  DEPOTS     │    │   STOCKS    │   │  SERVICES   │ │
│  │ (NEW)       │    │   (NEW)     │   │  (NEW)      │ │
│  └─────────────┘    └─────────────┘   └─────────────┘ │
│         │                  │                  │         │
│         ├──────────────────┼──────────────────┤         │
│         │                                     │         │
│         └─────────────────┬─────────────────┘         │
│                           │                            │
│           ┌───────────────┼───────────────┐           │
│           │               │               │           │
│  ┌─────────────────┐ ┌───────────┐ ┌──────────────┐ │
│  │ DepotService    │ │StockSvc   │ │ServiceService│ │
│  │ getInstance()   │ │getAll()   │ │getInstance()│ │
│  └─────────────────┘ └───────────┘ └──────────────┘ │
│           │               │               │           │
│           └───────────────┼───────────────┘           │
│                           │                            │
│                      DATABASE (MySQL)                  │
│                                                        │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Flux de Données

### Module Dépôts
```
User Click "Dépôts"
         ↓
AccueilController.showDepots()
         ↓
Load FrontDepots.fxml
         ↓
FrontDepotController.initialize()
         ↓
DepotService.getInstance().getAll()
         ↓
SQL Query: SELECT * FROM depot
         ↓
List<Depot> allDepots
         ↓
displayDepots(allDepots)
         ↓
Create VBox Cards (3 per row)
         ↓
Add to depotsContainer
         ↓
Display UI
```

### Module Stocks
```
User Click "Stocks"
         ↓
AccueilController.showStocks()
         ↓
Load FrontStocks.fxml
         ↓
FrontStockController.initialize()
         ↓
StockService.getInstance().getAll()
         ↓
SQL Query: SELECT * FROM stock JOIN product JOIN depot
         ↓
List<Stock> allStocks
         ↓
displayStocks(allStocks)
         ↓
Create VBox Cards with Status Icon
         ↓
Add to stocksContainer
         ↓
Display UI
```

### Module Services
```
User Click "Services"
         ↓
AccueilController.showServices()
         ↓
Load FrontServices.fxml
         ↓
FrontServiceController.initialize()
         ↓
ServiceService.getInstance().getAll()
         ↓
SQL Query: SELECT * FROM service
         ↓
List<Service> allServices
         ↓
displayServices(allServices)
         ↓
Create VBox Cards with Type Icon
         ↓
Add to servicesContainer
         ↓
Display UI
```

---

## 🧩 Composants Architecture

### 1. Vue (FXML Files)
```
FrontDepots.fxml
├── VBox (main container)
├── HBox (navbar)
│   ├── Logo
│   ├── Menu Items (Accueil, Dépôts, Stocks, Services)
│   └── Profile Dropdown
├── VBox (hero section)
├── HBox (filters)
│   ├── TextField (search)
│   └── ComboBox (city filter)
└── ScrollPane
    └── VBox (depotsContainer)
        └── HBox (card row)
            └── VBox (card)
```

### 2. Contrôleur (Java Controllers)
```
FrontDepotController
├── initialize()
│   ├── Setup user role check
│   ├── Load depots
│   └── Setup listeners
├── loadDepots()
│   └── Call DepotService.getAll()
├── filterDepots()
│   └── Apply search & filters
├── displayDepots()
│   └── Create & display cards
└── Navigation methods
    ├── goToAccueil()
    ├── showStocks()
    ├── showServices()
    └── logout()
```

### 3. Modèle de Données
```
Depot
├── id: int
├── nom: String
├── adresse: String
├── ville: String
├── capaciteDepot: int
├── responsableDepot: String
└── responsableTelephone: String

Stock
├── id: int
├── produit: Produit
├── quantiteDisponible: int
├── seuilMinimum: int
├── depot: Depot
└── ...

Service
├── id: int
├── nom: String
├── type: String
├── specialite: String
├── telephone: String
├── email: String
├── adresse: String
└── ...
```

### 4. Services (Business Logic)
```
DepotService.getInstance()
├── getAll(): List<Depot>
└── ...

StockService.getInstance()
├── getAll(): List<Stock>
└── ...

ServiceService.getInstance()
├── getAll(): List<Service>
└── ...

UserService.getInstance()
├── isAdmin(): boolean
└── logout()
```

---

## 📊 Diagramme d'Interaction

### Interaction Utilisateur - Système
```
┌─────────────────────────────────────────────────────────┐
│                      USER                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Click "Stocks"
                     ↓
┌─────────────────────────────────────────────────────────┐
│                   UI LAYER (FXML)                       │
│  FrontStocks.fxml                                       │
├─────────────────────────────────────────────────────────┤
│  ├─ TextField (search)                                  │
│  ├─ ComboBox (depotFilter)                              │
│  └─ VBox (stocksContainer) ← Cards Rendered Here       │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Load Data
                     ↓
┌─────────────────────────────────────────────────────────┐
│               CONTROLLER LAYER (Java)                   │
│  FrontStockController                                   │
├─────────────────────────────────────────────────────────┤
│  initialize() ─→ loadStocks() ─→ filterStocks()        │
│  displayStocks() ─→ createStockCard()                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Get Data
                     ↓
┌─────────────────────────────────────────────────────────┐
│               SERVICE LAYER (Business Logic)            │
│  StockService.getInstance()                             │
├─────────────────────────────────────────────────────────┤
│  getAll(): List<Stock>                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ SQL Query
                     ↓
┌─────────────────────────────────────────────────────────┐
│              DATABASE LAYER (MySQL)                     │
│  SELECT * FROM stock JOIN product JOIN depot           │
├─────────────────────────────────────────────────────────┤
│  Return: ResultSet with Stock data                     │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Flux de Recherche & Filtrage

### Recherche en Temps Réel
```
User types in SearchField
         │
         ↓
TextField.textProperty().addListener()
         │
         ↓
filterStocks() is called
         │
         ↓
allStocks.stream()
  .filter(search condition)
  .filter(depot condition)
  .filter(status condition)
  .collect(Collectors.toList())
         │
         ↓
displayStocks(filtered list)
         │
         ↓
Clear stocksContainer
         │
         ↓
Create new Cards for filtered results
         │
         ↓
Add to UI
         │
         ↓
User sees results
```

### Exemple: Filtrer Stocks
```
Input:
  - Search: "aspirine"
  - Depot: "Tunis"
  - Status: "Stock faible"

Processing:
  allStocks = [Stock1, Stock2, Stock3, Stock4, Stock5, ...]
  
  filter(search):
    keeps: [Stock1(Aspirine), Stock3(Aspirine500mg)]
  
  filter(depot):
    keeps: [Stock1(depot=Tunis), Stock3(depot=Sfax)] → [Stock1]
  
  filter(status):
    keeps: [Stock1(quantity=45, min=50)] ✓ (45 <= 50)
  
Result:
  [Stock1(Aspirine, Tunis, Quantity=45, Min=50)]

Display:
  ⚠️ Aspirine
  📦 Quantité: 45
  🎯 Seuil min: 50
  🏢 Dépôt: Tunis
  🟡 Stock faible
```

---

## 🔐 Architecture de Contrôle d'Accès

```
Login
  │
  ├─→ User.type = "admin"
  │     │
  │     ├─→ UserService.isAdmin() = true
  │     │     └─→ Dashboard visible in dropdown
  │     │
  │     └─→ Access to all modules ✓
  │
  └─→ User.type = "user"
        │
        ├─→ UserService.isAdmin() = false
        │     └─→ Dashboard hidden
        │
        └─→ Access to front-office only ✓
              (Depots, Stocks, Services - Read Only)
```

### Implémentation
```java
// FrontDepotController.initialize()
if (dashboardMenuItem != null) {
    dashboardMenuItem.setVisible(userService.isAdmin());
    dashboardMenuItem.setManaged(userService.isAdmin());
}
```

---

## 📦 Dépendances et Versioning

### Technologie Stack
```
JavaFX 21
├── javafx-controls
├── javafx-fxml
├── javafx-graphics
└── javafx-base

MySQL 8.0+
└── JDBC Driver

Maven 3.9+
└── Maven Compiler Plugin
```

### Réutilisation du Code Existant
```
✅ DepotService (existant)
   └── getFrontDépôts (utilise getAll())

✅ StockService (existant)
   └── getFrontStocks (utilise getAll())

✅ ServiceService (existant)
   └── getFrontServices (utilise getAll())

✅ UserService (existant)
   └── isAdmin() check pour visibilité

✅ CSS Styling (existant)
   └── Réutilise toutes les classes .card, .navbar-pill, etc.
```

---

## 🎨 Architecture UI/UX

### Layout Structure (Tous les modules)
```
┌─ VBox (main) ─────────────────────────────────────────┐
│  ┌─ HBox (navbar-pill) ──────────────────────────────┐ │
│  │ Logo │ Menu │ Icons │ Profile Dropdown            │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌─ VBox (hero-section) ──────────────────────────────┐ │
│  │ Title: "Nos Dépôts" / "Nos Stocks" / "Nos Services"│ │
│  │ Subtitle: Description                             │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌─ HBox (filters) ───────────────────────────────────┐ │
│  │ [Search] [Filter1] [Filter2]                       │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌─ ScrollPane ───────────────────────────────────────┐ │
│  │ ┌─ VBox (cardsContainer) ────────────────────────┐ │ │
│  │ │ ┌─ HBox (row) ─────────────────────────────┐  │ │ │
│  │ │ │ Card │ Card │ Card                       │  │ │ │
│  │ │ └────────────────────────────────────────┘  │ │ │
│  │ │ ┌─ HBox (row) ─────────────────────────────┐  │ │ │
│  │ │ │ Card │ Card                              │  │ │ │
│  │ │ └────────────────────────────────────────┘  │ │ │
│  │ └──────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

### Card Structure (Tous les modules)
```
┌─ VBox (card) ─────────┐
│ ┌─ VBox (icon) ──┐   │
│ │   Emoji Icon   │   │
│ └────────────────┘   │
│ ┌─ Label ────────┐   │
│ │   Title        │   │
│ └────────────────┘   │
│ ┌─ VBox (details)────┐
│ │ Label 1: 📍 info   │
│ │ Label 2: 🏙️ info   │
│ │ Label 3: 📦 info   │
│ │ Label 4: 👤 info   │
│ │ Label 5: 📞 info   │
│ └────────────────────┘
└────────────────────────┘
```

---

## 🔄 Cycle de Vie d'un Module

```
1. INITIALISATION
   ├─ FXMLLoader charges le FXML
   ├─ Controller est instantié
   └─ initialize() est appelé

2. LOAD DATA
   ├─ loadDepots() appelé
   ├─ ServiceService.getAll() appelé
   ├─ List<Depot> retournée
   └─ displayDepots(list) appelé

3. RENDER UI
   ├─ Créer HBox rows (3 cartes par ligne)
   ├─ Créer VBox cards
   ├─ Ajouter les détails à chaque card
   └─ Ajouter à depotsContainer

4. SETUP LISTENERS
   ├─ searchField.textProperty().addListener()
   ├─ filterCombo.valueProperty().addListener()
   └─ Chaque changement → filterDepots() → displayDepots()

5. USER INTERACTION
   ├─ User tape dans search
   ├─ filterDepots() est appelé
   ├─ Résultats filtrés retournés
   ├─ displayDepots() re-render
   └─ UI mise à jour
```

---

## 📈 Performance

### Optimizations
```
✅ Filtrage côté client
   └─ Pas d'aller-retour DB à chaque recherche

✅ Lazy loading des cartes
   └─ Créées à la demande lors du rendu

✅ Stream API Java
   └─ Filtrage efficace avec filter() & collect()

✅ ScrollPane
   └─ Gère automatiquement les grandes listes

✅ CSS réutilisé
   └─ Pas de création de styles à la volée
```

### Complexité
```
Search: O(n)     // n = nombre d'éléments
Filter: O(n)     // Filtre après recherche
Display: O(m)    // m = résultats filtrés
Total: O(n)      // Acceptable pour <10k items
```

---

## 🔗 Integration Points

### Avec le système existant
```
LOGIN PAGE
    └─→ Authentification réussie
        └─→ ACCUEIL PAGE (Gateway)
            ├─→ Dashboard (admin only)
            ├─→ Dépôts (NEW - read only)
            ├─→ Stocks (NEW - read only)
            └─→ Services (NEW - read only)

Dashboard (admin)
    └─→ Gestion CRUD
        ├─→ Gestion Dépôts
        ├─→ Gestion Stocks
        └─→ Gestion Services

Front-Office (user)
    └─→ Consultation
        ├─→ Voir Dépôts
        ├─→ Voir Stocks
        └─→ Voir Services
```

---

## 📝 Conclusion

L'architecture du front-office suit les bonnes pratiques MVC:
- **Model**: Classes métier (Depot, Stock, Service)
- **View**: Fichiers FXML
- **Controller**: Classes Controller Java

Avec une séparation claire des responsabilités, une réutilisation maximale du code existant, et une performance optimale pour les opérations de recherche et filtrage.

**Status:** ✅ Architecture Validée et Implémentée

