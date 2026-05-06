@echo off
REM Script pour lancer l'application JavaFX avec les modules corrects

setlocal enabledelayedexpansion

REM Définir le chemin Java
set JAVA_HOME=C:\Users\fahan\.jdks\ms-21.0.10
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

REM Définir les chemins des modules JavaFX
set MODULE_PATH=C:\Users\fahan\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar

REM Définir le classpath
set CLASSPATH=target\classes;C:\Users\fahan\.m2\repository\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;C:\Users\fahan\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar;C:\Users\fahan\.m2\repository\com\mysql\mysql-connector-j\8.2.0\mysql-connector-j-8.2.0.jar;C:\Users\fahan\.m2\repository\com\google\protobuf\protobuf-java\3.21.9\protobuf-java-3.21.9.jar

REM Lancer l'application avec les modules JavaFX activés
"%JAVA_EXE%" ^
  --module-path "%MODULE_PATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
  -Dprism.order=sw ^
  -Dprism.d3d=false ^
  -classpath "%CLASSPATH%" ^
  org.example.CuraVitaApp

pause

