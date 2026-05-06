# 🔴 SOLUTION POUR AFFICHER LES DONNÉES - Stocks et Services

## ❌ Le Problème
Vous cliquez sur "Stocks" ou "Services" → La page charge → **AUCUNE DONNÉE** n'apparaît

## ✅ La Cause
**La base de données est VIDE** - Il n'y a aucun enregistrement dans les tables

## 🚀 LA SOLUTION (3 ÉTAPES RAPIDES)

### ÉTAPE 1️⃣ - Ouvrir phpMyAdmin
```
URL: http://localhost/phpmyadmin
(ou via XAMPP/WAMP)
```

### ÉTAPE 2️⃣ - Exécuter le script SQL
1. Allez dans la base de données **pharmacie**
2. Cliquez sur l'onglet **"SQL"**
3. **COPIEZ-COLLEZ** le contenu du fichier: `INSERT_TEST_DATA.sql`
4. Cliquez sur **"Exécuter"** (le bouton bleu)
5. **Attendez** que ça finisse

### ÉTAPE 3️⃣ - Tester
1. **Relancez l'application**
2. **Allez sur Accueil**
3. **Cliquez "Stocks"** → Vous verrez 30 cartes de stocks! ✅
4. **Cliquez "Services"** → Vous verrez 12 cartes de services! ✅

---

## 📊 CE QUI SERA INSÉRÉ

**5 Dépôts** + **10 Produits** + **30 Stocks** + **12 Services** = **57 enregistrements**

```
✅ Aspirine 500mg - En stock - Tunis
🟡 Ibuprofène - Stock faible - Sfax  
🔴 Amoxicilline - Rupture - Sousse
+ 27 autres stocks

👨‍⚕️ Dr. Mohamed Belaid - Cardiologie
👨‍⚕️ Dr. Leila Mansouri - Pédiatrie
+ 10 autres services
```

---

## 🎯 RÉSULTAT FINAL

Après exécution du script:
- ✅ Les pages Stocks et Services afficheront les cartes
- ✅ La recherche fonctionnera  
- ✅ Les filtres fonctionneront
- ✅ Le front-office sera complètement opérationnel!

---

## 🆘 SI ÇA NE MARCHE PAS

### Erreur: "Table doesn't exist"
→ C'est normal si les tables n'existent pas encore
→ Créez les tables d'abord (voir base de données pharmacie.sql)

### Erreur: "Duplicate entry"  
→ Les données existent déjà
→ Exécutez ce script d'abord:
```sql
DELETE FROM stock;
DELETE FROM service;
DELETE FROM depot;
DELETE FROM produit;
```

### Les données ne s'affichent toujours pas
→ Fermez et relancez complètement l'application
→ Vérifiez que vous êtes bien connecté comme utilisateur

---

## 📁 FICHIER À EXÉCUTER

**Ouvrir:** `INSERT_TEST_DATA.sql`
**Copier:** Tout le contenu
**Coller:** Dans phpMyAdmin → SQL
**Exécuter:** Cliquez le bouton bleu

---

**Prêt? Allez-y! 🚀**
