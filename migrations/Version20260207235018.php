<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260207235018 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('CREATE TABLE commande (id_commande INT AUTO_INCREMENT NOT NULL, date_commande DATETIME NOT NULL, statut VARCHAR(50) NOT NULL, total NUMERIC(10, 2) NOT NULL, mode_paiement VARCHAR(50) NOT NULL, adresse_livraison VARCHAR(255) NOT NULL, telephone VARCHAR(20) NOT NULL, PRIMARY KEY(id_commande)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE depot (id_depot INT AUTO_INCREMENT NOT NULL, nom_depot VARCHAR(255) NOT NULL, adresse_depot VARCHAR(255) NOT NULL, capacite_depot INT NOT NULL, responsable_depot VARCHAR(255) NOT NULL, date_creation DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', PRIMARY KEY(id_depot)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE ordonnance (id_ordonnance INT AUTO_INCREMENT NOT NULL, id_utilisateur INT NOT NULL, numero_ordonnance VARCHAR(100) NOT NULL, date_ordonnance DATETIME NOT NULL, date_expiration DATETIME NOT NULL, statut VARCHAR(50) NOT NULL, note_medical LONGTEXT DEFAULT NULL, INDEX IDX_924B326C50EAE44 (id_utilisateur), PRIMARY KEY(id_ordonnance)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE produit (id_produit INT AUTO_INCREMENT NOT NULL, nom VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, prix NUMERIC(10, 2) NOT NULL, quantite_stock INT NOT NULL, date_expiration DATETIME DEFAULT NULL, categorie VARCHAR(100) NOT NULL, image VARCHAR(255) DEFAULT NULL, statut VARCHAR(20) NOT NULL, PRIMARY KEY(id_produit)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE promotion (id_promotion INT AUTO_INCREMENT NOT NULL, titre VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, type_promotion VARCHAR(20) NOT NULL, valeur_reduction DOUBLE PRECISION NOT NULL, date_debut DATETIME NOT NULL, date_fin DATETIME NOT NULL, statut VARCHAR(20) NOT NULL, id_produit INT DEFAULT NULL, id_admin INT NOT NULL, PRIMARY KEY(id_promotion)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE question (id_question INT AUTO_INCREMENT NOT NULL, id_utilisateur INT NOT NULL, type_ticket VARCHAR(20) NOT NULL, objet VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, priorite VARCHAR(20) NOT NULL, statut VARCHAR(20) NOT NULL, file_name VARCHAR(255) DEFAULT NULL, file_path VARCHAR(255) DEFAULT NULL, file_type VARCHAR(100) DEFAULT NULL, file_size INT DEFAULT NULL, created_at DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', INDEX IDX_B6F7494E50EAE44 (id_utilisateur), PRIMARY KEY(id_question)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE response_question (id_reponse INT AUTO_INCREMENT NOT NULL, id_question INT NOT NULL, id_utilisateur INT DEFAULT NULL, auteur_type VARCHAR(20) NOT NULL, reponse_text LONGTEXT NOT NULL, reponse_role VARCHAR(30) NOT NULL, action_type VARCHAR(30) NOT NULL, impact_statut VARCHAR(20) NOT NULL, file_name VARCHAR(255) DEFAULT NULL, file_path VARCHAR(255) DEFAULT NULL, file_type VARCHAR(100) DEFAULT NULL, file_size INT DEFAULT NULL, created_at DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', lu_par_client TINYINT(1) DEFAULT 0 NOT NULL, INDEX IDX_1E1AF33E62CA5DB (id_question), INDEX IDX_1E1AF3350EAE44 (id_utilisateur), PRIMARY KEY(id_reponse)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE stock (id_stock INT AUTO_INCREMENT NOT NULL, seuil_alerte INT NOT NULL, date_expiration DATETIME NOT NULL, etat_stock VARCHAR(50) NOT NULL, date_derniere_mise_ajour DATETIME NOT NULL, PRIMARY KEY(id_stock)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE traitement (id_traitement INT AUTO_INCREMENT NOT NULL, id_ordonnance INT NOT NULL, id_utilisateur INT NOT NULL, dosage VARCHAR(255) DEFAULT NULL, frequence VARCHAR(255) DEFAULT NULL, duree_jours INT DEFAULT NULL, date_debut DATETIME DEFAULT NULL, date_fin DATETIME DEFAULT NULL, status VARCHAR(50) NOT NULL, notes LONGTEXT DEFAULT NULL, INDEX IDX_2A356D2737C1B2BB (id_ordonnance), INDEX IDX_2A356D2750EAE44 (id_utilisateur), PRIMARY KEY(id_traitement)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE utilisateur (id_utilisateur INT AUTO_INCREMENT NOT NULL, nom VARCHAR(255) NOT NULL, prenom VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, mot_de_passe VARCHAR(255) NOT NULL, role VARCHAR(50) NOT NULL, telephone VARCHAR(20) DEFAULT NULL, adresse VARCHAR(255) DEFAULT NULL, etat_compte VARCHAR(20) NOT NULL, date_creation DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', roles JSON NOT NULL, UNIQUE INDEX UNIQ_1D1C63B3E7927C74 (email), PRIMARY KEY(id_utilisateur)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('ALTER TABLE ordonnance ADD CONSTRAINT FK_924B326C50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');
        $this->addSql('ALTER TABLE question ADD CONSTRAINT FK_B6F7494E50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF33E62CA5DB FOREIGN KEY (id_question) REFERENCES question (id_question)');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF3350EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2737C1B2BB FOREIGN KEY (id_ordonnance) REFERENCES ordonnance (id_ordonnance)');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2750EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur)');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE ordonnance DROP FOREIGN KEY FK_924B326C50EAE44');
        $this->addSql('ALTER TABLE question DROP FOREIGN KEY FK_B6F7494E50EAE44');
        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF33E62CA5DB');
        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF3350EAE44');
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2737C1B2BB');
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2750EAE44');
        $this->addSql('DROP TABLE commande');
        $this->addSql('DROP TABLE depot');
        $this->addSql('DROP TABLE ordonnance');
        $this->addSql('DROP TABLE produit');
        $this->addSql('DROP TABLE promotion');
        $this->addSql('DROP TABLE question');
        $this->addSql('DROP TABLE response_question');
        $this->addSql('DROP TABLE stock');
        $this->addSql('DROP TABLE traitement');
        $this->addSql('DROP TABLE utilisateur');
    }
}
