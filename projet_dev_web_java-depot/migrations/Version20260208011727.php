<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260208011727 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add missing id_produit column to traitement table';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE traitement ADD id_produit INT DEFAULT NULL');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D27F347EFB FOREIGN KEY (id_produit) REFERENCES produit (id_produit)');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D27F347EFB');
        $this->addSql('ALTER TABLE traitement DROP COLUMN id_produit');
    }
}
