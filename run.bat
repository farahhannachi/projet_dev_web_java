@echo off
REM Run CuraVita JavaFX Application
cd /d "C:\Users\ihebj\OneDrive\Bureau\Projet_java"

echo Running CuraVita...
REM Updated for JavaFX 21 with javafx-media
java --module-path "C:\Users\ihebj\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar;C:\Users\ihebj\.m2\repository\org\openjfx\javafx-media\21\javafx-media-21-win.jar" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target/classes;C:\Users\ihebj\.m2\repository\com\mysql\mysql-connector-j\8.2.0\mysql-connector-j-8.2.0.jar;C:\Users\ihebj\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar" org.example.CuraVitaApp

pause
