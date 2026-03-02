# CRUD Commandes - Gestion des Commandes

Ce document décrit les opérations CRUD pour la gestion des commandes dans CURAVITA.

---

## Opérations CRUD

### 1. READ - Liste des Commandes

#### Admin - Liste toutes les commandes
```
GET /admin/commandes
```
- Affiche toutes les commandes avec filtres
- Paramètres: search, date_min, date_max, statut, sort
- Retourne les statistiques (total, en_attente, confirmee, livree, annulee, suspectes)

#### Client - Liste ses commandes
```
GET /mes-commandes
```
- Affiche les commandes de l'utilisateur connecté
- Tri par date (ASC/DESC)

---

### 2. CREATE - Créer une Commande

#### Passage de commande (Client)
```
GET /commande          → Formulaire de commande
POST /commande         → Soumission du panier
```

#### Paiement (Client)
```
GET /commande/paiement     → Page de paiement
POST /commande/paiement    → Traitement paiement
```

#### Création admin
```
GET /admin/commande/new    → Formulaire
POST /admin/commande/new   → Création
```

---

### 3. UPDATE - Modifier une Commande

#### Modifier commande (Admin)
```
GET /admin/commande/{id}/edit    → Formulaire
POST /admin/commande/{id}/edit   → Mise à jour
```

#### Modifier statut (Admin)
```
POST /admin/commande/{id}/status
```
- Paramètre: `statut` (en_attente, confirmee, en_preparation, expediee, livree, annulee)
- Envoie email/SMS de notification au client

---

### 4. DELETE - Supprimer une Commande

```
POST /admin/commande/{id}/delete
```
- Supprime définitivement la commande

---

## APIs des Commandes

### API Client

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | /mes-commandes | Liste des commandes client |
| GET | /commande | Page de commande |
| POST | /commande | Soumettre commande |
| GET | /commande/paiement | Page paiement |
| POST | /commande/paiement | Valider paiement |

### API Admin

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | /admin/commandes | Liste admin |
| POST | /admin/commande/new | Créer |
| GET | /admin/commande/{id}/edit | Modifier |
| POST | /admin/commande/{id}/edit | Sauvegarder |
| POST | /admin/commande/{id}/delete | Supprimer |
| POST | /admin/commande/{id}/status | Changer statut |
| GET | /admin/commandes/export/csv | Exporter CSV |
| GET | /admin/commandes/export/pdf | Exporter PDF |

---

## Statuts des Commandes

| Statut | Description |
|--------|-------------|
| en_attente | En attente de confirmation |
| confirmee | Confirmée par le client |
| en_preparation | En cours de préparation |
| expediee | Expédiée |
| livrée | Livrée au client |
| annulee | Annulée |

---

## Flux de Commande Client

```
1. Client ajoute produits au panier
2. GET /commande → Affiche panier
3. POST /commande → Valide panier
4. GET /commande/paiement → Page paiement
5. POST /commande/paiement → Confirme & crée commande
6. Statut: "en_attente" → "confirmee"
7. Admin traite → "en_preparation" → "expediee" → "livree"
```

---

## Entité Commande

| Propriété | Type | Description |
|-----------|------|-------------|
| id | int | ID unique |
| dateCommande | datetime | Date de commande |
| statut | string | Statut actuel |
| nom | string | Nom client |
| email | string | Email client |
| telephone | string | Téléphone |
| adresseLivraison | string | Adresse |
| total | float | Montant total |
| modePaiement | string | Mode paiement |
| couponCode | string | Code promo |
| couponDiscount | float | Remise |
| produitsArray | json | Produits commandés |
| utilisateur | relation | Client |
