-- ============================================
-- FINAL DATABASE FIX FOR DEPOT TABLE
-- ============================================
-- This script ensures the depot table has the correct
-- column types to match the Java application
-- ============================================

-- Step 1: Check current structure
SELECT '=== Current depot table structure ===' AS '';
DESCRIBE depot;

-- Step 2: Drop and recreate latitude/longitude columns with correct type
-- First, check if columns exist
SET @lat_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'depot' 
                   AND COLUMN_NAME = 'latitude');

SET @lng_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'depot' 
                   AND COLUMN_NAME = 'longitude');

-- If latitude exists, drop and recreate it
SET @sql1 = IF(@lat_exists > 0,
               'ALTER TABLE depot DROP COLUMN latitude',
               'SELECT "latitude column does not exist" AS status');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- If longitude exists, drop and recreate it  
SET @sql2 = IF(@lng_exists > 0,
               'ALTER TABLE depot DROP COLUMN longitude',
               'SELECT "longitude column does not exist" AS status');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Add latitude with correct type
ALTER TABLE depot ADD COLUMN latitude decimal(10,7) DEFAULT NULL AFTER date_creation;

-- Add longitude with correct type
ALTER TABLE depot ADD COLUMN longitude decimal(10,7) DEFAULT NULL AFTER latitude;

-- Step 3: Add location_name column if it doesn't exist
SET @loc_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'depot' 
                   AND COLUMN_NAME = 'location_name');

SET @sql3 = IF(@loc_exists = 0,
               'ALTER TABLE depot ADD COLUMN location_name VARCHAR(500) DEFAULT NULL AFTER longitude',
               'SELECT "location_name column already exists" AS status');

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Step 4: Verify the final structure
SELECT '=== Final depot table structure ===' AS '';
DESCRIBE depot;

-- Step 5: Test insert
SELECT '=== Testing insert ===' AS '';

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
    'Dépôt Test Final',
    'Test Address',
    'Tunis',
    3000,
    'Test User',
    '0123456789',
    NOW(),
    48.8566,
    2.3522,
    'Test Location'
);

SELECT '=== Test data ===' AS '';
SELECT * FROM depot WHERE nom_depot = 'Dépôt Test Final';

-- Clean up (optional)
-- DELETE FROM depot WHERE nom_depot = 'Dépôt Test Final';

SELECT '=== Database fix complete! ===' AS '';
-- ============================================