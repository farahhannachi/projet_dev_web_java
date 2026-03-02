<?php
// diag.php
try {
    $pdo = new PDO('mysql:host=127.0.0.1;dbname=pharmacie', 'root', '');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- TABLES ---\n";
    $stmt = $pdo->query('SHOW TABLES');
    $tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
    foreach ($tables as $table) {
        echo $table . "\n";
    }

    if (in_array('utilisateur', $tables)) {
        echo "\n--- USERS ---\n";
        $stmt = $pdo->query('SELECT id_utilisateur, email, roles FROM utilisateur');
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            echo "ID: {$row['id_utilisateur']} | Email: {$row['email']} | Roles: {$row['roles']}\n";
        }
    } else {
        echo "\nTable 'utilisateur' NOT FOUND\n";
    }

} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
