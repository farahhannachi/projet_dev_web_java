@echo off
REM ========================================
REM LANCEMENT DU SYSTEME DE RESERVATION
REM CURAVITA v1.0
REM ========================================

cd /d "%~dp0"

echo.
echo ========================================
echo   SYSTEME DE RESERVATION CURAVITA
echo          Version 1.0 Complet
echo ========================================
echo.

REM Verifier que MySQL est en cours d'execution
echo [VERIFICATION] Vérification de MySQL...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo ✓ MySQL Server est en cours d'exécution
) else (
    echo ⚠ ATTENTION: MySQL Server ne semble pas en cours d'exécution
    echo Continuez? (O/N)
    set /p response=
    if /I not "%response%"=="O" exit /b 1
)

echo.
echo [COMPILATION] Compilation du projet...
call .\apache-maven-3.9.7\bin\mvn.cmd clean package -q -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] La compilation a échoué!
    echo Appuyez sur une touche pour continuer...
    pause
    exit /b 1
)
echo ✓ Compilation réussie!

echo.
echo [JAR] Vérification du JAR exécutable...
if not exist "target\Projet_java-1.0-SNAPSHOT-executable.jar" (
    echo [ERREUR] JAR exécutable non trouvé!
    pause
    exit /b 1
)
echo ✓ JAR trouvé (13.8 MB)

echo.
echo ========================================
echo   LANCEMENT DE L'APPLICATION
echo ========================================
echo.
echo Fonctionnalités:
echo  ✓ Système de réservation en ligne
echo  ✓ Services (Médecins/Infirmiers)
echo  ✓ Formulaire de validation
echo  ✓ Enregistrement en base de données
echo  ✓ Gestion des erreurs
echo.

setlocal enabledelayedexpansion

REM Chemin vers les JARs JavaFX dans le cache Maven
set JAVAFX_PATH=%USERPROFILE%\.m2\repository\org\openjfx

REM Construire le module path avec tous les JARs JavaFX
set MODULE_PATH=
for %%f in ("%JAVAFX_PATH%\javafx-base\21.0.2\javafx-base-21.0.2-win.jar") do set MODULE_PATH=!MODULE_PATH!%%f;
for %%f in ("%JAVAFX_PATH%\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar") do set MODULE_PATH=!MODULE_PATH!%%f;
for %%f in ("%JAVAFX_PATH%\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar") do set MODULE_PATH=!MODULE_PATH!%%f;
for %%f in ("%JAVAFX_PATH%\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar") do set MODULE_PATH=!MODULE_PATH!%%f;

REM Enlever le dernier point-virgule
set MODULE_PATH=%MODULE_PATH:~0,-1%

REM Lancer l'application avec JVM args
echo Démarrage de l'application...
echo.

java -Dprism.order=sw -Dprism.d3d=false --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -cp target/Projet_java-1.0-SNAPSHOT-executable.jar org.example.CuraVitaApp

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] L'exécution a échoué avec le code %ERRORLEVEL%
    echo Vérifiez:
    echo  1. MySQL Server est en cours d'exécution
    echo  2. La base de données 'pharmacie' existe
    echo  3. Les paramètres de connexion dans DatabaseUtil.java
    echo.
    pause
    exit /b 1
)

echo.
echo [SUCCES] Application terminée normalement!
echo.
pause

