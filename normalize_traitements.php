<?php
require_once __DIR__ . '/vendor/autoload.php';
use Symfony\Component\Dotenv\Dotenv;

$dotenv = new Dotenv();
$dotenv->load(__DIR__ . '/.env');

$dbUrl = $_ENV['DATABASE_URL'];
$parsedUrl = parse_url($dbUrl);
$host = $parsedUrl['host'];
$user = $parsedUrl['user'];
$pass = $parsedUrl['pass'] ?? '';
$path = ltrim($parsedUrl['path'], '/');

try {
    $pdo = new PDO("mysql:host=$host;dbname=$path;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- Normalizing Traitement Statuses ---\n";
    
    // Replace underscores with spaces for all statuses
    $statuses = [
        'en_attente' => 'en attente',
        'valide' => 'validé',
        'rejete' => 'rejeté',
        'annule' => 'annulé'
    ];

    foreach ($statuses as $old => $new) {
        $stmt = $pdo->prepare("UPDATE traitement SET status = :new WHERE status = :old");
        $stmt->execute(['new' => $new, 'old' => $old]);
        echo "Updated '{$old}' to '{$new}' for " . $stmt->rowCount() . " rows.\n";
    }

    echo "Normalization complete.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
