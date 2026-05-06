-- Script SQL pour insérer des données de test dans le front-office CuraVita
-- À exécuter après avoir créé les tables

-- ============================================
-- DONNÉES DE TEST - DÉPÔTS
-- ============================================

INSERT INTO depot (nom_depot, adresse_depot, ville, capacite_depot, responsable_depot, responsable_telephone) VALUES
('Dépôt Tunis Centre', 'Avenue Habib Bourguiba, 123', 'Tunis', 5000, 'Ahmed Ben Ali', '+216 71 123 456'),
('Dépôt Sfax Sud', 'Rue de la République, 456', 'Sfax', 3500, 'Fatma Trabelsi', '+216 74 234 567'),
('Dépôt Sousse Nord', 'Boulevard du 7 Novembre, 789', 'Sousse', 2800, 'Mohamed Chaabane', '+216 73 345 678'),
('Dépôt Tunis Nord', 'Rue de Carthage, 321', 'Tunis', 4200, 'Leila Mansouri', '+216 71 456 789'),
('Dépôt Sfax Centre', 'Avenue Farhat Hached, 654', 'Sfax', 3100, 'Karim Jelassi', '+216 74 567 890');

-- ============================================
-- DONNÉES DE TEST - PRODUITS
-- ============================================

INSERT INTO produit (nom, description, prix, categorie, stock_min, stock_max, date_creation) VALUES
('Aspirine 500mg', 'Analgésique et antipyrétique', 2.50, 'Analgésiques', 50, 500, NOW()),
('Paracétamol 1000mg', 'Antalgique et antipyrétique', 3.00, 'Antalgiques', 40, 400, NOW()),
('Ibuprofène 400mg', 'Anti-inflammatoire non stéroïdien', 4.50, 'Anti-inflammatoires', 30, 300, NOW()),
('Amoxicilline 500mg', 'Antibiotique', 8.00, 'Antibiotiques', 20, 200, NOW()),
('Vitamine C 1000mg', 'Supplément vitaminique', 6.50, 'Vitamines', 25, 250, NOW()),
('Crème solaire SPF50', 'Protection solaire haute', 15.00, 'Dermatologie', 15, 150, NOW()),
('Sirop pour la toux', 'Antitussif', 7.50, 'Respiratoire', 35, 350, NOW()),
('Bande de contention', 'Support médical', 12.00, 'Orthopédie', 10, 100, NOW()),
('Thermomètre électronique', 'Mesure de température', 25.00, 'Matériel médical', 8, 80, NOW()),
('Pansements adhésifs', 'Pansements stériles', 5.50, 'Pansements', 60, 600, NOW());

-- ============================================
-- DONNÉES DE TEST - STOCKS
-- ============================================

-- Stocks pour Dépôt Tunis Centre (id_depot = 1)
INSERT INTO stock (produit_id, depot_id, quantite, quantite_initiale, seuil_alerte, seuil_critique, date_entree, etat_stock, date_derniere_mise_a_jour) VALUES
(1, 1, 250, 250, 50, 50, NOW(), 'actif', NOW()),  -- Aspirine - En stock
(2, 1, 180, 180, 40, 40, NOW(), 'actif', NOW()),  -- Paracétamol - En stock
(3, 1, 45, 45, 30, 30, NOW(), 'actif', NOW()),    -- Ibuprofène - Stock faible
(4, 1, 0, 0, 20, 20, NOW(), 'actif', NOW()),      -- Amoxicilline - Rupture
(5, 1, 120, 120, 25, 25, NOW(), 'actif', NOW()),  -- Vitamine C - En stock
(6, 1, 75, 75, 15, 15, NOW(), 'actif', NOW()),    -- Crème solaire - En stock
(7, 1, 200, 200, 35, 35, NOW(), 'actif', NOW()),  -- Sirop - En stock
(8, 1, 5, 5, 10, 10, NOW(), 'actif', NOW()),      -- Bande - Stock faible
(9, 1, 50, 50, 8, 8, NOW(), 'actif', NOW()),      -- Thermomètre - En stock
(10, 1, 300, 300, 60, 60, NOW(), 'actif', NOW()); -- Pansements - En stock

-- Stocks pour Dépôt Sfax Sud (id_depot = 2)
INSERT INTO stock (produit_id, depot_id, quantite, quantite_initiale, seuil_alerte, seuil_critique, date_entree, etat_stock, date_derniere_mise_a_jour) VALUES
(1, 2, 180, 180, 50, 50, NOW(), 'actif', NOW()),  -- Aspirine - En stock
(2, 2, 35, 35, 40, 40, NOW(), 'actif', NOW()),    -- Paracétamol - Stock faible
(3, 2, 120, 120, 30, 30, NOW(), 'actif', NOW()),  -- Ibuprofène - En stock
(4, 2, 80, 80, 20, 20, NOW(), 'actif', NOW()),    -- Amoxicilline - En stock
(5, 2, 0, 0, 25, 25, NOW(), 'actif', NOW()),      -- Vitamine C - Rupture
(6, 2, 25, 25, 15, 15, NOW(), 'actif', NOW()),    -- Crème solaire - En stock
(7, 2, 150, 150, 35, 35, NOW(), 'actif', NOW()),  -- Sirop - En stock
(8, 2, 40, 40, 10, 10, NOW(), 'actif', NOW()),    -- Bande - En stock
(9, 2, 15, 15, 8, 8, NOW(), 'actif', NOW()),      -- Thermomètre - Stock faible
(10, 2, 450, 450, 60, 60, NOW(), 'actif', NOW()); -- Pansements - En stock

-- Stocks pour Dépôt Sousse Nord (id_depot = 3)
INSERT INTO stock (produit_id, depot_id, quantite, quantite_initiale, seuil_alerte, seuil_critique, date_entree, etat_stock, date_derniere_mise_a_jour) VALUES
(1, 3, 80, 80, 50, 50, NOW(), 'actif', NOW()),    -- Aspirine - En stock
(2, 3, 100, 100, 40, 40, NOW(), 'actif', NOW()),  -- Paracétamol - En stock
(3, 3, 0, 0, 30, 30, NOW(), 'actif', NOW()),      -- Ibuprofène - Rupture
(4, 3, 60, 60, 20, 20, NOW(), 'actif', NOW()),    -- Amoxicilline - En stock
(5, 3, 90, 90, 25, 25, NOW(), 'actif', NOW()),    -- Vitamine C - En stock
(6, 3, 50, 50, 15, 15, NOW(), 'actif', NOW()),    -- Crème solaire - En stock
(7, 3, 80, 80, 35, 35, NOW(), 'actif', NOW()),    -- Sirop - En stock
(8, 3, 20, 20, 10, 10, NOW(), 'actif', NOW()),    -- Bande - En stock
(9, 3, 30, 30, 8, 8, NOW(), 'actif', NOW()),      -- Thermomètre - En stock
(10, 3, 180, 180, 60, 60, NOW(), 'actif', NOW()); -- Pansements - En stock

-- ============================================
-- DONNÉES DE TEST - SERVICES
-- ============================================

INSERT INTO service (nom_service, type_service, specialite, telephone, email, adresse, date_creation) VALUES
('Dr. Mohamed Belaid', 'Médecin', 'Cardiologie', '+216 71 234 567', 'm.belaid@curavita.tn', 'Rue de la Santé, 45, Tunis', NOW()),
('Dr. Leila Mansouri', 'Médecin', 'Pédiatrie', '+216 71 345 678', 'l.mansouri@curavita.tn', 'Avenue de la République, 78, Tunis', NOW()),
('Dr. Karim Jelassi', 'Médecin', 'Dermatologie', '+216 74 456 789', 'k.jelassi@curavita.tn', 'Boulevard Farhat Hached, 12, Sfax', NOW()),
('Dr. Fatma Trabelsi', 'Médecin', 'Gynécologie', '+216 73 567 890', 'f.trabelsi@curavita.tn', 'Rue de l''Indépendance, 34, Sousse', NOW()),
('Dr. Ahmed Ben Ali', 'Médecin', 'Ophtalmologie', '+216 71 678 901', 'a.benali@curavita.tn', 'Place de la Kasbah, 56, Tunis', NOW()),
('Dr. Sonia Mejri', 'Médecin', 'Endocrinologie', '+216 74 789 012', 's.mejri@curavita.tn', 'Rue de la Victoire, 89, Sfax', NOW()),
('Dr. Hassen Chaabane', 'Médecin', 'Neurologie', '+216 73 890 123', 'h.chaabane@curavita.tn', 'Avenue du 14 Janvier, 67, Sousse', NOW()),
('Dr. Nadia Gharbi', 'Médecin', 'Rhumatologie', '+216 71 901 234', 'n.gharbi@curavita.tn', 'Rue de Rome, 23, Tunis', NOW()),
('Inf. Samir Bouslama', 'Infirmier', 'Soins généraux', '+216 74 012 345', 's.bouslama@curavita.tn', 'Rue de la Gare, 45, Sfax', NOW()),
('Inf. Amina Khaldi', 'Infirmier', 'Soins intensifs', '+216 73 123 456', 'a.khaldi@curavita.tn', 'Boulevard du 7 Novembre, 78, Sousse', NOW()),
('Inf. Mohamed Salah', 'Infirmier', 'Pédiatrie', '+216 71 234 567', 'm.salah@curavita.tn', 'Rue de Paris, 12, Tunis', NOW()),
('Inf. Raja Ben Amor', 'Infirmier', 'Gériatrie', '+216 74 345 678', 'r.benamor@curavita.tn', 'Avenue de la Liberté, 34, Sfax', NOW());

-- ============================================
-- VÉRIFICATION DES DONNÉES INSÉRÉES
-- ============================================

-- Vérifier les dépôts
SELECT 'Dépôts insérés:' as info, COUNT(*) as count FROM depot;

-- Vérifier les produits
SELECT 'Produits insérés:' as info, COUNT(*) as count FROM produit;

-- Vérifier les stocks
SELECT 'Stocks insérés:' as info, COUNT(*) as count FROM stock;

-- Vérifier les services
SELECT 'Services insérés:' as info, COUNT(*) as count FROM service;

-- Vérifier la distribution des stocks par dépôt
SELECT d.nom_depot, COUNT(s.id_stock) as nombre_stocks
FROM depot d
LEFT JOIN stock s ON d.id_depot = s.depot_id
GROUP BY d.id_depot, d.nom_depot
ORDER BY d.nom_depot;

-- Vérifier les statuts des stocks
SELECT
    SUM(CASE WHEN s.quantite > s.seuil_alerte THEN 1 ELSE 0 END) as en_stock,
    SUM(CASE WHEN s.quantite <= s.seuil_alerte AND s.quantite > 0 THEN 1 ELSE 0 END) as stock_faible,
    SUM(CASE WHEN s.quantite = 0 THEN 1 ELSE 0 END) as rupture
FROM stock s;

COMMIT;
