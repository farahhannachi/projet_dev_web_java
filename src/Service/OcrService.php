<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\HttpFoundation\File\File;

class OcrService
{
    private const API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';
    private string $apiKey;

    public function __construct(
        private HttpClientInterface $httpClient,
        string $apiKey
    ) {
        $this->apiKey = $apiKey;
    }

    /**
     * Extracts text from an image file path using Google Gemini.
     * 
     * @param string $filePath Absolute path to the image file
     * @return string Extracted text or error message
     */
    public function extractText(string $filePath): string
    {
        if (!file_exists($filePath)) {
            return "Erreur: Le fichier n'existe pas ($filePath).";
        }

        try {
            $imageData = base64_encode(file_get_contents($filePath));
            $mimeType = mime_content_type($filePath);

            $response = $this->httpClient->request('POST', self::API_URL, [
                'headers' => [
                    'x-goog-api-key' => $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => 'Analyse cette image et donne une description détaillée de ce qu\'elle contient. S\'il s\'agit d\'un produit pharmaceutique, précise le nom, le dosage et l\'usage si possible.'],
                                [
                                    'inline_data' => [
                                        'mime_type' => $mimeType,
                                        'data' => $imageData
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
            ]);

            if ($response->getStatusCode() !== 200) {
                return "Erreur Gemini API ({$response->getStatusCode()}): " . $response->getContent(false);
            }

            $data = $response->toArray();
            
            // Extract the text from Gemini response structure
            $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? '';
            
            if (empty(trim($text))) {
                return "Aucun texte détecté dans l'image par Gemini.";
            }

            return trim($text);

        } catch (\Throwable $e) {
            return "Erreur lors de l'extraction Gemini : " . $e->getMessage();
        }
    }
}
