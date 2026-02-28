-- ============================================
-- MISE À JOUR BASE DE DONNÉES PHARMACIE
-- Date: 2026-02-28
-- ============================================

-- Migration 1: Création des tables principales
-- Version20260223010125

CREATE TABLE IF NOT EXISTS address (
    id INT AUTO_INCREMENT NOT NULL,
    id_utilisateur INT DEFAULT NULL,
    full_name VARCHAR(255) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255) DEFAULT NULL,
    city VARCHAR(120) NOT NULL,
    region VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    INDEX IDX_D4E6F8150EAE44 (id_utilisateur),
    PRIMARY KEY(id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id INT NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id INT DEFAULT NULL,
    user_name VARCHAR(255) DEFAULT NULL,
    old_values JSON DEFAULT NULL,
    new_values JSON DEFAULT NULL,
    changed_fields JSON DEFAULT NULL,
    created_at DATETIME NOT NULL,
    ip_address VARCHAR(45) DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at),
    INDEX idx_user (user_id),
    PRIMARY KEY(id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS commande (
    id_commande INT AUTO_INCREMENT NOT NULL,
    id_utilisateur INT DEFAULT NULL,
    date_commande DATETIME NOT NULL,
    statut VARCHAR(50) DEFAULT 'en_attente' NOT NULL,
    total NUMERIC(10, 2) DEFAULT 0 NOT NULL,
    mode_paiement VARCHAR(50) NOT NULL,
    adresse_livraison VARCHAR(255) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    message LONGTEXT DEFAULT NULL,
    produits_ids LONGTEXT DEFAULT NULL,
    coupon_code VARCHAR(64) DEFAULT NULL,
    coupon_discount NUMERIC(10, 2) DEFAULT 0 NOT NULL,
    estimated_delivery_date DATETIME DEFAULT NULL,
    fraud_score INT DEFAULT 0 NOT NULL,
    base_shipping_cost NUMERIC(10, 2) DEFAULT 0 NOT NULL,
    INDEX IDX_6EEAA67D50EAE44 (id_utilisateur),
    PRIMARY KEY(id_commande)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS coupon (
    id INT AUTO_INCREMENT NOT NULL,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(20) NOT NULL,
    valeur NUMERIC(10, 2) NOT NULL,
    date_expiration DATETIME DEFAULT NULL,
    usage_max INT DEFAULT 1 NOT NULL,
    usage_count INT DEFAULT 0 NOT NULL,
    actif TINYINT(1) DEFAULT 1 NOT NULL,
    montant_minimum_panier NUMERIC(10, 2) DEFAULT 0 NOT NULL,
    UNIQUE INDEX uniq_coupon_code (code),
    PRIMARY KEY(id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS depot (
    id_depot INT AUTO_INCREMENT NOT NULL,
    nom_depot VARCHAR(255) NOT NULL,
    adresse_depot VARCHAR(255) NOT NULL,
    ville VARCHAR(100) DEFAULT NULL,
    capacite_depot INT NOT NULL,
    responsable_depot VARCHAR(255) NOT NULL,
    responsable_telephone VARCHAR(50) DEFAULT NULL,
    date_creation DATETIME NOT NULL,
    latitude NUMERIC(10, 7) DEFAULT NULL,
    longitude NUMERIC(10, 7) DEFAULT NULL,
    PRIMARY KEY(id_depot)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_shipment (
    id INT AUTO_INCREMENT NOT NULL,
    id_commande INT NOT NULL,
    address_id INT NOT NULL,
    items_json LONGTEXT NOT NULL,
    shipping_cost NUMERIC(10, 2) DEFAULT 0 NOT NULL,
    INDEX IDX_E333C26D3E314AE8 (id_commande),
    INDEX IDX_E333C26DF5B7AF75 (address_id),
    PRIMARY KEY(id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ordonnance (
    id_ordonnance INT AUTO_INCREMENT NOT NULL,
    id_utilisateur INT NOT NULL,
    numero_ordonnance VARCHAR(100) NOT NULL,
    date_ordonnance DATETIME NOT NULL,
    date_expiration DATETIME NOT NULL,
    statut VARCHAR(50) NOT NULL,
    note_medical LONGTEXT DEFAULT NULL,
    signature_electronique TINYINT(1) DEFAULT 0 NOT NULL,
    signature_date DATETIME DEFAULT NULL,
    signature_medecin VARCHAR(255) DEFAULT NULL,
    docusign_envelope_id VARCHAR(255) DEFAULT NULL,
    docusign_status VARCHAR(50) DEFAULT NULL,
    signature_document_path VARCHAR(500) DEFAULT NULL,
    signature_patient LONGTEXT DEFAULT NULL,
    signature_patient_date DATETIME DEFAULT NULL,
    signature_patient_ip VARCHAR(45) DEFAULT NULL,
    INDEX IDX_924B326C50EAE44 (id_utilisateur),
    PRIMARY KEY(id_ordonnance)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS produit (
    id_produit INT AUTO_INCREMENT NOT NULL,
    nom VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    prix NUMERIC(10, 2) NOT NULL,
    quantite_stock INT NOT NULL,
    date_expiration DATETIME NOT NULL,
    categorie VARCHAR(100) NOT NULL,
    image VARCHAR(255) DEFAULT NULL,
    statut VARCHAR(20) DEFAULT 'disponible' NOT NULL,
    PRIMARY KEY(id_produit)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS promotion (
    id_promotion INT AUTO_INCREMENT NOT NULL,
    id_produit INT DEFAULT NULL,
    titre VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    valeur_reduction DOUBLE PRECISION NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    statut VARCHAR(20) NOT NULL,
    id_admin INT NOT NULL,
    INDEX IDX_C11D7DD1F7384557 (id_produit),
    PRIMARY KEY(id_promotion)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS question (
    id_question INT AUTO_INCREMENT NOT NULL,
    id_utilisateur INT NOT NULL,
    type_ticket VARCHAR(20) NOT NULL,
    objet VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    priorite VARCHAR(20) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    file_path VARCHAR(255) DEFAULT NULL,
    file_type VARCHAR(100) DEFAULT NULL,
    file_size INT DEFAULT NULL,
    created_at DATETIME NOT NULL,
    INDEX IDX_B6F7494E50EAE44 (id_utilisateur),
    PRIMARY KEY(id_question)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS response_question (
    id_reponse INT AUTO_INCREMENT NOT NULL,
    id_question INT NOT NULL,
    id_utilisateur INT DEFAULT NULL,
    auteur_type VARCHAR(20) NOT NULL,
    reponse_text LONGTEXT NOT NULL,
    reponse_role VARCHAR(30) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    impact_statut VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    file_path VARCHAR(255) DEFAULT NULL,
    file_type VARCHAR(100) DEFAULT NULL,
    file_size INT DEFAULT NULL,
    created_at DATETIME NOT NULL,
    lu_par_client TINYINT(1) DEFAULT 0 NOT NULL,
    INDEX IDX_1E1AF33E62CA5DB (id_question),
    INDEX IDX_1E1AF3350EAE44 (id_utilisateur),
    PRIMARY KEY(id_reponse)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stock (
    id_stock INT AUTO_INCREMENT NOT NULL,
    produit_id INT DEFAULT NULL,
    depot_id INT DEFAULT NULL,
    quantite INT NOT NULL,
    quantite_initiale INT DEFAULT 0 NOT NULL,
    is_actif TINYINT(1) DEFAULT 1 NOT NULL,
    seuil_alerte INT NOT NULL,
    seuil_critique INT NOT NULL,
    date_entree DATETIME NOT NULL,
    date_expiration DATETIME DEFAULT NULL,
    etat_stock VARCHAR(20) NOT NULL,
    date_derniere_mise_a_jour DATETIME NOT NULL,
    derniere_entree DATETIME DEFAULT NULL,
    derniere_sortie DATETIME DEFAULT NULL,
    total_entrees INT DEFAULT 0 NOT NULL,
    total_sorties INT DEFAULT 0 NOT NULL,
    prix_achat_unitaire NUMERIC(10, 2) DEFAULT NULL,
    prix_vente_unitaire NUMERIC(10, 2) DEFAULT NULL,
    emplacement VARCHAR(100) DEFAULT NULL,
    batch_number VARCHAR(50) DEFAULT NULL,
    qr_code_token VARCHAR(128) DEFAULT NULL,
    qr_code_payload LONGTEXT DEFAULT NULL,
    fournisseur VARCHAR(100) DEFAULT NULL,
    notes LONGTEXT DEFAULT NULL,
    INDEX IDX_4B365660F347EFB (produit_id),
    INDEX IDX_4B3656608510D4DE (depot_id),
    UNIQUE INDEX uniq_stock_lot_depot (batch_number, depot_id),
    UNIQUE INDEX uniq_stock_qr_token (qr_code_token),
    PRIMARY KEY(id_stock)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stock_movement (
    id INT AUTO_INCREMENT NOT NULL,
    id_stock INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantite INT NOT NULL,
    quantite_before INT NOT NULL,
    quantite_after INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    motif VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL,
    INDEX IDX_BB1BC1B5A5B31750 (id_stock),
    PRIMARY KEY(id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS traitement (
    id_traitement INT AUTO_INCREMENT NOT NULL,
    id_ordonnance INT NOT NULL,
    id_utilisateur INT NOT NULL,
    id_produit INT DEFAULT NULL,
    dosage VARCHAR(255) DEFAULT NULL,
    frequence VARCHAR(255) DEFAULT NULL,
    duree_jours INT DEFAULT NULL,
    date_debut DATETIME DEFAULT NULL,
    date_fin DATETIME DEFAULT NULL,
    status VARCHAR(50) NOT NULL,
    notes LONGTEXT DEFAULT NULL,
    INDEX IDX_2A356D2737C1B2BB (id_ordonnance),
    INDEX IDX_2A356D2750EAE44 (id_utilisateur),
    INDEX IDX_2A356D27F7384557 (id_produit),
    PRIMARY KEY(id_traitement)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS utilisateur (
    id_utilisateur INT AUTO_INCREMENT NOT NULL,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    etat_compte VARCHAR(20) NOT NULL,
    date_creation DATETIME NOT NULL,
    roles JSON NOT NULL,
    loyalty_points INT DEFAULT 0 NOT NULL,
    loyalty_level VARCHAR(20) DEFAULT 'BRONZE' NOT NULL,
    segment VARCHAR(30) DEFAULT 'NEW_CUSTOMER' NOT NULL,
    last_activity_at DATETIME DEFAULT NULL,
    date_naissance DATE DEFAULT NULL,
    telephone VARCHAR(20) DEFAULT NULL,
    avatar_url LONGTEXT DEFAULT NULL,
    avatar_seed VARCHAR(100) DEFAULT NULL,
    has_seen_introduction TINYINT(1) DEFAULT 0 NOT NULL,
    reset_token VARCHAR(255) DEFAULT NULL,
    reset_token_expires_at DATETIME DEFAULT NULL,
    totp_secret VARCHAR(255) DEFAULT NULL,
    totp_enabled TINYINT(1) DEFAULT 0 NOT NULL,
    backup_codes JSON DEFAULT NULL,
    UNIQUE INDEX UNIQ_1D1C63B3E7927C74 (email),
    PRIMARY KEY(id_utilisateur)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE = InnoDB;

-- Ajout des contraintes de clés étrangères
ALTER TABLE address ADD CONSTRAINT FK_D4E6F8150EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL;
ALTER TABLE commande ADD CONSTRAINT FK_6EEAA67D50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL;
ALTER TABLE order_shipment ADD CONSTRAINT FK_E333C26D3E314AE8 FOREIGN KEY (id_commande) REFERENCES commande (id_commande) ON DELETE CASCADE;
ALTER TABLE order_shipment ADD CONSTRAINT FK_E333C26DF5B7AF75 FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE CASCADE;
ALTER TABLE ordonnance ADD CONSTRAINT FK_924B326C50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE;
ALTER TABLE promotion ADD CONSTRAINT FK_C11D7DD1F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit);
ALTER TABLE question ADD CONSTRAINT FK_B6F7494E50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE;
ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF33E62CA5DB FOREIGN KEY (id_question) REFERENCES question (id_question);
ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF3350EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL;
ALTER TABLE stock ADD CONSTRAINT FK_4B365660F347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit);
ALTER TABLE stock ADD CONSTRAINT FK_4B3656608510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot);
ALTER TABLE stock_movement ADD CONSTRAINT FK_BB1BC1B5A5B31750 FOREIGN KEY (id_stock) REFERENCES stock (id_stock) ON DELETE CASCADE;
ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2737C1B2BB FOREIGN KEY (id_ordonnance) REFERENCES ordonnance (id_ordonnance);
ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2750EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE;

-- Migration 2: Modification de la contrainte traitement.id_produit
-- Version20260223120000
ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D27F7384557;
ALTER TABLE traitement ADD CONSTRAINT FK_2A356D27F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit) ON DELETE CASCADE;

-- Migration 3: Ajout des champs student_id et id_card_image
-- Version20260227210000
ALTER TABLE utilisateur ADD student_id VARCHAR(100) DEFAULT NULL;
CREATE UNIQUE INDEX UNIQ_1D1C63B34DCD58BC ON utilisateur (student_id);
ALTER TABLE utilisateur ADD id_card_image VARCHAR(255) DEFAULT NULL;

-- Migration 4: Correction du collation de la base de données
-- Version20260228040000
ALTER DATABASE `pharmacie` COLLATE = utf8mb4_unicode_ci;

-- ============================================
-- FIN DE LA MISE À JOUR
-- ============================================
