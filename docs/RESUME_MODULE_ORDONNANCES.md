# Résumé - Module Ordonnances & Suivi Médical

## ✅ Ce qui a été fait

### 1. Base de données
- ✅ Table `ordonnance` créée avec 9 colonnes
- ✅ Table `traitement` créée avec 11 colonnes
- ✅ **Relation OneToMany/ManyToOne** entre Ordonnance et Traitement
- ✅ **Foreign Key** : traitement.ordonnance_id → ordonnance.id (CASCADE DELETE)
- ✅ Tables `client` et `utilisateur` supprimées (gérées par d'autres modules)
- ✅ Index optimisés pour les requêtes fréquentes

### 2. Entités PHP (Symfony)
- ✅ `src/Entity/Ordonnance.php` - Gestion des ordonnances
  - Collection de traitements (OneToMany)
  - Méthodes addTraitement() et removeTraitement()
- ✅ `src/Entity/Traitement.php` - Gestion des traitements
  - Relation ManyToOne vers Ordonnance
  - Méthodes getOrdonnance() et setOrdonnance()
- ✅ Validation des données avec annotations Symfony
- ✅ Relations simplifiées (client_id et validated_by_id en INT)

### 3. Repositories
- ✅ `src/Repository/OrdonnanceRepository.php`
  - Méthodes pour trouver les ordonnances par statut
  - Méthodes pour trouver les ordonnances par client_id
  - Comptage des ordonnances en attente
- ✅ `src/Repository/TraitementRepository.php`
  - Méthodes pour trouver les traitements actifs
  - Méthodes pour trouver les traitements par client_id

### 4. Commandes Symfony
- ✅ `src/Command/LoadTestDataCommand.php` - Charge des données de test
- ✅ `src/Command/ShowDataCommand.php` - Affiche les données avec relations

### 5. Documentation
- ✅ `docs/database_schema_ordonnance_traitement.md` - Documentation complète
- ✅ `docs/create_tables.sql` - Script SQL de création
- ✅ `docs/insert_test_data.sql` - Script SQL d'insertion
- ✅ `docs/RESUME_MODULE_ORDONNANCES.md` - Ce fichier

### 6. Données de test
- ✅ 5 ordonnances insérées (2 en attente, 2 validées, 1 rejetée)
- ✅ 5 traitements insérés (4 actifs, 1 complété)
- ✅ Relations fonctionnelles entre ordonnances et traitements

---

## 📊 Structure des tables

### Table `ordonnance`
```
id (PK)
client_id (INT) → Référence externe
validated_by_id (INT) → Référence externe
file_name
file_path
status (pending_validation | validated | rejected)
rejection_reason
uploaded_at
validated_at
```

### Table `traitement`
```
id (PK)
ordonnance_id (FK) → ordonnance.id (CASCADE DELETE)
client_id (INT) → Référence externe
dosage
frequency
duration_days
start_date
end_date
is_active
is_completed
notes
```

---

## 🔗 Relations

- **Ordonnance → Traitement** : OneToMany (une ordonnance peut avoir plusieurs traitements)
- **Traitement → Ordonnance** : ManyToOne (un traitement appartient à une ordonnance)
- **Cascade DELETE** : Supprimer une ordonnance supprime automatiquement ses traitements
- **Client externe** : Les deux tables référencent client_id (géré par un autre module)
- **Utilisateur externe** : Ordonnance référence validated_by_id (géré par un autre module)

---

## 🎯 Fonctionnalités disponibles

### Ordonnances
1. Téléversement de fichiers (PDF, JPG, PNG)
2. Validation/Rejet par pharmacien
3. Historique par client
4. Filtrage par statut
5. Comptage des ordonnances en attente
6. **Accès aux traitements via $ordonnance->getTraitements()**

### Traitements
1. Création après validation d'ordonnance
2. Suivi des traitements actifs
3. Marquage comme complété
4. Calcul automatique de la date de fin
5. Historique par client
6. **Accès à l'ordonnance via $traitement->getOrdonnance()**

---

## 🔧 Commandes utiles

```bash
# Charger les données de test
php bin/console app:load-test-data

# Afficher les données avec relations
php bin/console app:show-data

# Vérifier le schéma
php bin/console doctrine:schema:validate

# Voir les entités mappées
php bin/console doctrine:mapping:info

# Créer une migration
php bin/console doctrine:migrations:diff

# Exécuter les migrations
php bin/console doctrine:migrations:migrate
```

---

## 📝 Exemples d'utilisation

### Créer une ordonnance avec des traitements
```php
$ordonnance = new Ordonnance();
$ordonnance->setClientId(1);
$ordonnance->setFileName('ordonnance.pdf');
// ... autres propriétés

$traitement = new Traitement();
$traitement->setDosage('500mg');
$traitement->setFrequency('3 fois par jour');
// ... autres propriétés

$ordonnance->addTraitement($traitement);
$entityManager->persist($ordonnance);
$entityManager->flush();
```

### Récupérer les traitements d'une ordonnance
```php
$ordonnance = $ordonnanceRepository->find(2);
foreach ($ordonnance->getTraitements() as $traitement) {
    echo $traitement->getDosage();
}
```

### Récupérer l'ordonnance d'un traitement
```php
$traitement = $traitementRepository->find(1);
$ordonnance = $traitement->getOrdonnance();
echo $ordonnance->getFileName();
```

---

## 📌 Notes importantes

- Ce module est **indépendant** des modules Client et Utilisateur
- Les `client_id` et `validated_by_id` sont des entiers simples
- L'intégration avec les autres modules se fera via les IDs
- Les fichiers d'ordonnances doivent être stockés dans `var/uploads/prescriptions/`
- **La relation Ordonnance-Traitement est bidirectionnelle et fonctionnelle**
