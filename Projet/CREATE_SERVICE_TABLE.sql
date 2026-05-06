-- Création de la table SERVICE si elle n'existe pas
-- Exécute ce script dans PhpMyAdmin

USE pharmacie;

-- Vérifier et créer la table service
CREATE TABLE IF NOT EXISTS `service` (
  `id_service` int(11) NOT NULL AUTO_INCREMENT,
  `nom_service` varchar(255) NOT NULL,
  `type_service` enum('Médecin','Infirmier') NOT NULL,
  `specialite` varchar(255) NOT NULL,
  `telephone` varchar(50) NOT NULL,
  `email` varchar(255) NOT NULL,
  `adresse` varchar(255) NOT NULL,
  `date_creation` datetime NOT NULL,
  PRIMARY KEY (`id_service`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Message de confirmation
SELECT 'Table service créée avec succès!' AS status;

