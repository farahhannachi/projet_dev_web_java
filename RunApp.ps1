# Script pour compiler et exécuter l'application CuraVita
$projectDir = "C:\Users\HP\Downloads\projet_dev_web_java-Utilisateur_java\projet_dev_web_java-Utilisateur_java"
$m2Dir = "C:\Users\HP\.m2\repository"

# Créer le dossier target/classes s'il n'existe pas
if (-not (Test-Path "$projectDir\target\classes")) {
    New-Item -ItemType Directory -Path "$projectDir\target\classes" -Force | Out-Null
}

# Créer le dossier target/resources
if (-not (Test-Path "$projectDir\target\resources")) {
    New-Item -ItemType Directory -Path "$projectDir\target\resources" -Force | Out-Null
}

# Copier les ressources
Copy-Item -Path "$projectDir\src\main\resources\*" -Destination "$projectDir\target\resources\" -Recurse -Force 2>$null
Copy-Item -Path "$projectDir\src\main\resources\*" -Destination "$projectDir\target\classes\" -Recurse -Force 2>$null

# Construire le classpath pour toutes les JAR dans .m2
$jars = @()
Get-ChildItem -Path $m2Dir -Filter "*.jar" -Recurse | ForEach-Object {
    $jars += $_.FullName
}

$classPath = $jars -join ";"
$classPath = "$projectDir\src\main\java;$classPath"

Write-Host "Compilation en cours..."
Set-Location $projectDir

# Compiler avec les JARs
javac -d "$projectDir\target\classes" -cp "$classPath" -encoding UTF-8 `
  "$projectDir\src\main\java\org\example\CuraVitaApp.java" `
  "$projectDir\src\main\java\org\example\model\*.java" `
  "$projectDir\src\main\java\org\example\service\*.java" `
  "$projectDir\src\main\java\org\example\controller\*.java" `
  "$projectDir\src\main\java\org\example\util\*.java" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation réussie!"
    Write-Host "Lancement de l'application..."

    # Lancer l'application
    java -cp "$classPath;$projectDir\target\classes" org.example.CuraVitaApp
} else {
    Write-Host "Erreur de compilation!"
    exit 1
}

