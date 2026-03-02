-- =============================================================================
-- MySQL Server Configuration Fixes
-- Run this script as a user with SUPER privileges (typically root)
-- =============================================================================

-- Fix 1: Set Global Timezone to match PHP (Europe/Berlin)
-- This persists until MySQL restart (use my.cnf for permanent fix)
SET GLOBAL time_zone = 'Europe/Berlin';

-- Fix 2: Enable SQL Strict Mode
SET GLOBAL sql_mode = 'NO_ZERO_IN_DATE,NO_ZERO_DATE,NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO';

-- Fix 3: Increase InnoDB Buffer Pool Size (requires restart to take effect)
-- Note: This sets the variable but requires MySQL restart
SET GLOBAL innodb_buffer_pool_size = 536870912; -- 512MB

-- Fix 4: Development Only - Faster InnoDB flush (use 1 in production!)
SET GLOBAL innodb_flush_log_at_trx_commit = 2;

-- =============================================================================
-- Verify the changes
-- =============================================================================
SELECT '=== Timezone Configuration ===' AS 'Check';
SELECT @@global.time_zone AS 'Global Timezone', @@session.time_zone AS 'Session Timezone';

SELECT '=== SQL Mode ===' AS 'Check';
SELECT @@sql_mode AS 'SQL Mode';

SELECT '=== InnoDB Buffer Pool ===' AS 'Check';
SELECT @@innodb_buffer_pool_size AS 'Buffer Pool Size (bytes)', 
       @@innodb_buffer_pool_size / 1024 / 1024 AS 'Buffer Pool Size (MB)';

SELECT '=== InnoDB Flush Log ===' AS 'Check';
SELECT @@innodb_flush_log_at_trx_commit AS 'Flush Log Setting (2=dev, 1=prod)';
