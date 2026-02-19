<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260217101000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add QR-driven stock workflow fields and stock movement table.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql("ALTER TABLE stock ADD quantite_initiale INT NOT NULL DEFAULT 0, ADD is_actif TINYINT(1) NOT NULL DEFAULT 1, ADD qr_code_token VARCHAR(128) DEFAULT NULL, ADD qr_code_payload LONGTEXT DEFAULT NULL");
        $this->addSql('CREATE UNIQUE INDEX uniq_stock_qr_token ON stock (qr_code_token)');
        $this->addSql('CREATE UNIQUE INDEX uniq_stock_lot_depot ON stock (batch_number, depot_id)');

        $this->addSql('CREATE TABLE stock_movement (id INT AUTO_INCREMENT NOT NULL, id_stock INT NOT NULL, type VARCHAR(20) NOT NULL, quantite INT NOT NULL, quantite_before INT NOT NULL, quantite_after INT NOT NULL, status VARCHAR(20) NOT NULL, motif VARCHAR(255) DEFAULT NULL, created_at DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', INDEX IDX_2D1C21AB4B365660 (id_stock), PRIMARY KEY(id)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('ALTER TABLE stock_movement ADD CONSTRAINT FK_2D1C21AB4B365660 FOREIGN KEY (id_stock) REFERENCES stock (id_stock) ON DELETE CASCADE');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE stock_movement DROP FOREIGN KEY FK_2D1C21AB4B365660');
        $this->addSql('DROP TABLE stock_movement');

        $this->addSql('DROP INDEX uniq_stock_lot_depot ON stock');
        $this->addSql('DROP INDEX uniq_stock_qr_token ON stock');
        $this->addSql('ALTER TABLE stock DROP quantite_initiale, DROP is_actif, DROP qr_code_token, DROP qr_code_payload');
    }
}

