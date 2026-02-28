-- MySQL InnoDB Buffer Pool Size Fix
-- ===================================

-- Check current buffer pool size
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- For immediate effect (temporary, will reset after MySQL restart):
-- SET GLOBAL innodb_buffer_pool_size = 536870912;  -- 512MB

-- Note: To make the change permanent, you must edit your MySQL configuration file.
-- 
-- On Windows, the configuration file is typically located at:
--   C:\ProgramData\MySQL\MySQL Server 8.0\my.ini
--   or
--   C:\Program Files\MySQL\MySQL Server 8.0\my.ini
--
-- Add or modify the following in the [mysqld] section:
--
-- [mysqld]
-- innodb_buffer_pool_size = 536870912  ; 512MB
--
-- Then restart MySQL service.
