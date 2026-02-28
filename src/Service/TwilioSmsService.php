<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Twilio\Rest\Client;

class TwilioSmsService
{
    private ?Client $twilioClient = null;
    private LoggerInterface $logger;
    private bool $enabled = false;
    private string $fromNumber;

    public function __construct(
        LoggerInterface $logger,
        ?string $accountSid = null,
        ?string $authToken = null,
        ?string $fromNumber = null
    ) {
        $this->logger = $logger;
        $this->fromNumber = $fromNumber ?? '';

        // Vérifier si Twilio est configuré
        if (!empty($accountSid) && !empty($authToken) && !empty($fromNumber)) {
            try {
                $this->twilioClient = new Client($accountSid, $authToken);
                $this->enabled = true;
                $this->logger->info('Twilio SMS initialisé avec succès');
            } catch (\Exception $e) {
                $this->logger->error('Erreur lors de l\'initialisation de Twilio: ' . $e->getMessage());
            }
        } else {
            $this->logger->warning('Twilio non configuré - Les SMS sont désactivés');
        }
    }

    /**
     * Envoie un SMS de notification d'ordonnance prête
     */
    public function sendOrdonnancePrete(string $toNumber, string $numeroOrdonnance, string $nomPatient): bool
    {
        if (!$this->enabled) {
            $this->logger->info('SMS ignoré - Twilio non configuré');
            return false;
        }

        $message = "Bonjour {$nomPatient},\n\n"
                 . "Votre ordonnance {$numeroOrdonnance} est prête ! 🎉\n\n"
                 . "Vous pouvez venir la récupérer à la pharmacie.\n\n"
                 . "Cordialement,\nCURAVITA";

        return $this->sendSms($toNumber, $message);
    }

    /**
     * Envoie un SMS de notification d'ordonnance rejetée
     */
    public function sendOrdonnanceRejetee(string $toNumber, string $numeroOrdonnance, string $nomPatient, ?string $raison = null): bool
    {
        if (!$this->enabled) {
            $this->logger->info('SMS ignoré - Twilio non configuré');
            return false;
        }

        $message = "Bonjour {$nomPatient},\n\n"
                 . "Votre ordonnance {$numeroOrdonnance} a été rejetée.\n\n";
        
        if ($raison) {
            $message .= "Raison: {$raison}\n\n";
        }
        
        $message .= "Veuillez nous contacter pour plus d'informations.\n\n"
                  . "Cordialement,\nCURAVITA";

        return $this->sendSms($toNumber, $message);
    }

    /**
     * Envoie un SMS générique
     */
    public function sendSms(string $toNumber, string $message): bool
    {
        if (!$this->enabled) {
            $this->logger->error('SMS ignoré - Twilio non configuré. Vérifiez TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN et TWILIO_FROM_NUMBER dans .env');
            return false;
        }

        try {
            // Formater le numéro de téléphone (ajouter + si absent)
            if (!str_starts_with($toNumber, '+')) {
                $toNumber = '+' . $toNumber;
            }

            $this->logger->info("Tentative d'envoi SMS à {$toNumber} depuis {$this->fromNumber}");

            $this->twilioClient->messages->create(
                $toNumber,
                [
                    'from' => $this->fromNumber,
                    'body' => $message
                ]
            );

            $this->logger->info("✅ SMS envoyé avec succès à {$toNumber}");
            return true;
        } catch (\Exception $e) {
            $this->logger->error("❌ Erreur Twilio lors de l'envoi du SMS à {$toNumber}: " . $e->getMessage() . " | Code: " . $e->getCode());
            return false;
        }
    }

    /**
     * Vérifie si Twilio est activé
     */
    public function isEnabled(): bool
    {
        return $this->enabled;
    }
}
