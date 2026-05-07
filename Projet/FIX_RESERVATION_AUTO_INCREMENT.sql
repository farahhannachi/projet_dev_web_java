-- Fix for reservation table AUTO_INCREMENT issue
-- Execute this script in MySQL to fix the "Field 'id' doesn't have a default value" error

-- First, check current table structure
DESCRIBE reservation;

-- Fix the id column to be AUTO_INCREMENT
ALTER TABLE reservation
MODIFY id INT NOT NULL AUTO_INCREMENT;

-- Ensure id is the primary key (if not already)
-- This will fail if primary key already exists, which is fine
ALTER TABLE reservation
ADD PRIMARY KEY (id);

-- Verify the fix
DESCRIBE reservation;

-- Test that AUTO_INCREMENT works
INSERT INTO reservation (service_id, nom_client, email_client, telephone_client, date_reservation, date_rendez_vous, motif)
VALUES (1, 'Test Client', 'test@example.com', '123456789', NOW(), NOW(), 'Test reservation');

-- Check that id was auto-generated
SELECT * FROM reservation WHERE nom_client = 'Test Client';

-- Clean up test data
DELETE FROM reservation WHERE nom_client = 'Test Client';
