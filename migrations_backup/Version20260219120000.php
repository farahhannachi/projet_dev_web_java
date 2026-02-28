<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260219120000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Ajout des champs pour la signature électronique des ordonnances';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE ordonnance ADD signature_electronique TINYINT(1) DEFAULT 0 NOT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD signature_date DATETIME DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD signature_medecin VARCHAR(255) DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD docusign_envelope_id VARCHAR(255) DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD docusign_status VARCHAR(50) DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD signature_document_path VARCHAR(500) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE ordonnance DROP signature_electronique');
        $this->addSql('ALTER TABLE ordonnance DROP signature_date');
        $this->addSql('ALTER TABLE ordonnance DROP signature_medecin');
        $this->addSql('ALTER TABLE ordonnance DROP docusign_envelope_id');
        $this->addSql('ALTER TABLE ordonnance DROP docusign_status');
        $this->addSql('ALTER TABLE ordonnance DROP signature_document_path');
    }
}
