# 🏥 Guide de Navigation - CuraVita Pharmacie

## 🌐 Serveur de développement

Le serveur Symfony est démarré sur : **http://127.0.0.1:8000**

---

## 🏠 FRONTOFFICE (Interface Client)

### Pages disponibles :

#### 1. **Page d'accueil**
- **URL** : http://127.0.0.1:8000/
- **Description** : Page d'accueil avec présentation du module
- **Fonctionnalités** :
  - Vue d'ensemble des services
  - Accès rapide aux ordonnances et traitements
  - Informations sur les formats acceptés

#### 2. **Mes Ordonnances**
- **URL** : http://127.0.0.1:8000/ordonnances
- **Description** : Liste de toutes les ordonnances du client
- **Fonctionnalités** :
  - Affichage des ordonnances avec statuts (En attente, Validée, Rejetée)
  - Filtrage par statut
  - Accès aux détails de chaque ordonnance

#### 3. **Détails d'une ordonnance**
- **URL** : http://127.0.0.1:8000/ordonnances/{id}
- **Exemple** : http://127.0.0.1:8000/ordonnances/2
- **Description** : Détails complets d'une ordonnance
- **Fonctionnalités** :
  - Informations complètes (fichier, dates, statut)
  - Liste des traitements associés
  - Raison du rejet (si applicable)

#### 4. **Traitements d'une ordonnance**
- **URL** : http://127.0.0.1:8000/ordonnances/{id}/traitements
- **Exemple** : http://127.0.0.1:8000/ordonnances/2/traitements
- **Description** : Vue détaillée des traitements
- **Fonctionnalités** :
  - Affichage des traitements avec dosage, fréquence, durée
  - Statut actif/inactif/complété
  - Notes du pharmacien

#### 5. **Mes Traitements**
- **URL** : http://127.0.0.1:8000/traitements
- **Description** : Vue d'ensemble de tous les traitements
- **Fonctionnalités** :
  - Traitements actifs en priorité
  - Historique complet
  - Statistiques

---

## ⚙️ BACKOFFICE (Interface Pharmacien)

### Pages disponibles :

#### 1. **Dashboard Admin**
- **URL** : http://127.0.0.1:8000/admin
- **Description** : Tableau de bord principal
- **Fonctionnalités** :
  - Statistiques globales (ordonnances, traitements)
  - Alertes pour ordonnances en attente
  - Ordonnances récentes (7 derniers jours)
  - Actions rapides

#### 2. **Gestion des Ordonnances**
- **URL** : http://127.0.0.1:8000/admin/ordonnances
- **Description** : Liste complète des ordonnances
- **Fonctionnalités** :
  - Tableau avec toutes les ordonnances
  - Filtrage par statut (Toutes, En attente, Validées, Rejetées)
  - Compteur d'ordonnances en attente
  - Accès rapide aux détails

#### 3. **Détails Ordonnance (Admin)**
- **URL** : http://127.0.0.1:8000/admin/ordonnances/{id}
- **Exemple** : http://127.0.0.1:8000/admin/ordonnances/1
- **Description** : Vue détaillée avec actions
- **Fonctionnalités** :
  - Informations complètes
  - **Bouton Valider** (pour ordonnances en attente)
  - **Bouton Rejeter** avec formulaire de raison
  - Liste des traitements associés

#### 4. **Actions disponibles**
- **Valider une ordonnance** : POST /admin/ordonnances/{id}/valider
- **Rejeter une ordonnance** : POST /admin/ordonnances/{id}/rejeter

---

## 🎨 Design & Couleurs

### Palette de couleurs :
- **Vert principal** : #2ecc71
- **Vert foncé** : #27ae60
- **Blanc** : #ffffff
- **Gris clair** : #f8f9fa
- **Gris** : #ecf0f1

### Éléments visuels :
- 🏥 Logo pharmacie
- 📋 Icône ordonnances
- 💊 Icône traitements
- ✅ Statut validé (vert)
- 🟡 Statut en attente (jaune)
- ❌ Statut rejeté (rouge)

---

## 📊 Données de test

### Ordonnances disponibles :
1. **Ordonnance #1** - Client 1 - En attente
2. **Ordonnance #2** - Client 1 - Validée (2 traitements)
3. **Ordonnance #3** - Client 2 - Validée (3 traitements)
4. **Ordonnance #4** - Client 3 - En attente
5. **Ordonnance #5** - Client 2 - Rejetée

### Traitements disponibles :
- 5 traitements au total
- 4 actifs
- 1 complété

---

## 🚀 Navigation rapide

### Pour tester le frontoffice :
1. Ouvrir http://127.0.0.1:8000/
2. Cliquer sur "Mes Ordonnances"
3. Voir les détails d'une ordonnance validée
4. Consulter les traitements

### Pour tester le backoffice :
1. Ouvrir http://127.0.0.1:8000/admin
2. Voir le dashboard avec statistiques
3. Aller dans "Ordonnances"
4. Ouvrir une ordonnance en attente
5. Valider ou rejeter l'ordonnance

---

## 🔧 Commandes utiles

```bash
# Démarrer le serveur
symfony server:start

# Arrêter le serveur
symfony server:stop

# Voir les logs
symfony server:log

# Charger les données de test
php bin/console app:load-test-data

# Afficher les données
php bin/console app:show-data
```

---

## 📱 Responsive Design

Le site est entièrement responsive et s'adapte aux :
- 💻 Desktop (1200px+)
- 📱 Tablette (768px - 1199px)
- 📱 Mobile (< 768px)

---

## ✨ Fonctionnalités implémentées

### Frontoffice :
- ✅ Page d'accueil avec présentation
- ✅ Liste des ordonnances avec filtres
- ✅ Détails des ordonnances
- ✅ Vue des traitements
- ✅ Historique complet
- ✅ Design vert et blanc
- ✅ Navigation intuitive
- ✅ Footer avec informations

### Backoffice :
- ✅ Dashboard avec statistiques
- ✅ Gestion des ordonnances
- ✅ Validation d'ordonnances
- ✅ Rejet avec raison
- ✅ Filtrage par statut
- ✅ Alertes pour ordonnances en attente
- ✅ Design professionnel
- ✅ Tables interactives

---

## 🎯 Prochaines étapes suggérées

1. **Upload d'ordonnances** : Formulaire de téléversement
2. **Création de traitements** : Interface pour créer des traitements
3. **Notifications** : Système de notifications en temps réel
4. **Recherche** : Fonction de recherche avancée
5. **Export PDF** : Génération de PDF pour ordonnances
6. **Authentification** : Système de login complet
7. **API REST** : Endpoints pour mobile app

---

## 📞 Support

Pour toute question ou problème :
- 📧 Email: contact@curavita.com
- 📱 Tél: +33 1 23 45 67 89
