@echo off
REM Script de diagnostic de la base de données

cd /d "%~dp0"

echo.
echo ========================================
echo DIAGNOSTIC DE LA BASE DE DONNEES
echo ========================================
echo.

REM Vérifier que mysql-connector est disponible
echo [1/4] Verification des drivers JDBC...
dir /s "%USERPROFILE%\.m2\repository\mysql\mysql-connector-java" 2>nul >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ MySQL JDBC Driver trouvé
) else (
    echo ✗ MySQL JDBC Driver NON trouvé
)

echo.
echo [2/4] Verification du schema pharmacie.sql...
if exist "pharmacie.sql" (
    echo ✓ pharmacie.sql trouvé
    findstr /N "CREATE TABLE.*reservation" pharmacie.sql >nul
    if %ERRORLEVEL% EQU 0 (
        echo ✓ Table 'reservation' est définie dans pharmacie.sql
    ) else (
        echo ✓ Table 'reservation' sera créée automatiquement par DatabaseInitializer
    )
) else (
    echo ✗ pharmacie.sql NON trouvé
)

echo.
echo [3/4] Verification du script CREATE_RESERVATION_TABLE.sql...
if exist "CREATE_RESERVATION_TABLE.sql" (
    echo ✓ CREATE_RESERVATION_TABLE.sql trouvé
) else (
    echo ✗ CREATE_RESERVATION_TABLE.sql NON trouvé
)

echo.
echo [4/4] Verification de DatabaseInitializer...
if exist "src\main\java\org\example\util\DatabaseInitializer.java" (
    echo ✓ DatabaseInitializer.java trouvé
    findstr /N "CREATE TABLE IF NOT EXISTS reservation" src\main\java\org\example\util\DatabaseInitializer.java >nul
    if %ERRORLEVEL% EQU 0 (
        echo ✓ Table 'reservation' sera créée au démarrage
    )
) else (
    echo ✗ DatabaseInitializer.java NON trouvé
)

echo.
echo ========================================
echo RESULTAT: La base de données sera
echo initialisée automatiquement au démarrage
echo de l'application
echo ========================================
echo.
pause

