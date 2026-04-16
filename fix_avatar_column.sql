-- Add avatar_config column to utilisateur table if it doesn't exist
ALTER TABLE utilisateur ADD COLUMN avatar_config LONGTEXT DEFAULT NULL;
