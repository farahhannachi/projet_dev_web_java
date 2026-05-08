<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260217090000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add advanced commerce fields: coupons, multi-address shipments, loyalty, segmentation, fraud score.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('CREATE TABLE coupon (id INT AUTO_INCREMENT NOT NULL, code VARCHAR(64) NOT NULL, type VARCHAR(20) NOT NULL, valeur NUMERIC(10, 2) NOT NULL, date_expiration DATETIME DEFAULT NULL COMMENT \'(DC2Type:datetime_immutable)\', usage_max INT NOT NULL, usage_count INT NOT NULL, actif TINYINT(1) NOT NULL, montant_minimum_panier NUMERIC(10, 2) NOT NULL, UNIQUE INDEX UNIQ_3905DFB577153098 (code), PRIMARY KEY(id)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE address (id INT AUTO_INCREMENT NOT NULL, id_utilisateur INT DEFAULT NULL, full_name VARCHAR(255) NOT NULL, line1 VARCHAR(255) NOT NULL, line2 VARCHAR(255) DEFAULT NULL, city VARCHAR(120) NOT NULL, region VARCHAR(120) NOT NULL, postal_code VARCHAR(20) NOT NULL, country VARCHAR(100) NOT NULL, phone VARCHAR(20) DEFAULT NULL, INDEX IDX_D4E6F81F50EAE44 (id_utilisateur), PRIMARY KEY(id)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE order_shipment (id INT AUTO_INCREMENT NOT NULL, id_commande INT NOT NULL, address_id INT NOT NULL, items_json LONGTEXT NOT NULL, shipping_cost NUMERIC(10, 2) NOT NULL, INDEX IDX_3707E57AFCCF8D30 (id_commande), INDEX IDX_3707E57AF5B7AF75 (address_id), PRIMARY KEY(id)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');

        $this->addSql('ALTER TABLE utilisateur ADD loyalty_points INT NOT NULL DEFAULT 0, ADD loyalty_level VARCHAR(20) NOT NULL DEFAULT \'BRONZE\', ADD segment VARCHAR(30) NOT NULL DEFAULT \'NEW_CUSTOMER\', ADD last_activity_at DATETIME DEFAULT NULL COMMENT \'(DC2Type:datetime_immutable)\'');
        $this->addSql('ALTER TABLE commande ADD id_utilisateur INT DEFAULT NULL, ADD coupon_code VARCHAR(64) DEFAULT NULL, ADD coupon_discount NUMERIC(10, 2) NOT NULL DEFAULT \'0\', ADD estimated_delivery_date DATETIME DEFAULT NULL COMMENT \'(DC2Type:datetime_immutable)\', ADD fraud_score INT NOT NULL DEFAULT 0, ADD base_shipping_cost NUMERIC(10, 2) NOT NULL DEFAULT \'0\'');

        $this->addSql('ALTER TABLE address ADD CONSTRAINT FK_D4E6F81F50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL');
        $this->addSql('ALTER TABLE order_shipment ADD CONSTRAINT FK_3707E57AFCCF8D30 FOREIGN KEY (id_commande) REFERENCES commande (id_commande) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE order_shipment ADD CONSTRAINT FK_3707E57AF5B7AF75 FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE commande ADD CONSTRAINT FK_6EEAA67D50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE order_shipment DROP FOREIGN KEY FK_3707E57AFCCF8D30');
        $this->addSql('ALTER TABLE order_shipment DROP FOREIGN KEY FK_3707E57AF5B7AF75');
        $this->addSql('ALTER TABLE address DROP FOREIGN KEY FK_D4E6F81F50EAE44');
        $this->addSql('ALTER TABLE commande DROP FOREIGN KEY FK_6EEAA67D50EAE44');

        $this->addSql('DROP TABLE order_shipment');
        $this->addSql('DROP TABLE address');
        $this->addSql('DROP TABLE coupon');

        $this->addSql('ALTER TABLE utilisateur DROP loyalty_points, DROP loyalty_level, DROP segment, DROP last_activity_at');
        $this->addSql('ALTER TABLE commande DROP id_utilisateur, DROP coupon_code, DROP coupon_discount, DROP estimated_delivery_date, DROP fraud_score, DROP base_shipping_cost');
    }
}

