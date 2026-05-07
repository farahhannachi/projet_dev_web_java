# Script de test d'initialisation de la base de données

$projectPath = "C:\Users\fahan\Downloads\projet java\projet_dev_web_java-Utilisateur_java"
$mavenPath = "$projectPath\apache-maven-3.9.7\bin\mvn.cmd"

Write-Host "=== Vérification de l'initialisation de la base de données ===" -ForegroundColor Green

# Compiler le projet
Write-Host "`n1. Compilation du projet..." -ForegroundColor Cyan
cd $projectPath
& $mavenPath clean compile -q

# Empaqueter
Write-Host "`n2. Empaquetage du projet..." -ForegroundColor Cyan
& $mavenPath package -q -DskipTests

Write-Host "`n✓ Compilation et empaquetage réussis!" -ForegroundColor Green

# Créer un petit test Java pour vérifier l'initialisation
Write-Host "`n3. Test d'initialisation de la base de données..." -ForegroundColor Cyan

$testCode = @'
import org.example.util.DatabaseInitializer;
import org.example.util.DatabaseUtil;

public class TestDatabaseInit {
    public static void main(String[] args) {
        try {
            System.out.println("Initialisation de la base de données...");
            DatabaseInitializer.initializeDatabase();
            System.out.println("✓ Initialisation réussie!");

            if (DatabaseInitializer.reservationTableExists()) {
                System.out.println("✓ Table 'reservation' existe et est accessible!");
            } else {
                System.out.println("✗ Table 'reservation' n'existe pas");
            }

        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
'@

$testCode | Out-File -FilePath "$projectPath\src\main\java\TestDatabaseInit.java" -Encoding UTF8

Write-Host "`n4. Compilation du test..." -ForegroundColor Cyan
& $mavenPath compile -q

Write-Host "`n✓ Test compilé!" -ForegroundColor Green
Write-Host "`nPour lancer l'application, utilisez run_app.bat" -ForegroundColor Yellow

