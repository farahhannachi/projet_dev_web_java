CREATE DATABASE IF NOT EXISTS curavita;
USE curavita;

CREATE TABLE IF NOT EXISTS ordonnance (
    id_ordonnance INT(11) NOT NULL AUTO_INCREMENT,
    id_utilisateur INT(11) NOT NULL,
    numero_ordonnance VARCHAR(100) NOT NULL,
    date_ordonnance DATETIME NOT NULL,
    date_expiration DATETIME NOT NULL,
    statut VARCHAR(50) NOT NULL,
    note_medical LONGTEXT NULL,
    signature_electronique TINYINT(1) NOT NULL DEFAULT 0,
    signature_date DATETIME NULL,
    signature_medecin VARCHAR(255) NULL,
    docusign_envelope_id VARCHAR(255) NULL,
    docusign_status VARCHAR(50) NULL,
    signature_document_path VARCHAR(500) NULL,
    signature_patient TEXT NULL,
    signature_patient_date DATETIME NULL,
    signature_patient_ip VARCHAR(45) NULL,
    PRIMARY KEY (id_ordonnance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traitement (
    id_traitement INT(11) NOT NULL AUTO_INCREMENT,
    id_ordonnance INT(11) NOT NULL,
    id_utilisateur INT(11) NOT NULL,
    dosage VARCHAR(255) NULL,
    frequence VARCHAR(255) NULL,
    duree_jours INT(11) NULL,
    date_debut DATETIME NULL,
    date_fin DATETIME NULL,
    status VARCHAR(50) NOT NULL,
    notes LONGTEXT NULL,
    id_produit INT(11) NULL,
    PRIMARY KEY (id_traitement),
    FOREIGN KEY (id_ordonnance) REFERENCES ordonnance(id_ordonnance) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
