# 💻 Exemples de Code CRUD - Implémentation Pratique

## Table des Matières
1. [CREATE (Ajouter)](#create-ajouter)
2. [READ (Lire)](#read-lire)
3. [UPDATE (Modifier)](#update-modifier)
4. [DELETE (Supprimer)](#delete-supprimer)
5. [Validation Complète](#validation-complète)

---

## CREATE (Ajouter)

### 1️⃣ Contrôleur - Événement

```java
// UserManagementController.java

@FXML private TextField addModalNameField;
@FXML private TextField addModalEmailField;
@FXML private PasswordField addModalPasswordField;
@FXML private ComboBox<String> addModalRoleCombo;
@FXML private Label addModalErrorLabel;

@FXML
private void handleAddClient() {
    // Réinitialiser le formulaire
    addModalErrorLabel.setText("");
    addModalNameField.clear();
    addModalEmailField.clear();
    addModalPasswordField.clear();
    addModalRoleCombo.setValue("client");
    
    // Afficher la modal
    showModal(addClientModal);
}

@FXML
private void handleAddClientConfirm() {
    // 1. Récupérer les données du formulaire
    String name = addModalNameField.getText().trim();
    String email = addModalEmailField.getText().trim();
    String password = addModalPasswordField.getText();
    String role = addModalRoleCombo.getValue();
    
    // 2. VALIDATION (Niveau 1)
    String errors = validateForm(name, email, password);
    if (!errors.isEmpty()) {
        addModalErrorLabel.setText(errors);
        return;  // ❌ STOP si erreurs
    }
    
    // 3. Appel au service
    if (userService.addUser(name, email, password, role)) {
        // ✅ Succès
        loadUsers();           // Rafraîchir la liste
        loadStatistics();      // Rafraîchir les stats
        showSuccess("✅ Client ajouté avec succès!");
        closeModal();
    } else {
        // ❌ Erreur BD
        addModalErrorLabel.setText("❌ Email déjà utilisé ou erreur BD");
    }
}
```

### 2️⃣ Service - Logique Métier

```java
// UserService.java

public boolean addUser(String name, String email, String password, String role) {
    // VALIDATION (Niveau 2)
    // Check 1: Email existe déjà?
    if (emailExists(email)) {
        System.out.println("[DEBUG] Email déjà utilisé: " + email);
        return false;  // ❌ DUPLICATE EMAIL
    }
    
    // Check 2: Format email valide?
    if (!isValidEmail(email)) {
        System.out.println("[DEBUG] Format email invalide: " + email);
        return false;  // ❌ INVALID FORMAT
    }
    
    // SÉCURITÉ: Hash le password
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    System.out.println("[DEBUG] Password hashé: " + hashedPassword.substring(0, 20) + "...");
    
    // Déterminer les rôles (JSON)
    String roles = role.equals("admin") ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_CLIENT\"]";
    
    // SQL INSERT
    String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, etat_compte, date_creation, roles, loyalty_points, loyalty_level, segment) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        // Split nom/prenom
        String[] nameParts = name.split(" ", 2);
        String prenom = nameParts[0];
        String nom = nameParts.length > 1 ? nameParts[1] : "";
        
        // Binder les paramètres
        stmt.setString(1, nom);                      // nom (last name)
        stmt.setString(2, prenom);                   // prenom (first name)
        stmt.setString(3, email);                    // email
        stmt.setString(4, hashedPassword);           // mot_de_passe (HASHED)
        stmt.setString(5, "actif");                  // etat_compte
        stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));  // date_creation
        stmt.setString(7, roles);                    // roles (JSON)
        stmt.setInt(8, 0);                           // loyalty_points
        stmt.setString(9, "BRONZE");                 // loyalty_level
        stmt.setString(10, "NEW_CUSTOMER");          // segment
        
        // Exécuter la requête
        int rowsAffected = stmt.executeUpdate();
        
        if (rowsAffected > 0) {
            System.out.println("[DEBUG] User ajouté: " + email);
            return true;  // ✅ SUCCESS
        }
        
    } catch (SQLException e) {
        System.out.println("[ERROR] SQL Exception: " + e.getMessage());
        e.printStackTrace();
    }
    
    return false;  // ❌ FAIL
}
```

### 3️⃣ Validation Helper Methods

```java
private String validateForm(String name, String email, String password) {
    StringBuilder errors = new StringBuilder();
    
    // Validation 1: Nom
    if (name == null || name.isEmpty()) {
        errors.append("❌ Nom obligatoire\n");
    }
    
    // Validation 2: Email
    if (email == null || email.isEmpty()) {
        errors.append("❌ Email obligatoire\n");
    } else if (!isValidEmail(email)) {
        errors.append("❌ Email invalide (format: name@gmail.com)\n");
    }
    
    // Validation 3: Password
    if (password == null || password.length() < 6) {
        errors.append("❌ Mot de passe minimum 6 caractères\n");
    }
    
    return errors.toString();
}

private boolean isValidEmail(String email) {
    // Regex: name@gmail.com (numbers/letters allowed)
    String pattern = "^[a-zA-Z0-9]+@gmail\\.com$";
    return email.matches(pattern);
}

private boolean emailExists(String email) {
    String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            int count = rs.getInt(1);
            return count > 0;  // true si existe
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return false;
}
```

---

## READ (Lire)

### 1️⃣ Contrôleur - Charger les données

```java
// UserManagementController.java

private ObservableList<User> userList = FXCollections.observableArrayList();
@FXML private TableView<User> userTable;

@FXML
public void initialize() {
    setupTable();
    setupSearch();
    loadUsers();        // ← Load au démarrage
    loadStatistics();
}

private void loadUsers() {
    // Appel au service
    List<User> users = userService.getAllUsers();
    
    // Convertir en ObservableList (pour TableView)
    userList.clear();
    userList.addAll(users);
    
    // Mettre à jour le TableView
    userTable.setItems(userList);
    
    System.out.println("[DEBUG] Loaded " + users.size() + " users");
}

private void loadStatistics() {
    int total = userList.size();
    long active = userList.stream()
        .filter(u -> !u.isBlocked())
        .count();
    long blocked = userList.stream()
        .filter(User::isBlocked)
        .count();
    
    // Mettre à jour les labels
    totalClientsLabel.setText(String.valueOf(total));
    activeClientsLabel.setText(String.valueOf(active));
    blockedClientsLabel.setText(String.valueOf(blocked));
}
```

### 2️⃣ Service - Récupérer de la BD

```java
// UserService.java

public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM utilisateur";
    
    try (Connection conn = DatabaseUtil.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        // Parcourir le ResultSet
        while (rs.next()) {
            // Extract roles (JSON)
            String roles = rs.getString("roles");
            String userType = "client";
            if (roles != null && roles.contains("ROLE_ADMIN")) {
                userType = "admin";
            }
            
            // Extract nom complet
            String nom = rs.getString("nom") != null ? rs.getString("nom") : "";
            String prenom = rs.getString("prenom") != null ? rs.getString("prenom") : "";
            String fullName = prenom + " " + nom;
            
            // Create User object
            User user = new User(
                rs.getInt("id_utilisateur"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                userType,
                fullName.trim()
            );
            
            // Set avatar config
            user.setAvatarConfig(rs.getString("avatar_config"));
            
            // Check blocked status ⭐ IMPORTANT
            String etatCompte = rs.getString("etat_compte");
            if ("bloque".equalsIgnoreCase(etatCompte)) {
                user.setBlocked(true);  // 🔒 BLOQUÉ
            } else {
                user.setBlocked(false);  // ✅ ACTIF
            }
            
            // Add to list
            users.add(user);
        }
        
    } catch (SQLException e) {
        System.out.println("[ERROR] Failed to load users: " + e.getMessage());
        e.printStackTrace();
    }
    
    return users;
}
```

---

## UPDATE (Modifier)

### 1️⃣ Contrôleur - Éditer

```java
// UserManagementController.java

private User userToEdit;

private void showEditModal(User user) {
    userToEdit = user;  // Sauvegarder l'utilisateur à éditer
    editModalErrorLabel.setText("");
    
    // Pré-remplir les champs
    editModalNameField.setText(user.getNom());
    editModalEmailField.setText(user.getEmail());
    editModalPasswordField.clear();  // Pas de préfill password
    editModalRoleCombo.setValue(user.getType());
    
    showModal(editClientModal);
}

@FXML
private void handleEditClientConfirm() {
    if (userToEdit == null) return;
    
    String name = editModalNameField.getText().trim();
    String email = editModalEmailField.getText().trim();
    String password = editModalPasswordField.getText();  // Peut être vide
    String role = editModalRoleCombo.getValue();
    
    // Validation basique
    if (name.isEmpty() || email.isEmpty()) {
        editModalErrorLabel.setText("❌ Nom et email obligatoires");
        return;
    }
    
    if (!isValidEmail(email)) {
        editModalErrorLabel.setText("❌ Email invalide");
        return;
    }
    
    // Appel service (password vide = keep current)
    if (userService.updateUser(userToEdit.getId(), name, email, password, role)) {
        loadUsers();
        loadStatistics();
        showSuccess("✅ Client modifié!");
        closeModal();
    } else {
        editModalErrorLabel.setText("❌ Erreur mise à jour");
    }
}
```

### 2️⃣ Service - Mettre à jour en BD

```java
// UserService.java

public boolean updateUser(int id, String name, String email, String password, String role) {
    // Check 1: Email n'existe pas pour un AUTRE user
    String checkSql = "SELECT COUNT(*) FROM utilisateur WHERE email = ? AND id_utilisateur != ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setString(1, email);
        checkStmt.setInt(2, id);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            return false;  // ❌ Email taken by another user
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    
    // Check 2: Hash password si fourni (pas hash bcrypt)
    String hashedPassword = password;
    if (!password.isEmpty() && !password.startsWith("$2a$") && !password.startsWith("$2b$")) {
        // Nouveau password fourni → hasher
        hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    } else if (password.isEmpty()) {
        // Password vide → fetch current password from DB
        hashedPassword = getCurrentPasswordHash(id);
    }
    
    // Déterminer les rôles
    String roles = role.equals("admin") ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_CLIENT\"]";
    
    // SQL UPDATE
    String sql = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, roles = ? WHERE id_utilisateur = ?";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        // Split nom/prenom
        String[] nameParts = name.split(" ", 2);
        String prenom = nameParts[0];
        String nom = nameParts.length > 1 ? nameParts[1] : "";
        
        stmt.setString(1, nom);
        stmt.setString(2, prenom);
        stmt.setString(3, email);
        stmt.setString(4, hashedPassword);
        stmt.setString(5, roles);
        stmt.setInt(6, id);
        
        return stmt.executeUpdate() > 0;  // ✅ UPDATE successful
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return false;
}

// Helper: Get current password hash si pas de nouveau password
private String getCurrentPasswordHash(int id) {
    String sql = "SELECT mot_de_passe FROM utilisateur WHERE id_utilisateur = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getString("mot_de_passe");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "";
}
```

---

## DELETE (Supprimer)

### 1️⃣ Contrôleur - Confirmation puis suppression

```java
// UserManagementController.java

private User userToDelete;

private void showDeleteConfirm(User user) {
    userToDelete = user;
    deleteConfirmMessage.setText("Êtes-vous sûr de vouloir supprimer " + user.getNom() + " ?");
    showModal(deleteConfirmModal);
}

@FXML
private void handleDeleteConfirm() {
    if (userToDelete == null) return;
    
    if (userService.deleteUser(userToDelete.getId())) {
        // ✅ Succès
        loadUsers();
        loadStatistics();
        showSuccess("✅ Client supprimé!");
    } else {
        // ❌ Erreur
        showSuccess("❌ Erreur suppression");
    }
    
    closeModal();
}
```

### 2️⃣ Service - Supprimer de la BD

```java
// UserService.java

public boolean deleteUser(int id) {
    String sql = "DELETE FROM utilisateur WHERE id_utilisateur = ?";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setInt(1, id);
        int rowsAffected = stmt.executeUpdate();
        
        if (rowsAffected > 0) {
            System.out.println("[DEBUG] User deleted: id=" + id);
            return true;  // ✅ SUCCESS
        }
        
    } catch (SQLException e) {
        System.out.println("[ERROR] Failed to delete user: " + e.getMessage());
        e.printStackTrace();
    }
    
    return false;  // ❌ FAIL
}
```

---

## Validation Complète

### Exemple: Blocage d'un utilisateur

```java
// UserManagementController.java

private void handleBlockUserDirect(User user) {
    // Toggle blocked status
    user.setBlocked(!user.isBlocked());
    
    // Save to database
    userService.updateUserBlocked(user.getId(), user.isBlocked());
    
    // Refresh
    userTable.refresh();
    loadStatistics();
    
    String status = user.isBlocked() ? "🔒 bloqué" : "✅ débloqué";
    showSuccess("✅ Client " + status + "!");
}

// Service method
public boolean updateUserBlocked(int id, boolean blocked) {
    String etat = blocked ? "bloque" : "actif";
    String sql = "UPDATE utilisateur SET etat_compte = ? WHERE id_utilisateur = ?";
    
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, etat);
        stmt.setInt(2, id);
        return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}
```

### Vérification au login

```java
// LoginController.java

public void handleLogin() {
    String email = loginEmail.getText().trim();
    String password = loginPassword.getText();
    
    User user = userService.login(email, password);
    
    if (user != null) {
        // Check if user is blocked
        if (user.isBlocked()) {
            showBannedModal();  // 🔒 Cannot login
            return;
        }
        
        // ✅ Can login
        goToNextPage(user);
    } else {
        loginError.setText("❌ Invalid email or password");
    }
}
```

---

## 📊 Résumé des Opérations CRUD

| Opération | SQL | Méthode | Retour |
|-----------|-----|---------|--------|
| **C**reate | INSERT | addUser() | boolean |
| **R**ead | SELECT | getAllUsers() | List<User> |
| **U**pdate | UPDATE | updateUser() | boolean |
| **D**elete | DELETE | deleteUser() | boolean |

---

**Tous les exemples incluent:**
- ✅ Validation complète (4 niveaux)
- ✅ Gestion d'erreurs
- ✅ Sécurité (BCrypt)
- ✅ Commentaires explicatifs
- ✅ Logging [DEBUG]
- ✅ Bonnes pratiques MVC

