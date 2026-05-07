@echo off
REM Script de compilation et exécution du projet Maven
REM Assurez-vous que Maven est dans le PATH ou utilisez le chemin absolu

cd /d "C:\Users\fahan\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"

echo.
echo ========================================
echo COMPILATION DU PROJET MAVEN
echo ========================================
echo.

REM Clear Maven cache if needed (optional)
REM rmdir /s /q %USERPROFILE%\.m2\repository\org\openjfx

REM Compiler le projet avec Maven
echo Compilation en cours...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue!
    echo.
    pause
    exit /b 1
)

echo.
echo Compilation reussie!
echo.
echo ========================================
echo PACKAGE DU PROJET
echo ========================================
echo.

REM Créer le JAR
call mvn package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Le packaging a echoue!
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo EXECUTION DE L'APPLICATION
echo ========================================
echo.

REM Exécuter l'application
call mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] L'execution a echoue!
    echo.
    pause
    exit /b 1
)

echo.
echo [SUCCES] Execution terminee!
echo.
pause

