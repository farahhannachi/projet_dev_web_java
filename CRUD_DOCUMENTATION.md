# 📚 Documentation Complète CRUD - Architecture et Implémentation

## Table des Matières
1. [Architecture MVC](#architecture-mvc)
2. [Fonctionnalités CRUD](#fonctionnalités-crud)
3. [Structure des Fichiers](#structure-des-fichiers)
4. [Services](#services)
5. [Modèles](#modèles)
6. [Contrôleurs](#contrôleurs)
7. [Base de Données](#base-de-données)
8. [Validation des Données](#validation-des-données)
9. [Dépendances](#dépendances)
10. [Flux Complet](#flux-complet)

---

## 🏗️ Architecture MVC

### Vue d'ensemble
```
┌─────────────────────────────────────────────────────────┐
│                    UTILISATEUR (GUI)                     │
│                   (Fichiers .fxml)                       │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  CONTRÔLEUR (Controller)                 │
│        (Ex: UserManagementController.java)              │
│  - Gère les événements (clics, saisies)                │
│  - Valide les données                                   │
│  - Appelle les services                                │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   SERVICE (Service)                      │
│          (Ex: UserService.java)                         │
│  - Logique métier CRUD                                 │
│  - Appels à la base de données                         │
│  - Transformations de données                          │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   MODÈLE (Model)                         │
│            (Ex: User.java, Client.java)                │
│  - Représentation des données                          │
│  - Getters/Setters                                     │
│  - Logique d'objet simple                              │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│               BASE DE DONNÉES (MySQL)                    │
│  - Tables: utilisateur, client, produit, etc.          │
│  - Stockage persistant des données                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Fonctionnalités CRUD

### CRUD = Create, Read, Update, Delete

| Opération | SQL | Méthode | Entrée | Sortie |
|-----------|-----|---------|--------|--------|
| **C**reate | INSERT | `addUser()` | (nom, email, password) | Boolean (succès?) |
| **R**ead | SELECT | `getAllUsers()` | (aucune) | List<User> |
| **U**pdate | UPDATE | `updateUser()` | (id, nom, email, password) | Boolean (succès?) |
| **D**elete | DELETE | `deleteUser()` | (id) | Boolean (succès?) |

### Exemple: CREATE (Ajouter un utilisateur)

```
Utilisateur remplit le formulaire
           ↓
Contrôleur reçoit l'événement (onAction)
           ↓
Contrôleur valide les données
           ↓
Contrôleur appelle userService.addUser()
           ↓
Service hashe le mot de passe (BCrypt)
           ↓
Service exécute INSERT INTO utilisateur
           ↓
Base de données sauvegarde l'utilisateur
           ↓
Service retourne true/false
           ↓
Contrôleur affiche modal de succès/erreur
           ↓
Interface mise à jour
```

---

## 📁 Structure des Fichiers

### Hiérarchie du Projet
```
src/
├── main/
│   ├── java/
│   │   └── org/example/
│   │       ├── CuraVitaApp.java           ← Point d'entrée
│   │       ├── controller/                 ← Contrôleurs (MVC)
│   │       │   ├── LoginController.java
│   │       │   ├── DashboardController.java
│   │       │   ├── UserManagementController.java
│   │       │   └── ProfilController.java
│   │       ├── model/                      ← Modèles (MVC)
│   │       │   ├── User.java
│   │       │   ├── Client.java
│   │       │   ├── Produit.java
│   │       │   └── ...
│   │       ├── service/                    ← Services (Logique métier)
│   │       │   ├── UserService.java
│   │       │   ├── ClientService.java
│   │       │   ├── ProduitService.java
│   │       │   └── ...
│   │       └── util/                       ← Utilitaires
│   │           └── DatabaseUtil.java
│   └── resources/
│       ├── fxml/                           ← Interfaces (Vue FXML)
│       │   ├── Login.fxml
│       │   ├── Dashboard.fxml
│       │   ├── UserManagement.fxml
│       │   └── Profil.fxml
│       ├── css/
│       │   └── styles.css
│       └── images/
└── test/
    └── ...

pom.xml                                      ← Dépendances Maven
```

---

## 🔧 Services

### UserService.java

**Responsabilité:** Gestion des utilisateurs (authentication, CRUD)

#### Méthodes principales:

```java
1. login(String email, String password) -> User
   - Cherche l'utilisateur en BD
   - Vérifie le mot de passe (BCrypt)
   - Retourne l'utilisateur si OK, null sinon
   - Stocke currentUser en mémoire
   - Vérifie si compte est bloqué

2. signup(String email, String password, String name) -> Boolean
   - Crée un nouvel utilisateur
   - Hash le mot de passe
   - Insère dans BD
   - Retourne true si succès

3. getAllUsers() -> List<User>
   - Récupère TOUS les utilisateurs
   - Récupère etat_compte (bloqué/actif)
   - Récupère avatar_config
   - Retourne liste

4. addUser(String name, String email, String password, String role) -> Boolean
   - Admin crée un nouvel utilisateur
   - Valide l'email
   - Hash le password
   - INSERT en BD
   - Retourne true/false

5. updateUser(int id, String name, String email, String password, String role) -> Boolean
   - Admin modifie un utilisateur
   - Vérifie que email n'existe pas ailleurs
   - Hash le nouveau password
   - UPDATE en BD

6. updateUserBlocked(int id, boolean blocked) -> Boolean
   - Change etat_compte = 'bloque' ou 'actif'
   - UPDATE en BD

7. deleteUser(int id) -> Boolean
   - Supprime un utilisateur
   - DELETE de la BD
```

#### Connexion à la BD:
```java
// Exemple: login()
String sql = "SELECT * FROM utilisateur WHERE email = ?";
Connection conn = DatabaseUtil.getConnection();
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, email);
ResultSet rs = stmt.executeQuery();
```

### ClientService.java

**Responsabilité:** Gestion des clients (profils, détails)

#### Différence avec UserService:

| UserService | ClientService |
|-------------|---------------|
| Authentification | Détails du client |
| Comptes admin/client | Informations commerciales |
| Gestion des accès | Loyauté, historique |
| Blocage/déblocage | Préférences |
| 1 utilisateur = 1 compte | 1 client = 1 profil commercial |

#### Exemple de modèle Client:
```java
class Client {
    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDate registrationDate;
    private String loyaltyLevel;
    // Getters/Setters
}
```

---

## 📦 Modèles (Model)

### User.java

```java
public class User {
    private int id;                      // ID unique
    private String email;                // Email (unique)
    private String password;             // Hash BCrypt
    private String type;                 // "admin" ou "client"
    private String nom;                  // Nom complet
    private String avatarConfig;         // JSON avatar
    private Boolean blocked;             // Bloqué? true/false
    private LocalDateTime createdAt;     // Date création
    
    // Getters/Setters
    public String getNom() { return nom; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public Boolean isBlocked() { return blocked != null ? blocked : false; }
}
```

### Responsabilité du Modèle:
```
✅ Stocker les données
✅ Fournir getters/setters
✅ Représenter une entité
❌ Pas de logique métier complexe
❌ Pas de requêtes BD
```

---

## 🎮 Contrôleurs (Controller)

### UserManagementController.java

**Responsabilité:** Gérer l'interface de gestion des utilisateurs

#### Flux d'une action (Ajouter client):

```
1. Utilisateur clique bouton "Ajouter"
   ↓
2. @FXML private void handleAddClient()
   - Ouvre modal d'ajout
   
3. Utilisateur remplit le formulaire et clique OK
   ↓
4. @FXML private void handleAddClientConfirm()
   - Récupère les données du formulaire
   - Valide les données
   - Appelle userService.addUser()
   - Rafraîchit la liste
   - Affiche modal de succès
```

#### Exemple de code:
```java
@FXML
private void handleAddClientConfirm() {
    String name = addModalNameField.getText().trim();
    String email = addModalEmailField.getText().trim();
    String password = addModalPasswordField.getText();
    String role = addModalRoleCombo.getValue();
    
    // Validation
    String errors = validateForm(name, email, password);
    if (!errors.isEmpty()) {
        addModalErrorLabel.setText(errors);
        return;
    }
    
    // Appel au service
    if (userService.addUser(name, email, password, role)) {
        loadUsers();          // Rafraîchir la liste
        loadStatistics();     // Rafraîchir les stats
        showSuccess("✅ Client ajouté!");
        closeModal();
    } else {
        addModalErrorLabel.setText("❌ Email déjà utilisé");
    }
}
```

### Responsabilités du Contrôleur:
```
✅ Lier l'interface aux données
✅ Gérer les événements utilisateur
✅ Valider les entrées
✅ Appeler les services
✅ Rafraîchir l'interface
❌ Pas de requêtes BD directes
❌ Pas de logique métier complexe
```

---

## 🗄️ Base de Données

### Structure de la table `utilisateur`:

```sql
CREATE TABLE utilisateur (
    id_utilisateur INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,      -- Hash BCrypt
    etat_compte VARCHAR(20),                  -- 'actif' ou 'bloque'
    roles JSON,                               -- ["ROLE_ADMIN"] ou ["ROLE_CLIENT"]
    date_creation TIMESTAMP,
    avatar_config JSON,
    loyalty_points INT DEFAULT 0,
    loyalty_level VARCHAR(50),
    segment VARCHAR(50)
);
```

### Connexion à la BD:

**DatabaseUtil.java:**
```java
public class DatabaseUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/pharmacie";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public static boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
```

### Utilisation dans le service:
```java
public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM utilisateur";
    
    try (Connection conn = DatabaseUtil.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            User user = new User(
                rs.getInt("id_utilisateur"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                extractRoleFromJson(rs.getString("roles")),
                rs.getString("prenom") + " " + rs.getString("nom")
            );
            
            // Check blocked status
            String etatCompte = rs.getString("etat_compte");
            if ("bloque".equalsIgnoreCase(etatCompte)) {
                user.setBlocked(true);
            }
            
            users.add(user);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return users;
}
```

---

## ✅ Validation des Données (Contrôle de Saisie)

### 1. Validation côté Contrôleur

```java
// Dans UserManagementController.java

private String validateForm(String name, String email, String password) {
    StringBuilder errors = new StringBuilder();
    
    // Validation du nom
    if (name == null || name.isEmpty()) {
        errors.append("❌ Nom obligatoire\n");
    }
    
    // Validation de l'email
    if (email == null || email.isEmpty()) {
        errors.append("❌ Email obligatoire\n");
    } else if (!isValidEmail(email)) {
        errors.append("❌ Email invalide\n");
    }
    
    // Validation du password
    if (password == null || password.length() < 6) {
        errors.append("❌ Min 6 caractères\n");
    }
    
    return errors.toString();
}

private boolean isValidEmail(String email) {
    String pattern = "^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    return email.matches(pattern);
}
```

### 2. Validations supplémentaires en Service

```java
// Dans UserService.java

public boolean addUser(String name, String email, String password, String role) {
    // Check if email already exists
    if (emailExists(email)) {
        return false;  // Email existe déjà
    }
    
    // Validate email format
    if (!isValidEmail(email)) {
        return false;  // Format invalide
    }
    
    // Hash the password (sécurité)
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    
    // Insert into database
    // ...
}

private boolean emailExists(String email) {
    String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            return rs.getInt(1) > 0;  // Retourne true si existe
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return false;
}
```

### 3. Validation côté Interface FXML

```xml
<!-- Dans UserManagement.fxml -->
<TextField fx:id="addModalEmailField" 
           promptText="jean@gmail.com" 
           style="-fx-padding: 10; -fx-border-color: #d1d5db;"/>

<!-- Label d'erreur -->
<Label fx:id="addModalErrorLabel" 
       text="" 
       style="-fx-text-fill: #dc2626; -fx-font-size: 11;"/>
```

### Hiérarchie de Validation:
```
                    Interface (FXML)
                         ↓
                  Contrôleur (Validation 1)
                         ↓
    validateForm() + isValidEmail() (Validation 2)
                         ↓
                    Service (Validation 3)
                         ↓
              emailExists() + isValidEmail()
                         ↓
              Base de Données (Contraintes)
                         ↓
            UNIQUE constraint, NOT NULL, etc.
```

---

## 📦 Dépendances (pom.xml)

### Structure du pom.xml:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <!-- Propriétés du projet -->
    <groupId>org.example</groupId>
    <artifactId>CuraVita</artifactId>
    <version>1.0</version>
    <name>CuraVita Pharmacy Management</name>
    
    <!-- Dépendances -->
    <dependencies>
        <!-- JavaFX (Interface graphique) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>21.0.2</version>
        </dependency>
        
        <!-- MySQL Connector (BD) -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        
        <!-- BCrypt (Sécurité password) -->
        <dependency>
            <groupId>org.mindrot</groupId>
            <artifactId>jbcrypt</artifactId>
            <version>0.4</version>
        </dependency>
        
        <!-- Spark (Big Data - optionnel) -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.13</artifactId>
            <version>3.5.0</version>
        </dependency>
    </dependencies>
    
    <!-- Build Configuration -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Dépendances expliquées:

| Dépendance | Version | Utilité | Pourquoi? |
|------------|---------|---------|-----------|
| **JavaFX** | 21.0.2 | Interface graphique | Créer l'GUI |
| **MySQL Connector** | 8.0.33 | Connexion à MySQL | Accès à la BD |
| **BCrypt** | 0.4 | Hash des mots de passe | Sécurité |
| **Spark** | 3.5.0 | Big Data processing | Analytics (optionnel) |

---

## 🔍 Différences: UserService vs ClientService

### UserService (Authentification & Gestion des comptes)

```java
public class UserService {
    // AUTHENTIFICATION
    public User login(String email, String password)
    public boolean signup(String email, String password, String name)
    
    // GESTION DES COMPTES
    public List<User> getAllUsers()
    public boolean addUser(String name, String email, String password, String role)
    public boolean updateUser(int id, String name, String email, String password, String role)
    public boolean deleteUser(int id)
    
    // SÉCURITÉ
    public boolean updateUserBlocked(int id, boolean blocked)
    public boolean isAdmin()
    public boolean isClient()
}
```

**Responsabilités:**
- ✅ Authentication (login/signup)
- ✅ Gestion des accès (admin/client)
- ✅ Blocage de comptes
- ✅ Contrôle d'accès aux fonctionnalités

---

### ClientService (Profils & Détails commerciaux)

```java
public class ClientService {
    // CRUD DES CLIENTS
    public List<Client> getAll()
    public Client getById(int id)
    public void add(Client client)
    public void update(Client client)
    public void delete(int id)
    
    // MÉTRIQUE COMMERCIALE
    public int getTotalClients()
    public List<Client> getActiveClients()
    public List<Client> getByLoyaltyLevel(String level)
}
```

**Responsabilités:**
- ✅ Profils clients (détails personnels)
- ✅ Historique d'achat
- ✅ Points de loyauté
- ✅ Préférences commerciales

---

### Relation:

```
┌──────────────────────────┐
│  utilisateur (BD)        │
│  id_utilisateur: 1       │
│  email: jean@gmail.com   │
│  type: "client"          │
│  etat_compte: "actif"    │
└──────────────────────────┘
         ↓ (1-to-1)
┌──────────────────────────┐
│  client (BD)             │
│  id_client: 1            │
│  id_user: 1 (FK)         │
│  loyalty_points: 150     │
│  last_purchase: ...      │
└──────────────────────────┘
```

---

## 🔄 Flux Complet: Exemple (AJOUTER UN UTILISATEUR)

### Étape 1: Interface (fxml)
```xml
<!-- UserManagement.fxml -->
<Button text="👤 Ajouter" onAction="#handleAddClient"/>
```

### Étape 2: Événement (Controller)
```java
// UserManagementController.java
@FXML
private void handleAddClient() {
    // Ouvrir modal
    showModal(addClientModal);
}

@FXML
private void handleAddClientConfirm() {
    // Récupérer données
    String name = addModalNameField.getText().trim();
    String email = addModalEmailField.getText().trim();
    String password = addModalPasswordField.getText();
    String role = addModalRoleCombo.getValue();
    
    // VALIDATION 1: Contrôleur
    String errors = validateForm(name, email, password);
    if (!errors.isEmpty()) {
        addModalErrorLabel.setText(errors);
        return;
    }
    
    // Appel au service
    if (userService.addUser(name, email, password, role)) {
        loadUsers();
        loadStatistics();
        showSuccess("✅ Client ajouté!");
        closeModal();
    } else {
        addModalErrorLabel.setText("❌ Erreur");
    }
}

private String validateForm(String name, String email, String password) {
    StringBuilder errors = new StringBuilder();
    
    if (name == null || name.isEmpty()) {
        errors.append("❌ Nom obligatoire\n");
    }
    
    if (email == null || email.isEmpty()) {
        errors.append("❌ Email obligatoire\n");
    } else if (!isValidEmail(email)) {
        errors.append("❌ Email invalide\n");
    }
    
    if (password == null || password.length() < 6) {
        errors.append("❌ Min 6 caractères\n");
    }
    
    return errors.toString();
}

private boolean isValidEmail(String email) {
    return email.matches("^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
}
```

### Étape 3: Service (Business Logic)
```java
// UserService.java
public boolean addUser(String name, String email, String password, String role) {
    // VALIDATION 2: Service
    if (emailExists(email)) {
        return false;  // Email existe déjà
    }
    
    if (!isValidEmail(email)) {
        return false;  // Email invalide
    }
    
    // SÉCURITÉ: Hash le password
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    
    // Déterminer les rôles
    String roles = role.equals("admin") ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_CLIENT\"]";
    
    // INSERT dans la BD
    String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, etat_compte, date_creation, roles, loyalty_points, loyalty_level, segment) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        // Split nom/prenom
        String[] nameParts = name.split(" ", 2);
        String prenom = nameParts[0];
        String nom = nameParts.length > 1 ? nameParts[1] : "";
        
        // Binder les paramètres
        stmt.setString(1, nom);
        stmt.setString(2, prenom);
        stmt.setString(3, email);
        stmt.setString(4, hashedPassword);
        stmt.setString(5, "actif");
        stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
        stmt.setString(7, roles);
        stmt.setInt(8, 0);
        stmt.setString(9, "BRONZE");
        stmt.setString(10, "NEW_CUSTOMER");
        
        // Exécuter
        return stmt.executeUpdate() > 0;
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return false;
}

private boolean emailExists(String email) {
    String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}

private boolean isValidEmail(String email) {
    return email.matches("^[a-zA-Z0-9]+@gmail\\.com$");
}
```

### Étape 4: Modèle (Représentation)
```java
// User.java
public class User {
    private int id;
    private String email;
    private String password;
    private String type;
    private String nom;
    private String avatarConfig;
    private Boolean blocked;
    private LocalDateTime createdAt;
    
    // Getters/Setters
    public String getEmail() { return email; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
}
```

### Étape 5: Base de Données
```sql
-- Table utilisateur
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, etat_compte, date_creation, roles)
VALUES ('Dupont', 'Jean', 'jean@gmail.com', '$2a$10$hash...', 'actif', NOW(), '["ROLE_CLIENT"]');

-- Résultat: 1 row inserted ✅
```

### Résumé du Flux:
```
Interface (FXML)
    ↓ (Événement souris)
Contrôleur (handleAddClientConfirm)
    ↓ (Validation 1)
Validation Contrôleur (validateForm)
    ↓ (Appel)
Service (addUser)
    ↓ (Validation 2)
Validation Service (emailExists, isValidEmail)
    ↓ (Transformation)
Sécurité (BCrypt.hashpw)
    ↓ (Query)
DatabaseUtil.getConnection()
    ↓ (Execution)
PreparedStatement.executeUpdate()
    ↓ (Insert)
Base de Données (MySQL INSERT)
    ↓ (Retour)
Service retourne true/false
    ↓ (Callback)
Contrôleur reçoit boolean
    ↓ (Affichage)
Modal de succès/erreur
    ↓ (Rafraîchir)
loadUsers() + loadStatistics()
    ↓ (Mise à jour)
Interface affiche la liste mise à jour
```

---

## 📊 Résumé Architecture CRUD

### Pour CRÉER (Create):
1. **Interface** → Formulaire + Bouton
2. **Contrôleur** → handleAddClientConfirm() + validation
3. **Service** → addUser() + vérifications
4. **Modèle** → User object
5. **BD** → INSERT query

### Pour LIRE (Read):
1. **Contrôleur** → loadUsers() au démarrage
2. **Service** → getAllUsers()
3. **Modèle** → List<User>
4. **BD** → SELECT * FROM utilisateur
5. **Interface** → Afficher dans TableView

### Pour METTRE À JOUR (Update):
1. **Interface** → Modal d'édition
2. **Contrôleur** → handleEditClientConfirm()
3. **Service** → updateUser()
4. **Modèle** → User modified
5. **BD** → UPDATE utilisateur SET ...

### Pour SUPPRIMER (Delete):
1. **Interface** → Modal de confirmation
2. **Contrôleur** → handleDeleteConfirm()
3. **Service** → deleteUser()
4. **Modèle** → N/A
5. **BD** → DELETE FROM utilisateur

---

## 🔐 Sécurité (Validation Totale)

### Niveaux de Validation:

```
Niveau 1: Interface (Client-side)
├─ Vérification basique
├─ Prompttext + minLength
└─ Feedback immédiat

Niveau 2: Contrôleur
├─ Validation complète (validateForm)
├─ Regex email
├─ Longueur password min 6
└─ Erreurs affichées

Niveau 3: Service
├─ Vérification emailExists
├─ Validation email service
├─ BCrypt password hashing
└─ Double check

Niveau 4: Base de Données
├─ UNIQUE constraint (email)
├─ NOT NULL constraints
├─ Foreign keys
└─ Index sur email
```

### Sécurité du Password:
```
Utilisateur tape: "password123"
                    ↓
Contrôleur: min 6 chars ✅
                    ↓
Service: BCrypt.hashpw()
                    ↓
Hash: "$2a$10$YX7o0rHWL2kKL8X0..."
                    ↓
Sauvegardé en BD (JAMAIS plaintext)
                    ↓
À la connexion: BCrypt.checkpw(password, hash)
```

---

## 🎯 Conclusion

Le système CRUD fonctionne selon une architecture MVC stricte:

- **Modèle** = Représentation des données
- **Vue (FXML)** = Interface utilisateur
- **Contrôleur** = Logique de l'interface
- **Service** = Logique métier & BD
- **Utilitaire** = Connexion BD
- **pom.xml** = Dépendances Maven

Chaque niveau a une responsabilité clairement définie, ce qui rend le code maintenable, testable et scalable.


