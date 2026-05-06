# ⚡ COMMANDES RAPIDES - CONSOMMATION DE STOCK

## 🚀 Installation en 3 étapes

### Étape 1 : Base de données
```bash
# PhpMyAdmin → SQL → Copier/coller et exécuter:
MODIFIER_STOCK_MOVEMENT.sql
```

### Étape 2 : Code Java
```bash
# Copier les fichiers:
src/main/java/org/example/
├── model/StockMovement.java
├── service/ServiceConsommationService.java
└── controller/ServiceConsommationController.java

src/main/resources/fxml/
└── service_consommation.fxml
```

### Étape 3 : Vérification
```bash
# PhpMyAdmin → SQL → Copier/coller et exécuter:
VERIFIER_CONSOMMATION.sql
```

---

## 💻 Utilisation en Java

### Enregistrer une consommation
```java
ServiceConsommationService scs = 
    ServiceConsommationService.getInstance();

int idMouvement = scs.enregistrerConsommation(
    1,                      // ID Service
    1,                      // ID Stock
    5,                      // Quantité
    "Motif",               // Raison
    "ORD-2026-001"         // Ordonnance (opt)
);
```

### Récupérer l'historique d'un service
```java
List<StockMovement> historique = 
    scs.getHistoriqueService(1);
```

### Récupérer les mouvements d'un stock
```java
List<StockMovement> mouvements = 
    scs.getHistoriqueStock(1);
```

### Récupérer les mouvements récents
```java
List<StockMovement> recents = 
    scs.getMouvementsRecents(); // 30 derniers jours
```

---

## 📊 Requêtes SQL utiles

### Historique d'un service
```sql
SELECT m.*, s.nom_service, p.nom as produit, d.nom_depot
FROM stock_movement m
LEFT JOIN service s ON m.id_service = s.id_service
LEFT JOIN stock st ON m.id_stock_id = st.id_stock
LEFT JOIN produit p ON st.produit_id = p.id_produit
LEFT JOIN depot d ON st.depot_id = d.id_depot
WHERE m.id_service = 1
ORDER BY m.created_at DESC;
```

### Top 10 produits consommés
```sql
SELECT p.nom, SUM(m.quantite) as total
FROM stock_movement m
LEFT JOIN stock st ON m.id_stock_id = st.id_stock
LEFT JOIN produit p ON st.produit_id = p.id_produit
WHERE m.type_consommation = 'CONSOMMATION_SERVICE'
GROUP BY p.nom
ORDER BY total DESC
LIMIT 10;
```

### Top 10 services consommateurs
```sql
SELECT s.nom_service, COUNT(*) as nb, SUM(m.quantite) as total
FROM stock_movement m
LEFT JOIN service s ON m.id_service = s.id_service
WHERE m.type_consommation = 'CONSOMMATION_SERVICE'
GROUP BY s.nom_service
ORDER BY total DESC
LIMIT 10;
```

### Consommation du jour
```sql
SELECT DATE(m.created_at) as jour, s.nom_service, COUNT(*) as nb
FROM stock_movement m
LEFT JOIN service s ON m.id_service = s.id_service
WHERE DATE(m.created_at) = CURDATE()
GROUP BY DATE(m.created_at), s.nom_service;
```

### Stocks sous seuil critique
```sql
SELECT p.nom, st.quantite, st.seuil_critique
FROM stock st
LEFT JOIN produit p ON st.produit_id = p.id_produit
WHERE st.quantite < st.seuil_critique
ORDER BY st.quantite ASC;
```

### Historique par ordonnance
```sql
SELECT *
FROM stock_movement
WHERE reference_document = 'ORD-2026-0145'
ORDER BY created_at DESC;
```

---

## 🧪 Tests

### Lancer les tests automatisés
```bash
# Compile et exécute:
javac TestServiceConsommation.java
java org.example.test.TestServiceConsommation

# Ou directement depuis IDE:
Run → TestServiceConsommation.java
```

### Tester manuellement
```java
// Console Java ou tests unitaires:
ServiceConsommationService scs = 
    ServiceConsommationService.getInstance();

// Test 1: Consommation simple
scs.enregistrerConsommation(1, 1, 5, "Test", null);

// Test 2: Historique
List<StockMovement> m = scs.getMouvementsRecents();
System.out.println("Mouvements: " + m.size());

// Test 3: Erreur - Quantité négative
try {
    scs.enregistrerConsommation(1, 1, -5, "Test", null);
} catch (RuntimeException e) {
    System.out.println("❌ Correctement rejeté: " + e.getMessage());
}
```

---

## 🐛 Dépannage rapide

### "Colonne id_service not found"
→ Exécute MODIFIER_STOCK_MOVEMENT.sql

### "Class not found: StockMovement"
→ Copie les fichiers .java au bon endroit

### "Table stock_movement doesn't exist"
→ Crée la table avec CREATE_SERVICE_TABLE.sql + MODIFIER...sql

### Les mouvements ne s'affichent pas
```sql
-- Vérifie:
SELECT COUNT(*) FROM stock_movement;
SELECT * FROM stock_movement WHERE id_service IS NOT NULL;
```

### Compilation Java échoue
```bash
# Vérifie les imports (utilise same package):
import org.example.model.StockMovement;
import org.example.service.ServiceConsommationService;
```

---

## 📁 Structure finale

```
projet/
├── src/main/java/org/example/
│   ├── model/
│   │   └── StockMovement.java          ✅ NOUVEAU
│   ├── service/
│   │   └── ServiceConsommationService.java  ✅ NOUVEAU
│   ├── controller/
│   │   └── ServiceConsommationController.java  ✅ NOUVEAU
│   └── test/
│       └── TestServiceConsommation.java      ✅ TEST
│
├── src/main/resources/fxml/
│   └── service_consommation.fxml       ✅ NOUVEAU
│
└── SQL/
    ├── MODIFIER_STOCK_MOVEMENT.sql     (Migration)
    └── VERIFIER_CONSOMMATION.sql       (Tests)
```

---

## ✅ Checklist avant go-live

- [ ] MODIFIER_STOCK_MOVEMENT.sql exécuté
- [ ] Tous les fichiers .java copié
- [ ] service_consommation.fxml copié
- [ ] VERIFIER_CONSOMMATION.sql sans erreur
- [ ] TestServiceConsommation.java ✅
- [ ] Compilation Java OK
- [ ] Interface affichée
- [ ] Consommation créée
- [ ] Historique visible
- [ ] Erreurs gérées

---

## 📞 Besoin d'aide ?

1. **Erreur SQL?** → VERIFIER_CONSOMMATION.sql
2. **Erreur Java?** → Console + TestServiceConsommation.java
3. **Fonctionnalité?** → SYSTEM_CONSOMMATION_STOCK.md
4. **Installation?** → GUIDE_INTEGRATION_CONSOMMATION.md

---

**Créé:** 2026-04-13  
**Version:** 1.0  
**Statut:** Production Ready ✅

