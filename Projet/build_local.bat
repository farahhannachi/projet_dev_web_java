@echo off
REM Script de compilation simple et rapide
REM Utilise Maven local pour compiler le projet

cd /d "%~dp0"

echo.
echo ========================================
echo COMPILATION DU PROJET MAVEN
echo ========================================
echo.

setlocal enabledelayedexpansion

REM Utiliser Maven local
set MAVEN_HOME=%~dp0apache-maven-3.9.7
set MAVEN_CMD=!MAVEN_HOME!\bin\mvn.cmd

echo Etape 1: Nettoyage des artifacts...
call !MAVEN_CMD! clean

echo.
echo Etape 2: Compilation...
call !MAVEN_CMD! compile

if !ERRORLEVEL! NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue!
    echo.
    pause
    exit /b 1
)

echo.
echo Etape 3: Packaging...
call !MAVEN_CMD! package -DskipTests

if !ERRORLEVEL! EQU 0 (
    echo.
    echo [SUCCES] Compilation et packaging termines avec succes!
    echo JAR genere: target\Projet_java-1.0-SNAPSHOT.jar
    echo.
) else (
    echo.
    echo [ERREUR] Le packaging a echoue!
    echo.
)

pause

