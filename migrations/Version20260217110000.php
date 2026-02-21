<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260217110000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add GPS coordinates to depot table.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot ADD latitude NUMERIC(10, 7) DEFAULT NULL, ADD longitude NUMERIC(10, 7) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot DROP latitude, DROP longitude');
    }
}

