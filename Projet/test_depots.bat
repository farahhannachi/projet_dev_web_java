@echo off
REM Test script to run CuraVita with proper testing

cd /d "%~dp0"

echo.
echo ========================================
echo LANCEMENT DE CURAVITA AVEC TEST
echo ========================================
echo.

REM Run with Maven
echo Compilation et lancement de l'application...
echo.

.\apache-maven-3.9.7\bin\mvn.cmd javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] L'execution a echoue!
    echo.
    pause
)

