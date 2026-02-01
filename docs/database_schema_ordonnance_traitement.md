# Structure de la Base de Données - Module Ordonnances & Suivi Médical

## Table: `ordonnance`

Cette table stocke les ordonnances médicales téléversées par les clients.

### Structure

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identifiant unique |
| `file_name` | VARCHAR(255) | NOT NULL | Nom du fichier téléversé |
| `file_path` | VARCHAR(500) | NOT NULL | Chemin de stockage du fichier |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'pending_validation' | Statut de l'ordonnance |
| `rejection_reason` | TEXT | NULLABLE | Raison du rejet (si applicable) |
| `uploaded_at` | DATETIME | NOT NULL | Date de téléversement |
| `validated_at` | DATETIME | NULLABLE | Date de validation |
| `client_id` | INT | NOT NULL | ID du client (géré par un autre module) |
| `validated_by_id` | INT | NULLABLE | ID du pharmacien validateur (géré par un autre module) |

### Statuts possibles
- `pending_validation` : En attente de validation
- `validated` : Validée par un pharmacien
- `rejected` : Rejetée

### Relations externes
- `client_id` : Référence au client (module Clients)
- `validated_by_id` : Référence au pharmacien (module Users)

### Formats de fichiers acceptés
- PDF (.pdf)
- JPEG (.jpg, .jpeg)
- PNG (.png)

---

## Table: `traitement`

Cette table stocke les traitements médicaux créés à partir des ordonnances validées.

### Structure

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identifiant unique |
| `dosage` | VARCHAR(255) | NOT NULL | Dosage du médicament |
| `frequency` | VARCHAR(255) | NOT NULL | Fréquence de prise |
| `duration_days` | INT | NOT NULL, POSITIVE | Durée en jours |
| `start_date` | DATETIME | NOT NULL | Date de début |
| `end_date` | DATETIME | NOT NULL | Date de fin |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Traitement actif |
| `is_completed` | BOOLEAN | NOT NULL, DEFAULT FALSE | Traitement terminé |
| `notes` | TEXT | NULLABLE | Notes additionnelles |
| `ordonnance_id` | INT | NOT NULL | ID de l'ordonnance |
| `client_id` | INT | NOT NULL | ID du client (géré par un autre module) |

### Relations
- `ordonnance_id` : Référence à l'ordonnance (relation interne)
- `client_id` : Référence au client (module Clients)

### Logique métier
- `end_date` est calculée automatiquement : `start_date + duration_days`
- Un traitement est actif (`is_active = true`) jusqu'à sa date de fin
- Un client peut marquer un traitement comme complété (`is_completed = true`)

---

## Diagramme des Relations

```
Ordonnance (1) ----< (N) Traitement
     |
     | (client_id, validated_by_id)
     |
     v
Modules externes (Client, Utilisateur)
```

---

## Index

### Table `ordonnance`
- **Index** sur `status` pour les requêtes de filtrage
- **Index** sur `client_id` pour l'historique client

### Table `traitement`
- **Index composite** sur `(client_id, is_active)` pour les traitements actifs
- **Index** sur `ordonnance_id` pour les relations

---

## Requêtes Courantes

### Ordonnances en attente de validation
```sql
SELECT * FROM ordonnance 
WHERE status = 'pending_validation' 
ORDER BY uploaded_at ASC;
```

### Traitements actifs d'un client
```sql
SELECT * FROM traitement 
WHERE client_id = ? AND is_active = 1 
ORDER BY start_date DESC;
```

### Historique des ordonnances d'un client
```sql
SELECT * FROM ordonnance 
WHERE client_id = ? 
ORDER BY uploaded_at DESC;
```

---

## Sécurité

- Les fichiers d'ordonnances sont stockés dans un répertoire sécurisé hors du webroot
- Validation des types MIME lors du téléversement
- Seuls les pharmaciens (ROLE_PHARMACIEN) peuvent valider/rejeter les ordonnances
- Les clients ne peuvent voir que leurs propres ordonnances et traitements

---

## Note importante

Ce module gère uniquement les ordonnances et traitements. Les entités Client et Utilisateur sont gérées par d'autres modules du système CuraVita.
