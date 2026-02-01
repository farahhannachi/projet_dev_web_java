# 🏥 CuraVita Pharmacy - Module Ordonnances & Suivi Médical

## 📋 Description

Application web de gestion des ordonnances et suivi médical pour la pharmacie CuraVita. Ce module permet aux clients de gérer leurs ordonnances et traitements, et aux pharmaciens de valider les ordonnances et créer des plans de traitement.

## 🎨 Design

- **Couleurs** : Vert (#2ecc71, #27ae60) et Blanc
- **Style** : Moderne, professionnel, adapté au domaine médical
- **Responsive** : Compatible desktop, tablette et mobile

## 🚀 Démarrage rapide

### 1. Démarrer le serveur

```bash
cd curavita-pharmacy
symfony server:start
```

Le site sera accessible sur : **http://127.0.0.1:8000**

### 2. Charger les données de test

```bash
php bin/console app:load-test-data
```

### 3. Accéder au site

- **Frontoffice** : http://127.0.0.1:8000/
- **Backoffice** : http://127.0.0.1:8000/admin

## 📁 Structure du projet

```
curavita-pharmacy/
├── src/
│   ├── Controller/
│   │   ├── Front/          # Contrôleurs frontoffice
│   │   │   ├── HomeController.php
│   │   │   ├── OrdonnanceController.php
│   │   │   └── TraitementController.php
│   │   └── Back/           # Contrôleurs backoffice
│   │       ├── DashboardController.php
│   │       └── OrdonnanceController.php
│   ├── Entity/
│   │   ├── Ordonnance.php  # Entité ordonnance
│   │   └── Traitement.php  # Entité traitement
│   ├── Repository/
│   │   ├── OrdonnanceRepository.php
│   │   └── TraitementRepository.php
│   └── Command/
│       ├── LoadTestDataCommand.php
│       └── ShowDataCommand.php
├── templates/
│   ├── front/              # Vues frontoffice
│   │   ├── home/
│   │   ├── ordonnance/
│   │   └── traitement/
│   └── back/               # Vues backoffice
│       ├── dashboard/
│       └── ordonnance/
├── public/
│   └── css/
│       ├── front.css       # Styles frontoffice
│       └── back.css        # Styles backoffice
└── docs/                   # Documentation
    ├── GUIDE_NAVIGATION.md
    ├── database_schema_ordonnance_traitement.md
    └── VERIFICATION_JOINTURE.md
```

## 🌐 Pages disponibles

### Frontoffice (Client)

| Page | URL | Description |
|------|-----|-------------|
| Accueil | `/` | Page d'accueil |
| Mes Ordonnances | `/ordonnances` | Liste des ordonnances |
| Détails Ordonnance | `/ordonnances/{id}` | Détails d'une ordonnance |
| Traitements Ordonnance | `/ordonnances/{id}/traitements` | Traitements d'une ordonnance |
| Mes Traitements | `/traitements` | Liste de tous les traitements |

### Backoffice (Pharmacien)

| Page | URL | Description |
|------|-----|-------------|
| Dashboard | `/admin` | Tableau de bord |
| Ordonnances | `/admin/ordonnances` | Gestion des ordonnances |
| Détails | `/admin/ordonnances/{id}` | Détails avec actions |
| Valider | `/admin/ordonnances/{id}/valider` | Valider une ordonnance |
| Rejeter | `/admin/ordonnances/{id}/rejeter` | Rejeter une ordonnance |

## 💾 Base de données

### Tables

- **ordonnance** : Stocke les ordonnances médicales
- **traitement** : Stocke les traitements médicaux

### Relation

- Une ordonnance peut avoir plusieurs traitements (OneToMany)
- Un traitement appartient à une ordonnance (ManyToOne)
- Cascade DELETE : Supprimer une ordonnance supprime ses traitements

## 🎯 Fonctionnalités

### ✅ Implémentées

#### Frontoffice
- [x] Page d'accueil avec présentation
- [x] Liste des ordonnances avec statuts
- [x] Détails des ordonnances
- [x] Vue des traitements par ordonnance
- [x] Historique complet des traitements
- [x] Design responsive vert et blanc
- [x] Navigation intuitive
- [x] Footer informatif

#### Backoffice
- [x] Dashboard avec statistiques
- [x] Liste des ordonnances avec filtres
- [x] Validation d'ordonnances
- [x] Rejet d'ordonnances avec raison
- [x] Alertes pour ordonnances en attente
- [x] Vue détaillée des traitements
- [x] Design professionnel
- [x] Tables interactives

### 🔜 À venir

- [ ] Upload d'ordonnances (formulaire)
- [ ] Création de traitements (interface)
- [ ] Système de notifications
- [ ] Recherche avancée
- [ ] Export PDF
- [ ] Authentification complète
- [ ] API REST

## 🔧 Commandes utiles

```bash
# Charger les données de test
php bin/console app:load-test-data

# Afficher les données avec relations
php bin/console app:show-data

# Vérifier le schéma de base de données
php bin/console doctrine:schema:validate

# Créer une migration
php bin/console doctrine:migrations:diff

# Exécuter les migrations
php bin/console doctrine:migrations:migrate

# Vider le cache
php bin/console cache:clear
```

## 📊 Données de test

### Ordonnances (5)
- 2 en attente de validation
- 2 validées (avec traitements)
- 1 rejetée

### Traitements (5)
- 4 actifs
- 1 complété

## 🎨 Personnalisation

### Couleurs

Les couleurs sont définies dans les fichiers CSS :

```css
:root {
    --primary-green: #2ecc71;
    --dark-green: #27ae60;
    --white: #ffffff;
}
```

### Logo

Le logo peut être remplacé dans les templates :
- Frontoffice : `templates/front/base.html.twig`
- Backoffice : `templates/back/base.html.twig`

## 📱 Responsive

Le site s'adapte automatiquement aux différentes tailles d'écran :
- Desktop : 1200px+
- Tablette : 768px - 1199px
- Mobile : < 768px

## 🐛 Dépannage

### Le serveur ne démarre pas
```bash
symfony server:stop
symfony server:start
```

### Erreur de base de données
```bash
php bin/console doctrine:database:create --if-not-exists
php bin/console doctrine:schema:update --force
```

### Cache problématique
```bash
php bin/console cache:clear
```

## 📞 Contact

- **Email** : contact@curavita.com
- **Téléphone** : +33 1 23 45 67 89
- **Adresse** : 123 Rue de la Santé, Paris

## 📄 Licence

© 2026 CuraVita Pharmacie - Tous droits réservés

---

**Développé avec** : Symfony 6, PHP 8.1, Doctrine ORM, Twig
