-- ========================================
-- SQL DE TEST - SYSTEME DE RESERVATION
-- ========================================
-- Exécutez ce script après avoir lancé l'application une fois
-- pour avoir des données de test

USE pharmacie;

-- Vérifier que la table existe
SELECT 'Vérification de la table reservation...' as 'Status';
SHOW TABLES LIKE 'reservation';

-- Afficher la structure
DESCRIBE reservation;

-- Afficher le contenu (réservations)
SELECT
    r.id,
    r.nom_client,
    r.email_client,
    r.date_rendez_vous,
    r.statut,
    s.nom_service
FROM reservation r
JOIN service s ON r.service_id = s.id_service
ORDER BY r.date_rendez_vous DESC;

-- Statistiques
SELECT
    'STATISTIQUES' as 'Métriques',
    COUNT(*) as 'Total réservations',
    SUM(CASE WHEN statut = 'En attente' THEN 1 ELSE 0 END) as 'En attente',
    SUM(CASE WHEN statut = 'Confirmée' THEN 1 ELSE 0 END) as 'Confirmées',
    SUM(CASE WHEN statut = 'Annulée' THEN 1 ELSE 0 END) as 'Annulées'
FROM reservation;

-- Vérifier les services disponibles pour réservation
SELECT
    'SERVICES DISPONIBLES' as 'Liste',
    id_service,
    nom_service,
    type_service,
    specialite,
    email
FROM service
ORDER BY nom_service;

-- Dernières réservations (dernières 10)
SELECT
    'DERNIÈRES RÉSERVATIONS' as 'Historique',
    id,
    nom_client,
    email_client,
    date_rendez_vous,
    statut,
    date_creation
FROM reservation
ORDER BY date_creation DESC
LIMIT 10;

