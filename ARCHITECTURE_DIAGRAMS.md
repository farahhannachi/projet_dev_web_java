# 📊 Diagrammes Visuels - Architecture CRUD

## 1. Flux Global MVC

```
┌─────────────────────────────────────────────────────────────────┐
│                         UTILISATEUR (GUI)                        │
│                      ┌─────────────────┐                        │
│                      │  UserManagement │                        │
│                      │     .fxml       │                        │
│                      └────────┬────────┘                        │
│                               │ (Événement: onClick)            │
└───────────────────────────────┼────────────────────────────────┘
                                │
                ┌───────────────▼──────────────┐
                │   UserManagementController   │
                │   .java (Contrôleur)        │
                │ ┌────────────────────────┐  │
                │ │ @FXML methods:         │  │
                │ │ - handleAddClient()    │  │
                │ │ - handleEditClient()   │  │
                │ │ - validateForm()       │  │
                │ │ - loadUsers()          │  │
                │ └────────────────────────┘  │
                └───────────────┬──────────────┘
                                │ (appelle)
                ┌───────────────▼──────────────┐
                │    UserService.java          │
                │    (Service métier)          │
                │ ┌────────────────────────┐  │
                │ │ public methods:        │  │
                │ │ - addUser()            │  │
                │ │ - updateUser()         │  │
                │ │ - deleteUser()         │  │
                │ │ - getAllUsers()        │  │
                │ │ - emailExists()        │  │
                │ └────────────────────────┘  │
                └───────────────┬──────────────┘
                                │ (crée/modifie)
                ┌───────────────▼──────────────┐
                │     User.java (Modèle)       │
                │ ┌────────────────────────┐  │
                │ │ private fields:        │  │
                │ │ - id                   │  │
                │ │ - email                │  │
                │ │ - password             │  │
                │ │ - type                 │  │
                │ │ - blocked              │  │
                │ │ - createdAt            │  │
                │ └────────────────────────┘  │
                └───────────────┬──────────────┘
                                │ (utilise)
                ┌───────────────▼──────────────┐
                │  DatabaseUtil.java           │
                │ ┌────────────────────────┐  │
                │ │ getConnection()        │  │
                │ │ isDatabaseAvailable()  │  │
                │ └────────────────────────┘  │
                └───────────────┬──────────────┘
                                │ (SQL query)
                ┌───────────────▼──────────────┐
                │    Base de Données MySQL     │
                │    Table: utilisateur        │
                │ ┌────────────────────────┐  │
                │ │ id_utilisateur (PK)    │  │
                │ │ email (UNIQUE)         │  │
                │ │ mot_de_passe (hash)    │  │
                │ │ etat_compte            │  │
                │ │ roles (JSON)           │  │
                │ │ ...                    │  │
                │ └────────────────────────┘  │
                └────────────────────────────┘
```

---

## 2. Cycle de Vie: Ajouter un Utilisateur

```
┌───────────────────────────────────────────────────────────────┐
│                    ÉTAPE 1: INTERFACE                          │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─ UserManagement.fxml ─────────────────────────────────┐  │
│  │                                                        │  │
│  │  Modal d'ajout:                                       │  │
│  │  ┌──────────────────────────┐                        │  │
│  │  │ 👤 Ajouter Client        │                        │  │
│  │  ├──────────────────────────┤                        │  │
│  │  │ Nom: [_______________]   │                        │  │
│  │  │ Email: [_____________]   │                        │  │
│  │  │ Password: [____________] │                        │  │
│  │  │ Rôle: [Client ▼]         │                        │  │
│  │  │                          │                        │  │
│  │  │ [✅ Ajouter] [❌ Annuler]│                        │  │
│  │  └──────────────────────────┘                        │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │ (User clicks "Ajouter")        │
└──────────────────────────┼─────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────┐
│             ÉTAPE 2: CONTRÔLEUR (Validation 1)             │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  handleAddClientConfirm() {                              │
│      name = "Jean Dupont"                                │
│      email = "jean@gmail.com"                            │
│      password = "password123"                            │
│      role = "client"                                     │
│                                                           │
│      // VALIDATION 1                                     │
│      errors = validateForm(name, email, password)       │
│      if (!errors.isEmpty()) {                            │
│          showError("❌ Nom obligatoire...")             │
│          return;  // ❌ STOP                             │
│      }                                                    │
│                                                           │
│      // Si OK, appel service                             │
│      if (userService.addUser(...)) {  ✅ CONTINUE       │
│          loadUsers();                                    │
│          showSuccess("✅ Client ajouté!");               │
│      }                                                    │
│  }                                                        │
│                                                           │
│  validateForm() retourne:                                │
│  - "" (vide) = valide ✅                                 │
│  - "❌ Email invalide" = invalide ❌                     │
└──────────────────────────┬────────────────────────────────┘
                           │ (if OK, call)
┌──────────────────────────▼────────────────────────────────┐
│          ÉTAPE 3: SERVICE (Validation 2 + Logique)        │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  userService.addUser("Jean Dupont",                       │
│                      "jean@gmail.com",                   │
│                      "password123",                      │
│                      "client")                           │
│                                                           │
│  ┌─ VALIDATION 2 ─────────────────────────────────────┐ │
│  │                                                    │ │
│  │  if (emailExists("jean@gmail.com")) {             │ │
│  │      return false;  // ❌ Email existe déjà       │ │
│  │  }                                                 │ │
│  │                                                    │ │
│  │  if (!isValidEmail("jean@gmail.com")) {           │ │
│  │      return false;  // ❌ Format invalide         │ │
│  │  }                                                 │ │
│  │                                                    │ │
│  └────────────────────────────────────────────────────┘ │
│                    ✅ TOUT OK → CONTINUE                 │
│                                                           │
│  ┌─ SÉCURITÉ (BCrypt) ────────────────────────────────┐ │
│  │                                                    │ │
│  │  hashedPassword =                                  │ │
│  │    BCrypt.hashpw("password123",                   │ │
│  │                   BCrypt.gensalt())               │ │
│  │                                                    │ │
│  │  Result: "$2a$10$YX7o0rHWL2..."                   │ │
│  │                                                    │ │
│  └────────────────────────────────────────────────────┘ │
│                                                           │
│  ┌─ PRÉPARER SQL QUERY ───────────────────────────────┐ │
│  │                                                    │ │
│  │  sql = "INSERT INTO utilisateur                   │ │
│  │         (nom, prenom, email, mot_de_passe, ...)  │ │
│  │         VALUES (?, ?, ?, ?, ...)"                │ │
│  │                                                    │ │
│  │  stmt.setString(1, "Dupont");                     │ │
│  │  stmt.setString(2, "Jean");                       │ │
│  │  stmt.setString(3, "jean@gmail.com");             │ │
│  │  stmt.setString(4, "$2a$10$YX7o0...");            │ │
│  │  ...                                               │ │
│  │                                                    │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────────┬────────────────────────────────┘
                           │ (execute query)
┌──────────────────────────▼────────────────────────────────┐
│      ÉTAPE 4: DATABASE (MySQL + Contraintes)              │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  INSERT INTO utilisateur                                 │
│  (nom, prenom, email, mot_de_passe, etat_compte, ...)   │
│  VALUES                                                  │
│  ('Dupont', 'Jean', 'jean@gmail.com',                    │
│   '$2a$10$YX7o0rHWL2...', 'actif', ...)                 │
│                                                           │
│  ┌─ Vérifier contraintes ─────────────────────────────┐ │
│  │ ✅ email UNIQUE? Oui                              │ │
│  │ ✅ NOT NULL? Tous remplis                         │ │
│  │ ✅ Format mot_de_passe? Hash BCrypt OK            │ │
│  └────────────────────────────────────────────────────┘ │
│                                                           │
│  ✅ INSERT réussi!                                       │
│  Rows affected: 1                                        │
│                                                           │
│  BD maintenant contient:                                 │
│  id: 4                                                   │
│  nom: Dupont                                             │
│  prenom: Jean                                            │
│  email: jean@gmail.com                                   │
│  mot_de_passe: $2a$10$YX7o0rHWL2...                     │
│  etat_compte: actif                                      │
│  ...                                                     │
└──────────────────────────┬────────────────────────────────┘
                           │ (return true)
┌──────────────────────────▼────────────────────────────────┐
│         ÉTAPE 5: RETOUR & AFFICHAGE                       │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  Service retourne: true (succès)                         │
│                                                           │
│  Contrôleur:                                             │
│  ├─ loadUsers() → recharge la liste                     │
│  ├─ loadStatistics() → remet à jour les stats           │
│  ├─ showSuccess("✅ Client ajouté!")                   │
│  └─ closeModal()                                         │
│                                                           │
│  Interface:                                              │
│  ┌──────────────────────────────┐                       │
│  │ ✅ Succès!                   │                       │
│  │ Jean Dupont a été ajouté.    │                       │
│  │ [OK]                         │                       │
│  └──────────────────────────────┘                       │
│                                                           │
│  Tableau mis à jour:                                     │
│  ┌─────────────────────────────────┐                    │
│  │ Jean Dupont │ jean@gmail.com  ... │                    │
│  │ Marie T.    │ marie@gmail.com ... │                    │
│  │ Ahmed B.    │ ahmed@gmail.com ... │                    │
│  └─────────────────────────────────┘                    │
└───────────────────────────────────────────────────────────┘
```

---

## 3. Système de Validation Complète

```
                    ┌──────────────────────┐
                    │  DONNÉES UTILISATEUR  │
                    │ name = "Jean Dupont"  │
                    │ email = "jean@..."    │
                    │ password = "pass123"  │
                    └──────────────┬───────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ VALIDATION NIVEAU 1         │
                    │ Interface (Client-side)     │
                    ├─────────────────────────────┤
                    │ • minLength (password)      │
                    │ • promptText                │
                    │ • ComboBox validators       │
                    │ • Basic checks              │
                    │                             │
                    │ ✅ PASS → Continue         │
                    │ ❌ FAIL → Show error       │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ VALIDATION NIVEAU 2         │
                    │ Contrôleur (validateForm)   │
                    ├─────────────────────────────┤
                    │ if (name.isEmpty()) ❌      │
                    │ if (!isValidEmail()) ❌     │
                    │ if (password.length < 6) ❌ │
                    │                             │
                    │ ✅ PASS → Call service      │
                    │ ❌ FAIL → showError()       │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ VALIDATION NIVEAU 3         │
                    │ Service (logique métier)    │
                    ├─────────────────────────────┤
                    │ • emailExists() check       │
                    │ • isValidEmail() regex      │
                    │ • Format validation         │
                    │ • Business rules            │
                    │                             │
                    │ ✅ PASS → Hash password     │
                    │ ❌ FAIL → return false      │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ TRANSFORMATION              │
                    │ BCrypt hashing              │
                    ├─────────────────────────────┤
                    │ "password123"               │
                    │ → BCrypt.hashpw()           │
                    │ → "$2a$10$YX7o0rHW..."      │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ VALIDATION NIVEAU 4         │
                    │ Base de Données             │
                    ├─────────────────────────────┤
                    │ • UNIQUE constraint         │
                    │ • NOT NULL checks           │
                    │ • Foreign keys              │
                    │ • Data types                │
                    │                             │
                    │ ✅ PASS → INSERT           │
                    │ ❌ FAIL → DB error         │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │ ✅ SUCCÈS!                  │
                    │ Utilisateur créé en BD      │
                    └─────────────────────────────┘
```

---

## 4. Architecture Services vs Models

```
                    ┌─────────────────────────────────┐
                    │      USER (Modèle)              │
                    ├─────────────────────────────────┤
                    │                                 │
                    │  class User {                   │
                    │    private int id;              │
                    │    private String email;        │
                    │    private String password;     │
                    │    private String type;         │
                    │    private String nom;          │
                    │    private Boolean blocked;     │
                    │                                 │
                    │    // Getters/Setters only      │
                    │    public String getEmail() {   │
                    │      return email;              │
                    │    }                            │
                    │  }                              │
                    │                                 │
                    │  ✅ Responsabilité:            │
                    │  • Stocker les données         │
                    │  • Getters/Setters             │
                    │                                 │
                    │  ❌ Ne PAS faire:              │
                    │  • Requêtes BD                 │
                    │  • Validation complète         │
                    │  • Hashing passwords           │
                    └─────────────────────────────────┘


        ┌──────────────────────────────────────────────────┐
        │       USERSERVICE (Service métier)               │
        ├──────────────────────────────────────────────────┤
        │                                                  │
        │  class UserService {                            │
        │    private static User currentUser;             │
        │                                                  │
        │    // CRUD Operations                           │
        │    public List<User> getAllUsers() {            │
        │      String sql = "SELECT * FROM utilisateur";  │
        │      Connection conn = DatabaseUtil.getConnection();
        │      ...                                         │
        │      return users;                              │
        │    }                                             │
        │                                                  │
        │    public boolean addUser(...) {                │
        │      if (emailExists(email)) {                  │
        │        return false;  // Validation             │
        │      }                                           │
        │      String hashed = BCrypt.hashpw(...);        │
        │      stmt.executeUpdate();  // DB query         │
        │      return true;                               │
        │    }                                             │
        │                                                  │
        │    // Authentication                            │
        │    public User login(...) { ... }               │
        │                                                  │
        │    // Utilities                                 │
        │    private boolean emailExists(...) { ... }     │
        │    private boolean isValidEmail(...) { ... }    │
        │  }                                               │
        │                                                  │
        │  ✅ Responsabilité:                             │
        │  • Logique métier CRUD                          │
        │  • Requêtes BD                                  │
        │  • Validation business                          │
        │  • Sécurité (hashing)                           │
        │                                                  │
        │  ❌ Ne PAS faire:                               │
        │  • Afficher l'interface                         │
        │  • Gérer les événements                         │
        └──────────────────────────────────────────────────┘


            ┌────────────────────────────────┐
            │   CLIENT (Modèle)              │
            ├────────────────────────────────┤
            │                                │
            │  class Client {                │
            │    private int id;             │
            │    private String name;        │
            │    private String email;       │
            │    private String address;     │
            │    private LocalDate regDate;  │
            │    private String loyaltyLevel;│
            │                                │
            │    public String getName() {}  │
            │    public void setName() {}    │
            │  }                             │
            │                                │
            │  ✅ Responsabilité:           │
            │  • Infos client commercial    │
            │  • Getters/Setters            │
            └────────────────────────────────┘


        ┌────────────────────────────────────┐
        │  CLIENTSERVICE (Service métier)    │
        ├────────────────────────────────────┤
        │                                    │
        │  class ClientService {             │
        │    public List<Client> getAll() {} │
        │    public void add(Client c) {}    │
        │    public void update(...) {}      │
        │    public void delete(int id) {}   │
        │    public int getTotalClients() {} │
        │  }                                 │
        │                                    │
        │  ✅ Responsabilité:                │
        │  • CRUD clients                   │
        │  • Requêtes BD table client       │
        │  • Métriques commerciales        │
        └────────────────────────────────────┘
```

---

## 5. Flow DatabaseUtil → MySQL

```
┌─────────────────────────────────────────────────────────┐
│           DATABASEUTIL (Connexion)                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  public class DatabaseUtil {                           │
│    private static final String URL =                   │
│      "jdbc:mysql://localhost:3306/pharmacie";          │
│    private static final String USER = "root";          │
│    private static final String PASSWORD = "";          │
│                                                         │
│    public static Connection getConnection() {          │
│      try {                                              │
│        return DriverManager.getConnection(             │
│          URL, USER, PASSWORD                           │
│        );  // ← Retourne une connexion                 │
│      } catch (SQLException e) {                        │
│        e.printStackTrace();                            │
│      }                                                  │
│      return null;                                      │
│    }                                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
                        │
                        │ (Connection)
                        ▼
┌─────────────────────────────────────────────────────────┐
│           SERVICE (Utilise la connexion)               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  public List<User> getAllUsers() {                    │
│    String sql = "SELECT * FROM utilisateur";          │
│                                                         │
│    try (Connection conn = DatabaseUtil.getConnection();
│         Statement stmt = conn.createStatement();       │
│         ResultSet rs = stmt.executeQuery(sql)) {      │
│                                                         │
│      while (rs.next()) {                              │
│        User user = new User(                          │
│          rs.getInt("id_utilisateur"),                 │
│          rs.getString("email"),                       │
│          rs.getString("mot_de_passe"),                │
│          extractRole(rs.getString("roles")),          │
│          rs.getString("prenom") + " " +               │
│          rs.getString("nom")                          │
│        );                                              │
│        users.add(user);                               │
│      }                                                 │
│    }                                                   │
│    return users;                                       │
│  }                                                     │
└─────────────────────────────────────────────────────────┘
                        │
                        │ (SQL Query)
                        ▼
┌─────────────────────────────────────────────────────────┐
│            MySQL SERVER (Port 3306)                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Database: pharmacie                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Table: utilisateur                              │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ id | nom  | prenom | email        | mot_de_passe
│  ├────┼─────┼────────┼──────────────┼────────────┤   │
│  │ 1  │ Iheb│ Najjar │ iheb@...     │ $2a$10$... │   │
│  │ 2  │ Jean│ Dupont │ jean@...     │ $2a$10$... │   │
│  │ 3  │ Marie│ Lenoir│ marie@...    │ $2a$10$... │   │
│  │ 4  │ Ahmed│ Ben   │ ahmed@...    │ $2a$10$... │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Executing: SELECT * FROM utilisateur;                 │
│  ✅ Query successful                                   │
│  Rows returned: 4                                      │
└─────────────────────────────────────────────────────────┘
                        │
                        │ (ResultSet)
                        ▼
┌─────────────────────────────────────────────────────────┐
│      SERVICE (Traite ResultSet)                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  List<User> users = [                                  │
│    User(1, "iheb@...", "hash1", "admin", "Iheb Najjar"),
│    User(2, "jean@...", "hash2", "client", "Jean Dupont"),
│    User(3, "marie@...", "hash3", "client", "Marie Lenoir"),
│    User(4, "ahmed@...", "hash4", "client", "Ahmed Ben")
│  ]                                                     │
└─────────────────────────────────────────────────────────┘
                        │
                        │ (return List<User>)
                        ▼
┌─────────────────────────────────────────────────────────┐
│      CONTROLLER (Affiche les données)                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  List<User> users = userService.getAllUsers();         │
│  userTable.setItems(FXCollections.observableArrayList( 
│    users));                                             │
│                                                         │
│  ┌───────────────────────────────────┐                │
│  │ TableView Affichage:              │                │
│  ├───────────────────────────────────┤                │
│  │ Iheb Najjar  │ iheb@...  │ admin  │                │
│  │ Jean Dupont  │ jean@...  │ client │                │
│  │ Marie Lenoir │ marie@... │ client │                │
│  │ Ahmed Ben    │ ahmed@... │ client │                │
│  └───────────────────────────────────┘                │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Contrôle de Saisie Total (Validation en 4 niveaux)

```
NIVEAU 1: INTERFACE (Client-side hints)
┌──────────────────────────────────────┐
│ <TextField promptText="user@gmail.com"/>
│ <PasswordField minLength="6"/>
│                                      │
│ User types: "john"                   │
│ → promptText guides typing           │
│                                      │
│ Feedback: Immédiate visuellement    │
└──────────────────────────────────────┘
              ↓ (Submit)

NIVEAU 2: CONTROLLER (validateForm)
┌──────────────────────────────────────┐
│ handleAddClientConfirm() {            │
│   String name = field.getText();      │
│   String email = field.getText();     │
│                                      │
│   // Check 1: Not empty              │
│   if (name.isEmpty()) {              │
│     error = "❌ Nom obligatoire";    │
│   }                                   │
│                                      │
│   // Check 2: Email format (regex)   │
│   if (!email.matches("^[a-zA-Z]..")) │
│     error = "❌ Email invalide";     │
│   }                                   │
│                                      │
│   // Check 3: Password length        │
│   if (password.length() < 6) {       │
│     error = "❌ Min 6 caractères";   │
│   }                                   │
│                                      │
│   if (!error.isEmpty()) {            │
│     showError(error);  // ❌ STOP    │
│     return;                           │
│   }                                   │
│   // ✅ CONTINUE to service         │
│   userService.addUser(...);           │
│ }                                     │
└──────────────────────────────────────┘
              ↓ (if pass)

NIVEAU 3: SERVICE (Business validation)
┌──────────────────────────────────────┐
│ addUser(name, email, password, role) │
│                                      │
│ // Check 4: Email exists?            │
│ if (emailExists(email)) {            │
│   return false;  // ❌ DUPLICATE    │
│ }                                     │
│                                      │
│ // Check 5: Regex again (double)    │
│ if (!isValidEmail(email)) {          │
│   return false;  // ❌ INVALID      │
│ }                                     │
│                                      │
│ // Check 6: Security (hash)         │
│ String hash = BCrypt.hashpw(        │
│   password, BCrypt.gensalt());      │
│ // ✅ NOW SAFE TO INSERT            │
│                                      │
│ // Check 7: Prepare statement       │
│ stmt.setString(1, name);            │
│ stmt.setString(3, email);           │
│ stmt.setString(4, hash);            │
│ return stmt.executeUpdate() > 0;     │
└──────────────────────────────────────┘
              ↓ (if pass)

NIVEAU 4: DATABASE (Constraints)
┌──────────────────────────────────────┐
│ MySQL Constraints:                   │
│                                      │
│ CREATE TABLE utilisateur (           │
│   id_utilisateur INT PRIMARY KEY,    │
│   email VARCHAR(100) UNIQUE NOT NULL,│
│   mot_de_passe VARCHAR(255) NOT NULL,│
│   nom VARCHAR(100) NOT NULL,         │
│   ...                                │
│ );                                   │
│                                      │
│ Check 8: UNIQUE (email unique?)     │
│ → Constraint violation! ❌           │
│ → "Duplicate entry..."               │
│                                      │
│ Check 9: NOT NULL (all fields?)     │
│ → OK ✅                              │
│                                      │
│ Check 10: Data types (correct?)     │
│ → OK ✅                              │
│                                      │
│ ✅ INSERT SUCCESSFUL                │
│ Rows affected: 1                    │
└──────────────────────────────────────┘
```

---

## 7. Dépendances (pom.xml)

```
pom.xml (Maven Configuration)
│
├─ JavaFX 21.0.2
│  │
│  └─ Fournit:
│     ├─ javafx.controls (Button, TextField, etc.)
│     ├─ javafx.fxml (Chargement .fxml)
│     ├─ javafx.scene (Scenes, Stages)
│     └─ Animations, CSS support
│
├─ MySQL Connector 8.0.33
│  │
│  └─ Fournit:
│     ├─ java.sql.Connection
│     ├─ java.sql.Statement
│     ├─ java.sql.PreparedStatement
│     ├─ java.sql.ResultSet
│     └─ JDBC drivers pour MySQL
│
├─ BCrypt 0.4
│  │
│  └─ Fournit:
│     ├─ BCrypt.hashpw() (hash passwords)
│     ├─ BCrypt.gensalt() (generate salt)
│     ├─ BCrypt.checkpw() (verify passwords)
│     └─ Sécurité des mots de passe
│
└─ Apache Spark 3.5.0 (optionnel)
   │
   └─ Fournit:
      ├─ Distributed computing
      ├─ Data processing
      └─ Analytics (pas utilisé actuellement)
```

---

**Status**: ✅ Documentation Complète  
**Visibilité**: 📊 100% des concepts couverts  
**Compréhension**: 🎯 Facile à suivre avec diagrammes

