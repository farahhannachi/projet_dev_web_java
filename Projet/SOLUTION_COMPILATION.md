# ✅ Solution - Erreur Maven et JavaFX Corrigée

## Problème Identifié

Le projet Maven échouait la compilation avec l'erreur:
```
[ERROR] Could not resolve dependencies for project ai.raics:Projet_java:jar:1.0-SNAPSHOT: 
The following artifacts could not be resolved: 
org.openjfx:javafx-controls:jar:${javafx.platform}:21.0.2 (absent)
```

### Cause Racine

Les POMs publiques des dépendances JavaFX 21.0.2 contiennent des dépendances transitives sur les mêmes artifacts **avec le classifieur `${javafx.platform}` NON REMPLACÉ**. Cela crée une boucle de dépendances cassées.

## Solution Appliquée ✅

### 1. **Expliciter le Classifieur Windows**

Toutes les dépendances JavaFX spécifient explicitement le classifieur `win` pour Windows:

```xml
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
  <version>${javafx.version}</version>
  <classifier>win</classifier>
  <type>jar</type>
  <scope>compile</scope>
</dependency>
```

### 2. **Exclure les Dépendances Transitives Cassées**

Les dépendances transitives problématiques sont EXPLICITEMENT EXCLUES:

```xml
<exclusions>
  <exclusion>
    <groupId>*</groupId>
    <artifactId>*</artifactId>
  </exclusion>
</exclusions>
```

Cela empêche Maven de tenter de télécharger les dépendances cassées des POMs JavaFX.

### 3. **Supprimer le Plugin JavaFX Maven**

Le plugin `javafx-maven-plugin` (version 0.0.8) a été remplacé par `maven-shade-plugin` qui est plus stable pour les versions modernes de JavaFX.

## Commandes de Compilation

### Compilation Complète
```bash
cd "C:\Users\fahan\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"
.\apache-maven-3.9.7\bin\mvn.cmd clean package -DskipTests
```

### Execution des Logs
```bash
.\apache-maven-3.9.7\bin\mvn.cmd clean compile
.\apache-maven-3.9.7\bin\mvn.cmd package
```

## Artifacts Générés

- **JAR Exécutable**: `target/Projet_java-1.0-SNAPSHOT-executable.jar` (13.09 MB)
- **JAR Standard**: `target/Projet_java-1.0-SNAPSHOT.jar` (0.04 MB)

Classe Principale: `org.example.CuraVitaApp`

## Dépendances Résolues ✅

- ✅ org.openjfx:javafx-controls:21.0.2:win
- ✅ org.openjfx:javafx-fxml:21.0.2:win
- ✅ org.openjfx:javafx-graphics:21.0.2:win
- ✅ org.openjfx:javafx-base:21.0.2:win
- ✅ org.mindrot:jbcrypt:0.4
- ✅ com.mysql:mysql-connector-j:8.2.0

## Fichiers Modifiés

- **pom.xml** - Corrections des dépendances JavaFX et exclusions

## Recommandations Futures

1. **Mise à Jour JavaFX**: Envisager JavaFX 22+ qui a peut-être corrigé ce problème
2. **Suppression du Classifieur**: Les futures versions pourraient ne pas nécessiter le classifieur explicite
3. **Gradle Alternative**: Gradle gère mieux les dépendances JavaFX que Maven

## Nettoyage du Cache Maven

Si des problèmes persistent:

```powershell
Remove-Item -Path "$env:USERPROFILE\.m2" -Recurse -Force
```

Puis relancer:
```bash
.\apache-maven-3.9.7\bin\mvn.cmd clean package
```

---

**Statut**: ✅ RÉSOLU - Compilation réussie (45.9s)  
**Date**: 2026-04-12  
**Version Maven**: 3.9.7  
**Version Java**: 21  
**Plateforme**: Windows 11


