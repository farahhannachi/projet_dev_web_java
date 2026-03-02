<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260219130000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Création de la table audit_log pour l\'historique des modifications';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('CREATE TABLE audit_log (
            id INT AUTO_INCREMENT NOT NULL,
            entity_type VARCHAR(100) NOT NULL,
            entity_id INT NOT NULL,
            action VARCHAR(50) NOT NULL,
            user_id INT DEFAULT NULL,
            user_name VARCHAR(255) DEFAULT NULL,
            old_values LONGTEXT DEFAULT NULL COMMENT \'(DC2Type:json)\',
            new_values LONGTEXT DEFAULT NULL COMMENT \'(DC2Type:json)\',
            changed_fields LONGTEXT DEFAULT NULL COMMENT \'(DC2Type:json)\',
            created_at DATETIME NOT NULL,
            ip_address VARCHAR(45) DEFAULT NULL,
            user_agent VARCHAR(500) DEFAULT NULL,
            PRIMARY KEY(id),
            INDEX idx_entity (entity_type, entity_id),
            INDEX idx_created_at (created_at),
            INDEX idx_user (user_id)
        ) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP TABLE audit_log');
    }
}
