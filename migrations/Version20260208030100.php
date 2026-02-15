<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260208030100 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Make utilisateur deletions cascade to dependent tables (question, ordonnance, traitement) and set response_question.utilisateur to NULL.';
    }

    public function up(Schema $schema): void
    {
        // question -> utilisateur
        $this->addSql('ALTER TABLE question DROP FOREIGN KEY FK_B6F7494E50EAE44');
        $this->addSql('ALTER TABLE question ADD CONSTRAINT FK_B6F7494E50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');

        // ordonnance -> utilisateur
        $this->addSql('ALTER TABLE ordonnance DROP FOREIGN KEY FK_924B326C50EAE44');
        $this->addSql('ALTER TABLE ordonnance ADD CONSTRAINT FK_924B326C50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');

        // traitement -> utilisateur
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2750EAE44');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2750EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');

        // response_question -> utilisateur
        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF3350EAE44');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF3350EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE question DROP FOREIGN KEY FK_B6F7494E50EAE44');
        $this->addSql('ALTER TABLE question ADD CONSTRAINT FK_B6F7494E50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');

        $this->addSql('ALTER TABLE ordonnance DROP FOREIGN KEY FK_924B326C50EAE44');
        $this->addSql('ALTER TABLE ordonnance ADD CONSTRAINT FK_924B326C50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');

        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2750EAE44');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2750EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');

        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF3350EAE44');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF3350EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');
    }
}
