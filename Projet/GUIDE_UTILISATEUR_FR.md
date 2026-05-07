# 🎯 Guide d'Utilisation - Front-Office CuraVita

## 📌 Vue d'ensemble

Le front-office CuraVita permet aux utilisateurs réguliers de consulter:
- **Dépôts** 🏢 - Tous nos entrepôts et leurs informations
- **Stocks** 📦 - État des stocks de produits pharmaceutiques
- **Services** 👨‍⚕️ - Nos équipes médicales et leurs spécialités

---

## 🚀 Accès aux modules

### 1️⃣ Page d'accueil (Accueil)
Après vous être connecté, vous arrivez sur la page d'accueil avec un menu de navigation moderne.

```
┌─────────────────────────────────────────────────┐
│  CuraVita  │  Accueil │ Dépôts │ Stocks │ ...  │
└─────────────────────────────────────────────────┘
```

### 2️⃣ Accéder aux modules
Cliquez sur l'un des boutons du menu:
- **Dépôts** - Pour voir tous les dépôts
- **Stocks** - Pour voir l'état des stocks
- **Services** - Pour voir les services médicaux

---

## 📂 Module Dépôts

### Informations affichées
Chaque dépôt est affiché sous forme de carte avec:
- 🏢 **Icône** - Représente un dépôt
- **Nom du dépôt** - Titre principal
- 📍 **Adresse** - Localisation complète
- 🏙️ **Ville** - Ville du dépôt
- 📦 **Capacité** - Capacité de stockage maximale
- 👤 **Responsable** - Personne responsable
- 📞 **Téléphone** - Numéro de contact

### Filtrer et rechercher
```
┌─────────────────────────────────────────┐
│ [Rechercher...] [Filtrer par ville ▼]  │
└─────────────────────────────────────────┘
```

**Recherche**: Tapez le nom ou l'adresse du dépôt
**Filtre ville**: Sélectionnez une ville (Tunis, Sfax, Sousse)

### Exemple
```
🏢 Dépôt Tunis Centre
📍 Rue de la Liberté, 123
🏙️ Tunis
📦 Capacité: 5000
👤 Ahmed Ben Ali
📞 +216 71 123 456
```

---

## 📦 Module Stocks

### Informations affichées
Chaque stock est affiché avec un **statut visuel**:

#### ✅ En stock (Vert)
```
✅ Produit disponible en quantité suffisante
📦 Quantité: 250
🎯 Seuil min: 50
🏢 Dépôt: Tunis
🟢 En stock
```

#### 🟡 Stock faible (Orange)
```
⚠️ Stock approchant du seuil minimum
📦 Quantité: 45
🎯 Seuil min: 50
🏢 Dépôt: Sfax
🟡 Stock faible
```

#### 🔴 Rupture (Rouge)
```
❌ Produit en rupture de stock
📦 Quantité: 0
🎯 Seuil min: 50
🏢 Dépôt: Sousse
🔴 Rupture de stock
```

### Filtrer et rechercher
```
┌─────────────────────────────────────────────────────────┐
│ [Rechercher...] [Dépôt ▼] [Statut ▼]                   │
└─────────────────────────────────────────────────────────┘
```

**Recherche**: Tapez le nom du produit
**Filtre dépôt**: Sélectionnez un dépôt spécifique
**Filtre statut**: 
- En stock
- Stock faible
- Rupture

---

## 👨‍⚕️ Module Services

### Informations affichées
Chaque service est affiché avec des détails professionnels:

#### Médecin
```
👨‍⚕️ Dr. Mohamed Belaid
🏥 Type: Médecin
🔬 Spécialité: Cardiologie
📞 Tel: +216 71 234 567
📧 Email: m.belaid@curavita.tn
📍 Adresse: Rue de la Santé, 45, Tunis
```

#### Infirmier
```
👩‍⚕️ Fatima Trabelsi
🏥 Type: Infirmier
🔬 Spécialité: Soins généraux
📞 Tel: +216 71 345 678
📧 Email: f.trabelsi@curavita.tn
📍 Adresse: Avenue de l'Indépendance, 78, Sfax
```

### Filtrer et rechercher
```
┌──────────────────────────────────────────────┐
│ [Rechercher...] [Type ▼]                     │
└──────────────────────────────────────────────┘
```

**Recherche**: Tapez le nom ou la spécialité
**Filtre type**: 
- Médecin
- Infirmier

---

## 🔐 Profil Utilisateur

### Menu utilisateur (En haut à droite)
```
┌─────────────────────────┐
│ 👤 (Cliquez ici)        │
├─────────────────────────┤
│ Profil                  │
│ Dashboard (Admin)       │  ← Visible seulement pour admin
│ Logout                  │
└─────────────────────────┘
```

### Votre rôle détermine:
- **Utilisateur régulier**: Accès à la consultation (Dépôts, Stocks, Services)
- **Administrateur**: Accès au Dashboard pour la gestion complète

---

## ⚙️ Fonctionnalités disponibles

### Pour les utilisateurs réguliers ✅
- ✅ Consulter les dépôts
- ✅ Consulter les stocks et leur état
- ✅ Consulter les services médicaux
- ✅ Rechercher et filtrer les données
- ✅ Voir les détails complets de chaque élément
- ✅ Accéder au profil utilisateur
- ✅ Se déconnecter

### Pour les administrateurs ✅
- ✅ Accès à toutes les fonctionnalités utilisateurs
- ✅ Accès au Dashboard pour la gestion
- ✅ Modifier et créer des dépôts
- ✅ Gérer les stocks
- ✅ Gérer les services
- ✅ Voir les statistiques

---

## 🎨 Interface utilisateur

### Layout général
```
┌─────────────────────────────────────────────────────────┐
│ NAVBAR (Navigation pill-shaped)                         │
├─────────────────────────────────────────────────────────┤
│ HERO SECTION (Green - Welcome message)                 │
├─────────────────────────────────────────────────────────┤
│ FILTERS (Search + Dropdowns)                           │
├─────────────────────────────────────────────────────────┤
│  Card 1      Card 2      Card 3                         │
│  ┌─────┐    ┌─────┐    ┌─────┐                         │
│  │     │    │     │    │     │                         │
│  └─────┘    └─────┘    └─────┘                         │
│                                                         │
│  Card 4      Card 5      Card 6                         │
│  ┌─────┐    ┌─────┐    ┌─────┐                         │
│  │     │    │     │    │     │                         │
│  └─────┘    └─────┘    └─────┘                         │
└─────────────────────────────────────────────────────────┘
```

### Couleurs et thèmes
- **Vert principal**: #1f6f5c (Santé, confiance)
- **Blanc**: Cartes et arrière-plan
- **Gris**: Texte secondaire
- **Icônes emoji**: Pour une meilleure compréhension

---

## 💡 Conseils d'utilisation

### 1. Recherche efficace
- Tapez les premières lettres du nom pour trouver rapidement
- Les résultats se mettent à jour en temps réel

### 2. Filtrage
- Combinez la recherche et les filtres pour affiner vos résultats
- Réinitialisez en laissant les champs vides

### 3. Navigation
- Utilisez le menu de la navbar pour naviguer entre les modules
- Revenez à l'accueil à tout moment en cliquant sur "Accueil"

### 4. Responsive
- L'interface s'adapte à la taille de votre écran
- Utilisez le scroll pour voir plus de cartes

---

## ❓ Questions fréquemment posées

### Q: Je ne vois pas le Dashboard?
**R:** Le Dashboard n'est accessible que pour les administrateurs. Vérifiez votre rôle utilisateur.

### Q: Comment changer mes informations personnelles?
**R:** Cliquez sur "Profil" dans le menu utilisateur (👤).

### Q: Je ne vois pas mes résultats de recherche?
**R:** Assurez-vous:
- D'avoir tapé la bonne orthographe
- De ne pas avoir de filtres trop restrictifs
- D'actualiser la page si nécessaire

### Q: Les données ne se mettent pas à jour?
**R:** Les données se chargent automatiquement au démarrage. Revenez à l'accueil et cliquez à nouveau sur le module.

---

## 🔗 Navigation rapide

| Action | Navigation |
|--------|-----------|
| Accueil | Cliquez sur "Accueil" |
| Voir Dépôts | Cliquez sur "Dépôts" |
| Voir Stocks | Cliquez sur "Stocks" |
| Voir Services | Cliquez sur "Services" |
| Profil | Cliquez 👤 → Profil |
| Dashboard (Admin) | Cliquez 👤 → Dashboard |
| Logout | Cliquez 👤 → Logout |

---

## 📧 Support

Pour toute question ou problème:
- Contactez un administrateur
- Email: support@curavita.tn
- Téléphone: +216 71 000 000

---

**Dernier mise à jour:** 2026-04-15
**Version:** 1.0

