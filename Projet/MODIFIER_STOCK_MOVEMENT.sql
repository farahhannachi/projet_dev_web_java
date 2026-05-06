-- ============================================
-- MODIFICATION TABLE STOCK_MOVEMENT
-- Ajouter la relation avec Service
-- ============================================

USE pharmacie;

-- Ajouter la colonne id_service si elle n'existe pas
ALTER TABLE `stock_movement`
ADD COLUMN `id_service` INT(11) DEFAULT NULL AFTER `id_stock_id`,
ADD COLUMN `type_consommation` VARCHAR(50) DEFAULT 'SORTIE' AFTER `type`,
ADD COLUMN `reference_document` VARCHAR(100) DEFAULT NULL AFTER `motif`;

-- Ajouter l'index pour améliorer les recherches
ALTER TABLE `stock_movement`
ADD INDEX `idx_service` (`id_service`),
ADD INDEX `idx_type_consommation` (`type_consommation`),
ADD INDEX `idx_created_at` (`created_at`);

-- Ajouter les clés étrangères
ALTER TABLE `stock_movement`
ADD CONSTRAINT `FK_stock_movement_service`
FOREIGN KEY (`id_service`) REFERENCES `service` (`id_service`)
ON DELETE SET NULL;

-- Vérification
SELECT 'Table stock_movement modifiée avec succès!' AS status;

