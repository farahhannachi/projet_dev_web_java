<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

class ProfanityFilterController extends AbstractController
{
    private const API_URL = 'https://profanity-filter-by-api-ninjas.p.rapidapi.com/v1/profanityfilter';
    private const API_KEY = 'd1a454e7e7mshc40fe09d18d2472p1efabfjsn503d4dceda54';
    private const API_HOST = 'profanity-filter-by-api-ninjas.p.rapidapi.com';

    #[Route('/api/check-profanity', name: 'api_check_profanity', methods: ['POST'])]
    public function checkProfanity(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $text = $data['text'] ?? '';

        if (empty(trim($text))) {
            return new JsonResponse([
                'success' => true,
                'has_profanity' => false,
                'original' => $text,
                'censored' => $text
            ]);
        }

        try {
            $apiUrl = self::API_URL . '?text=' . urlencode($text);

            $ch = curl_init();
            
            curl_setopt_array($ch, [
                CURLOPT_URL => $apiUrl,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_TIMEOUT => 10,
                CURLOPT_HTTPHEADER => [
                    'x-rapidapi-key: ' . self::API_KEY,
                    'x-rapidapi-host: ' . self::API_HOST
                ]
            ]);

            $response = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $error = curl_error($ch);
            curl_close($ch);

            if ($error) {
                return new JsonResponse([
                    'success' => false,
                    'error' => 'Erreur de connexion à l\'API',
                    'has_profanity' => false
                ], 500);
            }

            if ($httpCode !== 200) {
                return new JsonResponse([
                    'success' => false,
                    'error' => 'API non disponible (code: ' . $httpCode . ')',
                    'has_profanity' => false
                ], 500);
            }

            $result = json_decode($response, true);

            if ($result === null || isset($result['message'])) {
                return new JsonResponse([
                    'success' => false,
                    'error' => $result['message'] ?? 'Réponse API invalide',
                    'has_profanity' => false
                ], 500);
            }

            return new JsonResponse([
                'success' => true,
                'has_profanity' => $result['has_profanity'] ?? false,
                'original' => $result['original'] ?? $text,
                'censored' => $result['censored'] ?? $text
            ]);

        } catch (\Exception $e) {
            return new JsonResponse([
                'success' => false,
                'error' => 'Erreur interne',
                'has_profanity' => false
            ], 500);
        }
    }
}
