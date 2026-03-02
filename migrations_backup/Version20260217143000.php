<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260217143000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Remove stock mailing alert columns from depot and stock tables.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot DROP alert_emails');
        $this->addSql('ALTER TABLE stock DROP rupture_alert_sent, DROP rupture_alert_sent_at');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE depot ADD alert_emails LONGTEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE stock ADD rupture_alert_sent TINYINT(1) DEFAULT 0 NOT NULL, ADD rupture_alert_sent_at DATETIME DEFAULT NULL COMMENT \'(DC2Type:datetime_immutable)\'');
    }
}
