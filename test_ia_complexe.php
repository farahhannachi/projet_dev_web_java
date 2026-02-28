<?php
/**
 * Test IA Hugging Face avec cas complexe
 */

$url = 'http://localhost:8000/api/verifier-interactions';

// Cas complexe que l'analyse locale ne connaît pas
$data = [
    'produit_id' => 11, // Maxilase
    'symptomes' => 'Mal de gorge persistant depuis 5 jours, difficulté à avaler, fièvre légère',
    'antecedents' => 'Je suis enceinte de 2 mois, premier trimestre de grossesse'
];

echo "🧪 TEST IA HUGGING FACE - CAS COMPLEXE\n";
echo str_repeat("=", 60) . "\n\n";

echo "Données du test:\n";
echo "  Produit: Maxilase (pour la gorge)\n";
echo "  Symptômes: Mal de gorge persistant\n";
echo "  Antécédents: Grossesse (2 mois)\n\n";

echo "⏳ Analyse IA en cours (5-10 secondes)...\n\n";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

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
            echo "✅ Aucun danger majeur détecté\n";
        }
        
        echo "\n💡 Note: ";
        if ($duration < 1) {
            echo "Analyse locale rapide utilisée\n";
        } else {
            echo "IA Hugging Face Mistral-7B utilisée!\n";
        }
    } else {
        echo "❌ Erreur: " . ($result['message'] ?? 'Inconnue') . "\n";
    }
} else {
    echo "❌ Erreur HTTP $httpCode\n";
    echo "Réponse: $response\n";
}

echo "\n" . str_repeat("=", 60) . "\n";
