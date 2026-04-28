@echo off
REM Script pour compiler et exécuter CuraVita
cd /d "C:\Users\HP\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"

REM Créer les dossiers nécessaires
if not exist target\classes mkdir target\classes
if not exist target\resources mkdir target\resources

REM Copier les ressources
xcopy /E /Y "src\main\resources\*" "target\resources\" >nul 2>&1
xcopy /E /Y "src\main\resources\*" "target\classes\" >nul 2>&1

REM Créer un fichier classpath temporaire
setlocal enabledelayedexpansion
set m2=C:\Users\HP\.m2\repository
set classpath=src\main\java
for /r "%m2%" %%F in (*.jar) do set classpath=!classpath!;%%F

REM Compiler les fichiers Java
echo Compilation en cours...
javac -d target\classes -cp "%classpath%" -encoding UTF-8 ^
  src\main\java\org\example\CuraVitaApp.java ^
  src\main\java\org\example\model\*.java ^
  src\main\java\org\example\service\*.java ^
  src\main\java\org\example\controller\*.java ^
  src\main\java\org\example\util\*.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation reussie!
    echo Lancement de l'application...

    REM Lancer l'application
    java -cp "%classpath%;target\classes" org.example.CuraVitaApp
) else (
    echo Erreur de compilation!
    pause
)

endlocal

