<?php

namespace App\Service;

use App\Entity\Question;
use App\Entity\ResponseQuestion;
use App\Entity\Commande;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use Psr\Log\LoggerInterface;

class MailerService
{
    private MailerInterface $mailer;
    private ?LoggerInterface $logger;
    private string $fromEmail = 'curavita123@gmail.com';
    private string $fromName = 'CURAVITA Support';
    private string $adminEmail = 'curavita123@gmail.com';
    private string $logFile;

    public function __construct(
        MailerInterface $mailer,
        string $fromEmail,
        string $fromName,
        string $adminEmail,
        ?LoggerInterface $logger = null
    ) {
        $this->mailer = $mailer;
        $this->fromEmail = $fromEmail;
        $this->fromName = $fromName;
        $this->adminEmail = $adminEmail;
        $this->logger = $logger;
        $this->logFile = dirname(__DIR__, 2) . '/public/email_debug.log';
    }

    /**
     * Write to debug log file
     */
    private function logToFile(string $message): void
    {
        $timestamp = date('Y-m-d H:i:s');
        $logMessage = "[{$timestamp}] {$message}\n";
        file_put_contents($this->logFile, $logMessage, FILE_APPEND);
    }

    /**
     * Envoie un email de confirmation lors de la création d'un ticket
     */
    public function sendTicketCreatedEmail(Question $question): void
    {
        $user = $question->getUtilisateur();
        
        if (!$user || !$user->getEmail()) {
            if ($this->logger) {
                $this->logger->warning('Cannot send email: no user or email');
            }
            $this->logToFile('Cannot send ticket email: no user or email');
            return;
        }

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($user->getEmail())
            ->subject('Ticket #' . $question->getId() . ' cree - CURAVITA')
            ->htmlTemplate('emails/ticket_created.html.twig')
            ->context([
                'user' => $user,
                'question' => $question,
                'ticketUrl' => '/question/' . $question->getId()
            ]);

        $this->logToFile("Attempting to send ticket email to: " . $user->getEmail());
        
        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Ticket email sent to: " . $user->getEmail());
            if ($this->logger) {
                $this->logger->info('Email sent successfully to: ' . $user->getEmail());
            }
        } catch (\Exception $e) {
            $this->logToFile("FAILED: " . $e->getMessage());
            if ($this->logger) {
                $this->logger->error('Email sending failed: ' . $e->getMessage());
            }
            throw $e;
        }
    }

    /**
     * Envoie un email de notification lors d'une réponse admin
     */
    public function sendTicketResponseEmail(Question $question, ResponseQuestion $response): void
    {
        $user = $question->getUtilisateur();
        
        if (!$user || !$user->getEmail()) {
            if ($this->logger) {
                $this->logger->warning('Cannot send response email: no user or email');
            }
            $this->logToFile('Cannot send response email: no user or email');
            return;
        }

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($user->getEmail())
            ->subject('Nouvelle reponse a votre ticket #' . $question->getId() . ' - CURAVITA')
            ->htmlTemplate('emails/ticket_response.html.twig')
            ->context([
                'user' => $user,
                'question' => $question,
                'response' => $response,
                'ticketUrl' => '/question/' . $question->getId()
            ]);

        $this->logToFile("Attempting to send ticket response email to: " . $user->getEmail());
        
        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Ticket response email sent to: " . $user->getEmail());
            if ($this->logger) {
                $this->logger->info('Response email sent successfully to: ' . $user->getEmail());
            }
        } catch (\Exception $e) {
            $this->logToFile("FAILED: " . $e->getMessage());
            if ($this->logger) {
                $this->logger->error('Response email sending failed: ' . $e->getMessage());
            }
            throw $e;
        }
    }

    /**
     * Envoie un email de facture pour une commande
     * @param array<int, array<string, mixed>> $items
     * @return bool True if email was sent successfully, false otherwise
     */
    public function sendCommandeInvoiceEmail(Commande $commande, array $items): bool
    {
        if (!$commande->getEmail()) {
            $this->logToFile('Cannot send invoice: no email address in commande');
            return false;
        }

        $this->logToFile("Attempting to send invoice to: " . $commande->getEmail());

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($commande->getEmail())
            ->subject('Facture commande #' . $commande->getId() . ' - CURAVITA')
            ->htmlTemplate('emails/commande_invoice.html.twig')
            ->context([
                'commande' => $commande,
                'items' => $items,
            ]);

        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Invoice email sent to: " . $commande->getEmail());
            return true;
        } catch (\Exception $e) {
            $this->logToFile("FAILED: " . $e->getMessage());
            if ($this->logger) {
                $this->logger->error('Failed to send invoice email: ' . $e->getMessage());
            }
            return false;
        }
    }

    /**
     * Envoie une notification admin pour une nouvelle commande
     * @return bool True if email was sent successfully, false otherwise
     */
    /**
     * @param array<int, array<string, mixed>> $items
     */
    public function sendAdminCommandeNotification(Commande $commande, array $items): bool
    {
        $this->logToFile("Attempting to send admin notification for commande #" . $commande->getId());

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($this->adminEmail)
            ->subject('Nouvelle commande #' . $commande->getId())
            ->htmlTemplate('emails/commande_admin_alert.html.twig')
            ->context([
                'commande' => $commande,
                'items' => $items,
            ]);

        
        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Admin notification sent for commande #" . $commande->getId());
            return true;
        } catch (\Exception $e) {
            $this->logToFile("FAILED: Admin notification - " . $e->getMessage());
            if ($this->logger) {
                $this->logger->error('Failed to send admin notification email: ' . $e->getMessage());
            }
            return false;
        }
    }

    public function sendCommandeStatusUpdateEmail(Commande $commande, string $oldStatus, string $newStatus): bool
    {
        if (!$commande->getEmail()) {
            $this->logToFile('Cannot send status update: no email address');
            return false;
        }

        $this->logToFile("Attempting to send status update to: " . $commande->getEmail());

        $labels = [
            'en_attente' => 'En attente',
            'confirmee' => 'Confirmee',
            'annulee' => 'Annulee',
            'livree' => 'Livree',
            'review' => 'En revue anti-fraude',
            'bloquee' => 'Bloquee',
        ];

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($commande->getEmail())
            ->subject('Mise a jour de votre commande #' . $commande->getId())
            ->htmlTemplate('emails/commande_status_update.html.twig')
            ->context([
                'commande' => $commande,
                'old_status' => $labels[$oldStatus] ?? $oldStatus,
                'new_status' => $labels[$newStatus] ?? $newStatus,
            ]);

        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Status update email sent to: " . $commande->getEmail());
            return true;
        } catch (\Exception $e) {
            $this->logToFile("FAILED: Status update - " . $e->getMessage());
            return false;
        }
    }

    /**
     * Send a raw text email to a specific address
     * Used for AI Stress Scan contact admin feature
     */
    public function sendRawEmail(string $to, string $subject, string $body): bool
    {
        $email = (new Email())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($to)
            ->subject($subject)
            ->text($body);

        try {
            $this->mailer->send($email);
            $this->logToFile("SUCCESS: Raw email sent to: " . $to);
            return true;
        } catch (\Exception $e) {
            $this->logToFile("FAILED: Raw email - " . $e->getMessage());
            if ($this->logger) {
                $this->logger->error('Failed to send raw email: ' . $e->getMessage());
            }
            return false;
        }
    }
}
