-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Apr 11, 2026 at 09:42 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `pharmacie`
--

-- --------------------------------------------------------

--
-- Table structure for table `address`
--

CREATE TABLE `address` (
  `id` int(11) NOT NULL,
  `id_utilisateur_id` int(11) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `line1` varchar(255) NOT NULL,
  `line2` varchar(255) DEFAULT NULL,
  `city` varchar(120) NOT NULL,
  `region` varchar(120) NOT NULL,
  `postal_code` varchar(20) NOT NULL,
  `country` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `audit_log`
--

CREATE TABLE `audit_log` (
  `id` int(11) NOT NULL,
  `entity_type` varchar(100) NOT NULL,
  `entity_id` int(11) NOT NULL,
  `action` varchar(50) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `old_values` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`old_values`)),
  `new_values` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`new_values`)),
  `changed_fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`changed_fields`)),
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `commande`
--

CREATE TABLE `commande` (
  `id_commande` int(11) NOT NULL,
  `id_utilisateur_id` int(11) DEFAULT NULL,
  `date_commande` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `statut` varchar(50) NOT NULL DEFAULT 'en_attente',
  `total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `mode_paiement` varchar(50) NOT NULL,
  `adresse_livraison` varchar(255) NOT NULL,
  `telephone` varchar(20) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `message` longtext DEFAULT NULL,
  `produits_ids` longtext DEFAULT NULL,
  `coupon_code` varchar(64) DEFAULT NULL,
  `coupon_discount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `estimated_delivery_date` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `fraud_score` int(11) NOT NULL DEFAULT 0,
  `base_shipping_cost` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `coupon`
--

CREATE TABLE `coupon` (
  `id` int(11) NOT NULL,
  `code` varchar(64) NOT NULL,
  `type` varchar(20) NOT NULL,
  `valeur` decimal(10,2) NOT NULL,
  `date_expiration` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `usage_max` int(11) NOT NULL DEFAULT 1,
  `usage_count` int(11) NOT NULL DEFAULT 0,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `montant_minimum_panier` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `depot`
--

CREATE TABLE `depot` (
  `id_depot` int(11) NOT NULL,
  `nom_depot` varchar(255) NOT NULL,
  `adresse_depot` varchar(255) NOT NULL,
  `ville` varchar(100) DEFAULT NULL,
  `capacite_depot` int(11) NOT NULL,
  `responsable_depot` varchar(255) NOT NULL,
  `responsable_telephone` varchar(50) DEFAULT NULL,
  `date_creation` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260223010125', '2026-02-28 21:51:14', 1324),
('DoctrineMigrations\\Version20260223120000', '2026-02-28 21:51:16', 67),
('DoctrineMigrations\\Version20260227210000', '2026-02-28 21:51:16', 33),
('DoctrineMigrations\\Version20260228040000', '2026-02-28 21:51:16', 2);

-- --------------------------------------------------------

--
-- Table structure for table `order_shipment`
--

CREATE TABLE `order_shipment` (
  `id` int(11) NOT NULL,
  `id_commande_id` int(11) NOT NULL,
  `address_id` int(11) DEFAULT NULL,
  `items_json` longtext NOT NULL,
  `shipping_cost` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `ordonnance`
--

CREATE TABLE `ordonnance` (
  `id_ordonnance` int(11) NOT NULL,
  `numero_ordonnance` varchar(100) NOT NULL,
  `date_ordonnance` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `date_expiration` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `statut` varchar(50) NOT NULL,
  `note_medical` longtext DEFAULT NULL,
  `signature_electronique` tinyint(1) NOT NULL DEFAULT 0,
  `signature_date` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `signature_medecin` varchar(255) DEFAULT NULL,
  `docusign_envelope_id` varchar(255) DEFAULT NULL,
  `docusign_status` varchar(50) DEFAULT NULL,
  `signature_document_path` varchar(500) DEFAULT NULL,
  `signature_patient` longtext DEFAULT NULL,
  `signature_patient_date` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `signature_patient_ip` varchar(45) DEFAULT NULL,
  `id_utilisateur_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `produit`
--

CREATE TABLE `produit` (
  `id_produit` int(11) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `prix` decimal(10,2) NOT NULL,
  `quantite_stock` int(11) NOT NULL,
  `date_expiration` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `categorie` varchar(100) NOT NULL,
  `image` varchar(255) DEFAULT NULL,
  `statut` varchar(20) NOT NULL DEFAULT 'disponible'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `promotion`
--

CREATE TABLE `promotion` (
  `id_promotion` int(11) NOT NULL,
  `id_produit_id` int(11) DEFAULT NULL,
  `titre` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `valeur_reduction` double NOT NULL,
  `date_debut` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `date_fin` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `statut` varchar(20) NOT NULL,
  `id_admin` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `question`
--

CREATE TABLE `question` (
  `id_question` int(11) NOT NULL,
  `type_ticket` varchar(20) NOT NULL,
  `objet` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `priorite` varchar(20) NOT NULL,
  `statut` varchar(20) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `file_size` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `id_utilisateur_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `response_question`
--

CREATE TABLE `response_question` (
  `id_reponse` int(11) NOT NULL,
  `id_question_id` int(11) NOT NULL,
  `id_utilisateur_id` int(11) DEFAULT NULL,
  `auteur_type` varchar(20) NOT NULL,
  `reponse_text` longtext NOT NULL,
  `reponse_role` varchar(30) NOT NULL,
  `action_type` varchar(30) NOT NULL,
  `impact_statut` varchar(20) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `file_size` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `lu_par_client` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `stock`
--

CREATE TABLE `stock` (
  `id_stock` int(11) NOT NULL,
  `produit_id` int(11) DEFAULT NULL,
  `depot_id` int(11) DEFAULT NULL,
  `quantite` int(11) NOT NULL,
  `quantite_initiale` int(11) NOT NULL DEFAULT 0,
  `is_actif` tinyint(1) NOT NULL DEFAULT 1,
  `seuil_alerte` int(11) NOT NULL,
  `seuil_critique` int(11) NOT NULL,
  `date_entree` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `date_expiration` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `etat_stock` varchar(20) NOT NULL,
  `date_derniere_mise_a_jour` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `derniere_entree` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `derniere_sortie` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `total_entrees` int(11) NOT NULL DEFAULT 0,
  `total_sorties` int(11) NOT NULL DEFAULT 0,
  `prix_achat_unitaire` decimal(10,2) DEFAULT NULL,
  `prix_vente_unitaire` decimal(10,2) DEFAULT NULL,
  `emplacement` varchar(100) DEFAULT NULL,
  `batch_number` varchar(50) DEFAULT NULL,
  `qr_code_token` varchar(128) DEFAULT NULL,
  `qr_code_payload` longtext DEFAULT NULL,
  `fournisseur` varchar(100) DEFAULT NULL,
  `notes` longtext DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `stock_movement`
--

CREATE TABLE `stock_movement` (
  `id` int(11) NOT NULL,
  `id_stock_id` int(11) NOT NULL,
  `type` varchar(20) NOT NULL,
  `quantite` int(11) NOT NULL,
  `quantite_before` int(11) NOT NULL,
  `quantite_after` int(11) NOT NULL,
  `status` varchar(20) NOT NULL,
  `motif` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `traitement`
--

CREATE TABLE `traitement` (
  `id_traitement` int(11) NOT NULL,
  `id_utilisateur_id` int(11) DEFAULT NULL,
  `dosage` varchar(255) DEFAULT NULL,
  `frequence` varchar(255) DEFAULT NULL,
  `duree_jours` int(11) DEFAULT NULL,
  `date_debut` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `date_fin` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `status` varchar(50) NOT NULL,
  `notes` longtext DEFAULT NULL,
  `id_ordonnance_id` int(11) NOT NULL,
  `id_produit_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `utilisateur`
--

CREATE TABLE `utilisateur` (
  `id_utilisateur` int(11) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `etat_compte` varchar(20) NOT NULL,
  `date_creation` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `roles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`roles`)),
  `loyalty_points` int(11) NOT NULL DEFAULT 0,
  `loyalty_level` varchar(20) NOT NULL DEFAULT 'BRONZE',
  `segment` varchar(30) NOT NULL DEFAULT 'NEW_CUSTOMER',
  `last_activity_at` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `date_naissance` date DEFAULT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `avatar_url` longtext DEFAULT NULL,
  `avatar_seed` varchar(100) DEFAULT NULL,
  `avatar_config` longtext DEFAULT NULL,
  `has_seen_introduction` tinyint(1) NOT NULL DEFAULT 0,
  `reset_token` varchar(255) DEFAULT NULL,
  `reset_token_expires_at` datetime DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)',
  `totp_secret` varchar(255) DEFAULT NULL,
  `totp_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `backup_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`backup_codes`)),
  `student_id` varchar(100) DEFAULT NULL,
  `id_card_image` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `utilisateur`
--

INSERT INTO `utilisateur` (`id_utilisateur`, `nom`, `prenom`, `email`, `mot_de_passe`, `etat_compte`, `date_creation`, `roles`, `loyalty_points`, `loyalty_level`, `segment`, `last_activity_at`, `date_naissance`, `telephone`, `avatar_url`, `avatar_seed`, `avatar_config`, `has_seen_introduction`, `reset_token`, `reset_token_expires_at`, `totp_secret`, `totp_enabled`, `backup_codes`, `student_id`, `id_card_image`) VALUES
(1, 'iheb', 'ben jbir', 'ihebjbir10@gmail.com', '$2y$13$di7nD.wZFCN91cCALH7Q4.4.NT7Iocfd.mUKsDCkRY9irlR71aUv.', 'actif', '2026-02-28 21:55:59', '[\"ROLE_ADMIN\"]', 0, 'BRONZE', 'NEW_CUSTOMER', NULL, NULL, NULL, 'https://api.dicebear.com/7.x/bottts/svg?seed=avatar_69a35810f1734&scale=80', 'bottts|avatar_69a35810f1734', 0, NULL, NULL, 'EFWYROFE7I3KD6THK6AU6TRIAY5MGQ5YK7G4PCSR362QFDGRP5JIBZ6BKY26XM2JD56CVFPGCJBDLEP4RKJLNGYAPZWS23JKHINO3AI', 1, NULL, '231JMTO0405', 'uploads/id_cards/9c4ef0a4-dfba-41d9-aab9-72d522e3721b-69b455a53122c.jpg');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `address`
--
ALTER TABLE `address`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_D4E6F81C6EE5C49` (`id_utilisateur_id`);

--
-- Indexes for table `audit_log`
--
ALTER TABLE `audit_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_entity` (`entity_type`,`entity_id`),
  ADD KEY `idx_created_at` (`created_at`),
  ADD KEY `idx_user` (`user_id`);

--
-- Indexes for table `commande`
--
ALTER TABLE `commande`
  ADD PRIMARY KEY (`id_commande`),
  ADD KEY `IDX_6EEAA67DC6EE5C49` (`id_utilisateur_id`);

--
-- Indexes for table `coupon`
--
ALTER TABLE `coupon`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_coupon_code` (`code`);

--
-- Indexes for table `depot`
--
ALTER TABLE `depot`
  ADD PRIMARY KEY (`id_depot`);

--
-- Indexes for table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Indexes for table `order_shipment`
--
ALTER TABLE `order_shipment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_E333C26DF5B7AF75` (`address_id`),
  ADD KEY `IDX_E333C26D9AF8E3A3` (`id_commande_id`);

--
-- Indexes for table `ordonnance`
--
ALTER TABLE `ordonnance`
  ADD PRIMARY KEY (`id_ordonnance`),
  ADD KEY `IDX_924B326CC6EE5C49` (`id_utilisateur_id`);

--
-- Indexes for table `produit`
--
ALTER TABLE `produit`
  ADD PRIMARY KEY (`id_produit`);

--
-- Indexes for table `promotion`
--
ALTER TABLE `promotion`
  ADD PRIMARY KEY (`id_promotion`),
  ADD KEY `IDX_C11D7DD1AABEFE2C` (`id_produit_id`);

--
-- Indexes for table `question`
--
ALTER TABLE `question`
  ADD PRIMARY KEY (`id_question`),
  ADD KEY `IDX_B6F7494EC6EE5C49` (`id_utilisateur_id`);

--
-- Indexes for table `response_question`
--
ALTER TABLE `response_question`
  ADD PRIMARY KEY (`id_reponse`),
  ADD KEY `IDX_1E1AF336353B48` (`id_question_id`),
  ADD KEY `IDX_1E1AF33C6EE5C49` (`id_utilisateur_id`);

--
-- Indexes for table `stock`
--
ALTER TABLE `stock`
  ADD PRIMARY KEY (`id_stock`),
  ADD UNIQUE KEY `uniq_stock_lot_depot` (`batch_number`,`depot_id`),
  ADD UNIQUE KEY `uniq_stock_qr_token` (`qr_code_token`),
  ADD KEY `IDX_4B365660F347EFB` (`produit_id`),
  ADD KEY `IDX_4B3656608510D4DE` (`depot_id`);

--
-- Indexes for table `stock_movement`
--
ALTER TABLE `stock_movement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_BB1BC1B55D168D85` (`id_stock_id`);

--
-- Indexes for table `traitement`
--
ALTER TABLE `traitement`
  ADD PRIMARY KEY (`id_traitement`),
  ADD KEY `IDX_2A356D2795DAEAEA` (`id_ordonnance_id`),
  ADD KEY `IDX_2A356D27C6EE5C49` (`id_utilisateur_id`),
  ADD KEY `IDX_2A356D27AABEFE2C` (`id_produit_id`);

--
-- Indexes for table `utilisateur`
--
ALTER TABLE `utilisateur`
  ADD PRIMARY KEY (`id_utilisateur`),
  ADD UNIQUE KEY `UNIQ_1D1C63B3E7927C74` (`email`),
  ADD UNIQUE KEY `UNIQ_1D1C63B34DCD58BC` (`student_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `address`
--
ALTER TABLE `address`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `audit_log`
--
ALTER TABLE `audit_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `commande`
--
ALTER TABLE `commande`
  MODIFY `id_commande` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `coupon`
--
ALTER TABLE `coupon`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `depot`
--
ALTER TABLE `depot`
  MODIFY `id_depot` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `order_shipment`
--
ALTER TABLE `order_shipment`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `ordonnance`
--
ALTER TABLE `ordonnance`
  MODIFY `id_ordonnance` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `produit`
--
ALTER TABLE `produit`
  MODIFY `id_produit` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `promotion`
--
ALTER TABLE `promotion`
  MODIFY `id_promotion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `question`
--
ALTER TABLE `question`
  MODIFY `id_question` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `response_question`
--
ALTER TABLE `response_question`
  MODIFY `id_reponse` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `stock`
--
ALTER TABLE `stock`
  MODIFY `id_stock` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `stock_movement`
--
ALTER TABLE `stock_movement`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `traitement`
--
ALTER TABLE `traitement`
  MODIFY `id_traitement` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `utilisateur`
--
ALTER TABLE `utilisateur`
  MODIFY `id_utilisateur` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `address`
--
ALTER TABLE `address`
  ADD CONSTRAINT `FK_D4E6F81C6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE SET NULL;

--
-- Constraints for table `commande`
--
ALTER TABLE `commande`
  ADD CONSTRAINT `FK_6EEAA67DC6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE SET NULL;

--
-- Constraints for table `order_shipment`
--
ALTER TABLE `order_shipment`
  ADD CONSTRAINT `FK_E333C26D9AF8E3A3` FOREIGN KEY (`id_commande_id`) REFERENCES `commande` (`id_commande`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_E333C26DF5B7AF75` FOREIGN KEY (`address_id`) REFERENCES `address` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `ordonnance`
--
ALTER TABLE `ordonnance`
  ADD CONSTRAINT `FK_924B326CC6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE CASCADE;

--
-- Constraints for table `promotion`
--
ALTER TABLE `promotion`
  ADD CONSTRAINT `FK_C11D7DD1AABEFE2C` FOREIGN KEY (`id_produit_id`) REFERENCES `produit` (`id_produit`);

--
-- Constraints for table `question`
--
ALTER TABLE `question`
  ADD CONSTRAINT `FK_B6F7494EC6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE CASCADE;

--
-- Constraints for table `response_question`
--
ALTER TABLE `response_question`
  ADD CONSTRAINT `FK_1E1AF336353B48` FOREIGN KEY (`id_question_id`) REFERENCES `question` (`id_question`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_1E1AF33C6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE SET NULL;

--
-- Constraints for table `stock`
--
ALTER TABLE `stock`
  ADD CONSTRAINT `FK_4B3656608510D4DE` FOREIGN KEY (`depot_id`) REFERENCES `depot` (`id_depot`),
  ADD CONSTRAINT `FK_4B365660F347EFB` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id_produit`);

--
-- Constraints for table `stock_movement`
--
ALTER TABLE `stock_movement`
  ADD CONSTRAINT `FK_BB1BC1B55D168D85` FOREIGN KEY (`id_stock_id`) REFERENCES `stock` (`id_stock`) ON DELETE CASCADE;

--
-- Constraints for table `traitement`
--
ALTER TABLE `traitement`
  ADD CONSTRAINT `FK_2A356D2795DAEAEA` FOREIGN KEY (`id_ordonnance_id`) REFERENCES `ordonnance` (`id_ordonnance`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_2A356D27AABEFE2C` FOREIGN KEY (`id_produit_id`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_2A356D27C6EE5C49` FOREIGN KEY (`id_utilisateur_id`) REFERENCES `utilisateur` (`id_utilisateur`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
