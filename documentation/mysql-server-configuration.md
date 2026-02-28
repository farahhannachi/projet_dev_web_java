# MySQL Server Configuration Fixes

This document outlines the MySQL server-level configuration changes required to fix the remaining database issues detected by Doctrine Doctor.

## Issues Requiring MySQL Server Configuration

### 1. Timezone Configuration (CRITICAL)

**Issue**: MySQL timezone is "SYSTEM" (resolves to Africa/Lagos) but PHP uses "Europe/Berlin"

**Fix Options:**

#### Option A: Edit MySQL Configuration File (Recommended)

Locate your MySQL configuration file:
- **Windows**: `C:\ProgramData\MySQL\MySQL Server X.X\my.ini`
- **Linux**: `/etc/mysql/my.cnf` or `/etc/my.cnf`
- **macOS**: `/usr/local/etc/my.cnf` or `/etc/my.cnf`

Add or modify under `[mysqld]` section:
```ini
[mysqld]
default-time-zone = 'Europe/Berlin'
```

Restart MySQL service after changes.

#### Option B: Set Dynamically (Temporary - Lost on Restart)

Connect to MySQL as root and run:
```sql
SET GLOBAL time_zone = 'Europe/Berlin';
```

---

### 2. Load MySQL Timezone Tables (CRITICAL)

**Issue**: MySQL timezone tables are empty, preventing named timezone usage

**Fix:**

#### Linux/macOS:
```bash
# Run on host machine
mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root -p mysql
```

#### Windows:
1. Download timezone tables from: https://dev.mysql.com/downloads/timezones.html
2. Extract the SQL file
3. Import using:
```cmd
mysql -u root -p mysql < timezone_2024a_posix_sql.sql
```

#### Verify:
```sql
SELECT COUNT(*) FROM mysql.time_zone_name;
-- Should return > 0 (typically 500+)
```

---

### 3. InnoDB Buffer Pool Size (WARNING)

**Issue**: Current 16MB is too small. Recommended: 512MB+ for development

**Fix:**

Edit MySQL configuration file and add under `[mysqld]`:
```ini
[mysqld]
innodb_buffer_pool_size = 536870912  # 512MB for development
```

**Sizing Guidelines:**
- Development: 256MB - 512MB minimum
- Production: 50-70% of available RAM
- Example 8GB RAM server: 4-5GB (4294967296 - 5368709120 bytes)

Restart MySQL after changes.

---

### 4. SQL Strict Mode (WARNING)

**Note**: This is now configured at the application level in Doctrine, but for server-wide enforcement:

Edit MySQL configuration file:
```ini
[mysqld]
sql_mode = 'NO_ZERO_IN_DATE,NO_ZERO_DATE,NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO'
```

Restart MySQL after changes.

---

### 5. InnoDB Flush Log at Transaction Commit (INFO)

**Issue**: Currently set to 1 (full ACID durability) - slow in development

**Development Only Fix:**

Edit MySQL configuration file:
```ini
[mysqld]
innodb_flush_log_at_trx_commit = 2  # Development only - NOT for production!
```

**Values:**
- `0` = Flush every second (fastest, data loss on crash)
- `1` = Flush on every commit (slowest, full ACID) - **USE IN PRODUCTION**
- `2` = Flush to OS cache every commit, disk every second (balanced for dev)

**⚠️ IMPORTANT**: Keep value 1 in production for data safety!

---

## Quick Fix Summary

Create or edit your MySQL configuration file with all fixes:

```ini
[mysqld]
# Timezone configuration
default-time-zone = 'Europe/Berlin'

# Buffer pool size (512MB for dev)
innodb_buffer_pool_size = 536870912

# SQL Strict Mode
sql_mode = 'NO_ZERO_IN_DATE,NO_ZERO_DATE,NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO'

# Development only: Faster writes (set to 1 in production!)
innodb_flush_log_at_trx_commit = 2
```

## Verification Commands

After making changes and restarting MySQL:

```sql
-- Check timezone
SELECT @@time_zone, @@system_time_zone;

-- Check buffer pool size
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- Check SQL mode
SELECT @@sql_mode;

-- Check flush log setting
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';

-- Check timezone tables loaded
SELECT COUNT(*) FROM mysql.time_zone_name;
```

## Application-Level Fixes Already Applied

The following fixes have been applied in the Symfony application:

1. ✅ **Doctrine DBAL Connection Options**: Timezone and SQL mode are now set on each connection
   - File: `config/packages/doctrine.yaml`

2. ✅ **Database Collation Migration**: Database default collation will be changed to match tables
   - Migration: `migrations/Version20260228040000.php`
   - Run: `php bin/console doctrine:migrations:migrate`

## Restart MySQL Service

### Windows:
```cmd
# Via Services panel or Command Prompt as Administrator
net stop MySQL80
net start MySQL80
```

### Linux:
```bash
sudo systemctl restart mysql
# or
sudo service mysql restart
```

### macOS:
```bash
brew services restart mysql
```
