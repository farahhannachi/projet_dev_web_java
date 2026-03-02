<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class GroqLlmService
{
    public function __construct(
        private HttpClientInterface $httpClient,
        private string $groqApiKey,
        private string $groqModel,
    ) {}

    /**
     * @return array Decoded JSON response
     */
    public function chat(array $messages, float $temperature = 0.0, int $maxTokens = 120): array
    {
        $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
            'headers' => [
                'Content-Type'  => 'application/json',
                'Authorization' => 'Bearer ' . $this->groqApiKey,
            ],
            'json' => [
                'model' => $this->groqModel,
                'messages' => $messages,
                'temperature' => $temperature,
                'max_tokens' => $maxTokens,
            ],
            'timeout' => 15,
        ]);

        return $response->toArray(false); // false => don’t throw on 4xx/5xx automatically
    }
}