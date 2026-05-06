-- ============================================
-- VÉRIFICATION DU SYSTÈME DE CONSOMMATION
-- ============================================

USE pharmacie;

-- 1️⃣ Vérifier que la table stock_movement a les bonnes colonnes
SELECT '📋 Structure de stock_movement:' AS 'Vérification';
DESCRIBE stock_movement;

-- 2️⃣ Vérifier les clés étrangères
SELECT '🔗 Clés étrangères:' AS 'Vérification';
SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'stock_movement';

-- 3️⃣ Vérifier les indexes
SELECT '📊 Indexes:' AS 'Vérification';
SHOW INDEXES FROM stock_movement;

-- 4️⃣ Vérifier que les tables existence
SELECT '✅ Vérification des tables requises:' AS 'Vérification';
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'pharmacie'
AND TABLE_NAME IN ('service', 'stock', 'stock_movement', 'produit', 'depot');

-- 5️⃣ Compter les mouvements existants
SELECT '📈 Statistiques actuelles:' AS 'Vérification';
SELECT
    'Total mouvements' as Statistique, COUNT(*) as Valeur
FROM stock_movement
UNION ALL
SELECT 'Total services', COUNT(*) FROM service
UNION ALL
SELECT 'Total stocks', COUNT(*) FROM stock
UNION ALL
SELECT 'Total produits', COUNT(*) FROM produit;

-- 6️⃣ Exemple de requête : Historique d'un service
SELECT '📝 Exemple requête - Historique service 1:' AS 'Vérification';
SELECT
    m.id,
    m.created_at,
    s.nom_service,
    p.nom as produit,
    d.nom_depot as depot,
    m.quantite,
    m.quantite_before as 'avant',
    m.quantite_after as 'après',
    m.motif,
    m.reference_document,
    m.status
FROM stock_movement m
LEFT JOIN service s ON m.id_service = s.id_service
LEFT JOIN stock st ON m.id_stock_id = st.id_stock
LEFT JOIN produit p ON st.produit_id = p.id_produit
LEFT JOIN depot d ON st.depot_id = d.id_depot
WHERE m.id_service = 1
ORDER BY m.created_at DESC
LIMIT 5;

-- 7️⃣ Vérifier les mouvements des 7 derniers jours
SELECT '🔍 Mouvements des 7 derniers jours:' AS 'Vérification';
SELECT
    DATE(created_at) as jour,
    COUNT(*) as nombre_mouvements,
    SUM(quantite) as quantite_totale
FROM stock_movement
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
AND type_consommation = 'CONSOMMATION_SERVICE'
GROUP BY DATE(created_at)
ORDER BY jour DESC;

-- ============================================
-- ✅ Si toutes les requêtes passent sans erreur,
-- le système est correctement installé !
-- ============================================

