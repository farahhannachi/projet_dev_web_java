@echo off
REM Simple batch script to test compilation
cd /d "C:\Users\ihebj\OneDrive\Bureau\Projet_java"

echo Compiling Java files...
javac -d target/classes ^
  -cp "src/main/java" ^
  --module-path "C:\Users\ihebj\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1-win.jar" ^
  -encoding UTF-8 ^
  src/main/java/org/example/*.java ^
  src/main/java/org/example/model/*.java ^
  src/main/java/org/example/service/*.java ^
  src/main/java/org/example/controller/*.java

if %ERRORLEVEL% EQU 0 (
  echo Compilation successful!
) else (
  echo Compilation failed!
)

pause

