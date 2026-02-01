-- ============================================
-- Script d'insertion de données de test
-- Module: Ordonnances & Suivi Médical
-- ============================================

-- Insertion d'ordonnances de test
INSERT INTO ordonnance (client_id, validated_by_id, file_name, file_path, status, rejection_reason, uploaded_at, validated_at) VALUES
(1, NULL, 'ordonnance_001.pdf', 'prescriptions/2026/02/ordonnance_001.pdf', 'pending_validation', NULL, '2026-02-01 10:00:00', NULL),
(1, 2, 'ordonnance_002.pdf', 'prescriptions/2026/02/ordonnance_002.pdf', 'validated', NULL, '2026-01-28 14:30:00', '2026-01-29 09:15:00'),
(2, 2, 'ordonnance_003.jpg', 'prescriptions/2026/01/ordonnance_003.jpg', 'validated', NULL, '2026-01-25 11:20:00', '2026-01-26 10:00:00'),
(3, NULL, 'ordonnance_004.pdf', 'prescriptions/2026/02/ordonnance_004.pdf', 'pending_validation', NULL, '2026-02-01 15:45:00', NULL),
(2, 2, 'ordonnance_005.png', 'prescriptions/2026/01/ordonnance_005.png', 'rejected', 'Document illisible, veuillez téléverser une meilleure qualité', '2026-01-20 16:00:00', '2026-01-21 08:30:00');

-- Insertion de traitements de test (liés aux ordonnances validées)
INSERT INTO traitement (ordonnance_id, client_id, dosage, frequency, duration_days, start_date, end_date, is_active, is_completed, notes) VALUES
-- Traitements pour l'ordonnance 2 (client 1)
(2, 1, '500mg', '3 fois par jour', 7, '2026-01-29 00:00:00', '2026-02-05 00:00:00', 1, 0, 'Prendre après les repas'),
(2, 1, '10mg', '1 fois par jour le soir', 30, '2026-01-29 00:00:00', '2026-02-28 00:00:00', 1, 0, 'Avant le coucher'),

-- Traitements pour l'ordonnance 3 (client 2)
(3, 2, '250mg', '2 fois par jour', 14, '2026-01-26 00:00:00', '2026-02-09 00:00:00', 1, 0, 'Matin et soir'),
(3, 2, '5ml', '3 fois par jour', 10, '2026-01-26 00:00:00', '2026-02-05 00:00:00', 1, 0, 'Sirop contre la toux'),

-- Traitement complété (ancien)
(3, 2, '100mg', '1 fois par jour', 5, '2026-01-15 00:00:00', '2026-01-20 00:00:00', 0, 1, 'Traitement terminé avec succès');

-- ============================================
-- Vérification des données insérées
-- ============================================

-- Compter les ordonnances par statut
SELECT status, COUNT(*) as count 
FROM ordonnance 
GROUP BY status;

-- Compter les traitements actifs
SELECT is_active, is_completed, COUNT(*) as count 
FROM traitement 
GROUP BY is_active, is_completed;

-- Afficher les ordonnances avec leurs traitements
SELECT 
    o.id as ordonnance_id,
    o.file_name,
    o.status,
    o.client_id,
    COUNT(t.id) as nombre_traitements
FROM ordonnance o
LEFT JOIN traitement t ON t.ordonnance_id = o.id
GROUP BY o.id, o.file_name, o.status, o.client_id
ORDER BY o.id;

-- Afficher les traitements actifs avec leurs ordonnances
SELECT 
    t.id as traitement_id,
    t.dosage,
    t.frequency,
    t.duration_days,
    t.start_date,
    t.end_date,
    o.file_name as ordonnance_file,
    o.status as ordonnance_status
FROM traitement t
INNER JOIN ordonnance o ON t.ordonnance_id = o.id
WHERE t.is_active = 1
ORDER BY t.start_date DESC;
