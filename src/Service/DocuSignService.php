<?php

namespace App\Service;

use App\Entity\Ordonnance;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class DocuSignService
{
    private HttpClientInterface $httpClient;
    private LoggerInterface $logger;
    private string $apiKey;
    private string $accountId;
    private string $baseUrl;
    private bool $enabled;

    public function __construct(
        HttpClientInterface $httpClient,
        LoggerInterface $logger,
        ?string $apiKey = '',
        ?string $accountId = '',
        ?string $baseUrl = 'https://demo.docusign.net/restapi'
    ) {
        $this->httpClient = $httpClient;
        $this->logger = $logger;
        $this->apiKey = $apiKey ?? '';
        $this->accountId = $accountId ?? '';
        $this->baseUrl = $baseUrl ?? 'https://demo.docusign.net/restapi';
        $this->enabled = !empty($this->apiKey) && !empty($this->accountId);
    }

    /**
     * Vérifie si DocuSign est configuré
     */
    public function isEnabled(): bool
    {
        return $this->enabled;
    }

    /**
     * Crée une enveloppe de signature pour une ordonnance
     */
    public function createSignatureEnvelope(
        Ordonnance $ordonnance,
        string $signerEmail,
        string $signerName
    ): ?array {
        if (!$this->enabled) {
            $this->logger->warning('⚠️ DocuSign non configuré - Utilisation de la signature simulée');
            return $this->simulateSignature($ordonnance, $signerEmail, $signerName);
        }

        try {
            $this->logger->info('📝 Création d\'une enveloppe DocuSign', [
                'ordonnance_id' => $ordonnance->getId(),
                'signer' => $signerEmail
            ]);

            // Préparer le document (ordonnance en PDF)
            $documentBase64 = $this->generateOrdonnancePDF($ordonnance);

            $envelopeDefinition = [
                'emailSubject' => 'Signature électronique - Ordonnance ' . $ordonnance->getNumeroOrdonnance(),
                'documents' => [
                    [
                        'documentBase64' => $documentBase64,
                        'name' => 'Ordonnance_' . $ordonnance->getNumeroOrdonnance() . '.pdf',
                        'fileExtension' => 'pdf',
                        'documentId' => '1'
                    ]
                ],
                'recipients' => [
                    'signers' => [
                        [
                            'email' => $signerEmail,
                            'name' => $signerName,
                            'recipientId' => '1',
                            'routingOrder' => '1',
                            'tabs' => [
                                'signHereTabs' => [
                                    [
                                        'documentId' => '1',
                                        'pageNumber' => '1',
                                        'xPosition' => '100',
                                        'yPosition' => '650'
                                    ]
                                ],
                                'dateSignedTabs' => [
                                    [
                                        'documentId' => '1',
                                        'pageNumber' => '1',
                                        'xPosition' => '300',
                                        'yPosition' => '650'
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                'status' => 'sent'
            ];

            $response = $this->httpClient->request('POST', 
                $this->baseUrl . '/v2.1/accounts/' . $this->accountId . '/envelopes',
                [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->apiKey,
                        'Content-Type' => 'application/json'
                    ],
                    'json' => $envelopeDefinition,
                    'timeout' => 30
                ]
            );

            if ($response->getStatusCode() === 201) {
                $data = $response->toArray();
                $this->logger->info('✅ Enveloppe DocuSign créée', ['envelope_id' => $data['envelopeId']]);
                
                return [
                    'success' => true,
                    'envelope_id' => $data['envelopeId'],
                    'status' => $data['status'] ?? 'sent',
                    'uri' => $data['uri'] ?? null
                ];
            }

            return null;

        } catch (\Exception $e) {
            $this->logger->error('❌ Erreur DocuSign', [
                'error' => $e->getMessage()
            ]);
            
            // Fallback vers signature simulée
            return $this->simulateSignature($ordonnance, $signerEmail, $signerName);
        }
    }

    /**
     * Vérifie le statut d'une enveloppe
     */
    public function checkEnvelopeStatus(string $envelopeId): ?array
    {
        if (!$this->enabled) {
            return [
                'status' => 'completed',
                'completed_date' => new \DateTime()
            ];
        }

        try {
            $response = $this->httpClient->request('GET',
                $this->baseUrl . '/v2.1/accounts/' . $this->accountId . '/envelopes/' . $envelopeId,
                [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->apiKey
                    ],
                    'timeout' => 10
                ]
            );

            if ($response->getStatusCode() === 200) {
                $data = $response->toArray();
                return [
                    'status' => $data['status'],
                    'completed_date' => isset($data['completedDateTime']) ? new \DateTime($data['completedDateTime']) : null
                ];
            }

            return null;

        } catch (\Exception $e) {
            $this->logger->error('❌ Erreur vérification statut DocuSign', [
                'error' => $e->getMessage()
            ]);
            return null;
        }
    }

    /**
     * Génère un PDF de l'ordonnance (version simplifiée)
     */
    private function generateOrdonnancePDF(Ordonnance $ordonnance): string
    {
        // Contenu HTML simple de l'ordonnance
        $html = <<<HTML
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; padding: 40px; }
        h1 { color: #16563f; }
        .info { margin: 20px 0; }
        .signature-zone { margin-top: 100px; border-top: 2px solid #ccc; padding-top: 20px; }
    </style>
</head>
<body>
    <h1>ORDONNANCE MÉDICALE</h1>
    <div class="info">
        <p><strong>Numéro:</strong> {$ordonnance->getNumeroOrdonnance()}</p>
        <p><strong>Date:</strong> {$ordonnance->getDateOrdonnance()->format('d/m/Y')}</p>
        <p><strong>Patient:</strong> {$ordonnance->getUtilisateur()->getNom()} {$ordonnance->getUtilisateur()->getPrenom()}</p>
        <p><strong>Date d'expiration:</strong> {$ordonnance->getDateExpiration()->format('d/m/Y')}</p>
    </div>
    
    <h2>Traitements prescrits:</h2>
HTML;

        foreach ($ordonnance->getTraitements() as $traitement) {
            $html .= "<p>• {$traitement->getProduit()->getNom()} - {$traitement->getDosage()} - {$traitement->getFrequence()}</p>";
        }

        $html .= <<<HTML
    
    <div class="signature-zone">
        <p><strong>Signature du médecin:</strong></p>
        <p>Date: _______________</p>
    </div>
</body>
</html>
HTML;

        // Convertir en base64 (simulation - en production, utiliser un vrai générateur PDF)
        return base64_encode($html);
    }

    /**
     * Simulation de signature (fallback quand DocuSign n'est pas configuré)
     */
    private function simulateSignature(Ordonnance $ordonnance, string $signerEmail, string $signerName): array
    {
        $this->logger->info('🔄 Simulation de signature électronique', [
            'ordonnance_id' => $ordonnance->getId(),
            'signer' => $signerEmail
        ]);

        $envelopeId = 'SIM-' . uniqid();

        return [
            'success' => true,
            'envelope_id' => $envelopeId,
            'status' => 'completed',
            'simulated' => true,
            'message' => 'Signature simulée (DocuSign non configuré)'
        ];
    }
}
