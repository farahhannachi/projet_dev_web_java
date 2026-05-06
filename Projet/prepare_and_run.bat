@echo off
REM Script de préparation finale avant le lancement

cd /d "%~dp0"

echo.
echo ========================================
echo PREPARATION FINALE - CURAVITA
echo ========================================
echo.

echo [1/3] Nettoyage et recompilation...
call .\apache-maven-3.9.7\bin\mvn.cmd clean package -q -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Compilation échouée!
    pause
    exit /b 1
)
echo ✓ Compilation réussie!

echo.
echo [2/3] Vérification des ressources...
if not exist "target\Projet_java-1.0-SNAPSHOT-executable.jar" (
    echo [ERREUR] JAR executable non trouvé!
    pause
    exit /b 1
)
echo ✓ JAR executable trouvé!

echo.
echo [3/3] Vérification des fichiers FXML...
if exist "target\classes\fxml\ReservationForm.fxml" (
    echo ✓ ReservationForm.fxml trouvé
) else (
    echo [AVERTISSEMENT] ReservationForm.fxml non trouvé
)

echo.
echo ========================================
echo PREPARATION TERMINEE AVEC SUCCES!
echo ========================================
echo.
echo Avant de lancer l'application:
echo 1. Assurez-vous que MySQL est en cours d'exécution
echo 2. Assurez-vous que la base 'pharmacie' existe
echo.
echo Lancement de l'application...
echo.

call run_app.bat

