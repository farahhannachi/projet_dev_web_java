<?php

namespace App\Service;

use App\Entity\Question;
use Dompdf\Dompdf;
use Dompdf\Options;
use Twig\Environment;

/**
 * Service de génération de PDF pour les tickets et leurs réponses
 */
class PdfService
{
    private Environment $twig;

    public function __construct(Environment $twig)
    {
        $this->twig = $twig;
    }

    /**
     * Génère un PDF pour un ticket avec ses réponses
     */
    public function generateTicketPdf(Question $question): string
    {
        // Configuration de DomPDF
        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isPhpEnabled', true);
        $options->set('isRemoteEnabled', true);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);

        // Générer le HTML à partir du template Twig
        $html = $this->twig->render('pdf/ticket.html.twig', [
            'question' => $question,
            'reponses' => $question->getReponses(),
            'date_generation' => new \DateTime(),
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return $dompdf->output();
    }

    /**
     * Génère le nom du fichier PDF
     */
    public function generateFileName(Question $question): string
    {
        $date = $question->getCreatedAt() ? $question->getCreatedAt()->format('Y-m-d') : date('Y-m-d');
        $ticketId = $question->getId();
        $type = $question->getTypeTicket() ?? 'ticket';
        
        return sprintf('ticket_%s_%d_%s.pdf', $type, $ticketId, $date);
    }
}
