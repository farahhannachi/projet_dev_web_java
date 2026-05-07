# 🎯 COMMANDES DE TEST - Exécute ceci dans l'ordre!

## **ÉTAPE 1: Nettoyage complet**
```bash
mvn clean
```

## **ÉTAPE 2: Compilation simple (vérifier les erreurs)**
```bash
mvn compile
```

Si ✅ SUCCESS → continue vers Étape 3
Si ❌ ERREUR → relance avec:
```bash
mvn compile -X
```
Et cherche `ERROR` au début de la console

## **ÉTAPE 3: Compilation + préparation**
```bash
mvn package -DskipTests
```

## **ÉTAPE 4: LANCER L'APPLICATION** 🚀
```bash
mvn javafx:run
```

Si ✅ Fenêtre s'ouvre → SUCCÈS!
Si ❌ Erreur → relance avec debug:

## **ÉTAPE 5: DEBUG COMPLET**
```bash
mvn javafx:run -e
```

Remonte tout en haut et cherche:
- `Exception in thread`
- `Caused by:`
- `ERROR`

## **ÉTAPE 6: DEBUG EXTRA VERBOSE**
```bash
mvn javafx:run -X 2>&1 | head -100
```

## 📋 Checklist si ça crash:

- [ ] Accueil.fxml et Dashboard.fxml existent
- [ ] styles.css existe
- [ ] CuraVitaApp.java compile
- [ ] AccueilController.java compile
- [ ] DashboardController.java compile
- [ ] pom.xml n'a pas d'erreur XML
- [ ] Pas d'accent dans les chemins

## ✅ Si tout réussit:

1. Fenêtre 1400x900 s'ouvre
2. Titre: "CuraVita - Gestion de Pharmacie"
3. Navbar blanche avec logo vert
4. Clic sur 👤 → dropdown apparaît
5. Clic sur "Dashboard" → page dashboard

## 🛑 STOP ET DEBUG si:

- Exception found in "JavaFX Application Thread"
- NullPointerException
- "Cannot find resource"
- "Cannot resolve symbol"

C'est BON = Application marche! 🎉

