<?php

namespace App\Service;

class SmsService
{
    private ?string $lastError = null;

    public function __construct(
        private readonly ?string $twilioSid = null,
        private readonly ?string $twilioAuthToken = null,
        private readonly ?string $twilioFrom = null,
        private readonly ?string $brevoSmsApiKey = null,
        private readonly ?string $brevoSmsSender = null
    ) {
    }

    public function canSend(): bool
    {
        return $this->canSendWithTwilio() || $this->canSendWithBrevo();
    }

    public function sendSms(string $toPhone, string $message): bool
    {
        $this->lastError = null;

        $to = $this->normalizePhone($toPhone, true);
        if ($to === null) {
            $this->lastError = 'Numero telephone vide ou invalide.';
            return false;
        }

        if ($this->canSendWithTwilio()) {
            return $this->sendWithTwilio($to, $message);
        }

        if ($this->canSendWithBrevo()) {
            return $this->sendWithBrevo($to, $message);
        }

        return false;
    }

    public function getLastError(): ?string
    {
        return $this->lastError;
    }

    private function sendWithTwilio(string $toPhoneE164, string $message): bool
    {
        $url = sprintf('https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json', (string) $this->twilioSid);
        $payload = http_build_query([
            'From' => (string) $this->twilioFrom,
            'To' => $toPhoneE164,
            'Body' => $message,
        ]);

        $headers = [
            'Content-Type: application/x-www-form-urlencoded',
            'Authorization: Basic ' . base64_encode((string) $this->twilioSid . ':' . (string) $this->twilioAuthToken),
        ];

        [$status, $body] = $this->sendCurlRequest($url, $payload, $headers);
        if (!($status >= 200 && $status < 300)) {
            $this->lastError = sprintf('Twilio HTTP %d: %s', $status, $this->extractApiError($body));
        }
        return $status >= 200 && $status < 300;
    }

    private function sendWithBrevo(string $toPhoneE164, string $message): bool
    {
        // Brevo expects recipient without '+', e.g. 33612345678
        $recipient = ltrim($toPhoneE164, '+');

        $payload = json_encode([
            'sender' => (string) $this->brevoSmsSender,
            'recipient' => $recipient,
            'content' => $message,
            'type' => 'transactional',
        ], JSON_THROW_ON_ERROR);

        $headers = [
            'accept: application/json',
            'content-type: application/json',
            'api-key: ' . (string) $this->brevoSmsApiKey,
        ];

        [$status, $body] = $this->sendCurlRequest('https://api.brevo.com/v3/transactionalSMS/send', $payload, $headers);
        if (!($status >= 200 && $status < 300)) {
            $this->lastError = sprintf('Brevo HTTP %d: %s', $status, $this->extractApiError($body));
        }
        return $status >= 200 && $status < 300;
    }

    /**
     * @return array{0:int,1:string}
     */
    private function sendCurlRequest(string $url, string $payload, array $headers): array
    {
        $ch = curl_init($url);
        if ($ch === false) {
            return [0, 'curl_init failed'];
        }

        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_TIMEOUT, 20);
        $responseBody = (string) curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($status === 0 && $curlError !== '') {
            return [0, $curlError];
        }

        return [$status, $responseBody];
    }

    private function extractApiError(string $body): string
    {
        if ($body === '') {
            return 'No response body';
        }

        $decoded = json_decode($body, true);
        if (!is_array($decoded)) {
            return $body;
        }

        $message = $decoded['message'] ?? $decoded['description'] ?? null;
        if (is_string($message) && $message !== '') {
            return $message;
        }

        return json_encode($decoded, JSON_UNESCAPED_UNICODE) ?: 'Unknown API error';
    }

    private function canSendWithTwilio(): bool
    {
        return (bool) ($this->twilioSid && $this->twilioAuthToken && $this->twilioFrom);
    }

    private function canSendWithBrevo(): bool
    {
        return (bool) ($this->brevoSmsApiKey && $this->brevoSmsSender);
    }

    private function normalizePhone(string $phone, bool $toE164 = false): ?string
    {
        $clean = preg_replace('/[^\d+]/', '', trim($phone)) ?? '';
        if ($clean === '') {
            return null;
        }

        if (str_starts_with($clean, '+')) {
            return $toE164 ? $clean : ltrim($clean, '+');
        }

        // Default: Tunisia country code if local format is used.
        if (strlen($clean) === 8) {
            return $toE164 ? ('+216' . $clean) : ('216' . $clean);
        }

        if (str_starts_with($clean, '00')) {
            return $toE164 ? ('+' . substr($clean, 2)) : substr($clean, 2);
        }

        return $toE164 ? ('+' . $clean) : $clean;
    }
}
