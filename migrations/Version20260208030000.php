<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260208030000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Remove unused columns: promotion.type_promotion, utilisateur.role, utilisateur.telephone, utilisateur.adresse';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE promotion DROP COLUMN type_promotion');
        $this->addSql('ALTER TABLE utilisateur DROP COLUMN role');
        $this->addSql('ALTER TABLE utilisateur DROP COLUMN telephone');
        $this->addSql('ALTER TABLE utilisateur DROP COLUMN adresse');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE promotion ADD type_promotion VARCHAR(20) NOT NULL');
        $this->addSql('ALTER TABLE utilisateur ADD role VARCHAR(50) NOT NULL');
        $this->addSql('ALTER TABLE utilisateur ADD telephone VARCHAR(20) DEFAULT NULL');
        $this->addSql('ALTER TABLE utilisateur ADD adresse VARCHAR(255) DEFAULT NULL');
    }
}
