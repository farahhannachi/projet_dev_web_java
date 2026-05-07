@echo off
REM Script simple pour compiler le projet Maven

cd /d "C:\Users\fahan\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"

echo.
echo ========================================
echo COMPILATION DU PROJET MAVEN
echo ========================================
echo.

REM Compiler et packager le projet
call mvn clean package

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCES] Compilation et packaging termines avec succes!
    echo JAR genere: target\Projet_java-1.0-SNAPSHOT.jar
    echo.
) else (
    echo.
    echo [ERREUR] La compilation a echoue!
    echo.
)

pause

