<?php
// diag_v2.php
try {
    $pdo = new PDO('mysql:host=127.0.0.1;dbname=pharmacie', 'root', '');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- ALL USERS ---\n";
    $stmt = $pdo->query('SELECT id_utilisateur, email, roles FROM utilisateur');
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "Total users found: " . count($users) . "\n";
    foreach ($users as $user) {
        echo "ID: {$user['id_utilisateur']} | Email: {$user['email']} | Roles: {$user['roles']}\n";
    }

} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
