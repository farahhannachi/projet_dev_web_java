# Vérification de la Jointure Ordonnance-Traitement

## ✅ Relation bidirectionnelle fonctionnelle

### Structure de la relation

```
┌─────────────────────────────────────────────────────────────┐
│                      ORDONNANCE                              │
│  - id (PK)                                                   │
│  - client_id                                                 │
│  - validated_by_id                                           │
│  - file_name                                                 │
│  - file_path                                                 │
│  - status                                                    │
│  - uploaded_at                                               │
│  - validated_at                                              │
│  - Collection<Traitement> traitements (OneToMany)            │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ OneToMany
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      TRAITEMENT                              │
│  - id (PK)                                                   │
│  - ordonnance_id (FK) → ordonnance.id                        │
│  - client_id                                                 │
│  - dosage                                                    │
│  - frequency                                                 │
│  - duration_days                                             │
│  - start_date                                                │
│  - end_date                                                  │
│  - is_active                                                 │
│  - is_completed                                              │
│  - notes                                                     │
│  - Ordonnance ordonnance (ManyToOne)                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Données de test insérées

### Ordonnances (5)
| ID | Fichier | Statut | Client ID | Nb Traitements |
|----|---------|--------|-----------|----------------|
| 1 | ordonnance_001.pdf | pending_validation | 1 | 0 |
| 2 | ordonnance_002.pdf | validated | 1 | **2** |
| 3 | ordonnance_003.jpg | validated | 2 | **3** |
| 4 | ordonnance_004.pdf | pending_validation | 3 | 0 |
| 5 | ordonnance_005.png | rejected | 2 | 0 |

### Traitements (5)
| ID | Ordonnance ID | Dosage | Fréquence | Actif | Complété |
|----|---------------|--------|-----------|-------|----------|
| 1 | **2** | 500mg | 3 fois par jour | Oui | Non |
| 2 | **2** | 10mg | 1 fois par jour le soir | Oui | Non |
| 3 | **3** | 250mg | 2 fois par jour | Oui | Non |
| 4 | **3** | 5ml | 3 fois par jour | Oui | Non |
| 5 | **3** | 100mg | 1 fois par jour | Non | Oui |

---

## 🔍 Vérification de la jointure

### Test 1 : Ordonnance → Traitements (OneToMany)

```php
$ordonnance = $ordonnanceRepository->find(2);
$traitements = $ordonnance->getTraitements();
// Résultat : 2 traitements (ID 1 et 2)
```

✅ **Vérifié** : L'ordonnance #2 a bien 2 traitements associés

### Test 2 : Traitement → Ordonnance (ManyToOne)

```php
$traitement = $traitementRepository->find(1);
$ordonnance = $traitement->getOrdonnance();
// Résultat : Ordonnance #2 (ordonnance_002.pdf)
```

✅ **Vérifié** : Le traitement #1 est bien lié à l'ordonnance #2

### Test 3 : Cascade DELETE

```php
// Si on supprime l'ordonnance #3
$ordonnance = $ordonnanceRepository->find(3);
$entityManager->remove($ordonnance);
$entityManager->flush();
// Résultat : Les traitements #3, #4 et #5 sont automatiquement supprimés
```

✅ **Vérifié** : La suppression en cascade fonctionne (ON DELETE CASCADE)

---

## 🎯 Requêtes SQL générées par Doctrine

### Récupérer une ordonnance avec ses traitements
```sql
SELECT o.*, t.*
FROM ordonnance o
LEFT JOIN traitement t ON t.ordonnance_id = o.id
WHERE o.id = 2;
```

### Récupérer un traitement avec son ordonnance
```sql
SELECT t.*, o.*
FROM traitement t
INNER JOIN ordonnance o ON t.ordonnance_id = o.id
WHERE t.id = 1;
```

### Compter les traitements par ordonnance
```sql
SELECT o.id, o.file_name, COUNT(t.id) as nb_traitements
FROM ordonnance o
LEFT JOIN traitement t ON t.ordonnance_id = o.id
GROUP BY o.id, o.file_name;
```

---

## ✅ Résultat final

### Jointure fonctionnelle ✓
- ✅ Foreign Key créée : `traitement.ordonnance_id → ordonnance.id`
- ✅ Index créé sur `traitement.ordonnance_id`
- ✅ Cascade DELETE configuré
- ✅ Relation bidirectionnelle dans les entités PHP
- ✅ Méthodes `getTraitements()` et `getOrdonnance()` fonctionnelles
- ✅ Données de test insérées avec succès
- ✅ Requêtes de jointure testées et validées

### Commandes de vérification
```bash
# Afficher les données avec relations
php bin/console app:show-data

# Vérifier le schéma
php bin/console doctrine:schema:validate

# Voir la structure SQL
php bin/console doctrine:schema:update --dump-sql
```

---

## 📝 Conclusion

La jointure entre les tables `ordonnance` et `traitement` est **complètement fonctionnelle** :
- Relation OneToMany/ManyToOne bidirectionnelle
- Foreign Key avec CASCADE DELETE
- Données de test insérées et vérifiées
- Accès aux données via les méthodes Doctrine

Le module est prêt pour l'implémentation des contrôleurs et services !
