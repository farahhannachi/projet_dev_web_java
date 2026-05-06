@echo off
REM Script d'execution de l'application CuraVita avec JavaFX

cd /d "%~dp0"

echo.
echo ========================================
echo EXECUTION DE CURAVITA
echo ========================================
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
for %%f in ("%JAVAFX_PATH%\javafx-web\21.0.2\javafx-web-21.0.2-win.jar") do set MODULE_PATH=!MODULE_PATH!%%f;

REM Enlever le dernier point-virgule
set MODULE_PATH=%MODULE_PATH:~0,-1%

echo Module path: %MODULE_PATH%
echo.

REM Executer l'application
java -Dprism.order=sw -Dprism.d3d=false --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp target/Projet_java-1.0-SNAPSHOT-executable.jar org.example.CuraVitaApp

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] L'execution a echoue!
    echo.
    pause
    exit /b 1
)

echo.
echo [SUCCES] Application terminee!
echo.
pause
