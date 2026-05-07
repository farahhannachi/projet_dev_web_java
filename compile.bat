@echo off
cd /d "C:\Users\ihebj\OneDrive\Bureau\Projet_java"

javac -d target/classes ^
  -encoding UTF-8 ^
  src/main/java/org/example/*.java ^
  src/main/java/org/example/model/*.java ^
  src/main/java/org/example/service/*.java ^

if %ERRORLEVEL% EQU 0 (
  echo Compilation successful!
) else (
  echo Compilation failed!
)

pause
