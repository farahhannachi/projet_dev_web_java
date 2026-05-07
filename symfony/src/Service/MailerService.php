<?php

namespace App\Service;

use App\Entity\Question;
use App\Entity\ResponseQuestion;
use App\Entity\Commande;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use Twig\Environment;
use Psr\Log\LoggerInterface;

class MailerService
{
    private MailerInterface $mailer;
    private Environment $twig;
    private ?LoggerInterface $logger;
    private string $fromEmail = 'curavita123@gmail.com';
    private string $fromName = 'CURAVITA Support';
    private string $adminEmail = 'curavita123@gmail.com';

    public function __construct(MailerInterface $mailer, Environment $twig, ?LoggerInterface $logger = null)
    {
        $this->mailer = $mailer;
        $this->twig = $twig;
        $this->logger = $logger;
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

        // Use Gmail transport
        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');

        try {
            $this->mailer->send($email);
            if ($this->logger) {
                $this->logger->info('Email sent successfully to: ' . $user->getEmail());
            }
        } catch (\Exception $e) {
            if ($this->logger) {
                $this->logger->error('Email sending failed: ' . $e->getMessage());
            }
            // Re-throw to see the error
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

        // Use Gmail transport
        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');

        try {
            $this->mailer->send($email);
            if ($this->logger) {
                $this->logger->info('Response email sent successfully to: ' . $user->getEmail());
            }
        } catch (\Exception $e) {
            if ($this->logger) {
                $this->logger->error('Response email sending failed: ' . $e->getMessage());
            }
            throw $e;
        }
    }

    public function sendCommandeInvoiceEmail(Commande $commande, array $items): void
    {
        if (!$commande->getEmail()) {
            return;
        }

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($commande->getEmail())
            ->subject('Facture commande #' . $commande->getId() . ' - CURAVITA')
            ->htmlTemplate('emails/commande_invoice.html.twig')
            ->context([
                'commande' => $commande,
                'items' => $items,
            ]);

        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');
        $this->mailer->send($email);
    }

    public function sendAdminCommandeNotification(Commande $commande, array $items): void
    {
        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($this->adminEmail)
            ->subject('Nouvelle commande #' . $commande->getId())
            ->htmlTemplate('emails/commande_admin_alert.html.twig')
            ->context([
                'commande' => $commande,
                'items' => $items,
            ]);

        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');
        $this->mailer->send($email);
    }

    public function sendCommandeStatusUpdateEmail(Commande $commande, string $oldStatus, string $newStatus): void
    {
        if (!$commande->getEmail()) {
            return;
        }

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

        $email->getHeaders()->addTextHeader('X-Transport', 'gmail');
        $this->mailer->send($email);
    }
}
