# CRUD Dépôts - Gestion des Points de Retrait

Ce document décrit les opérations CRUD pour la gestion des dépôts (points de retrait) dans CURAVITA.

---

## Opérations CRUD

### 1. READ - Liste des Dépôts

#### Admin - Liste tous les dépôts
```
GET /admin/depots
```
- Affiche tous les dépôts avec filtres
- Paramètres: search, ville, capacite_min, capacite_max, sort

#### Client - Liste les dépôts disponibles
```
GET /depots
```
- Affiche les dépôts pour retrait client
- Filtres: recherche, ville, capacité

#### API - Santé d'un dépôt
```
GET /api/depots/{id}/health
```
- Retourne le score de santé du dépôt

---

### 2. CREATE - Créer un Dépôt

#### Création admin
```
GET /admin/depot/new    → Formulaire
POST /admin/depot/new   → Création
```
- Crée un nouveau dépôt
- Géocodage automatique de l'adresse

---

### 3. UPDATE - Modifier un Dépôt

#### Modifier dépôt
```
GET /admin/depot/{id}/edit    → Formulaire
POST /admin/depot/{id}/edit   → Mise à jour
```

#### Régulation IA
```
GET /admin/depot/{id}/ai-regulation
```
- Utilise l'IA pour suggérer des ajustements de stock

#### Dépôt le plus proche
```
GET /admin/depots/nearest
```
- Trouve le dépôt le plus proche avec stock disponible

---

### 4. DELETE - Supprimer un Dépôt

```
POST /admin/depot/{id}/delete
```
- Supprime le dépôt
- Vérifie qu'il n'a pas de stocks associés
- Empêche la suppression si des stocks existent

---

## APIs des Dépôts

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | /admin/depots | Liste admin |
| GET | /depots | Liste client |
| POST | /admin/depot/new | Créer |
| POST | /admin/depot/{id}/edit | Modifier |
| POST | /admin/depot/{id}/delete | Supprimer |
| GET | /admin/depot/{id}/pdf | Générer PDF |
| GET | /api/depots/{id}/health | Score santé |
| GET | /admin/depot/{id}/ai-regulation | Régulation IA |
| GET | /admin/depots/nearest | Dépôt le plus proche |

---

## Entité Dépôt

| Propriété | Type | Description |
|-----------|------|-------------|
| id | int | ID unique |
| nomDepot | string | Nom du dépôt |
| adresseDepot | string | Adresse |
| ville | string | Ville |
| responsableDepot | string | Nom du responsable |
| capaciteDepot | int | Capacité de stockage |
| telephoneDepot | string | Téléphone |
| emailDepot | string | Email |
| latitude | float | Latitude GPS |
| longitude | float | Longitude GPS |
| dateCreation | datetime | Date de création |

---

## Services Associés

### DepotGeocodingService
- Géocodage automatique des adresses
- Conversion adresse → coordonnées GPS

### DepotHealthScoreService
- Calcul du score de santé du dépôt
- Évaluation performance

### StockAIService
- Régulation intelligente des stocks
- Prévisions basées sur l'IA

---

## Flux de Création

```
1. Admin remplit le formulaire
2. DepotGeocodingService géocode l'adresse
3. Coordonnées (lat/long) enregistrées
4. Dépôt créé avec succès
5. Redirection vers liste des dépôts
```

---

## Vérifications de Suppression

Avant suppression, le système vérifie:
- Le dépôt n'a pas de stocks associés
- Pas de commandes en attente pour ce dépôt

Si des stocks existent → Message d'erreur → Suppression bloquée
