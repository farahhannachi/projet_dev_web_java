<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260214214032 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('DROP TABLE mouvement_stock');
        $this->addSql('CREATE TEMPORARY TABLE __temp__stock AS SELECT id_stock, produit_id, depot_id, seuil_alerte, date_expiration, etat_stock, date_derniere_mise_ajour, quantite FROM stock');
        $this->addSql('DROP TABLE stock');
        $this->addSql('CREATE TABLE stock (id_stock INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, produit_id INTEGER DEFAULT NULL, depot_id INTEGER DEFAULT NULL, seuil_alerte INTEGER NOT NULL, date_expiration DATETIME DEFAULT NULL, etat_stock VARCHAR(20) NOT NULL, date_entree DATETIME NOT NULL, quantite INTEGER NOT NULL, seuil_critique INTEGER NOT NULL, date_derniere_mise_a_jour DATETIME NOT NULL, derniere_entree DATETIME DEFAULT NULL, derniere_sortie DATETIME DEFAULT NULL, total_entrees INTEGER DEFAULT 0 NOT NULL, total_sorties INTEGER DEFAULT 0 NOT NULL, prix_achat_unitaire NUMERIC(10, 2) DEFAULT NULL, prix_vente_unitaire NUMERIC(10, 2) DEFAULT NULL, emplacement VARCHAR(100) DEFAULT NULL, batch_number VARCHAR(50) DEFAULT NULL, fournisseur VARCHAR(100) DEFAULT NULL, notes CLOB DEFAULT NULL, CONSTRAINT FK_4B365660F347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_4B3656608510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE)');
        $this->addSql('INSERT INTO stock (id_stock, produit_id, depot_id, seuil_alerte, date_expiration, etat_stock, date_entree, quantite) SELECT id_stock, produit_id, depot_id, seuil_alerte, date_expiration, etat_stock, date_derniere_mise_ajour, quantite FROM __temp__stock');
        $this->addSql('DROP TABLE __temp__stock');
        $this->addSql('CREATE INDEX IDX_4B3656608510D4DE ON stock (depot_id)');
        $this->addSql('CREATE INDEX IDX_4B365660F347EFB ON stock (produit_id)');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('CREATE TABLE mouvement_stock (id_mouvement INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, stock_id INTEGER NOT NULL, produit_id INTEGER NOT NULL, depot_id INTEGER DEFAULT NULL, type_mouvement VARCHAR(50) NOT NULL COLLATE "BINARY", type_operation VARCHAR(50) NOT NULL COLLATE "BINARY", quantite INTEGER NOT NULL, quantite_avant INTEGER NOT NULL, quantite_apres INTEGER NOT NULL, date_mouvement DATETIME NOT NULL, motif VARCHAR(255) DEFAULT NULL COLLATE "BINARY", utilisateur_id INTEGER DEFAULT NULL, reference VARCHAR(100) DEFAULT NULL COLLATE "BINARY", created_at DATETIME NOT NULL, CONSTRAINT FK_61E2C8EBDCD6110 FOREIGN KEY (stock_id) REFERENCES stock (id_stock) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_61E2C8EBF347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_61E2C8EB8510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot) ON UPDATE NO ACTION ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE)');
        $this->addSql('CREATE INDEX IDX_61E2C8EB8510D4DE ON mouvement_stock (depot_id)');
        $this->addSql('CREATE INDEX IDX_61E2C8EBF347EFB ON mouvement_stock (produit_id)');
        $this->addSql('CREATE INDEX IDX_61E2C8EBDCD6110 ON mouvement_stock (stock_id)');
        $this->addSql('CREATE TEMPORARY TABLE __temp__stock AS SELECT id_stock, produit_id, depot_id, quantite, seuil_alerte, date_expiration, etat_stock FROM stock');
        $this->addSql('DROP TABLE stock');
        $this->addSql('CREATE TABLE stock (id_stock INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, produit_id INTEGER DEFAULT NULL, depot_id INTEGER DEFAULT NULL, quantite INTEGER NOT NULL, seuil_alerte INTEGER NOT NULL, date_expiration DATETIME NOT NULL, etat_stock VARCHAR(50) NOT NULL, date_derniere_mise_ajour DATETIME NOT NULL, CONSTRAINT FK_4B365660F347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit) NOT DEFERRABLE INITIALLY IMMEDIATE, CONSTRAINT FK_4B3656608510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot) NOT DEFERRABLE INITIALLY IMMEDIATE)');
        $this->addSql('INSERT INTO stock (id_stock, produit_id, depot_id, quantite, seuil_alerte, date_expiration, etat_stock) SELECT id_stock, produit_id, depot_id, quantite, seuil_alerte, date_expiration, etat_stock FROM __temp__stock');
        $this->addSql('DROP TABLE __temp__stock');
        $this->addSql('CREATE INDEX IDX_4B365660F347EFB ON stock (produit_id)');
        $this->addSql('CREATE INDEX IDX_4B3656608510D4DE ON stock (depot_id)');
    }
}
