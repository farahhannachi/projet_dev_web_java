<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260223120000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add CASCADE DELETE to traitement.id_produit foreign key';
    }

    public function up(Schema $schema): void
    {
        // Drop the existing foreign key constraint
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D27F7384557');
        // Re-create it with CASCADE delete
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D27F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit) ON DELETE CASCADE');
    }

    public function down(Schema $schema): void
    {
        // Revert to original constraint without CASCADE
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D27F7384557');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D27F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit)');
    }
}
