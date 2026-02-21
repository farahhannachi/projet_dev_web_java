<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260216235119 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('CREATE TABLE commande (id_commande INT AUTO_INCREMENT NOT NULL, date_commande DATETIME NOT NULL, statut VARCHAR(50) DEFAULT \'en_attente\' NOT NULL, total NUMERIC(10, 2) DEFAULT \'0\' NOT NULL, mode_paiement VARCHAR(50) NOT NULL, adresse_livraison VARCHAR(255) NOT NULL, telephone VARCHAR(20) NOT NULL, nom VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, message LONGTEXT DEFAULT NULL, produits_ids LONGTEXT DEFAULT NULL, PRIMARY KEY(id_commande)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE depot (id_depot INT AUTO_INCREMENT NOT NULL, nom_depot VARCHAR(255) NOT NULL, adresse_depot VARCHAR(255) NOT NULL, ville VARCHAR(100) DEFAULT NULL, capacite_depot INT NOT NULL, responsable_depot VARCHAR(255) NOT NULL, date_creation DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', PRIMARY KEY(id_depot)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE ordonnance (id_ordonnance INT AUTO_INCREMENT NOT NULL, id_utilisateur INT NOT NULL, numero_ordonnance VARCHAR(100) NOT NULL, date_ordonnance DATETIME NOT NULL, date_expiration DATETIME NOT NULL, statut VARCHAR(50) NOT NULL, note_medical LONGTEXT DEFAULT NULL, INDEX IDX_924B326C50EAE44 (id_utilisateur), PRIMARY KEY(id_ordonnance)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE produit (id_produit INT AUTO_INCREMENT NOT NULL, nom VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, prix NUMERIC(10, 2) NOT NULL, quantite_stock INT NOT NULL, date_expiration DATETIME NOT NULL, categorie VARCHAR(100) NOT NULL, image VARCHAR(255) DEFAULT NULL, statut VARCHAR(20) DEFAULT \'disponible\' NOT NULL, PRIMARY KEY(id_produit)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE promotion (id_promotion INT AUTO_INCREMENT NOT NULL, id_produit INT DEFAULT NULL, titre VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, valeur_reduction DOUBLE PRECISION NOT NULL, date_debut DATETIME NOT NULL, date_fin DATETIME NOT NULL, statut VARCHAR(20) NOT NULL, id_admin INT NOT NULL, INDEX IDX_C11D7DD1F7384557 (id_produit), PRIMARY KEY(id_promotion)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE question (id_question INT AUTO_INCREMENT NOT NULL, id_utilisateur INT NOT NULL, type_ticket VARCHAR(20) NOT NULL, objet VARCHAR(255) NOT NULL, description LONGTEXT NOT NULL, priorite VARCHAR(20) NOT NULL, statut VARCHAR(20) NOT NULL, file_name VARCHAR(255) DEFAULT NULL, file_path VARCHAR(255) DEFAULT NULL, file_type VARCHAR(100) DEFAULT NULL, file_size INT DEFAULT NULL, created_at DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', INDEX IDX_B6F7494E50EAE44 (id_utilisateur), PRIMARY KEY(id_question)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE response_question (id_reponse INT AUTO_INCREMENT NOT NULL, id_question INT NOT NULL, id_utilisateur INT DEFAULT NULL, auteur_type VARCHAR(20) NOT NULL, reponse_text LONGTEXT NOT NULL, reponse_role VARCHAR(30) NOT NULL, action_type VARCHAR(30) NOT NULL, impact_statut VARCHAR(20) NOT NULL, file_name VARCHAR(255) DEFAULT NULL, file_path VARCHAR(255) DEFAULT NULL, file_type VARCHAR(100) DEFAULT NULL, file_size INT DEFAULT NULL, created_at DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', lu_par_client TINYINT(1) DEFAULT 0 NOT NULL, INDEX IDX_1E1AF33E62CA5DB (id_question), INDEX IDX_1E1AF3350EAE44 (id_utilisateur), PRIMARY KEY(id_reponse)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE stock (id_stock INT AUTO_INCREMENT NOT NULL, produit_id INT DEFAULT NULL, depot_id INT DEFAULT NULL, quantite INT NOT NULL, seuil_alerte INT NOT NULL, seuil_critique INT NOT NULL, date_entree DATETIME NOT NULL, date_expiration DATETIME DEFAULT NULL, etat_stock VARCHAR(20) NOT NULL, date_derniere_mise_a_jour DATETIME NOT NULL, derniere_entree DATETIME DEFAULT NULL, derniere_sortie DATETIME DEFAULT NULL, total_entrees INT DEFAULT 0 NOT NULL, total_sorties INT DEFAULT 0 NOT NULL, prix_achat_unitaire NUMERIC(10, 2) DEFAULT NULL, prix_vente_unitaire NUMERIC(10, 2) DEFAULT NULL, emplacement VARCHAR(100) DEFAULT NULL, batch_number VARCHAR(50) DEFAULT NULL, fournisseur VARCHAR(100) DEFAULT NULL, notes LONGTEXT DEFAULT NULL, INDEX IDX_4B365660F347EFB (produit_id), INDEX IDX_4B3656608510D4DE (depot_id), PRIMARY KEY(id_stock)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE traitement (id_traitement INT AUTO_INCREMENT NOT NULL, id_ordonnance INT NOT NULL, id_utilisateur INT NOT NULL, id_produit INT DEFAULT NULL, dosage VARCHAR(255) DEFAULT NULL, frequence VARCHAR(255) DEFAULT NULL, duree_jours INT DEFAULT NULL, date_debut DATETIME DEFAULT NULL, date_fin DATETIME DEFAULT NULL, status VARCHAR(50) NOT NULL, notes LONGTEXT DEFAULT NULL, INDEX IDX_2A356D2737C1B2BB (id_ordonnance), INDEX IDX_2A356D2750EAE44 (id_utilisateur), INDEX IDX_2A356D27F7384557 (id_produit), PRIMARY KEY(id_traitement)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('CREATE TABLE utilisateur (id_utilisateur INT AUTO_INCREMENT NOT NULL, nom VARCHAR(255) NOT NULL, prenom VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, mot_de_passe VARCHAR(255) NOT NULL, etat_compte VARCHAR(20) NOT NULL, date_creation DATETIME NOT NULL COMMENT \'(DC2Type:datetime_immutable)\', roles JSON NOT NULL, UNIQUE INDEX UNIQ_1D1C63B3E7927C74 (email), PRIMARY KEY(id_utilisateur)) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');
        $this->addSql('ALTER TABLE ordonnance ADD CONSTRAINT FK_924B326C50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE promotion ADD CONSTRAINT FK_C11D7DD1F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit)');
        $this->addSql('ALTER TABLE question ADD CONSTRAINT FK_B6F7494E50EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF33E62CA5DB FOREIGN KEY (id_question) REFERENCES question (id_question)');
        $this->addSql('ALTER TABLE response_question ADD CONSTRAINT FK_1E1AF3350EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL');
        $this->addSql('ALTER TABLE stock ADD CONSTRAINT FK_4B365660F347EFB FOREIGN KEY (produit_id) REFERENCES produit (id_produit)');
        $this->addSql('ALTER TABLE stock ADD CONSTRAINT FK_4B3656608510D4DE FOREIGN KEY (depot_id) REFERENCES depot (id_depot)');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2737C1B2BB FOREIGN KEY (id_ordonnance) REFERENCES ordonnance (id_ordonnance)');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D2750EAE44 FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE traitement ADD CONSTRAINT FK_2A356D27F7384557 FOREIGN KEY (id_produit) REFERENCES produit (id_produit)');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE ordonnance DROP FOREIGN KEY FK_924B326C50EAE44');
        $this->addSql('ALTER TABLE promotion DROP FOREIGN KEY FK_C11D7DD1F7384557');
        $this->addSql('ALTER TABLE question DROP FOREIGN KEY FK_B6F7494E50EAE44');
        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF33E62CA5DB');
        $this->addSql('ALTER TABLE response_question DROP FOREIGN KEY FK_1E1AF3350EAE44');
        $this->addSql('ALTER TABLE stock DROP FOREIGN KEY FK_4B365660F347EFB');
        $this->addSql('ALTER TABLE stock DROP FOREIGN KEY FK_4B3656608510D4DE');
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2737C1B2BB');
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D2750EAE44');
        $this->addSql('ALTER TABLE traitement DROP FOREIGN KEY FK_2A356D27F7384557');
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
