@echo off
REM Lancement facile de CuraVita avec les dépôts fonctionnels

cd /d "%~dp0"

echo.
echo ========================================
echo LANCEMENT CURAVITA
echo Gestion des Dépôts - FONCTIONNELLE
echo ========================================
echo.

echo Compilation et lancement en cours...
echo.

REM Build and run with Maven
.\apache-maven-3.9.7\bin\mvn.cmd javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] L'application n'a pas pu démarrer
    echo.
    pause
)

