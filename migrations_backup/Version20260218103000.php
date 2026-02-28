<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260218103000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add responsable telephone to depot.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot ADD responsable_telephone VARCHAR(50) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot DROP responsable_telephone');
    }
}
