<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Add student_id and id_card_image fields to utilisateur table
 */
final class Version20260227210000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add student_id and id_card_image fields to utilisateur table for ID card scan login feature';
    }

    public function up(Schema $schema): void
    {
        // Add student_id column (unique, nullable)
        $this->addSql('ALTER TABLE utilisateur ADD student_id VARCHAR(100) DEFAULT NULL');
        $this->addSql('CREATE UNIQUE INDEX UNIQ_1D1C63B34DCD58BC ON utilisateur (student_id)');
        
        // Add id_card_image column (nullable)
        $this->addSql('ALTER TABLE utilisateur ADD id_card_image VARCHAR(255) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        // Remove the columns if migration is rolled back
        $this->addSql('DROP INDEX UNIQ_1D1C63B34DCD58BC ON utilisateur');
        $this->addSql('ALTER TABLE utilisateur DROP student_id, DROP id_card_image');
    }
}
