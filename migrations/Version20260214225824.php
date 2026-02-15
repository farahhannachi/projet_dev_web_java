<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260214225824 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('DROP TABLE mouvement_stock');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('CREATE TABLE mouvement_stock (id_mouvement INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, stock_id INTEGER NOT NULL, produit_id INTEGER NOT NULL, depot_id INTEGER DEFAULT NULL, type_mouvement VARCHAR(50) NOT NULL COLLATE "BINARY", type_operation VARCHAR(50) NOT NULL COLLATE "BINARY", quantite INTEGER NOT NULL, quantite_avant INTEGER NOT NULL, quantite_apres INTEGER NOT NULL, date_mouvement DATETIME NOT NULL, motif VARCHAR(255) DEFAULT NULL COLLATE "BINARY", utilisateur_id INTEGER DEFAULT NULL, reference VARCHAR(100) DEFAULT NULL COLLATE "BINARY", created_at DATETIME NOT NULL, CONSTRAINT FK_61E2C8EBDCD6110 FOREIGN KEY (stock_id) REFERENCES stock (id_stock) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_61E2C8EBF347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_61E2C8EB8510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE)');
        $this->addSql('CREATE INDEX IDX_61E2C8EB8510D4DE ON mouvement_stock (depot_id)');
        $this->addSql('CREATE INDEX IDX_61E2C8EBF347EFB ON mouvement_stock (produit_id)');
        $this->addSql('CREATE INDEX IDX_61E2C8EBDCD6110 ON mouvement_stock (stock_id)');
    }
}
