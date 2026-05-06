@echo off
REM Vérification et lancement rapide de CuraVita avec Dépôts

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo.
echo ========================================
echo VERIFICATION AVANT LANCEMENT
echo ========================================
echo.

echo Vérification des fichiers critiques...

REM Check Depots.fxml
if exist "src\main\resources\fxml\Depots.fxml" (
    echo ✓ Depots.fxml existe
) else (
    echo X Depots.fxml MANQUANT!
    pause
    exit /b 1
)

REM Check DepotController
if exist "src\main\java\org\example\controller\DepotController.java" (
    echo ✓ DepotController.java existe
) else (
    echo X DepotController.java MANQUANT!
    pause
    exit /b 1
)

REM Check DepotService
if exist "src\main\java\org\example\service\DepotService.java" (
    echo ✓ DepotService.java existe
) else (
    echo X DepotService.java MANQUANT!
    pause
    exit /b 1
)

echo.
echo ========================================
echo COMPILATION ET LANCEMENT
echo ========================================
echo.

echo Compilation du projet...
call .\apache-maven-3.9.7\bin\mvn.cmd clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR DE COMPILATION!
    pause
    exit /b 1
)

echo ✓ Compilation réussie
echo.
echo Lancement de l'application...
echo.
echo Instructions:
echo 1. Se connecter au Dashboard
echo 2. Cliquer sur "Dépôts" dans la sidebar
echo 3. Voir le tableau CRUD avec les dépôts
echo.

call .\apache-maven-3.9.7\bin\mvn.cmd javafx:run

pause

