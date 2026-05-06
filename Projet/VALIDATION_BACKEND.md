# 🔒 Validation côté Serveur (Backend) - Module Dépôt

## 📋 Vue d'ensemble

Un système complet de **validation côté serveur (backend)** a été implémenté pour le module Dépôt. Toute la validation est faite en **Java** (équivalent du PHP backend), **sans aucune validation JavaScript**.

---

## 🎯 Architecture

### 1. **DepotValidator.java** (Classe de validation)
Classe responsable de valider tous les champs avant insertion/modification en base de données.

**Chemin :** `src/main/java/org/example/util/DepotValidator.java`

**Validations implémentées :**

| Champ | Validations |
|-------|------------|
| **Nom** | - Obligatoire (non vide)<br>- Min 2 caractères<br>- Max 255 caractères<br>- Caractères autorisés (alphanumériques + accents) |
| **Adresse** | - Obligatoire<br>- Min 5 caractères<br>- Max 255 caractères |
| **Ville** | - Obligatoire<br>- Min 2 caractères<br>- Max 100 caractères |
| **Capacité** | - Obligatoire<br>- Nombre entier positif<br>- Entre 1 et 1,000,000 |
| **Responsable** | - Obligatoire<br>- Min 2 caractères<br>- Max 255 caractères |
| **Téléphone** | - Obligatoire<br>- Format français: 0X XXXXXXXX (ex: 0123456789) |
| **Latitude** | - Optionnel<br>- Format décimal (-90 à +90) |
| **Longitude** | - Optionnel<br>- Format décimal (-180 à +180) |

### 2. **DepotService.java** (Couche métier)
Le service implémente la validation avant toute insertion/modification.

**Méthodes validées :**
```java
// Validation + insertion en base
public boolean add(Depot depot) throws ValidationException

// Validation + modification en base
public boolean update(Depot depot) throws ValidationException

// Récupérer les erreurs de validation
public String getValidationErrors()
```

### 3. **DepotFormController.java** (Contrôleur formulaire)
Gère l'affichage des erreurs de validation au formulaire.

**Flux :**
1. L'utilisateur remplit le formulaire
2. Clique sur "Enregistrer"
3. **Validation côté serveur** dans `DepotValidator`
4. Si erreurs → Affiche les messages d'erreur
5. Si valide → Insertion en base de données

### 4. **ValidationException.java** (Exception personnalisée)
Exception levée en cas d'erreur de validation.

---

## 💻 Exemple d'utilisation

### Ajout d'un dépôt avec validation

```java
DepotService service = DepotService.getInstance();

Depot newDepot = new Depot();
newDepot.setNom("Dépôt Central");
newDepot.setAdresse("10 Rue du Stock, Paris");
newDepot.setVille("Paris");
newDepot.setCapaciteDepot(1000);
newDepot.setResponsableDepot("Jean Dupont");
newDepot.setResponsableTelephone("0123456789");
newDepot.setDateCreation(LocalDateTime.now());

try {
    // La validation est automatique
    service.add(newDepot);
    System.out.println("✅ Dépôt ajouté avec succès!");
} catch (ValidationException e) {
    // Afficher les erreurs
    System.err.println("❌ Erreur de validation: " + e.getMessage());
}
```

---

## 🔍 Gestion des erreurs

### Messages d'erreur affichés à l'utilisateur

**Exemple 1 : Validation échouée**
```
❌ • Le nom du dépôt est obligatoire
• L'adresse doit contenir au moins 5 caractères
• Format invalide. Utilisez: 0X XXXXXXXX (ex: 0123456789)
```

**Exemple 2 : Validation réussie**
```
✅ Dépôt ajouté avec succès!
```

Les messages sont affichés dans le label `errorLabel` du formulaire, et les valeurs saisies sont **conservées** en cas d'erreur.

---

## 🛡️ Sécurité

### Fonctions de sécurisation des données

```java
// Nettoyer et échapper les caractères dangereux
String cleaned = DepotValidator.sanitize(input);

// Validation + nettoyage
String safe = DepotValidator.validateAndSanitize(input);
```

**Opérations de sécurisation :**
- ✅ **Trim** : Supprime les espaces avant/après
- ✅ **HTML Escape** : Échappe `< > & " '`
- ✅ **Regex Validation** : Vérifie le format exact
- ✅ **Type Safety** : Conversions strictes (parseInt, parseDouble, etc.)

---

## 📝 Validation du formulaire (DepotFormController)

### Flux de validation côté formulaire

```java
@FXML
private void handleSave() {
    // 1. Récupérer les valeurs
    String nom = nomField.getText();
    String adresse = adresseField.getText();
    // ... autres champs
    
    // 2. VALIDATION CÔTÉ SERVEUR
    DepotValidator validator = new DepotValidator();
    if (!validator.validate(nom, adresse, ville, capacite, 
                          responsable, telephone, latitude, longitude)) {
        // 3. Afficher les erreurs
        String errorMessage = validator.getErrorMessage();
        errorLabel.setText(errorMessage);
        return; // Ne pas procéder à l'insertion
    }
    
    // 4. Les données sont valides, les valeurs sont conservées
    // 5. Appel du service (qui va aussi valider)
    try {
        depotService.add(newDepot);
        NotificationUtil.showSuccess("✅ Dépôt ajouté avec succès!");
    } catch (ValidationException e) {
        errorLabel.setText("❌ " + e.getMessage());
    }
}
```

---

## ✅ Checklist d'implémentation

- ✅ Classe `DepotValidator` : Validation de tous les champs
- ✅ Exception `ValidationException` : Gestion des erreurs
- ✅ Service `DepotService` : Validation + insertion atomique
- ✅ Contrôleur `DepotFormController` : Affichage des erreurs
- ✅ Sécurisation des données : Nettoyage et escape
- ✅ Messages d'erreur clairs : Détail par champ
- ✅ Conservation des valeurs : En cas d'erreur
- ✅ **AUCUNE validation JavaScript** : Tout est côté backend

---

## 🚀 Prochaines étapes

1. **Test du CRUD complet** : Ajouter, modifier, supprimer des dépôts
2. **Gestion des erreurs DB** : Gérer les contraintes d'unicité, les contraintes étrangères
3. **Audit** : Logger les insertions/modifications (pour traçabilité)
4. **Pagination** : Affichage des dépôts avec pagination
5. **Module Stock** : Implémenter la même validation pour le module Stock

---

## 📌 Notes importantes

⚠️ **Pas de validation JavaScript** : Toute la validation est côté backend (Java)
⚠️ **Validation stricte** : Les données ne sont JAMAIS insérées si validation échoue
⚠️ **Messages clairs** : Un message d'erreur par champ invalide
⚠️ **Sécurité** : Protection contre les injections SQL et XSS

---

## 🔗 Fichiers modifiés/créés

| Fichier | Type | Action |
|---------|------|--------|
| `DepotValidator.java` | Nouveau | ✅ Créé |
| `ValidationException.java` | Nouveau | ✅ Créé |
| `DepotService.java` | Modifié | ✅ Mis à jour |
| `DepotFormController.java` | Modifié | ✅ Mis à jour |
| `DashboardController.java` | Modifié | ✅ Mis à jour |
| `TestDepotService.java` | Modifié | ✅ Mis à jour |

**Compilation :** ✅ BUILD SUCCESS

