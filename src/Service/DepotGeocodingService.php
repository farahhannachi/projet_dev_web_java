<?php

namespace App\Service;

use App\Entity\Depot;

class DepotGeocodingService
{
    public function canGeocode(): bool
    {
        return true;
    }

    public function geocodeDepot(Depot $depot): bool
    {
        $address = trim((string) $depot->getAdresseDepot());
        $city = trim((string) $depot->getVille());
        if ($address === '' && $city === '') {
            return false;
        }

        // Try multiple queries, from most specific to least specific.
        $queries = [];
        if ($address !== '' && $city !== '') {
            $queries[] = sprintf('%s, %s', $address, $city);
            $queries[] = sprintf('%s, %s, Tunisie', $address, $city);
        }
        if ($address !== '') {
            $queries[] = $address;
            $queries[] = sprintf('%s, Tunisie', $address);
        }
        if ($city !== '') {
            $queries[] = $city;
            $queries[] = sprintf('%s, Tunisie', $city);
        }

        foreach (array_values(array_unique($queries)) as $query) {
            $coords = $this->geocodeAddress($query);
            if ($coords === null) {
                continue;
            }

            $depot->setLatitude($coords['lat']);
            $depot->setLongitude($coords['lng']);
            return true;
        }

        return false;
    }

    /**
     * @return array{lat: float, lng: float}|null
     */
    public function geocodeAddress(string $address): ?array
    {
        $url = sprintf('https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=%s', rawurlencode($address));

        $json = $this->requestJson($url);
        if (!is_array($json) || !isset($json[0]) || !is_array($json[0])) {
            return null;
        }
        if (!isset($json[0]['lat'], $json[0]['lon'])) {
            return null;
        }

        return [
            'lat' => (float) $json[0]['lat'],
            'lng' => (float) $json[0]['lon'],
        ];
    }

    /**
     * @return array<mixed>|null
     */
    private function requestJson(string $url): ?array
    {
        $headers = "User-Agent: PIDEV-Depot-Geocoder/1.0\r\nAccept-Language: fr,en;q=0.8\r\n";

        $context = stream_context_create([
            'http' => [
                'method' => 'GET',
                'timeout' => 8,
                'ignore_errors' => true,
                'header' => $headers,
            ],
            'ssl' => [
                'verify_peer' => false,
                'verify_peer_name' => false,
            ],
        ]);

        $response = @file_get_contents($url, false, $context);
        if (is_string($response) && $response !== '') {
            $json = json_decode($response, true);
            if (is_array($json)) {
                return $json;
            }
        }

        // Fallback cURL if available.
        if (function_exists('curl_init')) {
            $ch = curl_init($url);
            if ($ch !== false) {
                curl_setopt_array($ch, [
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_FOLLOWLOCATION => true,
                    CURLOPT_TIMEOUT => 8,
                    CURLOPT_HTTPHEADER => [
                        'User-Agent: PIDEV-Depot-Geocoder/1.0',
                        'Accept-Language: fr,en;q=0.8',
                    ],
                    CURLOPT_SSL_VERIFYPEER => false,
                    CURLOPT_SSL_VERIFYHOST => false,
                ]);
                $body = curl_exec($ch);
                curl_close($ch);
                if (is_string($body) && $body !== '') {
                    $json = json_decode($body, true);
                    if (is_array($json)) {
                        return $json;
                    }
                }
            }
        }

        return null;
    }
}
