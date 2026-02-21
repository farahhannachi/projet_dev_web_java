<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260220140000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add avatar fields to utilisateur table for custom avatar support';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE utilisateur ADD avatar_url LONGTEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE utilisateur ADD avatar_seed VARCHAR(100) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE utilisateur DROP avatar_url');
        $this->addSql('ALTER TABLE utilisateur DROP avatar_seed');
    }
}
