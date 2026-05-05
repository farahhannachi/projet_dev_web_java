@echo off
REM Compile with JavaFX 21 including javafx-media
cd /d "C:\Users\ihebj\OneDrive\Bureau\Projet_java"

echo Compiling Java files with JavaFX 21...
javac -d target/classes ^
  --module-path "C:\Users\ihebj\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-media\21\javafx-media-21-win.jar" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media ^
  -cp "C:\Users\ihebj\.m2\repository\com\mysql\mysql-connector-j\8.2.0\mysql-connector-j-8.2.0.jar;C:\Users\ihebj\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar" ^
  -encoding UTF-8 ^
  src/main/java/org/example/*.java ^
  src/main/java/org/example/model/*.java ^
  src/main/java/org/example/service/*.java ^
  src/main/java/org/example/controller/*.java ^
  src/main/java/org/example/util/*.java

if %ERRORLEVEL% EQU 0 (
  echo Compilation successful!
) else (
  echo Compilation failed!
)

pause
