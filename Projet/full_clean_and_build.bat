@echo off
REM Script de nettoyage complet du cache Maven

cd /d %~dp0

echo.
echo ========================================
echo NETTOYAGE COMPLET DU CACHE MAVEN
echo ========================================
echo.

REM Supprimer le dossier .m2 complet
echo Suppression du cache .m2...
if exist "%USERPROFILE%\.m2" (
    rmdir /s /q "%USERPROFILE%\.m2" 2>nul
    timeout /t 2 /nobreak
)

REM Supprimer le dossier target
echo Suppression du dossier target...
if exist "target" (
    rmdir /s /q "target" 2>nul
)

echo.
echo ========================================
echo RECOMPILATION AVEC CACHE VIERGE
echo ========================================
echo.

REM Recompiler
.\apache-maven-3.9.7\bin\mvn.cmd clean compile

pause

