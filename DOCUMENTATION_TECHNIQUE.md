# Documentation Technique — CuraVita
## Système de Gestion de Pharmacie avec IA et Services Avancés

---

## 1. Architecture Générale

### Pattern MVC (Model-View-Controller)

Le projet suit strictement le pattern **MVC** (Model-View-Controller) :

```
src/main/
├── java/org/example/
│   ├── model/          → Entités métier (POJO)
│   ├── service/        → Couche d'accès aux données (CRUD SQL)
│   ├── controller/     → Logique UI JavaFX (FXML controllers)
│   └── util/           → Services transversaux (IA, API, QR, PDF...)
└── resources/
    ├── fxml/           → Vues déclaratives JavaFX
    └── css/            → Styles de l'interface
```

### Choix techniques fondamentaux

| Technologie | Version | Rôle |
|---|---|---|
| Java | 21 (LTS) | Langage principal |
| JavaFX | 21.0.2 | Interface graphique desktop |
| Maven | 3.9.7 | Gestion des dépendances et build |
| MariaDB/MySQL | 10.4 | Base de données relationnelle |
| JDBC | mysql-connector-j 8.2.0 | Connexion Java ↔ MySQL |

### Pattern Singleton

Tous les services utilisent le **pattern Singleton** (instance unique partagée) :

```java
public static ServiceX getInstance() {
    if (instance == null) instance = new ServiceX();
    return instance;
}
```

Pourquoi : éviter les connexions multiples à la base, partager l'état (ex: serveur HTTP déjà démarré), économiser la mémoire.

---

## 2. Couche Modèle (model/)

### Entités principales

- **Ordonnance** : `id_ordonnance`, `numero_ordonnance`, `date_ordonnance`, `date_expiration`, `statut`, `note_medical`, `id_utilisateur_id`, `signature_medecin`, `signature_date`, `signature_patient`, `signature_patient_date`
- **Traitement** : `id_traitement`, `id_utilisateur_id`, `dosage`, `frequence`, `duree_jours`, `date_debut`, `date_fin`, `status`, `notes`, `id_ordonnance_id`, `id_produit_id`, `repas`
- **Produit** : `id_produit`, `nom`, `description`, `prix`, `quantite_stock`, `date_expiration`, `categorie`, `statut`
- **User** : `id_utilisateur`, `nom`, `prenom`, `email`, `telephone`, `role`

### Relation clé

```
Ordonnance (1) ──── (N) Traitement (N) ──── (1) Produit
     │
     └── (1) User (patient)
```

Un traitement est toujours lié à une ordonnance ET à un produit (médicament).

---

## 3. Couche Service (service/)

### OrdonnanceService

CRUD complet sur la table `ordonnance` via **JDBC PreparedStatement**.

Méthodes :
- `add(Ordonnance)` → INSERT avec récupération de la clé générée (`RETURN_GENERATED_KEYS`)
- `update(Ordonnance)` → UPDATE par `id_ordonnance`
- `delete(int id)` → DELETE
- `getAll()` → SELECT ORDER BY date DESC
- `getById(int id)` → SELECT WHERE id
- `search(String query)` → filtre en mémoire sur numéro, statut, note

**Choix technique** : `PreparedStatement` plutôt que `Statement` pour prévenir les injections SQL.

### TraitementService

Même structure que OrdonnanceService. Méthode supplémentaire :
- `getByOrdonnanceId(int ordonnanceId)` → récupère tous les traitements d'une ordonnance (jointure logique)

**Mapping** : `mapResultSet(ResultSet rs)` convertit chaque ligne SQL en objet Java `Traitement` avec gestion des `null` sur les timestamps (`Timestamp.toLocalDateTime()`).

---

## 4. Services Utilitaires Avancés (util/)

### 4.1 DatabaseUtil — Connexion Singleton

**Rôle** : fournit une connexion JDBC unique et réutilisable.

```
URL : jdbc:mysql://localhost:3306/pharmacie
     ?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
User : root / Password : (vide, XAMPP local)
```

**Choix** : connexion lazy (créée à la première demande), reconnexion automatique si fermée (`connection.isClosed()`).

---

### 4.2 OpenFDAService — API Médicamenteuse Externe

**Rôle** : récupère les effets secondaires, contre-indications et interactions d'un médicament via l'API publique de la FDA américaine.

**API utilisée** : `https://api.fda.gov/drug/label.json`

**Stratégie de recherche en cascade** (5 niveaux) :
1. Base locale (médicaments français/marocains connus) — priorité absolue
2. Recherche par `openfda.brand_name` exact
3. Recherche par `openfda.generic_name` exact
4. Recherche par brand_name normalisé (sans accents)
5. Recherche libre (full-text)

**Normalisation** : `java.text.Normalizer.normalize(nom, NFD)` + suppression des diacritiques (`\p{InCombiningDiacriticalMarks}`) + toLowerCase. Permet de matcher "Aspégic" → "aspegic".

**Base locale embarquée** : 12 familles de médicaments codées en dur avec données issues de `drugs.com`, `medicines.org.uk (emc)`, `NIH/NLM` :
- Paracétamol (Doliprane, Panadol, Efferalgan, Dafalgan, Tylenol...)
- Ibuprofène (Advil, Nurofen, Brufen, Motrin...)
- Aspirine (Aspégic, Kardégic, Cardioaspirine...)
- Amoxicilline (Clamoxyl, Augmentin...)
- Azithromycine (Zithromax...)
- Oméprazole (Mopral, Pantoprazole, Losec...)
- Cétirizine (Zyrtec, Virlix...) / Loratadine (Clarityne...)
- Metformine (Glucophage, Stagid...)
- Tramadol (Topalgic, Contramal, Zamudol...)
- Warfarine (Coumadine...)
- Simvastatine/Atorvastatine (Zocor, Tahor...)
- Salbutamol/Aerol (Ventoline, Albuterol...)

**Parsing JSON manuel** : pas de bibliothèque JSON externe — extraction par `indexOf` sur les patterns `"champ":["valeur"]` et `"champ": ["valeur"]`. Troncature à 350 caractères.

**Classe interne DrugInfo** : DTO (Data Transfer Object) avec 3 champs String : `effetsSecondaires`, `contreIndications`, `interactions`.

---

### 4.3 DrugInteractionService — Détection d'Interactions et Allergies

**Rôle** : détecte les interactions médicamenteuses dangereuses et les allergies du patient.

#### Interactions locales (règles codées)

15 paires de médicaments incompatibles :
```
aspirine ↔ ibuprofène, warfarine ↔ aspirine, metformine ↔ alcool,
paracétamol ↔ alcool, amoxicilline ↔ méthotrexate,
simvastatine ↔ érythromycine, digoxine ↔ amiodarone,
lithium ↔ ibuprofène, clopidogrel ↔ oméprazole,
fluoxétine ↔ tramadol, sertraline ↔ tramadol...
```

#### Résolution des principes actifs

**Mapping noms commerciaux → principe actif** : 22 groupes de médicaments. Permet de détecter qu'un patient allergique au "paracétamol" ne doit pas prendre "Panadol" ou "Doliprane".

```java
resoudrePrincipeActif("Panadol") → "paracetamol"
```

#### Vérification allergie intelligente (`verifierAllergieIntelligente`)

Analyse les antécédents textuels du patient. Détecte les mots-clés : `allergi`, `intoleran`, `reaction allergique`, `hypersensib`, `choc anaphylactique`. Retourne un `AllergieResult` avec :
- `critique` (boolean) : bloquant ou non
- `problemes` (List) : liste des problèmes détectés
- `recommandation` : texte affiché au médecin
- `alternativeSuggestion` : médicament alternatif suggéré

#### Interactions via OpenFDA API (`verifierInteractionsOpenFDA`)

Endpoint : `https://api.fda.gov/drug/event.json` — recherche des événements indésirables signalés pour une paire de médicaments. Timeout : 3 secondes.

#### Méthode `verifierTout`

Orchestre les 3 niveaux de vérification :
1. Allergie intelligente (antécédents textuels)
2. Interactions avec traitements actifs en base (SQL)
3. Interactions avec produits déjà sélectionnés dans le formulaire + OpenFDA

---

### 4.4 TreatmentSuggestionService — IA de Suggestion de Traitement

**Rôle** : suggère automatiquement dosage, fréquence, moment de prise et durée pour un médicament.

**Algorithme en 2 étapes** :

**Étape 1 — Apprentissage par historique (Machine Learning simplifié)**

Requête SQL d'agrégation :
```sql
SELECT dosage, frequence, repas, duree_jours, COUNT(*) AS nb
FROM traitement
WHERE id_produit_id = ? AND status IN ('actif', 'terminé')
AND dosage IS NOT NULL AND dosage != ''
GROUP BY dosage, frequence, repas, duree_jours
ORDER BY nb DESC LIMIT 1
```
→ Retourne la combinaison la plus prescrite pour ce médicament. Source indiquée : `"historique (N cas similaires)"`.

**Étape 2 — Règles médicales par défaut (Knowledge Base)**

Si aucun historique : règles codées par famille de médicament + ajustement selon les symptômes du patient :
- `"chronique"` ou `"longue durée"` dans les notes → durée = 30 jours
- `"aigu"` ou `"urgent"` → durée = 3 jours
- `"allergi"` ou `"gastrite"` avec ibuprofène → repas = "Après le repas"

**Classe Suggestion** : DTO avec `dosage`, `frequence`, `repas`, `dureeJours`, `source`.

---

### 4.5 AdherencePredictor — Prédiction de Non-Adhérence

**Rôle** : calcule un score de risque de non-adhérence au traitement pour chaque patient.

**Modèle de scoring pondéré** :

| Facteur | Poids | Calcul |
|---|---|---|
| Taux d'abandon | 45% | `traitementsAbandonnes / totalTraitements × 0.45` |
| Ordonnances expirées sans complétion | max 30% | `nb × 0.15` (plafonné) |
| Traitements en attente > 30 jours | max 25% | `nb × 0.10` (plafonné) |

**Score final** : entre 0.0 et 1.0 (plafonné à 1.0)
- `>= 0.6` → Niveau **Élevé**
- `>= 0.3` → Niveau **Modéré**
- `< 0.3` → Niveau **Faible**

**Requêtes SQL utilisées** :
- Traitements abandonnés : `status = 'annulé'`
- Ordonnances expirées sans complétion : jointure `ordonnance` + `traitement` avec `statut = 'expirée'` et `status != 'terminé'`
- Traitements en attente trop longs : `DATEDIFF(NOW(), date_debut) > 30`

---

### 4.6 QRCodeService — Génération de QR Code

**Bibliothèque** : Google ZXing (Zebra Crossing) 3.5.3

**Paramètres d'encodage** :
- Format : `BarcodeFormat.QR_CODE`
- Correction d'erreur : `ErrorCorrectionLevel.M` (15% de récupération)
- Marge : 2 modules
- Charset : UTF-8

**Rendu JavaFX** : `WritableImage` + `PixelWriter` — pixel par pixel depuis la `BitMatrix` ZXing. Noir = module QR, Blanc = fond.

**Contenu encodé** : texte structuré lisible (numéro ordonnance, patient, date, liste des traitements avec dosage/fréquence/repas/durée).

**DTO TraitementInfo** : objet de transfert pour passer les données traitement au service QR sans dépendance sur le modèle.

---

### 4.7 QRPdfServerService — Serveur HTTP Embarqué + Génération PDF

**Rôle** : génère un PDF médical complet et le sert via un mini serveur HTTP intégré à l'application, accessible depuis n'importe quel téléphone sur le même réseau WiFi.

#### Serveur HTTP (fait maison, sans framework)

- Port : **8765**
- `ServerSocket` Java standard
- `ExecutorService` avec `newCachedThreadPool` (threads daemon)
- Protocole HTTP/1.1 minimal : parse la ligne `GET /fichier.pdf HTTP/1.1`
- Headers de réponse : `Content-Type: application/pdf`, `Content-Disposition: inline`, `Content-Length`

#### Détection IP réseau (fix APIPA)

Problème : `InetAddress.getLocalHost()` retourne parfois `169.254.x.x` (adresse APIPA — auto-assignée sans DHCP).

Solution en 2 étapes :
1. `DatagramSocket` connecté à `8.8.8.8:80` (pas de trafic réel) → retourne l'IP de l'interface réseau active
2. Fallback : parcours des `NetworkInterface` en filtrant loopback, APIPA (`169.254.*`), IPv6

#### Génération PDF (iText 7)

**Bibliothèque** : iText 7.2.5

Structure du PDF :
- En-tête : titre + sous-titre + séparateur `SolidLine`
- Tableau infos ordonnance : `Table` 2 colonnes (label/valeur)
- Tableau traitements : une `Table` par médicament avec en-tête coloré vert
- Bloc sensibilisation dosage : `Cell` avec bordure gauche orange (`SolidBorder`)
- Images de sensibilisation : chargées depuis `/resources/images/` via `getResourceAsStream`
- Fallback texte si images absentes : 7 conseils sur fond bleu clair
- Pied de page : date de génération

**Couleurs** : `DeviceRgb` (RGB custom) — vert `(31,111,92)`, orange `(230,126,34)`, bleu `(41,128,185)`.

**Stockage** : dossier temporaire `Files.createTempDirectory("curavita_qr")` — recréé à chaque démarrage.

---

### 4.8 ElectronicSignatureService — Signature Électronique

**Rôle** : génère et stocke une signature électronique pour les ordonnances (médecin et patient).

**Algorithme** : SHA-256 sur un payload structuré :
```
CURAVITA|{numeroOrdonnance}|{signataire}|{role}|{timestamp}|{signatureData}
```
Hash encodé en **Base64** (URL-safe).

**Stockage en base** : format `"NomSignataire|2026-04-28 10:30:00|HASH..."` dans les colonnes :
- `signature_medecin` (varchar 255) + `signature_date` (datetime)
- `signature_patient` (longtext) + `signature_patient_date` (datetime)

**Vérification** : `verifierSignatures(ordonnanceId)` retourne `boolean[2]` — [0] médecin signé, [1] patient signé.

---

### 4.9 AuditService — Traçabilité Complète

**Rôle** : enregistre chaque action CRUD dans la table `audit_log` pour traçabilité réglementaire.

**Table `audit_log`** (existante en base) :
- `entity_type` : "ordonnance" ou "traitement"
- `entity_id` : ID de l'enregistrement
- `action` : "CRÉATION", "MODIFICATION", "SUPPRESSION"
- `changed_fields` : champ modifié (JSON string)
- `old_values` / `new_values` : valeurs avant/après (JSON string)
- `user_name` : nom de l'admin
- `created_at` : horodatage

**Méthodes utilitaires** :
- `logCreation` / `logSuppression` : raccourcis
- `logSiModifie(champ, avant, apres)` : ne logue que si la valeur a changé (comparaison String)
- `getHistorique(entite, id)` : récupère l'historique trié par ID DESC

---

### 4.10 EmailService — Notifications Email

**Protocole** : SMTP avec STARTTLS (chiffrement opportuniste)
**Serveur** : `smtp.gmail.com:587`
**Bibliothèque** : Jakarta Mail 2.0.1 (anciennement JavaMail)

**Authentification** : `Authenticator` avec `PasswordAuthentication` — mot de passe d'application Gmail (pas le mot de passe principal).

**Envoi asynchrone** : dans un `Thread` séparé pour ne pas bloquer l'UI JavaFX.

**Format** : HTML (`text/html; charset=UTF-8`) — permet des emails mis en forme.

---

### 4.11 SmsService — Notifications SMS

**API** : Twilio REST API v2010
**Bibliothèque** : twilio-java SDK 9.14.0

**Numéro émetteur** : numéro Twilio Trial `+17122157412`
**Fallback** : si le numéro destinataire est vide → envoi au numéro vérifié de test

**Normalisation E.164** : `formatNumber()` gère les formats tunisiens :
- `26581955` → `+21626581955`
- `026581955` → `+21626581955`
- `00216XXXXXXXX` → `+216XXXXXXXX`

**Envoi asynchrone** : thread daemon pour ne pas bloquer l'UI.

**Gestion d'erreurs Twilio** :
- Code `21608` : numéro non vérifié (compte Trial)
- Code `21211` : numéro invalide
- Code `20003` : credentials incorrects

---

## 5. Logique Métier Avancée (Controllers)

### Anti-Spam Ordonnances

Détection de soumissions répétées dans `TraitementController` :
- Blocage si même ordonnance soumise plusieurs fois en peu de temps
- Dialog d'avertissement avec délai de 24h
- `showSpamBlockDialog()` / `showSpamBlockDialog24h()`

### Détection d'Interactions en Temps Réel

À chaque sélection d'un médicament dans le ComboBox :
1. Appel `OpenFDAService.getInfo()` en thread daemon (non-bloquant)
2. Affichage des effets secondaires, contre-indications, interactions
3. Vérification `DrugInteractionService.verifierInteractionsLocales()` avec les produits déjà sélectionnés
4. Mise à jour de `fdaInfoBox` via `Platform.runLater()` (thread UI JavaFX)

### Recommandation IA

Bouton "Recommandation IA" dans le formulaire traitement :
- Appel `TreatmentSuggestionService.suggerer(produitId, symptomes)`
- Pré-remplissage automatique des champs dosage/fréquence/repas/durée
- Affichage de la source ("historique N cas" ou "règles médicales")

### Génération QR Code + PDF

À la soumission d'une ordonnance :
1. `QRPdfServerService.genererPdfEtGetUrl()` → génère le PDF et retourne l'URL
2. `QRCodeService.generateQRImage()` → encode l'URL en QR code JavaFX Image
3. Affichage du QR dans l'interface
4. Scan depuis téléphone → ouvre le PDF dans le navigateur mobile

---

## 6. Dépendances Maven (pom.xml)

| Dépendance | Version | Usage |
|---|---|---|
| `org.openjfx:javafx-controls` | 21.0.2 | Composants UI (Button, ComboBox, TableView...) |
| `org.openjfx:javafx-fxml` | 21.0.2 | Chargement des fichiers FXML |
| `org.openjfx:javafx-graphics` | 21.0.2 | Rendu graphique, WritableImage |
| `org.openjfx:javafx-base` | 21.0.2 | ObservableList, propriétés réactives |
| `com.mysql:mysql-connector-j` | 8.2.0 | Driver JDBC MySQL/MariaDB |
| `org.mindrot:jbcrypt` | 0.4 | Hachage BCrypt des mots de passe |
| `com.twilio.sdk:twilio` | 9.14.0 | Envoi SMS via API Twilio |
| `com.itextpdf:itext7-core` | 7.2.5 | Génération PDF (ordonnances) |
| `com.sun.mail:jakarta.mail` | 2.0.1 | Envoi emails SMTP |
| `com.google.zxing:core` | 3.5.3 | Encodage QR Code (BitMatrix) |
| `com.google.zxing:javase` | 3.5.3 | Utilitaires ZXing pour Java SE |

---

## 7. Glossaire des Termes Techniques

| Terme | Définition |
|---|---|
| **MVC** | Model-View-Controller — séparation des responsabilités : données, affichage, logique |
| **Singleton** | Pattern de conception garantissant une seule instance d'une classe |
| **JDBC** | Java Database Connectivity — API standard Java pour accéder aux bases de données |
| **PreparedStatement** | Requête SQL paramétrée, protège contre les injections SQL |
| **FXML** | Format XML déclaratif pour définir les interfaces JavaFX |
| **DTO** | Data Transfer Object — objet simple pour transporter des données entre couches |
| **POJO** | Plain Old Java Object — classe Java simple sans dépendances framework |
| **API REST** | Interface web standardisée (HTTP GET/POST) pour échanger des données JSON |
| **OpenFDA** | API publique de la FDA (Food and Drug Administration) américaine sur les médicaments |
| **ZXing** | Bibliothèque Google de génération/lecture de codes-barres et QR codes |
| **BitMatrix** | Matrice binaire représentant les modules (pixels) d'un QR code |
| **iText 7** | Bibliothèque Java de génération de documents PDF |
| **SMTP** | Simple Mail Transfer Protocol — protocole d'envoi d'emails |
| **STARTTLS** | Extension SMTP pour chiffrer la connexion (port 587) |
| **Twilio** | Plateforme cloud d'envoi de SMS/appels via API REST |
| **E.164** | Format international de numéro de téléphone (+[indicatif][numéro]) |
| **SHA-256** | Algorithme de hachage cryptographique 256 bits (famille SHA-2) |
| **Base64** | Encodage binaire → texte ASCII (utilisé pour les hashes de signature) |
| **APIPA** | Automatic Private IP Addressing — adresse 169.254.x.x auto-assignée sans DHCP |
| **DatagramSocket** | Socket UDP Java — utilisé ici pour détecter l'IP réseau active sans trafic réel |
| **ExecutorService** | Pool de threads Java pour exécution asynchrone |
| **Platform.runLater** | Méthode JavaFX pour exécuter du code sur le thread UI depuis un thread background |
| **NFD** | Normalization Form D — décomposition Unicode des caractères accentués |
| **BCrypt** | Algorithme de hachage de mots de passe avec sel (salt) intégré |
| **RETURN_GENERATED_KEYS** | Flag JDBC pour récupérer l'ID auto-incrémenté après un INSERT |
| **DeviceRgb** | Classe iText 7 pour définir une couleur RGB dans un PDF |
| **WritableImage** | Image JavaFX modifiable pixel par pixel via PixelWriter |
| **ErrorCorrectionLevel.M** | Niveau de correction d'erreur QR code : 15% de données récupérables si endommagé |
| **daemon thread** | Thread Java qui s'arrête automatiquement quand l'application principale se ferme |
| **DATEDIFF** | Fonction SQL calculant la différence en jours entre deux dates |
| **audit_log** | Table de traçabilité enregistrant toutes les modifications avec horodatage |
| **Knowledge Base** | Base de connaissances — règles médicales codées en dur dans le système |
| **Score de risque** | Valeur numérique 0.0-1.0 calculée par modèle pondéré pour évaluer un risque |
| **Principe actif** | Molécule responsable de l'effet thérapeutique d'un médicament |
| **Nom commercial** | Marque déposée d'un médicament (ex: Doliprane = nom commercial du paracétamol) |


---

## 8. Fichiers FXML — Structure des Vues

### Principe FXML

FXML est un format XML déclaratif qui décrit l'interface JavaFX. Le lien entre la vue et le controller se fait via l'attribut `fx:controller` sur le nœud racine. Les éléments avec `fx:id` sont injectés automatiquement dans le controller via l'annotation `@FXML`.

```xml
<!-- FXML déclare le controller -->
<ScrollPane fx:controller="org.example.controller.TraitementController">
    <TextField fx:id="nomPrenomField"/>  <!-- injecté dans le controller -->
</ScrollPane>
```

```java
// Controller reçoit l'injection automatique
@FXML private TextField nomPrenomField;  // lié au fx:id="nomPrenomField"
```

Les actions utilisateur sont liées via `onAction="#nomMethode"` — JavaFX appelle la méthode annotée `@FXML` correspondante dans le controller.

---

### 8.1 Traitement.fxml — Formulaire de Demande de Traitement

**Racine** : `ScrollPane` (fitToWidth) → permet le scroll vertical si le contenu dépasse l'écran.

**Structure** :
```
ScrollPane
└── VBox (root)
    ├── HBox (navbar-pill)          ← barre de navigation flottante
    ├── VBox (mesord-hero)          ← section hero dégradé vert
    └── VBox (fond blanc)
        └── HBox (2 colonnes)
            ├── VBox (card) ← formulaire principal (gauche)
            │   ├── TextField fx:id="nomPrenomField"   (pré-rempli, non éditable)
            │   ├── TextField fx:id="emailField"        (pré-rempli, non éditable)
            │   ├── DatePicker fx:id="dateNaissanceField"
            │   ├── TextArea fx:id="antecedentsField"
            │   ├── TextArea fx:id="symptomesField"
            │   ├── HBox
            │   │   ├── ComboBox fx:id="produitCombo"
            │   │   └── Button "+" onAction="#addProduit"
            │   ├── Button "Recommandation IA" onAction="#handleRecommandationIA"
            │   ├── VBox fx:id="fdaInfoBox" (visible=false par défaut)
            │   │   ├── Label fx:id="fdaTitleLabel"
            │   │   ├── Label fx:id="fdaEffetsLabel"
            │   │   ├── Label fx:id="fdaContraLabel"
            │   │   └── Label fx:id="fdaInterLabel"
            │   ├── VBox fx:id="selectedProduitsBox"  (tags produits ajoutés)
            │   ├── CheckBox fx:id="conditionsCheck"
            │   ├── Button "Envoyer" onAction="#handleSubmit"
            │   └── Label fx:id="errorLabel"
            └── VBox (panneau info droite)
                ├── card "Validation Pharmaceutique"
                └── card "Informations importantes"
```

**Panneau OpenFDA** (`fdaInfoBox`) : invisible par défaut (`visible="false" managed="false"`). Devient visible dès qu'un produit est sélectionné dans le ComboBox. Style inline : fond bleu clair `#f0f8ff`, bordure bleue `#3498db`, radius 10.

**Dropdown Ordonnance** : `StackPane` contenant un `Button` et un `VBox` superposé (`ordonnanceDropdown`). Le VBox est masqué par défaut et affiché au survol via `setOnMouseEntered/Exited` dans le controller.

---

### 8.2 Ordonnance.fxml — Création d'Ordonnance

**Racine** : `ScrollPane` → `VBox`

**Éléments clés** :
- `Label fx:id="numeroBannerLabel"` : affiche le numéro auto-généré dans le bandeau vert en haut
- `TextField fx:id="numeroField"` : `editable="false"` — rempli automatiquement par le controller
- `DatePicker fx:id="dateOrdonnanceField"` / `dateExpirationField`
- `TextArea fx:id="noteMedicalField"` : message optionnel pour le pharmacien
- `VBox fx:id="traitementInfoBox"` : affiche le traitement associé (produit + statut)
- `Button "Signer mon ordonnance"` : `onAction="#handleSignerPatient"` → appelle `ElectronicSignatureService`

**Panneau droit** : 3 cartes d'information (jaune "Vérifié par un Pharmacien", blanche "Réponse Rapide", verte "Besoin d'aide ?")

**Barre verte** : `HBox styleClass="ord-green-bar"` — simple barre décorative de 6px de hauteur.

---

### 8.3 MesOrdonnances.fxml — Historique des Ordonnances

**Racine** : `ScrollPane` → `VBox`

**Éléments dynamiques** (remplis par le controller) :
- `VBox fx:id="cardsContainer"` : conteneur vide dans le FXML, rempli dynamiquement par le controller avec des cartes JavaFX construites en Java
- `PieChart fx:id="statPieChart"` : graphique camembert des statuts, masqué par défaut
- `VBox fx:id="statsContainer"` : conteneur du PieChart, toggle via `#toggleStats`
- `TextField fx:id="searchField"` : recherche en temps réel (4 derniers chiffres, numéro, produit, dosage)

**Filtres par statut** : 5 boutons (`filterAll`, `filterEnAttente`, `filterValidee`, `filterBrouillon`, `filterExpiree`) — chacun filtre la liste `cardsContainer`.

**Tri** : `Button fx:id="triButton"` toggle entre "Plus récent" et "Plus ancien".

**Dropdown profil** : `StackPane` avec `VBox fx:id="profileDropdown"` — même pattern que le dropdown ordonnance.

---

### 8.4 BackOrdonnance.fxml / BackTraitement.fxml — Back-Office Admin

**Racine** : `BorderPane` (layout principal admin)

**Structure** :
```
BorderPane
├── left: VBox (sidebar verte)
│   ├── Label "CuraVita" (sidebar-logo)
│   ├── Boutons navigation (sidebar-item / sidebar-item-active)
│   ├── Region VBox.vgrow="ALWAYS"  ← pousse les boutons du bas vers le bas
│   ├── Button "← Back" onAction="#goToDashboard"
│   └── Button "Logout" onAction="#logout"
└── center: VBox fx:id="pageContainer"  ← contenu injecté dynamiquement
```

**`pageContainer`** : VBox vide dans le FXML. Le controller (`BackOrdonnanceController`, `BackTraitementController`) y injecte dynamiquement les tableaux, formulaires et panneaux d'audit via Java.

**`sidebar-item-active`** : classe CSS différente pour l'item actif (fond blanc semi-transparent + bold).

**`Region VBox.vgrow="ALWAYS"`** : composant invisible qui prend tout l'espace disponible, poussant les boutons "Back" et "Logout" en bas de la sidebar.

---

### 8.5 Dashboard.fxml — Tableau de Bord Admin

**Racine** : `BorderPane` → sidebar gauche + `ScrollPane` central

**Statistiques** : `GridPane` 6 colonnes avec des `VBox styleClass="stat-card"` contenant :
- `Label` statique (titre)
- `Label fx:id="totalClientsLabel"` etc. (valeur mise à jour par le controller)

**Actions rapides** : `HBox` avec 5 boutons `button-primary` déclenchant des handlers.

---

## 9. Styles CSS — Système de Design

### Palette de couleurs principale

| Couleur | Valeur hex | Usage |
|---|---|---|
| Vert principal | `#1f6f5c` | Boutons primaires, sidebar, logo, accents |
| Vert clair | `#258562` | Hover des boutons verts |
| Vert foncé | `#174f42` | Pressed / hover foncé |
| Orange | `#e67e22` / `#f39c12` | Bouton submit ordonnance, badges, alertes |
| Rouge | `#E74C3C` | Labels champs obligatoires, erreurs, bouton submit traitement |
| Bleu | `#2980b9` | Signature électronique, panneau OpenFDA |
| Violet | `#6c3483` | Bouton Recommandation IA |
| Gris | `#6c757d` | Bouton annuler, textes secondaires |

### Système de classes CSS

#### Composants globaux

**`.card`** : carte blanche avec ombre portée
```css
-fx-background-color: white;
-fx-background-radius: 20;
-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0.4, 0, 4);
```
Hover : ombre plus prononcée (`rgba(0,0,0,0.15)`).

**`.navbar-pill`** : barre de navigation flottante arrondie
```css
-fx-background-color: white;
-fx-background-radius: 40;
-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0.5, 0, 5);
```

**`.lol-error` / `.error-label`** : texte d'erreur rouge `#E74C3C`, 12px.

#### Sidebar (back-office)

**`.sidebar`** : fond vert `#1F6F54`, padding `20 15`, largeur min 200px.

**`.sidebar-item`** : bouton transparent, texte blanc, hover fond blanc 10% opacité.

**`.sidebar-item-active`** : fond blanc 20% opacité + bold — indique la page courante.

**`.sidebar-button-bottom`** : fond blanc 10% opacité — boutons Back/Logout.

#### Page Traitement

**`.traitement-field-label`** : label rouge `#E74C3C`, bold 14px — attire l'attention sur les champs obligatoires.

**`.traitement-input`** : fond blanc, bordure grise `#ddd`, radius 8. Focus : bordure verte `#1f6f5c` épaisseur 2.

**`.traitement-btn-submit`** : rouge `#E74C3C`, radius 25, bold 15px, padding `12 35`.

**`.traitement-btn-ia`** : violet `#6c3483`, radius 10 — visuellement distinct pour signaler la fonctionnalité IA.

**`.traitement-btn-add`** : bouton circulaire vert (`border-radius: 50`), 36×36px, texte "+".

**`.traitement-produit-tag`** : tag vert clair `#e8f5e9` avec bordure verte, radius 20 — affiche les médicaments sélectionnés.

**`.traitement-btn-remove`** : bouton circulaire rouge 24×24px, texte "×" — supprime un tag produit.

#### Page Ordonnance

**`.ord-header-banner`** : dégradé vertical vert foncé → vert, radius haut seulement (`15 15 0 0`).

**`.ord-green-bar`** : barre décorative 6px hauteur, vert `#1f6f5c`.

**`.ord-btn-submit`** : dégradé horizontal orange `#e67e22` → `#f39c12`, radius 25.

**`.ord-info-card-yellow`** : fond jaune clair `#fff3cd`, bordure `#ffc107` — carte "Vérifié par un Pharmacien".

#### Page Mes Ordonnances

**`.mesord-hero`** : dégradé diagonal vert foncé → vert → vert clair, padding 60px, hauteur min 220px.

**`.mesord-badge`** : badge orange arrondi `rgba(243,156,18,0.9)`, texte blanc — ex: "🩺 Suivi Médical".

**`.mesord-badge-outline`** : badge contour vert (transparent + bordure), texte vert.

**`.mesord-filter-btn`** : bouton contour vert, hover → fond vert texte blanc (inversion).

**`.mesord-search`** : champ de recherche arrondi (radius 25), focus bordure verte épaisseur 2.

**`.mesord-card`** : carte blanche radius 15, ombre légère. Hover : ombre plus forte.

**Badges de statut** :
- `.mesord-stat-attente` : fond jaune `#fff3cd`, texte `#856404`
- `.mesord-stat-validee` : fond vert `#d4edda`, texte `#155724`
- `.mesord-stat-brouillon` : fond gris `#e2e3e5`, texte `#383d41`

#### Page Login (thème "lol")

**`.lol-root`** : dégradé diagonal vert `#1F6F54` → `#2E8B57`.

**`.lol-card`** : carte blanche avec ombre forte (`rgba(0,0,0,0.4)`), radius 20.

**`.sliding-panel`** : panneau vert `#1F6F54` qui glisse (animation Java) pour basculer login/register.

**`.lol-input`** : fond gris clair `#f5f7fa`, radius 10. Focus : fond blanc + bordure verte 2px.

**`.lol-button-primary`** : vert `#1F6F54`, radius 20, letter-spacing 2. Pressed : scale 0.98.

**`.lol-button-panel`** : transparent + bordure blanche — bouton sur le panneau vert.

**`.floating-circle`** : cercles décoratifs animés (opacité 0 → animés en Java).

#### Stat Cards (Dashboard)

**`.stat-card`** : carte blanche radius 10, padding `15 25`, ombre légère.

**`.stat-value`** : 24px bold vert `#1F6F54`.

**`.stat-card-mini-admin`** : fond vert `#1F6F54` — variante admin inversée.

---

## 10. Communication entre Couches — Flux de Données

### Schéma général

```
FXML (vue déclarative)
    │  fx:id → injection @FXML
    │  onAction → appel méthode @FXML
    ▼
Controller (logique UI)
    │  getInstance() → appel Singleton
    ├──► Service (CRUD SQL)
    │       │  JDBC PreparedStatement
    │       ▼
    │    Base de données MySQL
    │
    ├──► util/OpenFDAService      (API externe HTTP)
    ├──► util/DrugInteractionService (règles locales + OpenFDA)
    ├──► util/TreatmentSuggestionService (SQL + règles)
    ├──► util/AdherencePredictor  (SQL)
    ├──► util/QRCodeService       (ZXing)
    ├──► util/QRPdfServerService  (iText + ServerSocket)
    ├──► util/ElectronicSignatureService (SHA-256 + SQL)
    ├──► util/AuditService        (SQL audit_log)
    ├──► util/EmailService        (SMTP Jakarta Mail)
    ├──► util/SmsService          (Twilio API)
    └──► util/DialogService       (popups JavaFX)
```

### Flux détaillé : sélection d'un médicament dans le ComboBox

```
1. Utilisateur sélectionne "2 - Aspegic" dans produitCombo
   │
2. produitCombo.valueProperty().addListener() déclenché
   │  (TraitementController.initialize())
   │
3. Extraction du nom : "2 - Aspegic".split(" - ", 2)[1] → "Aspegic"
   │
4. Nouveau Thread daemon démarré (non-bloquant UI)
   │
5. Thread appelle OpenFDAService.getInstance().getInfo("Aspegic")
   │   ├── normaliser("Aspegic") → "aspegic"
   │   ├── getInfoLocale("aspegic") → match "aspegic" → retourne DrugInfo
   │   └── retourne DrugInfo {effetsSecondaires, contreIndications, interactions}
   │
6. Platform.runLater(() -> { ... })  ← retour sur le thread UI JavaFX
   │   ├── fdaTitleLabel.setText("ℹ️ Informations OpenFDA : Aspegic")
   │   ├── fdaEffetsLabel.setText(info.effetsSecondaires)
   │   ├── fdaContraLabel.setText(info.contreIndications)
   │   ├── Pour chaque produit déjà sélectionné :
   │   │   └── DrugInteractionService.verifierInteractionsLocales(nomProduit, userId)
   │   │       → SQL : SELECT produits actifs du patient → compare avec INTERACTIONS_LOCALES
   │   ├── fdaInterLabel.setText(interText)
   │   └── fdaInfoBox.setVisible(true) + setManaged(true)
```

### Flux détaillé : clic sur "+" (ajout d'un médicament)

```
1. Utilisateur clique sur Button "+" → onAction="#addProduit"
   │
2. TraitementController.addProduit()
   │
3. Extraction nomProduit depuis produitCombo.getValue()
   │
4. DrugInteractionService.getInstance().verifierAllergieIntelligente(nomProduit, antecedents)
   │   ├── Analyse antecedents textuels (mots-clés allergie)
   │   ├── resoudrePrincipeActif(nomProduit) → principe actif
   │   └── Retourne AllergieResult {critique, problemes, recommandation, alternativeSuggestion}
   │
5a. Si allergie critique → showAllergieBloquanteDialog()
    │   └── Stage modal APPLICATION_MODAL (bloquant)
    │       Construit entièrement en Java (pas de FXML)
    │       Affiche : icône ✕, problèmes, recommandation, alternative IA
    └── return (produit non ajouté)
   │
5b. Sinon → DrugInteractionService.verifierTout(nomProduit, antecedents, userId, selectedProduits)
    │   ├── verifierAllergie() (simple)
    │   ├── verifierInteractionsLocales() (SQL traitements actifs)
    │   └── Pour chaque produit déjà sélectionné :
    │       └── verifierInteractionsOpenFDA() (API FDA events)
    │
6a. Si alertes → DialogService.showInteractionWarning(nomProduit, alertes)
    │   └── Stage non-bloquant avec liste des alertes + bouton "Continuer quand même"
    │   Si confirmé → selectedProduits.add(selected) + refreshProduitsBox()
    │
6b. Sinon → selectedProduits.add(selected) + refreshProduitsBox()
```

### Flux détaillé : clic sur "Recommandation IA"

```
1. Utilisateur clique → onAction="#handleRecommandationIA"
   │
2. TraitementController.handleRecommandationIA()
   │
3. Extraction mots-clés des symptômes (mots > 3 lettres)
   │
4. SQL : SELECT produits les plus prescrits pour ces symptômes
   │   JOIN traitement + produit + ordonnance
   │   WHERE LOWER(note_medical) LIKE '%mot%' OR ...
   │   GROUP BY produit ORDER BY COUNT DESC LIMIT 10
   │
5. Fallback si vide : produits les plus prescrits en général
   │
6. Pour chaque candidat :
   │   DrugInteractionService.verifierAllergieIntelligente(candidat, antecedents)
   │   → Premier candidat non-allergène sélectionné
   │
7. selectedProduits.add(meilleur) + refreshProduitsBox()
   │
8. showIAConfirmation(produitNom, nbPrescriptions, symptomes)
   │   └── Stage non-bloquant (Modality.NONE)
   │       Fermeture automatique après 4 secondes (PauseTransition)
```

### Flux détaillé : soumission du formulaire traitement

```
1. Utilisateur clique "Envoyer la demande" → onAction="#handleSubmit"
   │
2. Validations :
   │   ├── Champs obligatoires non vides
   │   ├── Antécédents min 5 chars
   │   ├── Au moins 1 produit sélectionné
   │   └── conditionsCheck coché
   │
3. Anti-spam : vérification délai depuis dernière soumission
   │   → showSpamBlockDialog() si trop rapide
   │
4. Vérification allergies sur tous les produits sélectionnés
   │   → showAllergiesSoumissionDialog() si critique
   │
5. Pour chaque produit sélectionné :
   │   TreatmentSuggestionService.getInstance().suggerer(produitId, symptomes)
   │   ├── chercherDansHistorique(produitId) → SQL GROUP BY + COUNT
   │   └── reglesParDefaut(produitId, symptomes) → knowledge base
   │   → Retourne Suggestion {dosage, frequence, repas, dureeJours, source}
   │
6. TraitementService.getInstance().add(traitement)
   │   → INSERT INTO traitement (PreparedStatement)
   │
7. AuditService.getInstance().logCreation("traitement", id, resume, user)
   │   → INSERT INTO audit_log
   │
8. QRPdfServerService.getInstance().genererPdfEtGetUrl(numero, patient, traitements)
   │   ├── genererPdf() → iText 7 → fichier dans tempDir
   │   └── getLocalIP() + PORT → URL http://IP:8765/ordonnance_XXX.pdf
   │
9. QRCodeService.getInstance().generateQRImage(url, 300)
   │   → ZXing BitMatrix → WritableImage JavaFX
   │
10. EmailService.getInstance().send(email, sujet, htmlBody)
    │   → Thread séparé → SMTP Gmail
    │
11. SmsService.getInstance().send(telephone, message)
    │   → Thread daemon → Twilio API
    │
12. Navigation vers Ordonnance.fxml (FXMLLoader)
    │   → Passage du traitementId via UserService ou champ statique
```

### Flux détaillé : signature électronique

```
1. Utilisateur clique "Signer mon ordonnance" → onAction="#handleSignerPatient"
   │
2. OrdonnanceController.handleSignerPatient()
   │
3. ElectronicSignatureService.getInstance().signer(numeroOrdonnance, nomPatient, "PATIENT", signatureData)
   │   ├── payload = "CURAVITA|ORD-2026-1227|NomPatient|PATIENT|2026-04-28 10:30:00|data"
   │   ├── SHA-256(payload) → bytes
   │   └── Base64.encode(bytes) → hash string
   │   → Retourne SignatureResult {success, signatureHash, signedAt, signataire, role}
   │
4. ElectronicSignatureService.sauvegarderSignaturePatient(ordonnanceId, sig)
   │   → UPDATE ordonnance SET signature_patient = "NomPatient|date|HASH",
   │                           signature_patient_date = NOW()
   │     WHERE id_ordonnance = ?
   │
5. AuditService.logCreation("ordonnance", id, "Signature patient", nomPatient)
```

### Communication util ↔ util

Les services util ne s'appellent pas directement entre eux sauf :

- `TraitementController` orchestre `DrugInteractionService` + `OpenFDAService` + `TreatmentSuggestionService` + `QRCodeService` + `QRPdfServerService` + `EmailService` + `SmsService` + `AuditService`
- `DrugInteractionService.verifierTout()` appelle en interne `verifierInteractionsOpenFDA()` (HTTP) et `verifierInteractionsLocales()` (SQL via `DatabaseUtil`)
- `OpenFDAService.getInfo()` appelle `getInfoLocale()` (base embarquée) puis `fetch()` (HTTP)
- Tous les services SQL appellent `DatabaseUtil.getInstance().getConnection()`

### Injection de scène (navigation entre pages)

```java
// Pattern de navigation utilisé dans tous les controllers
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ordonnance.fxml"));
Parent root = loader.load();
Stage stage = (Stage) submitButton.getScene().getWindow();
stage.setScene(new Scene(root));
stage.show();
```

`getScene().getWindow()` récupère la fenêtre courante depuis n'importe quel nœud FXML injecté. `FXMLLoader` instancie le controller cible et injecte ses `@FXML`.

### Notifications construites en Java pur (sans FXML)

Les popups de notification (allergie, IA, spam, interactions) sont construites entièrement en Java dans le controller, sans fichier FXML dédié :

```java
Stage popup = new Stage();
popup.initModality(Modality.NONE);  // non-bloquant
// ou
popup.initModality(Modality.APPLICATION_MODAL);  // bloquant

VBox root = new VBox(12);
root.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
// ... construction des composants ...
popup.setScene(new Scene(root));
popup.show();  // ou showAndWait() pour bloquant
```

**Fermeture automatique** (notification IA) :
```java
PauseTransition pause = new PauseTransition(Duration.seconds(4));
pause.setOnFinished(e -> popup.close());
pause.play();
```

**`Platform.runLater()`** : obligatoire pour toute modification de l'UI depuis un thread non-UI (threads OpenFDA, SMS, Email). JavaFX est single-threaded pour l'UI — toute modification depuis un autre thread doit passer par cette méthode.
