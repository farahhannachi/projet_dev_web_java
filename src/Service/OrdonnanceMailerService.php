<?php

namespace App\Service;

use App\Entity\Ordonnance;
use App\Repository\TraitementRepository;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use Psr\Log\LoggerInterface;

class OrdonnanceMailerService
{
    private MailerInterface $mailer;
    private TraitementRepository $traitementRepository;
    private LoggerInterface $logger;
    private string $fromEmail = 'curavita123@gmail.com';
    private string $fromName = 'CURAVITA Pharmacie';

    public function __construct(
        MailerInterface $mailer,
        TraitementRepository $traitementRepository,
        LoggerInterface $logger
    ) {
        $this->mailer = $mailer;
        $this->traitementRepository = $traitementRepository;
        $this->logger = $logger;
    }

    /**
     * Envoie un email avec l'ordonnance validée et ses traitements
     */
    public function sendOrdonnanceValideeEmail(Ordonnance $ordonnance): bool
    {
        $user = $ordonnance->getUtilisateur();
        
        if (!$user || !$user->getEmail()) {
            $this->logger->warning('Cannot send ordonnance email: no user or email for ordonnance #' . $ordonnance->getId());
            return false;
        }

        // Récupérer les traitements associés à cette ordonnance
        $traitements = $this->traitementRepository->findBy(
            ['ordonnance' => $ordonnance],
            ['id' => 'ASC']
        );

        $email = (new TemplatedEmail())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($user->getEmail())
            ->subject('Votre ordonnance ' . $ordonnance->getNumeroOrdonnance() . ' est prête - CURAVITA')
            ->htmlTemplate('emails/ordonnance_validee.html.twig')
            ->context([
                'user' => $user,
                'ordonnance' => $ordonnance,
                'traitements' => $traitements,
            ]);

        try {
            $this->mailer->send($email);
            $this->logger->info('✅ Ordonnance email sent successfully to: ' . $user->getEmail() . ' for ordonnance #' . $ordonnance->getNumeroOrdonnance());
            return true;
        } catch (\Exception $e) {
            $this->logger->error('❌ Ordonnance email sending failed: ' . $e->getMessage());
            $this->logger->error('Stack trace: ' . $e->getTraceAsString());
            throw new \RuntimeException('Email sending failed: ' . $e->getMessage());
        }
    }
}
