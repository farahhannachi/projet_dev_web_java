-- ============================================
-- UPDATE DATABASE SCHEMA FOR DEPOT TABLE
-- ============================================
-- Run this script in phpMyAdmin or MySQL client
-- to fix the depot table schema
-- ============================================

-- Step 1: Check current structure
SELECT 'Current depot table structure:' AS '';
DESCRIBE depot;

-- Step 2: Fix latitude column (should be decimal(10,7))
ALTER TABLE depot MODIFY COLUMN latitude decimal(10,7) DEFAULT NULL;

-- Step 3: Fix longitude column (should be decimal(10,7))
ALTER TABLE depot MODIFY COLUMN longitude decimal(10,7) DEFAULT NULL;

-- Step 4: Add location_name column if it doesn't exist
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'depot' 
                      AND COLUMN_NAME = 'location_name');

SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE depot ADD COLUMN location_name VARCHAR(500) DEFAULT NULL AFTER longitude',
              'SELECT "location_name column already exists" AS status');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 5: Verify the final structure
SELECT 'Updated depot table structure:' AS '';
DESCRIBE depot;

-- Step 6: Test with sample data
SELECT 'Testing with sample data...' AS '';

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
    'Dépôt Test Paris',
    '10 Rue du Stock',
    'Paris',
    1000,
    'Jean Dupont',
    '0145678901',
    NOW(),
    48.8566,
    2.3522,
    'Centre-Ville Paris'
);

SELECT 'Test data inserted successfully!' AS '';
SELECT * FROM depot WHERE nom_depot = 'Dépôt Test Paris';

-- Clean up test data (optional)
-- DELETE FROM depot WHERE nom_depot = 'Dépôt Test Paris';

SELECT 'Database schema update complete!' AS '';
-- ============================================