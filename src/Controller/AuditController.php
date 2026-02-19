<?php

namespace App\Controller;

use App\Service\AuditService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class AuditController extends AbstractController
{
    #[Route('/admin/audit/{entityType}/{entityId}', name: 'admin_audit_history', methods: ['GET'])]
    public function history(
        string $entityType,
        int $entityId,
        AuditService $auditService
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $history = $auditService->getEntityHistory($entityType, $entityId);

        return $this->render('Admin/audit/history.html.twig', [
            'entityType' => $entityType,
            'entityId' => $entityId,
            'history' => $history
        ]);
    }
}
