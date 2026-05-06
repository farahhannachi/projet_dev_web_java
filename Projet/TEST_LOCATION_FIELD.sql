-- ============================================
-- TEST SCRIPT FOR LOCATION FIELD IN DEPOT TABLE
-- ============================================
-- This script demonstrates how to use the location_name field
-- in the depot table of the pharmacie database

-- ============================================
-- 1. VERIFY THE LOCATION COLUMN EXISTS
-- ============================================
DESCRIBE depot;

-- Should show location_name column:
-- location_name | varchar(500) | YES  |     | NULL    | 

-- ============================================
-- 2. INSERT NEW DEPOT WITH LOCATION NAME
-- ============================================
INSERT INTO depot (
    nom_depot,
    adresse_depot,
    ville,
    capacite_depot,
    responsable_depot,
    responsable_telephone,
    date_creation,
    latitude,
    longitude,
    location_name
) VALUES (
    'Dépôt Test Centre',
    'Rue de Test, 123',
    'Tunis',
    3000,
    'Test Responsable',
    '+216 12 345 678',
    NOW(),
    36.8065,
    10.1815,
    'Centre-Ville Tunis'
);

-- ============================================
-- 3. INSERT NEW DEPOT WITHOUT LOCATION (uses default)
-- ============================================
INSERT INTO depot (
    nom_depot,
    adresse_depot,
    ville,
    capacite_depot,
    responsable_depot,
    responsable_telephone,
    date_creation,
    latitude,
    longitude,
    location_name
) VALUES (
    'Dépôt Test Nord',
    'Avenue Nord, 456',
    'Ariana',
    2500,
    'Nord Responsable',
    '071234567',
    NOW(),
    36.8667,
    10.1667,
    NULL  -- Will use ville (Ariana) as default
);

-- ============================================
-- 4. UPDATE EXISTING DEPOT WITH LOCATION
-- ============================================
UPDATE depot
SET location_name = 'Zone Industrielle'
WHERE nom_depot = 'Dépôt Test Centre';

-- ============================================
-- 5. QUERY DEPOTS WITH LOCATION INFORMATION
-- ============================================
SELECT 
    id_depot,
    nom_depot,
    ville,
    location_name,
    latitude,
    longitude,
    responsable_depot
FROM depot
WHERE location_name IS NOT NULL
ORDER BY nom_depot;

-- ============================================
-- 6. SEARCH DEPOTS BY LOCATION NAME
-- ============================================
SELECT 
    nom_depot,
    ville,
    location_name,
    adresse_depot
FROM depot
WHERE location_name LIKE '%Centre%'
   OR location_name LIKE '%Zone%';

-- ============================================
-- 7. EXAMPLE LOCATION NAME VALUES
-- ============================================
-- You can use various location identifiers:
-- 
-- Neighborhood names:
--   'Centre-Ville Tunis'
--   'Lac de Tunis'
--   'La Marsa'
--   'Carthage'
--
-- District/Zone names:
--   'Zone Industrielle'
--   'Zone Commerciale'
--   'Parc d'Activités'
--
-- Landmark references:
--   'Près Hôpital Charles Nicolle'
--   'Zone Touristique'
--   'Port de Commerce'
--
-- Or any descriptive location identifier up to 500 characters

-- ============================================
-- 8. VERIFY ALL TEST DATA
-- ============================================
SELECT 
    id_depot AS 'ID',
    nom_depot AS 'Nom',
    ville AS 'Ville',
    COALESCE(location_name, '(non défini)') AS 'Localisation',
    capacite_depot AS 'Capacité',
    responsable_depot AS 'Responsable'
FROM depot
WHERE nom_depot LIKE 'Dépôt Test%'
ORDER BY id_depot;

-- ============================================
-- 9. CLEANUP TEST DATA (optional)
-- ============================================
-- DELETE FROM depot WHERE nom_depot LIKE 'Dépôt Test%';

-- ============================================
-- NOTES:
-- ============================================
-- - location_name is VARCHAR(500) and can be NULL
-- - If NULL, the system uses the ville name as default
-- - Maximum 500 characters for location name
-- - Can contain any descriptive location identifier
-- - Useful for mapping and depot identification
-- ============================================