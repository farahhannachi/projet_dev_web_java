<?php
// Test simple de l'API
$url = 'http://localhost:8000/api/verifier-interactions';

$data = [
    'produit_id' => 1,
    'symptomes' => 'Mal de tête intense',
    'antecedents' => 'Je suis allergique au paracétamol'
];

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

echo "HTTP Code: $httpCode\n";
echo "Response: $response\n";

curl_close($ch);
