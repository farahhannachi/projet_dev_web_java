-- ============================================
-- Script de création des tables
-- Module: Ordonnances & Suivi Médical
-- ============================================

-- Table: ordonnance
-- Stocke les ordonnances médicales téléversées
CREATE TABLE IF NOT EXISTS ordonnance (
    id INT AUTO_INCREMENT NOT NULL,
    client_id INT NOT NULL,
    validated_by_id INT DEFAULT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending_validation',
    rejection_reason TEXT DEFAULT NULL,
    uploaded_at DATETIME NOT NULL,
    validated_at DATETIME DEFAULT NULL,
    
    PRIMARY KEY (id),
    
    INDEX idx_ordonnance_client (client_id),
    INDEX idx_ordonnance_validated_by (validated_by_id),
    INDEX idx_ordonnance_status (status),
        
    CONSTRAINT chk_ordonnance_status 
        CHECK (status IN ('pending_validation', 'validated', 'rejected'))
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: traitement
-- Stocke les traitements médicaux issus des ordonnances
CREATE TABLE IF NOT EXISTS traitement (
    id INT AUTO_INCREMENT NOT NULL,
    ordonnance_id INT NOT NULL,
    client_id INT NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    frequency VARCHAR(255) NOT NULL,
    duration_days INT NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    is_completed TINYINT(1) NOT NULL DEFAULT 0,
    notes TEXT DEFAULT NULL,
    
    PRIMARY KEY (id),
    
    INDEX idx_traitement_ordonnance (ordonnance_id),
    INDEX idx_traitement_client (client_id),
    INDEX idx_traitement_active (client_id, is_active),
        
    CONSTRAINT chk_traitement_duration 
        CHECK (duration_days > 0)
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Commentaires sur les tables
-- ============================================

-- ordonnance: Gère le cycle de vie des ordonnances médicales
--   - Téléversement par le client
--   - Validation/Rejet par le pharmacien
--   - Stockage sécurisé des fichiers
--   - client_id et validated_by_id sont gérés par d'autres modules

-- traitement: Gère les plans de traitement
--   - Créés après validation d'une ordonnance
--   - Suivi de l'état actif/complété
--   - Calcul automatique de la date de fin
--   - client_id est géré par un autre module
