# Script pour tester l'application CuraVita
Write-Host "========================================" -ForegroundColor Green
Write-Host "TEST CURAVITA - DEPOT LOADING" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

$projectPath = "C:\Users\fahan\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"
$targetPath = "$projectPath\target"

Write-Host "Vérification des fichiers..." -ForegroundColor Yellow

# Check Depots.fxml
if (Test-Path "$projectPath\src\main\resources\fxml\Depots.fxml") {
    Write-Host "✅ Depots.fxml existe" -ForegroundColor Green
} else {
    Write-Host "❌ Depots.fxml manquant!" -ForegroundColor Red
}

# Check DepotController
if (Test-Path "$projectPath\src\main\java\org\example\controller\DepotController.java") {
    Write-Host "✅ DepotController.java existe" -ForegroundColor Green
} else {
    Write-Host "❌ DepotController.java manquant!" -ForegroundColor Red
}

# Check DepotService singleton
if (Select-String -Path "$projectPath\src\main\java\org\example\service\DepotService.java" -Pattern "getInstance" -Quiet) {
    Write-Host "✅ DepotService.getInstance() trouvé (singleton)" -ForegroundColor Green
} else {
    Write-Host "❌ DepotService singleton non trouvé!" -ForegroundColor Red
}

# Check DashboardController uses singleton
if (Select-String -Path "$projectPath\src\main\java\org\example\controller\DashboardController.java" -Pattern "getInstance" -Quiet) {
    Write-Host "✅ DashboardController utilise le singleton" -ForegroundColor Green
} else {
    Write-Host "⚠️ DashboardController pourrait ne pas utiliser le singleton" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Pour lancer l'application, exécutez:" -ForegroundColor Cyan
Write-Host "cd '$projectPath'" -ForegroundColor Cyan
Write-Host ".\apache-maven-3.9.7\bin\mvn.cmd javafx:run" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Green

