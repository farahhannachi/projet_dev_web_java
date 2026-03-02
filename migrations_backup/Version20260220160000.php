<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260220160000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add password reset token fields to utilisateur table';
    }

    public function up(Schema $schema): void
    {
        // Add reset_token column
        $this->addSql('ALTER TABLE utilisateur ADD reset_token VARCHAR(255) DEFAULT NULL');
        $this->addSql('ALTER TABLE utilisateur ADD reset_token_expires_at DATETIME DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE utilisateur DROP COLUMN reset_token');
        $this->addSql('ALTER TABLE utilisateur DROP COLUMN reset_token_expires_at');
    }
}
