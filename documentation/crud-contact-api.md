# CRUD Contact (Tickets/Questions)

Ce document décrit les opérations CRUD pour le système de contact/tickets dans CURAVITA.

---

## Opérations CRUD

### 1. READ - Liste des Contacts/Tickets

#### Client - Liste ses tickets
```
GET /question
```
- Affiche les tickets du client connecté

#### Admin - Liste tous les tickets
```
GET /question
```
- Affiche tous les tickets (admin voit tout)

#### Détail d'un ticket
```
GET /question/{id}
```
- Affiche le détail avec les réponses

---

### 2. CREATE - Créer un Ticket

#### Client - Nouveau contact
```
GET /contact              → Formulaire contact
POST /contact             → Crée le ticket
```

#### Via QuestionController
```
GET /question/new         → Formulaire
POST /question/new        → Crée le ticket
```

---

### 3. UPDATE - Modifier un Ticket

#### Modifier le ticket
```
GET /question/{id}/edit   → Formulaire
POST /question/{id}/edit  → Mise à jour
```

#### Mettre à jour le statut
```
POST /question/{id}/statut
```

#### Modifier un champ
```
POST /question/{id}/update
```

#### Ajouter une réponse
```
POST /question/{id}
```
- Client ou admin peut répondre

---

### 4. DELETE - Supprimer un Ticket

```
POST /question/{id}/delete
```
- Supprime le ticket et ses réponses

---

## APIs AI pour Contact

### Analyse de question
```
POST /api/assistant/analyze-question
```
- Utilise l'IA pour analyser une question
- Catégorie, sentiment, priorité

### Générer une réponse
```
POST /api/assistant/generate-reply
```
- Utilise Groq LLM pour suggérer une réponse

### Admin Assistant
```
POST /api/admin/assistant/chat
```
- Chat avec l'assistant admin
- Analyse les tickets récents
- Propose des réponses

---

## API Admin - Contact

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | /question | Liste tous les tickets |
| GET | /question/new | Créer ticket |
| GET | /question/{id} | Détail ticket |
| POST | /question/{id}/statut | Mettre à jour statut |
| POST | /question/{id}/update | Modifier champ |
| GET | /question/{id}/edit | Formulaire édition |
| POST | /question/{id}/edit | Sauvegarder |
| POST | /question/{id}/delete | Supprimer |
| GET | /question/{id}/export-pdf | Exporter PDF |

---

## API Client - Contact

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | /contact | Page contact + mes tickets |
| POST | /contact | Créer nouveau ticket |
| GET | /question/{id} | Voir détail |
| POST | /question/{id} | Répondre au ticket |

---

## Statuts des Tickets

| Statut | Description |
|--------|-------------|
| ouvert | En attente de réponse |
| en_cours | En traitement |
| résolu | Problème résolu |
| fermé | Ticket fermé |

---

## Flux Contact

```
1. Client envoie un message via /contact
2. Ticket créé avec statut "ouvert"
3. Admin répond via /question/{id}
4. Statut change → "en_cours" → "résolu"
5. Client peut répondre
6. Admin ferme le ticket
```

---

## Entité Question

| Propriété | Type | Description |
|-----------|------|-------------|
| id | int | ID unique |
| titre | string | Sujet du ticket |
| description | string | Message |
| statut | string | Statut actuel |
| categorie | string | Catégorie |
| utilisateur | relation | Client |
| dateCreation | datetime | Date création |

---

## Entité ResponseQuestion

| Propriété | Type | Description |
|-----------|------|-------------|
| id | int | ID unique |
| reponse | string | Contenu réponse |
| auteurType | string | client/agent |
| question | relation | Question parente |
| utilisateur | relation | Auteur réponse |
