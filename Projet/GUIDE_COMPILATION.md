# Guide de Compilation et Exécution du Projet CuraVita

## Problème Corrigé ✅

Le projet avait une erreur dans `pom.xml` : les dépendances JavaFX utilisaient un classifieur `${javafx.platform}` qui n'était pas correctement remplacé.

**Solution Appliquée**: Les dépendances JavaFX ont été mises à jour pour utiliser explicitement le classifieur `win` pour Windows.

## Étapes pour Compiler et Exécuter

### Option 1: Utiliser le Script de Compilation (Recommandé)

```bash
# Naviguez dans le répertoire du projet
cd C:\Users\fahan\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java

# Lancez le script de compilation local
.\build_local.bat
```

### Option 2: Commande Maven Directe

```bash
# Nettoyage et compilation
.\apache-maven-3.9.7\bin\mvn.cmd clean compile

# Packaging du projet en JAR
.\apache-maven-3.9.7\bin\mvn.cmd package

# Exécution de l'application
.\apache-maven-3.9.7\bin\mvn.cmd javafx:run
```

### Option 3: Commande Unique

```bash
# Compile, package et exécute en une seule commande
.\apache-maven-3.9.7\bin\mvn.cmd clean package javafx:run
```

## Configuration Corrigée du pom.xml

Les dépendances JavaFX ont été corrigées:

```xml
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
  <version>21.0.2</version>
  <classifier>win</classifier>  <!-- Classifieur pour Windows -->
</dependency>
```

Tous les artifacts JavaFX (controls, fxml, graphics, base) utilisent le classifieur `win` pour télécharger les versions spécifiques à Windows.

## Résolution de Problèmes

### Si la compilation échoue encore:

1. **Nettoyer le cache Maven local**:
   ```powershell
   Remove-Item -Path "$env:USERPROFILE\.m2\repository\org\openjfx" -Recurse -Force -ErrorAction SilentlyContinue
   ```

2. **Supprimer le dossier target**:
   ```powershell
   Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
   ```

3. **Réessayer la compilation**:
   ```bash
   .\apache-maven-3.9.7\bin\mvn.cmd clean compile -U
   ```

## Configuration Requise

- **Java 21** (Eclipse Adoptium JDK 21+)
- **Maven 3.9.7** (inclus dans le projet)
- **Windows 10/11** (pour JavaFX win)
- **MySQL** (pour la base de données)

## Artefacts Générés

- JAR Principal: `target/Projet_java-1.0-SNAPSHOT.jar`
- Classe Principale: `org.example.CuraVitaApp`

## Dépendances Principales

- **JavaFX 21.0.2** - Interface Graphique
- **MySQL Connector 8.2.0** - Base de Données
- **jBCrypt 0.4** - Hachage de Mots de Passe


