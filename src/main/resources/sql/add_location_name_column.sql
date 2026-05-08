
-- Ajouter la colonne location_name à la table depot
ALTER TABLE `depot` ADD COLUMN `location_name` VARCHAR(500) DEFAULT NULL AFTER `longitude`;

