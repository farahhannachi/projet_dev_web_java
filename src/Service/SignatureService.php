<?php

namespace App\Service;

use App\Entity\Ordonnance;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\HttpFoundation\RequestStack;

class SignatureService
{
    private EntityManagerInterface $entityManager;
    private RequestStack $requestStack;

    public function __construct(
        EntityManagerInterface $entityManager,
        RequestStack $requestStack
    ) {
        $this->entityManager = $entityManager;
        $this->requestStack = $requestStack;
    }

    /**
     * Génère une signature électronique pour le patient
     */
    public function signerParPatient(Ordonnance $ordonnance): array
    {
        try {
            $utilisateur = $ordonnance->getUtilisateur();
            
            if (!$utilisateur) {
                return [
                    'success' => false,
                    'message' => 'Utilisateur non trouvé'
                ];
            }

            // Générer la signature (format: nom complet + date + hash)
            $nomComplet = $utilisateur->getNom() . ' ' . $utilisateur->getPrenom();
            $timestamp = (new \DateTime())->format('Y-m-d H:i:s');
            $hash = substr(md5($nomComplet . $timestamp . $ordonnance->getNumeroOrdonnance()), 0, 8);
            
            $signature = sprintf(
                "Signé électroniquement par %s\nLe %s\nCode de vérification: %s",
                $nomComplet,
                $timestamp,
                strtoupper($hash)
            );

            // Récupérer l'IP du client
            $request = $this->requestStack->getCurrentRequest();
            $ipAddress = $request ? $request->getClientIp() : 'unknown';

            // Enregistrer la signature
            $ordonnance->setSignaturePatient($signature);
            $ordonnance->setSignaturePatientDate(new \DateTime());
            $ordonnance->setSignaturePatientIp($ipAddress);

            $this->entityManager->flush();

            return [
                'success' => true,
                'message' => 'Signature enregistrée avec succès',
                'signature' => $signature,
                'date' => $timestamp,
                'code' => strtoupper($hash)
            ];

        } catch (\Exception $e) {
            return [
                'success' => false,
                'message' => 'Erreur lors de la signature: ' . $e->getMessage()
            ];
        }
    }

    /**
     * Génère une signature électronique pour l'admin/médecin
     */
    public function signerParAdmin(Ordonnance $ordonnance, string $nomMedecin): array
    {
        try {
            // Générer la signature admin
            $timestamp = (new \DateTime())->format('Y-m-d H:i:s');
            $hash = substr(md5($nomMedecin . $timestamp . $ordonnance->getNumeroOrdonnance()), 0, 8);
            
            $signature = sprintf(
                "Validé et signé par %s\nLe %s\nCode de vérification: %s",
                $nomMedecin,
                $timestamp,
                strtoupper($hash)
            );

            // Enregistrer la signature
            $ordonnance->setSignatureMedecin($nomMedecin);
            $ordonnance->setSignatureElectronique(true);
            $ordonnance->setSignatureDate(new \DateTime());
            $ordonnance->setDocusignStatus('completed');
            $ordonnance->setDocusignEnvelopeId('SIG-' . time() . '-' . $hash);

            $this->entityManager->flush();

            return [
                'success' => true,
                'message' => 'Signature admin enregistrée avec succès',
                'signature' => $signature,
                'date' => $timestamp,
                'code' => strtoupper($hash)
            ];

        } catch (\Exception $e) {
            return [
                'success' => false,
                'message' => 'Erreur lors de la signature: ' . $e->getMessage()
            ];
        }
    }

    /**
     * Vérifie si une ordonnance est complètement signée
     */
    public function estCompletementSignee(Ordonnance $ordonnance): bool
    {
        return $ordonnance->getSignaturePatient() !== null 
            && $ordonnance->getSignatureMedecin() !== null;
    }

    /**
     * Génère un document PDF avec les signatures (optionnel)
     */
    public function genererDocumentSigne(Ordonnance $ordonnance): ?string
    {
        // TODO: Implémenter la génération PDF si nécessaire
        return null;
    }
}
