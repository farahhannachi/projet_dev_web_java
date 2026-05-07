# 🔧 Guide - Insertion des Données de Test Front-Office

## 📋 Problème Identifié

Quand vous cliquez sur les boutons "Stocks" ou "Services" dans le front-office, **aucune donnée ne s'affiche** parce que la base de données est vide.

## ✅ Solution - Insérer des Données de Test

### Étape 1: Ouvrir phpMyAdmin ou MySQL Workbench
```
URL: http://localhost/phpmyadmin
Utilisateur: root
Mot de passe: [votre mot de passe MySQL]
Base de données: pharmacie
```

### Étape 2: Exécuter le Script SQL
1. Ouvrez le fichier `INSERT_TEST_DATA.sql`
2. Copiez tout le contenu
3. Collez-le dans l'onglet "SQL" de phpMyAdmin
4. Cliquez sur "Exécuter"

### Étape 3: Vérifier l'Insertion
Après exécution, vous devriez voir:
```
Dépôts insérés: 5
Produits insérés: 10
Stocks insérés: 30
Services insérés: 12
```

## 📊 Données Insérées

### 🏢 Dépôts (5)
- Dépôt Tunis Centre
- Dépôt Sfax Sud
- Dépôt Sousse Nord
- Dépôt Tunis Nord
- Dépôt Sfax Centre

### 📦 Stocks (30)
- **10 produits** × **3 dépôts** = 30 stocks
- **Différents statuts**:
  - ✅ En stock (quantité > seuil)
  - 🟡 Stock faible (quantité ≤ seuil)
  - 🔴 Rupture (quantité = 0)

### 👨‍⚕️ Services (12)
- **8 Médecins** avec spécialités variées
- **4 Infirmiers** avec différents domaines

## 🎯 Résultat Attendu

Après insertion des données:

### Page Stocks
```
✅ Aspirine 500mg - En stock
🟡 Ibuprofène 400mg - Stock faible
🔴 Amoxicilline 500mg - Rupture
... (27 autres stocks)
```

### Page Services
```
👨‍⚕️ Dr. Mohamed Belaid - Médecin (Cardiologie)
👩‍⚕️ Inf. Samir Bouslama - Infirmier (Soins généraux)
... (10 autres services)
```

## 🔍 Vérification

### Via phpMyAdmin
```sql
-- Compter les enregistrements
SELECT COUNT(*) FROM depot;      -- Doit retourner 5
SELECT COUNT(*) FROM produit;    -- Doit retourner 10
SELECT COUNT(*) FROM stock;      -- Doit retourner 30
SELECT COUNT(*) FROM service;    -- Doit retourner 12
```

### Via l'Application
1. Relancer l'application
2. Se connecter
3. Cliquer sur "Stocks" → Voir 30 cartes
4. Cliquer sur "Services" → Voir 12 cartes

## 🚨 Dépannage

### Erreur: "Table doesn't exist"
**Cause:** Les tables n'ont pas été créées
**Solution:** Exécuter d'abord le script de création des tables

### Erreur: "Duplicate entry"
**Cause:** Les données existent déjà
**Solution:** Vider les tables d'abord
```sql
DELETE FROM stock;
DELETE FROM service;
DELETE FROM depot;
DELETE FROM produit;
```

### Erreur: "Foreign key constraint"
**Cause:** Ordre d'insertion incorrect
**Solution:** Le script respecte l'ordre: depot → produit → stock/service

## 📁 Fichiers Impliqués

- `INSERT_TEST_DATA.sql` - Script d'insertion des données
- `FrontStockController.java` - Controller qui charge les stocks
- `FrontServiceController.java` - Controller qui charge les services
- `StockService.java` - Service qui récupère les stocks
- `ServiceService.java` - Service qui récupère les services

## ✅ Test Final

Après avoir inséré les données:

1. **Compiler** l'application
2. **Lancer** l'application
3. **Se connecter** comme utilisateur
4. **Cliquer "Stocks"** → Voir les 30 stocks avec statuts
5. **Cliquer "Services"** → Voir les 12 services avec icônes
6. **Tester les filtres** → Recherche et filtres fonctionnels

**Résultat:** Front-office complètement fonctionnel avec données! 🎉
