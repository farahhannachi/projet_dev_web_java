# 🧪 TEST DE NAVIGATION - Boutons Stock et Dépôt

## 🎯 OBJECTIF
Vérifier que les boutons "Stock" et "Dépôt" dans le front-office fonctionnent correctement.

---

## 📋 PRÉ-REQUIS

### 1. Données en base
```sql
-- Vérifier que les données existent
SELECT COUNT(*) FROM depot;   -- Doit retourner > 0
SELECT COUNT(*) FROM stock;   -- Doit retourner > 0
```

### 2. Application démarrée
```bash
mvn javafx:run
# Se connecter comme utilisateur régulier
```

---

## 🧪 TESTS À EFFECTUER

### Test 1: Boutons dans la Navbar
```
Page: Accueil
Action: Cliquer sur "Dépôts" dans la barre de menu
Résultat Attendu:
✅ Page change vers "Nos Dépôts"
✅ Console affiche: "🔄 Navigation: Chargement du module Dépôts..."
✅ Console affiche: "✅ Module Dépôts chargé avec succès!"
✅ 5 cartes de dépôts affichées
```

### Test 2: Boutons dans la Navbar (Stocks)
```
Page: Accueil
Action: Cliquer sur "Stocks" dans la barre de menu
Résultat Attendu:
✅ Page change vers "Nos Stocks"
✅ Console affiche: "🔄 Navigation: Chargement du module Stocks..."
✅ Console affiche: "✅ Module Stocks chargé avec succès!"
✅ 30 cartes de stocks affichées
```

### Test 3: Cartes Cliquables
```
Page: Accueil
Action: Cliquer sur la carte "Nos Dépôts"
Résultat Attendu:
✅ Même résultat que Test 1
```

### Test 4: Cartes Cliquables (Stocks)
```
Page: Accueil
Action: Cliquer sur la carte "Nos Stocks"
Résultat Attendu:
✅ Même résultat que Test 2
```

### Test 5: Navigation Inter-Modules
```
Page: Module Dépôts
Action: Cliquer sur "Stocks" dans la navbar
Résultat Attendu:
✅ Navigation vers module Stocks
```

---

## 🔍 DIAGNOSTIC SI ÇA NE MARCHE PAS

### Problème: Boutons ne réagissent pas du tout
```
Cause: Méthodes non appelées
Solution: Vérifier les logs console
- Si pas de logs → problème de liaison FXML
- Vérifier onAction="#showDepots" et onAction="#showStocks"
```

### Problème: Page ne change pas
```
Cause: Erreur de chargement FXML
Solution: Vérifier les fichiers existent
- FrontDepots.fxml existe?
- FrontStocks.fxml existe?
- Controllers correctement référencés?
```

### Problème: Page change mais pas de données
```
Cause: Base de données vide
Solution: Insérer les données de test
- Exécuter INSERT_TEST_DATA.sql
- Vérifier les counts > 0
```

### Problème: Boutons visibles mais pas d'effet hover
```
Cause: CSS non chargé
Solution: Vérifier styles.css chargé
- Vérifier .menu-item:hover existe
- Vérifier .card:hover existe
```

---

## ✅ RÉSULTATS ATTENDUS

### Console (lors des clics)
```
🔄 Navigation: Chargement du module Dépôts...
✅ Module Dépôts chargé avec succès!

🔄 Navigation: Chargement du module Stocks...
✅ Module Stocks chargé avec succès!
```

### Interface Utilisateur
```
✅ Boutons changent de couleur au hover
✅ Boutons se pressent (effet visuel)
✅ Page change immédiatement
✅ Données affichées (si BD remplie)
✅ Navigation fluide entre modules
```

---

## 🚨 SI TOUJOURS PAS DE RÉACTION

### Vérifications d'urgence:

1. **Recompiler l'application**
```bash
mvn clean compile
mvn package
mvn javafx:run
```

2. **Vérifier les fichiers FXML**
```bash
# Les fichiers existent?
ls src/main/resources/fxml/Front*.fxml

# Controllers référencés?
grep "FrontStockController" src/main/resources/fxml/FrontStocks.fxml
grep "FrontDepotController" src/main/resources/fxml/FrontDepots.fxml
```

3. **Vérifier les méthodes Java**
```bash
# Méthodes existent?
grep "showStocks\|showDepots" src/main/java/org/example/controller/AccueilController.java
```

4. **Logs détaillés**
```bash
# Ajouter plus de logs si nécessaire
System.out.println("DEBUG: Bouton cliqué");
```

---

## 🎉 SUCCÈS

Si tous les tests passent:
- ✅ Navigation fonctionnelle
- ✅ Boutons réactifs
- ✅ Interface fluide
- ✅ Données affichées
- ✅ Front-office opérationnel!

**La navigation Stock/Dépôt est maintenant complètement fonctionnelle!** 🎯
