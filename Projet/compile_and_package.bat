@echo off
REM Script de compilation et execution simplifie - CuraVita

cd /d "%~dp0"

echo.
echo ========================================
echo COMPILATION ET EXECUTION - CURAVITA
echo ========================================
echo.

setlocal enabledelayedexpansion

set MAVEN_HOME=%~dp0apache-maven-3.9.7
set MAVEN_CMD=!MAVEN_HOME!\bin\mvn.cmd

echo Etape 1: Nettoyage des artifacts...
call !MAVEN_CMD! clean

echo.
echo Etape 2: Construction complete...
call !MAVEN_CMD! package -DskipTests

if !ERRORLEVEL! EQU 0 (
    echo.
    echo [SUCCES] Compilation et packaging termines!
    echo.
    echo JAR Executable: target\Projet_java-1.0-SNAPSHOT-executable.jar
    echo.
    echo Pour executer l'application:
    echo   java -jar target\Projet_java-1.0-SNAPSHOT-executable.jar
    echo.
) else (
    echo.
    echo [ERREUR] La compilation a echoue!
    echo Veuillez verifier le message d'erreur ci-dessus.
    echo.
)

pause

