@echo off
REM ============================================================
REM  Script de compilation — machine Hazem
REM  JavaFX 21.0.2 + toutes les dependances du projet
REM ============================================================
cd /d "%~dp0"

set M2=%USERPROFILE%\.m2\repository
set JFXVER=21.0.2

set JFXPATH=%M2%\org\openjfx\javafx-controls\%JFXVER%\javafx-controls-%JFXVER%-win.jar
set JFXPATH=%JFXPATH%;%M2%\org\openjfx\javafx-fxml\%JFXVER%\javafx-fxml-%JFXVER%-win.jar
set JFXPATH=%JFXPATH%;%M2%\org\openjfx\javafx-graphics\%JFXVER%\javafx-graphics-%JFXVER%-win.jar
set JFXPATH=%JFXPATH%;%M2%\org\openjfx\javafx-base\%JFXVER%\javafx-base-%JFXVER%-win.jar
set JFXPATH=%JFXPATH%;%M2%\org\openjfx\javafx-media\%JFXVER%\javafx-media-%JFXVER%-win.jar
set JFXPATH=%JFXPATH%;%M2%\org\openjfx\javafx-web\%JFXVER%\javafx-web-%JFXVER%-win.jar

set CPPATH=%M2%\com\mysql\mysql-connector-j\8.2.0\mysql-connector-j-8.2.0.jar
set CPPATH=%CPPATH%;%M2%\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set CPPATH=%CPPATH%;%M2%\org\eclipse\angus\angus-mail\2.0.3\angus-mail-2.0.3.jar
set CPPATH=%CPPATH%;%M2%\org\eclipse\angus\jakarta.mail\2.0.3\jakarta.mail-2.0.3.jar
set CPPATH=%CPPATH%;%M2%\jakarta\mail\jakarta.mail-api\2.1.3\jakarta.mail-api-2.1.3.jar
set CPPATH=%CPPATH%;%M2%\com\google\code\gson\gson\2.11.0\gson-2.11.0.jar
set CPPATH=%CPPATH%;%M2%\com\google\zxing\core\3.5.2\core-3.5.2.jar
set CPPATH=%CPPATH%;%M2%\com\google\zxing\javase\3.5.2\javase-3.5.2.jar
set CPPATH=%CPPATH%;%M2%\org\apache\pdfbox\pdfbox\2.0.32\pdfbox-2.0.32.jar
set CPPATH=%CPPATH%;%M2%\org\apache\pdfbox\fontbox\2.0.32\fontbox-2.0.32.jar
set CPPATH=%CPPATH%;%M2%\com\itextpdf\kernel\7.2.5\kernel-7.2.5.jar
set CPPATH=%CPPATH%;%M2%\com\itextpdf\layout\7.2.5\layout-7.2.5.jar
set CPPATH=%CPPATH%;%M2%\com\itextpdf\io\7.2.5\io-7.2.5.jar
set CPPATH=%CPPATH%;%M2%\com\itextpdf\commons\7.2.5\commons-7.2.5.jar
set CPPATH=%CPPATH%;%M2%\com\alphacephei\vosk\0.3.45\vosk-0.3.45.jar
set CPPATH=%CPPATH%;%M2%\net\java\dev\jna\jna\5.13.0\jna-5.13.0.jar
set CPPATH=%CPPATH%;%M2%\net\java\dev\jna\jna-platform\5.13.0\jna-platform-5.13.0.jar
set CPPATH=%CPPATH%;%M2%\jakarta\activation\jakarta.activation-api\2.1.3\jakarta.activation-api-2.1.3.jar
set CPPATH=%CPPATH%;%M2%\org\eclipse\angus\angus-activation\2.0.2\angus-activation-2.0.2.jar
set CPPATH=%CPPATH%;%M2%\commons-logging\commons-logging\1.2\commons-logging-1.2.jar
set CPPATH=%CPPATH%;%M2%\com\github\librepdf\openpdf\1.3.39\openpdf-1.3.39.jar
set CPPATH=%CPPATH%;%M2%\com\github\jai-imageio\jai-imageio-core\1.4.0\jai-imageio-core-1.4.0.jar
set CPPATH=%CPPATH%;%M2%\org\bouncycastle\bcprov-jdk15on\1.70\bcprov-jdk15on-1.70.jar

echo Compiling Java files with JavaFX %JFXVER% (including javafx.web)...

REM Generer la liste des sources dans un fichier temporaire
dir /s /b src\main\java\org\example\*.java > sources_list.txt

javac -d target/classes ^
  --module-path "%JFXPATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web ^
  -cp "%CPPATH%" ^
  -encoding UTF-8 ^
  @sources_list.txt

del sources_list.txt

if %ERRORLEVEL% EQU 0 (
  echo.
  echo Compilation successful!
) else (
  echo.
  echo Compilation failed! Check errors above.
)

pause
