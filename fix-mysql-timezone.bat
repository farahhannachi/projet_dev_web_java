@echo off
REM MySQL Timezone Tables Setup Script for Windows
REM This script helps load timezone tables into MySQL

echo ==========================================================
echo MySQL Timezone Tables Setup
echo ==========================================================
echo.
echo This script will help you load timezone tables into MySQL.
echo.
echo STEP 1: Download timezone tables from:
echo https://dev.mysql.com/downloads/timezones.html
echo.
echo STEP 2: Extract the downloaded SQL file to this directory.
echo.
echo STEP 3: Run this script as Administrator.
echo.
echo ==========================================================
echo.

REM Check if MySQL is in PATH
where mysql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: MySQL is not in your PATH.
    echo Please add MySQL bin directory to your PATH or run this script from MySQL bin folder.
    echo Example: C:\Program Files\MySQL\MySQL Server 8.0\bin
    pause
    exit /b 1
)

echo MySQL found in PATH.
echo.

REM Check for timezone SQL files in current directory
echo Looking for timezone SQL files in current directory...
if exist "timezone*.sql" (
    echo Found timezone SQL file(s):
    dir /b timezone*.sql
    echo.
    goto :found_file
)

REM Check for common filename patterns
if exist "time_zone*.sql" (
    echo Found timezone SQL file(s):
    dir /b time_zone*.sql
    echo.
    goto :found_file
)

REM Not found
echo ERROR: No timezone SQL file found in current directory.
echo.
echo Please download the timezone tables from:
echo https://dev.mysql.com/downloads/timezones.html
echo.
echo Then extract the SQL file to this directory and run this script again.
echo.
pause
exit /b 1

:found_file
echo Ready to import timezone tables into MySQL.
echo.
echo You will be prompted for MySQL root password.
echo.

REM Import timezone tables
mysql -u root -p mysql < timezone*.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to import timezone tables.
    echo Please check:
    echo 1. MySQL server is running
    echo 2. You entered the correct root password
    echo 3. You have sufficient privileges
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================================
echo SUCCESS! Timezone tables imported successfully.
echo ==========================================================
echo.
echo Now you can set the timezone to Europe/Berlin by running:
echo.
echo   SET GLOBAL time_zone = 'Europe/Berlin';
echo.
echo Or restart MySQL after updating my.ini with:
echo.
echo   default-time-zone = 'Europe/Berlin'
echo.
echo ==========================================================
echo.
pause
