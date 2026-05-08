@echo off
setlocal enabledelayedexpansion

cd /d "C:\Users\HP\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"

REM Créer les dossiers nécessaires
if not exist target\classes mkdir target\classes
if not exist target\resources mkdir target\resources

REM Copier les ressources
xcopy /E /Y "src\main\resources\*" "target\resources\" >nul 2>&1
xcopy /E /Y "src\main\resources\*" "target\classes\" >nul 2>&1

REM Construire le classpath avec les JARs spécifiques
set m2=C:\Users\HP\.m2\repository
set CLASSPATH=src\main\java

REM Ajouter les JARs JavaFX (win versions)
set CLASSPATH=!CLASSPATH!;%m2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar
set CLASSPATH=!CLASSPATH!;%m2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar
set CLASSPATH=!CLASSPATH!;%m2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar
set CLASSPATH=!CLASSPATH!;%m2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar

REM Ajouter les autres dépendances
set CLASSPATH=!CLASSPATH!;%m2%\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set CLASSPATH=!CLASSPATH!;%m2%\com\mysql\mysql-connector-j\8.2.0\mysql-connector-j-8.2.0.jar

echo Compilation en cours...
javac -d target\classes -cp "!CLASSPATH!" -encoding UTF-8 ^
  src\main\java\org\example\CuraVitaApp.java ^
  src\main\java\org\example\model\Client.java ^
  src\main\java\org\example\model\Commande.java ^
  src\main\java\org\example\model\Coupon.java ^
  src\main\java\org\example\model\Depot.java ^
  src\main\java\org\example\model\Produit.java ^
  src\main\java\org\example\model\Stock.java ^
  src\main\java\org\example\model\User.java ^
  src\main\java\org\example\util\DatabaseUtil.java ^
  src\main\java\org\example\service\ClientService.java ^
  src\main\java\org\example\service\CommandeService.java ^
  src\main\java\org\example\service\CouponService.java ^
  src\main\java\org\example\service\DepotService.java ^
  src\main\java\org\example\service\ProduitService.java ^
  src\main\java\org\example\service\StockService.java ^
  src\main\java\org\example\service\UserService.java ^
  src\main\java\org\example\controller\AccueilController.java ^
  src\main\java\org\example\controller\DashboardController.java ^
  src\main\java\org\example\controller\LoginController.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation reussie!
    echo Lancement de l'application CuraVita...

    REM Construire le module-path pour JavaFX
    set MODPATH=%m2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar
    set MODPATH=!MODPATH!;%m2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar
    set MODPATH=!MODPATH!;%m2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar
    set MODPATH=!MODPATH!;%m2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar

    java --module-path "!MODPATH!" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -cp "!CLASSPATH!;target\classes" org.example.CuraVitaApp
) else (
    echo Erreur de compilation!
    pause
    exit /b 1
)

endlocal

