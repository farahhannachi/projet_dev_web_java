# 📧 Informations de Connexion - Page Sign In

## Compte Admin Disponible

### Email et Mot de Passe

```
Email: ihebjbir10@gmail.com
Mot de passe: iheb123
Rôle: ADMIN
```

### Ce Compte Donne Accès À:
- ✅ Dashboard Administrateur
- ✅ Gestion des produits
- ✅ Gestion des commandes
- ✅ Gestion des dépôts
- ✅ Gestion des stocks
- ✅ Gestion des utilisateurs
- ✅ Rapports et statistiques

---

## 🆕 Créer un Nouveau Compte (Sign Up)

Si vous voulez créer un nouveau compte client, cliquez sur "Sign Up" et :

1. **Entrez votre nom complet**
2. **Entrez votre email** (format: `nom@gmail.com` ou `nom123@gmail.com`)
3. **Créez un mot de passe**

### Exemple:
```
Nom: Jean Dupont
Email: jean.dupont@gmail.com
Mot de passe: MonMotDePasse123
```

**Important:** L'email doit se terminer par `@gmail.com`

---

## 🔐 Sécurité

- ✅ Les mots de passe sont hashés avec BCrypt
- ✅ La session est sécurisée
- ✅ Vous serez automatiquement redirigé vers:
  - **Dashboard** si vous êtes admin
  - **Page d'accueil** si vous êtes client

---

## 📝 Détails du Compte Admin

| Champ | Valeur |
|-------|--------|
| **ID** | 1 |
| **Nom** | iheb |
| **Prénom** | ben jbir |
| **Email** | ihebjbir10@gmail.com |
| **Mot de passe** | iheb123 |
| **Rôle** | ROLE_ADMIN |
| **État du compte** | Actif |
| **Date de création** | 2026-02-28 |

---

## 🎯 Pour Tester l'Application

### Accès Admin (Pour tout voir)
```
Email: ihebjbir10@gmail.com
Mot de passe: iheb123
```
→ Vous accédez au Dashboard (gestion complète)

### Accès Client (Pour tester l'expérience utilisateur)
Créez un nouveau compte via Sign Up
→ Vous accédez à la page d'accueil client

---

## ⚙️ Détails Techniques

Le système utilise:
- **Base de données:** MySQL
- **Table utilisateur:** `utilisateur`
- **Hachage mot de passe:** BCrypt ($2y$13$)
- **Rôles:** ROLE_ADMIN, ROLE_CLIENT
- **Authentification:** Session utilisateur stockée en mémoire

---

## 🆘 En Cas de Problème

### "Database not connected"
- ✅ Assurez-vous que MySQL est démarré
- ✅ Vérifiez la configuration de la base de données dans `DatabaseUtil.java`
- ✅ Relancez l'application

### "Invalid email or password"
- ✅ Vérifiez l'email exact: `ihebjbir10@gmail.com`
- ✅ Vérifiez le mot de passe: `iheb123`
- ✅ Vérifiez que la BD contient bien l'utilisateur

### Format d'email invalide (Sign Up)
- ✅ L'email doit être au format: `nom@gmail.com`
- ✅ Seuls les emails Gmail sont acceptés
- ✅ Exemple valide: `mundo36@gmail.com`, `jean@gmail.com`

---

**Status:** ✅ Compte actif et testé

