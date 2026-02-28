<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260228040000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Fix database collation to match table collations (utf8mb4_unicode_ci)';
    }

    public function up(Schema $schema): void
    {
        // Change database default collation to match tables
        $this->addSql("ALTER DATABASE `pharmacie` COLLATE = utf8mb4_unicode_ci");
    }

    public function down(Schema $schema): void
    {
        // Revert to original collation
        $this->addSql("ALTER DATABASE `pharmacie` COLLATE = utf8mb4_general_ci");
    }
}
