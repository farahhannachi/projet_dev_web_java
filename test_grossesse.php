<?php
$url = 'http://localhost:8000/api/verifier-interactions';

$data = [
    'produit_id' => 12, // Ibuprofène
    'symptomes' => 'Douleurs musculaires au niveau du dos, inflammation',
    'antecedents' => 'Je suis enceinte de 3 mois, premier trimestre de grossesse'
];

echo "🧪 TEST: Grossesse + Ibuprofène\n";
echo str_repeat("=", 60) . "\n\n";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "HTTP Code: $httpCode\n\n";

if ($httpCode === 200) {
    $result = json_decode($response, true);
    
    if ($result['success']) {
        $analyse = $result['analyse'];
        
        echo "Résultat:\n";
        echo "  Dangereux: " . ($analyse['dangereux'] ? '❌ OUI' : '✅ NON') . "\n";
        echo "  Niveau: " . strtoupper($analyse['niveau_risque']) . "\n";
        echo "  Peut continuer: " . ($analyse['peut_continuer'] ? '✅ OUI' : '❌ NON') . "\n\n";
        
        if (!empty($analyse['raisons'])) {
            echo "Raisons:\n";
            foreach ($analyse['raisons'] as $i => $raison) {
                echo "  " . ($i + 1) . ". $raison\n";
            }
            echo "\n";
        }
        
        echo "Recommandation:\n  " . $analyse['recommandation'] . "\n\n";
        
        if ($analyse['dangereux'] && !$analyse['peut_continuer']) {
            echo "✅ TEST RÉUSSI - Danger critique détecté et blocage activé!\n";
        } else {
            echo "❌ TEST ÉCHOUÉ - Devrait bloquer!\n";
        }
    }
} else {
    echo "❌ Erreur HTTP\n";
}
