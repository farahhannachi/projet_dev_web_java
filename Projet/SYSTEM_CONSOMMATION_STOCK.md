# 📦 SYSTÈME DE CONSOMMATION DE STOCK PAR SERVICES

## 🎯 OBJECTIF

Implémenter une traçabilité complète du flux de consommation de stock par les services médicaux (médecins/infirmiers).

```
Service (Médecin/Infirmier)
        ↓
   Demande Stock
        ↓
 Consommation Validée
        ↓
 Stock Décrémenté
        ↓
 Mouvement Enregistré
        ↓
   Traçabilité Complète
```

---

## 📊 ARCHITECTURE DE DONNÉES

### Tables impliquées :

```
┌─────────────┐         ┌──────────────┐         ┌─────────┐
│  SERVICE    │◄────────│STOCK_MOVEMENT├────────►│  STOCK  │
│             │ 1..n    │              │ 1..n    │         │
│ id_service  │         │ id_service   │         │id_stock │
│ nom_service │         │ id_stock_id  │         │quantite │
└─────────────┘         │ type         │         └─────────┘
                        │ type_consomm │              ▲
                        │ quantite     │              │
                        │ status       │              │
                        │ motif        │              │
                        │ ref_document │         ┌────────────┐
                        │ created_at   │         │   DEPOT    │
                        └──────────────┘         │            │
                                                 │ id_depot   │
                                                 └────────────┘
```

### Colonnes ajoutées à `stock_movement` :

| Colonne | Type | Description |
|---------|------|-------------|
| `id_service` | INT | Clé étrangère vers Service |
| `type_consommation` | VARCHAR(50) | Type: CONSOMMATION_SERVICE, RETOUR, etc. |
| `reference_document` | VARCHAR(100) | Num. ordonnance ou document source |

---

## 🔧 INSTALLATION

### 1️⃣ Exécuter le script SQL de modification

```sql
-- Fichier: MODIFIER_STOCK_MOVEMENT.sql
-- Exécute dans PhpMyAdmin
```

Ce script :
- ✅ Ajoute `id_service` à `stock_movement`
- ✅ Ajoute `type_consommation` pour typer les mouvements
- ✅ Ajoute `reference_document` pour tracer les ordonnances
- ✅ Crée les indexes pour performance
- ✅ Ajoute la contrainte de clé étrangère

### 2️⃣ Code Java créé

Fichiers à intégrer :

```
src/main/java/org/example/
├── model/
│   └── StockMovement.java          ✅ Modèle enrichi
├── service/
│   └── ServiceConsommationService.java   ✅ Logique métier
└── controller/
    └── ServiceConsommationController.java ✅ Interface UI

src/main/resources/fxml/
└── service_consommation.fxml      ✅ Interface FXML
```

---

## 🚀 UTILISATION

### Service de consommation : `ServiceConsommationService`

#### Enregistrer une consommation :
```java
ServiceConsommationService service = ServiceConsommationService.getInstance();

int idMouvement = service.enregistrerConsommation(
    idService,              // ID du médecin/infirmier
    idStock,                // ID du produit en stock
    quantiteConsommee,      // Quantité à consommer
    "Traitement patient",   // Motif
    "ORD-2026-001"         // Numéro ordonnance (optionnel)
);

System.out.println("Mouvement créé: " + idMouvement);
```

#### Récupérer l'historique d'un service :
```java
List<StockMovement> historique = service.getHistoriqueService(idService);

for (StockMovement m : historique) {
    System.out.println(m.toString());
    // Affiche: Mouvement: CONSOMMATION_SERVICE - 5 unités par Dr. Dupont le 2026-04-13T10:30:00
}
```

#### Récupérer les mouvements d'un stock :
```java
List<StockMovement> mouvements = service.getHistoriqueStock(idStock);
```

#### Récupérer les mouvements récents (30 jours) :
```java
List<StockMovement> recents = service.getMouvementsRecents();
```

---

## 📈 FLUX D'EXÉCUTION

### Lors d'une consommation :

```
1. Validation des données
   ├─ Service existe ?
   ├─ Stock existe ?
   └─ Quantité positive ?

2. Vérification de disponibilité
   ├─ Stock suffisant ?
   └─ Quantité demandée <= Quantité disponible ?

3. Transaction ATOMIQUE (tout ou rien)
   ├─ Enregistrer le mouvement en BD
   ├─ Décrémenter le stock
   ├─ Mettre à jour les timestamps
   ├─ Incrémenter compteur sorties
   └─ COMMIT

4. Retour du résultat
   └─ ID du mouvement créé ou erreur
```

### Exemple d'exécution :

```
Stock AVANT:  Paracétamol = 100 unités, dépôt A

Consommation:
- Service: Dr. Martin (Cardiologue)
- Quantité: 5 unités
- Motif: Traitement hypertension
- Ordonnance: ORD-2026-0145

Stock APRÈS:  Paracétamol = 95 unités

MOUVEMENT ENREGISTRÉ:
┌─────────────────────────────────────┐
│ ID Mouvement: 42                    │
│ Date: 2026-04-13 14:25:30           │
│ Service: Dr. Martin                 │
│ Produit: Paracétamol                │
│ Type: CONSOMMATION_SERVICE          │
│ Quantité: 5                         │
│ Avant: 100 → Après: 95              │
│ Motif: Traitement hypertension      │
│ Ordonnance: ORD-2026-0145           │
│ Dépôt: A                            │
│ Status: APPROUVEE                   │
└─────────────────────────────────────┘
```

---

## 📋 TRAÇABILITÉ COMPLÈTE

### Questions répondues par le système :

✅ **Qui ?** → Nom du service (médecin/infirmier)  
✅ **Quoi ?** → Produit consommé  
✅ **Combien ?** → Quantité exacte  
✅ **Quand ?** → Date/heure précise  
✅ **D'où ?** → Dépôt source  
✅ **Pourquoi ?** → Motif de la consommation  
✅ **Ref. Doc ?** → Numéro ordonnance/document  
✅ **Status ?** → APPROUVEE, EN_ATTENTE, REJETEE  

---

## ⚠️ CONTRÔLES DE SÉCURITÉ

### 1. Validation des données
```java
if (quantiteConsommee <= 0) {
    throw new RuntimeException("Quantité doit être positive");
}
```

### 2. Vérification de disponibilité
```java
if (stock.getQuantite() < quantiteConsommee) {
    throw new RuntimeException("Stock insuffisant");
}
```

### 3. Transaction atomique
```java
conn.setAutoCommit(false);
try {
    // Opérations
    conn.commit();
} catch (Exception e) {
    conn.rollback();  // Annule tout si erreur
    throw e;
}
```

### 4. Logs et traçabilité
- Chaque mouvement est enregistré
- Timestamps automatiques
- Service et dépôt tracés
- Historique consultable

---

## 🔍 REQUÊTES SQL DISPONIBLES

### Historique d'un service :
```sql
SELECT m.*, s.nom_service, p.nom as produit
FROM stock_movement m
LEFT JOIN service s ON m.id_service = s.id_service
LEFT JOIN stock st ON m.id_stock_id = st.id_stock
LEFT JOIN produit p ON st.produit_id = p.id_produit
WHERE m.id_service = ?
ORDER BY m.created_at DESC;
```

### Historique d'un produit :
```sql
SELECT m.*, p.nom, d.nom_depot, s.nom_service
FROM stock_movement m
LEFT JOIN stock st ON m.id_stock_id = st.id_stock
LEFT JOIN produit p ON st.produit_id = p.id_produit
LEFT JOIN depot d ON st.depot_id = d.id_depot
LEFT JOIN service s ON m.id_service = s.id_service
WHERE m.id_stock_id = ?
ORDER BY m.created_at DESC;
```

### Consommations du jour :
```sql
SELECT *
FROM stock_movement
WHERE DATE(created_at) = CURDATE()
AND type_consommation = 'CONSOMMATION_SERVICE'
ORDER BY created_at DESC;
```

---

## 🎓 EXEMPLE D'INTÉGRATION DANS UN CONTRÔLEUR

```java
@FXML
private void handleConsommer() {
    // 1. Récupérer les données du formulaire
    Service service = serviceCombo.getValue();
    Stock stock = stockCombo.getValue();
    int quantite = quantiteSpinner.getValue();
    String motif = motifField.getText();
    String refDoc = referenceDocumentField.getText();

    // 2. Enregistrer la consommation
    try {
        int idMouvement = consommationService.enregistrerConsommation(
            service.getId(),
            stock.getId(),
            quantite,
            motif,
            refDoc
        );
        
        NotificationUtil.showSuccess("✅ Consommation enregistrée!");
        
        // 3. Rafraîchir l'affichage
        chargerStocks();
        rafraichirHistorique();
        
    } catch (RuntimeException e) {
        NotificationUtil.showError("❌ Erreur: " + e.getMessage());
    }
}
```

---

## 🧪 TEST

Pour tester la consommation :

```java
ServiceConsommationService scs = ServiceConsommationService.getInstance();

try {
    int mouvement = scs.enregistrerConsommation(
        1,              // Service ID 1 (Dr. Martin)
        1,              // Stock ID 1
        5,              // 5 unités
        "Test consommation",
        "TEST-001"
    );
    
    System.out.println("✅ Consommation enregistrée: " + mouvement);
    
    // Vérifier l'historique
    List<StockMovement> historique = scs.getMouvementsRecents();
    System.out.println("📊 Total mouvements: " + historique.size());
    
} catch (RuntimeException e) {
    System.err.println("❌ Erreur: " + e.getMessage());
}
```

---

## 📞 SUPPORT

- 🔴 **Stock insuffisant ?** Le système refuse la consommation
- 🟠 **Alerte stock faible ?** Avertissement dans l'interface
- ✅ **Consommation approuvée ?** Enregistrée avec toutes les données
- 📊 **Besoin d'historique ?** Requête SQL disponible

---

**Status:** ✅ Système complet et prêt à l'emploi  
**Dernière mise à jour:** 2026-04-13

