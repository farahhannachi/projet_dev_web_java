<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260208011726 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add missing columns to commande table: nom, email, message';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE commande ADD nom VARCHAR(255) NOT NULL');
        $this->addSql('ALTER TABLE commande ADD email VARCHAR(255) NOT NULL');
        $this->addSql('ALTER TABLE commande ADD message LONGTEXT DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE commande DROP COLUMN nom');
        $this->addSql('ALTER TABLE commande DROP COLUMN email');
        $this->addSql('ALTER TABLE commande DROP COLUMN message');
    }
}
