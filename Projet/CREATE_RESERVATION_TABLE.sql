-- Script SQL pour créer la table des réservations
-- Exécuter ce script dans votre base de données MySQL

CREATE TABLE IF NOT EXISTS reservation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    service_id INT NOT NULL,
    nom_client VARCHAR(100) NOT NULL,
    email_client VARCHAR(100) NOT NULL,
    telephone_client VARCHAR(20) NOT NULL,
    date_reservation DATETIME NOT NULL,
    date_rendez_vous DATETIME NOT NULL,
    motif LONGTEXT NOT NULL,
    statut VARCHAR(50) DEFAULT 'En attente',
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_id) REFERENCES service(id_service) ON DELETE CASCADE,
    INDEX idx_service (service_id),
    INDEX idx_date (date_rendez_vous),
    INDEX idx_statut (statut)
);

-- Ajouter des colonnes si la table existe déjà
-- ALTER TABLE reservation ADD COLUMN email_client VARCHAR(100) IF NOT EXISTS;
-- ALTER TABLE reservation ADD COLUMN telephone_client VARCHAR(20) IF NOT EXISTS;

