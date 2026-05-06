-- ============================================
-- FIX DATABASE SCHEMA FOR DEPOT TABLE
-- ============================================
-- This script fixes the depot table schema to match
-- what the Java application expects
-- ============================================

-- 1. First, check current table structure
DESCRIBE depot;

-- 2. If latitude/longitude columns don't exist or have wrong type, fix them
-- The columns should be: decimal(10,7) to match the Java double type

-- Option A: If columns don't exist, add them
-- ALTER TABLE depot ADD COLUMN latitude decimal(10,7) DEFAULT NULL AFTER date_creation;
-- ALTER TABLE depot ADD COLUMN longitude decimal(10,7) DEFAULT NULL AFTER latitude;

-- Option B: If columns exist but have wrong type, modify them
-- ALTER TABLE depot MODIFY COLUMN latitude decimal(10,7) DEFAULT NULL;
-- ALTER TABLE depot MODIFY COLUMN longitude decimal(10,7) DEFAULT NULL;

-- 3. Add location_name column if it doesn't exist
-- ALTER TABLE depot ADD COLUMN location_name VARCHAR(500) DEFAULT NULL AFTER longitude;

-- 4. Verify the final structure
DESCRIBE depot;

-- 5. Test insert with sample data
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
    'Test Dépôt',
    'Test Address',
    'Tunis',
    3000,
    'Test User',
    '0123456789',
    NOW(),
    20.20,
    20.20,
    'Test Location'
);

SELECT * FROM depot WHERE nom_depot = 'Test Dépôt';

-- 6. Clean up test data (optional)
-- DELETE FROM depot WHERE nom_depot = 'Test Dépôt';

-- ============================================
-- NOTES:
-- ============================================
-- - decimal(10,7) means: 10 total digits, 7 after decimal point
-- - This gives us 3 digits before decimal: -999.9999999 to 999.9999999
-- - Valid for latitude (-90 to 90) and longitude (-180 to 180)
-- - If you get "Out of range" error, check the actual column type
-- - Run: DESCRIBE depot; to see current structure
-- ============================================