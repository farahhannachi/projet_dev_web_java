# 🔧 Guide de Dépannage - Système de Réservation CuraVita

## Problèmes Courants et Solutions

### ❌ Erreur 1: "Table 'pharmacie.reservation' doesn't exist"

**Cause**: La table de réservation n'a pas été créée
**Solutions**:

1. **Automatique** (Recommandé):
   - Laissez l'application se lancer - elle créera la table automatiquement
   - Vérifiez que MySQL Server est en cours d'exécution
   - Vérifiez que la base 'pharmacie' existe

2. **Manuel**:
   ```sql
   -- Exécutez dans phpMyAdmin ou MySQL Workbench
   CREATE TABLE IF NOT EXISTS reservation (
       id INT PRIMARY KEY AUTO_INCREMENT,
       service_id INT NOT NULL,
       nom_client VARCHAR(100) NOT NULL,
       email_client VARCHAR(100) NOT NULL,
       telephone_client VARCHAR(20) NOT NULL,
       date_reservation DATETIME NOT NULL,
       date_rendez_vous DATETIME NOT NULL,
       motif LONGTEXT NOT NULL,
       statut VARCHAR(50) DEFAULT 'En attente',
       date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (service_id) REFERENCES service(id_service) ON DELETE CASCADE,
       INDEX idx_service (service_id),
       INDEX idx_date (date_rendez_vous),
       INDEX idx_statut (statut)
   );
   ```

3. **Vérification**:
   ```sql
   SELECT * FROM reservation;
   ```
   Devrait retourner une table vide (ou avec des données si déjà utilisée)

---

### ❌ Erreur 2: "Cannot connect to database"

**Cause**: Connexion MySQL impossible
**Solutions**:

1. **Vérifier que MySQL est actif**:
   - Windows: Menu Démarrer → Services → MySQL80 (doit être "En cours d'exécution")
   - Ou: `tasklist | find /i "mysql"`

2. **Vérifier les paramètres de connexion**:
   - Fichier: `src/main/java/org/example/util/DatabaseUtil.java`
   - Vérifiez ligne 9-11:
     ```java
     private static final String DB_URL = "jdbc:mysql://localhost:3306/pharmacie?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
     private static final String DB_USER = "root";
     private static final String DB_PASSWORD = "";
     ```
   - **Si votre config est différente**:
     - DB_USER: remplacez par votre nom d'utilisateur MySQL
     - DB_PASSWORD: remplacez par votre mot de passe MySQL
     - DB_URL: modifiez l'hôte/port si nécessaire

3. **Redémarrer MySQL**:
   ```bash
   # Windows CMD (Administrateur)
   net stop MySQL80
   net start MySQL80
   ```

4. **Tester la connexion**:
   ```bash
   # Depuis cmd
   mysql -h localhost -u root -p
   # Appuyez sur Entrée si pas de mot de passe
   
   # Devrait afficher: mysql>
   # Tapez: SHOW DATABASES;
   # Vérifiez que 'pharmacie' est dans la liste
   ```

---

### ❌ Erreur 3: "Service avec id X n'existe pas"

**Cause**: Aucun service n'existe dans la table service
**Solutions**:

1. **Insérer des services de test**:
   ```sql
   INSERT INTO service (nom_service, type_service, specialite, telephone, email, adresse, date_creation)
   VALUES 
   ('Dr. Marie Dupont', 'Médecin', 'Cardiologie', '01-23-45-67-89', 'marie@clinic.com', '123 Rue de la Santé', NOW()),
   ('Dr. Jean Martin', 'Médecin', 'Dermatologie', '01-23-45-67-90', 'jean@clinic.com', '123 Rue de la Santé', NOW()),
   ('Infirmier Pierre Dubois', 'Infirmier', 'Soins généraux', '01-23-45-67-91', 'pierre@clinic.com', '123 Rue de la Santé', NOW());
   ```

2. **Vérifier les services**:
   ```sql
   SELECT * FROM service;
   ```

---

### ❌ Erreur 4: "Le type d'élément 'ComboBox' doit être suivi..."

**Cause**: Syntaxe FXML invalide (génériques en XML)
**Statut**: ✅ CORRIGÉ dans notre implémentation
**Action**: Rien à faire, c'est déjà fixé

---

### ❌ Erreur 5: Fenêtre modale ne s'ouvre pas

**Cause 1**: ReservationForm.fxml n'existe pas
**Solution**:
```bash
# Vérifier que le fichier existe:
dir src\main\resources\fxml\ReservationForm.fxml

# Doit afficher le fichier avec ~2 KB
```

**Cause 2**: Contrôleur non trouvé
**Solution**:
```bash
# Vérifier que ReservationFormController existe:
dir src\main\java\org\example\controller\ReservationFormController.java

# Vérifier le chemin dans FXML (ligne 6):
# fx:controller="org.example.controller.ReservationFormController"
```

**Cause 3**: Service non sélectionné
**Solution**:
- Le service doit être sélectionné avant d'ouvrir le formulaire
- Vérifiez `FrontServicesController.openReservationForm()`

---

### ❌ Erreur 6: "NullPointerException dans le formulaire"

**Cause**: Champ FXML n'existe pas ou mal bindé
**Solution**:

1. **Vérifier les fx:id dans FXML**:
   ```xml
   <TextField fx:id="nomClientField" .../>
   <TextField fx:id="emailField" .../>
   <TextField fx:id="telephoneField" .../>
   <DatePicker fx:id="datePicker" .../>
   <ComboBox fx:id="heureCombo" .../>
   <TextArea fx:id="motifArea" .../>
   <Label fx:id="serviceName" .../>
   <Label fx:id="serviceType" .../>
   <Label fx:id="serviceSpecialite" .../>
   <Label fx:id="errorLabel" .../>
   ```

2. **Vérifier les @FXML dans le contrôleur**:
   - Tous les fx:id doivent avoir un @FXML dans ReservationFormController
   - Les noms doivent être identiques (case-sensitive!)

3. **Recompiler**:
   ```bash
   .\apache-maven-3.9.7\bin\mvn.cmd clean compile
   ```

---

### ❌ Erreur 7: Réservation ne s'enregistre pas

**Cause 1**: Validation échouée silencieusement
**Solution**:
- Vérifiez les messages d'erreur en rouge dans le formulaire
- Tous les champs avec * sont obligatoires

**Cause 2**: Erreur SQL dans le journal
**Solution**:
- Regardez la console lors du lancement
- Les erreurs sont affichées avec "Erreur lors de l'ajout de la réservation:"
- Vérifiez que la table est bien créée

**Cause 3**: Service ID invalide
**Solution**:
- Vérifiez que le service_id existe dans la table service
- ```sql
  SELECT id_service FROM service LIMIT 5;
  ```

---

### ❌ Erreur 8: "Application timeout" ou "Freeze"

**Cause**: Base de données bloquée ou connexion gelée
**Solution**:

1. **Redémarrer MySQL**:
   ```bash
   # Arrêter tous les processus MySQL
   taskkill /IM mysqld.exe /F
   
   # Relancer
   net start MySQL80
   ```

2. **Vérifier les connexions ouvertes**:
   ```sql
   SHOW PROCESSLIST;
   ```

3. **Augmenter le timeout** (optionnel):
   - Dans DatabaseUtil.java, ligne 9:
   ```java
   private static final String DB_URL = "jdbc:mysql://localhost:3306/pharmacie?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000&socketTimeout=10000";
   ```

---

### ❌ Erreur 9: JavaFX "Cannot find renderingContext"

**Cause**: Problème de rendu graphique DirectX
**Statut**: ✅ CORRIGÉ dans notre config
**Info**: Les JVM args `-Dprism.order=sw -Dprism.d3d=false` forcent le rendu logiciel

---

### ❌ Erreur 10: Port 3306 déjà utilisé

**Cause**: Un autre processus MySQL tourne déjà
**Solution**:

1. **Trouver le processus**:
   ```bash
   netstat -ano | findstr :3306
   ```

2. **Terminer le processus**:
   ```bash
   taskkill /PID [PID] /F
   ```

3. **Ou utiliser un port différent** dans DatabaseUtil.java:
   ```java
   private static final String DB_URL = "jdbc:mysql://localhost:3307/pharmacie?...";
   ```

---

## ✅ Checklist de Démarrage

Avant de lancer l'application, vérifiez:

- [ ] **MySQL Server en cours d'exécution**
  ```bash
  tasklist | find /i "mysql"
  ```

- [ ] **Base 'pharmacie' existe**
  ```sql
  SHOW DATABASES;
  ```

- [ ] **Table 'service' a des données**
  ```sql
  SELECT COUNT(*) FROM service;
  ```

- [ ] **Fichier DatabaseUtil.java a les bons paramètres**
  - Vérifiez URL, utilisateur, mot de passe

- [ ] **Tous les fichiers FXML existent**
  - ReservationForm.fxml ✅
  - FrontServices.fxml ✅
  - Login.fxml ✅
  - Dashboard.fxml ✅

- [ ] **Compilation réussie**
  ```bash
  .\apache-maven-3.9.7\bin\mvn.cmd clean compile
  ```

- [ ] **JAR exécutable créé**
  ```bash
  .\apache-maven-3.9.7\bin\mvn.cmd package -DskipTests
  ```

---

## 🚀 Lancement Rapide

### Option 1: Script Automatique
```bash
LAUNCH_RESERVATION_SYSTEM.bat
```

### Option 2: Manuel
```bash
java -Dprism.order=sw -Dprism.d3d=false `
  --module-path "C:\Users\[VOTRE_USER]\.m2\repository\org\openjfx\*" `
  --add-modules javafx.controls,javafx.fxml `
  -cp target\Projet_java-1.0-SNAPSHOT-executable.jar `
  org.example.CuraVitaApp
```

---

## 📞 Support Avancé

### Activer les Logs Détaillés
Dans ReservationService.java, ligne 51:
```java
System.err.println("Erreur SQL: " + e.getMessage());
e.printStackTrace(); // Ajouter cette ligne
```

### Vérifier la Requête SQL
Dans DatabaseInitializer.java, ligne 38:
```java
System.out.println("✓ Table 'reservation' créée ou déjà existe");
System.out.println("Requête exécutée: " + createReservationTable); // Ajouter
```

### Activer le Mode Debug
Dans pom.xml, ajouter:
```xml
<compilerArgs>
    <arg>-g</arg>
    <arg>-verbose</arg>
</compilerArgs>
```

---

## 📊 Vérifications Post-Lancement

Après avoir réservé un service:

1. **Vérifier en base**:
   ```sql
   SELECT * FROM reservation ORDER BY id DESC LIMIT 1;
   ```

2. **Compter les réservations**:
   ```sql
   SELECT COUNT(*) as 'Total' FROM reservation;
   ```

3. **Vérifier le statut**:
   ```sql
   SELECT DISTINCT statut FROM reservation;
   ```

4. **Voir les réservations par jour**:
   ```sql
   SELECT DATE(date_rendez_vous), COUNT(*) 
   FROM reservation 
   GROUP BY DATE(date_rendez_vous);
   ```

---

**Dernière mise à jour**: 2026-04-16  
**Version**: 1.0 - Guide Complet de Dépannage

