<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260219140000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Ajout des champs de signature patient pour les ordonnances';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE ordonnance ADD signature_patient TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD signature_patient_date DATETIME DEFAULT NULL');
        $this->addSql('ALTER TABLE ordonnance ADD signature_patient_ip VARCHAR(45) DEFAULT NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE ordonnance DROP signature_patient');
        $this->addSql('ALTER TABLE ordonnance DROP signature_patient_date');
        $this->addSql('ALTER TABLE ordonnance DROP signature_patient_ip');
    }
}
