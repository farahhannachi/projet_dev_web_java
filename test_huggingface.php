<?php
/**
 * Test de l'IA Hugging Face
 */

$url = 'http://localhost:8000/api/verifier-interactions';

// Test avec un cas complexe
$data = [
    'produit_id' => 12, // Ibuprofène
    'symptomes' => 'Douleurs musculaires intenses après le sport',
    'antecedents' => 'J\'ai un ulcère gastrique diagnostiqué il y a 6 mois'
];

echo "🧪 TEST IA HUGGING FACE\n";
echo str_repeat("=", 60) . "\n\n";

echo "Données du test:\n";
echo "  Produit: Ibuprofène\n";
echo "  Symptômes: Douleurs musculaires\n";
echo "  Antécédents: Ulcère gastrique\n\n";

echo "⏳ Analyse en cours (peut prendre 5-10 secondes)...\n\n";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_TIMEOUT, 30); // 30 secondes max

$start = microtime(true);
$response = curl_exec($ch);
$duration = round(microtime(true) - $start, 2);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

curl_close($ch);

echo "HTTP Code: $httpCode\n";
echo "Durée: {$duration}s\n\n";

if ($httpCode === 200) {
    $result = json_decode($response, true);
    
    if ($result['success']) {
        $analyse = $result['analyse'];
        
        echo "✅ ANALYSE RÉUSSIE\n\n";
        echo "Résultat:\n";
        echo "  Dangereux: " . ($analyse['dangereux'] ? '❌ OUI' : '✅ NON') . "\n";
        echo "  Niveau de risque: " . strtoupper($analyse['niveau_risque']) . "\n";
        echo "  Peut continuer: " . ($analyse['peut_continuer'] ? '✅ OUI' : '❌ NON') . "\n\n";
        
        if (!empty($analyse['raisons'])) {
            echo "Raisons détectées:\n";
            foreach ($analyse['raisons'] as $i => $raison) {
                echo "  " . ($i + 1) . ". $raison\n";
            }
            echo "\n";
        }
        
        echo "Recommandation:\n";
        echo "  " . $analyse['recommandation'] . "\n\n";
        
        if ($analyse['dangereux']) {
            echo "🚨 L'IA a détecté un danger!\n";
        } else {
            echo "✅ Aucun danger détecté\n";
        }
    } else {
        echo "❌ Erreur: " . ($result['message'] ?? 'Inconnue') . "\n";
    }
} else {
    echo "❌ Erreur HTTP $httpCode\n";
    echo "Réponse: $response\n";
}

echo "\n" . str_repeat("=", 60) . "\n";
