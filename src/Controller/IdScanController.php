<?php

namespace App\Controller;

use App\Entity\Utilisateur;
use App\Repository\UtilisateurRepository;
use App\Service\MailerService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Core\Authentication\Token\UsernamePasswordToken;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\String\Slugger\SluggerInterface;

class IdScanController extends AbstractController
{
    public function __construct(
        private EntityManagerInterface $entityManager,
        private UtilisateurRepository $utilisateurRepository,
        private SluggerInterface $slugger,
        private MailerService $mailerService,
    ) {
    }

    /**
     * Handle ID card scan login
     * Receives student_id from OCR and authenticates user programmatically
     */
    #[Route('/scan-id-login', name: 'scan_id_login', methods: ['POST'])]
    public function scanIdLogin(Request $request): JsonResponse
    {
        try {
            $data = json_decode($request->getContent(), true);
            
            if (!isset($data['student_id']) || empty($data['student_id'])) {
                return $this->json([
                    'status' => 'error',
                    'message' => 'No ID detected. Please try again.'
                ], Response::HTTP_BAD_REQUEST);
            }

            // Sanitize the student ID
            $studentId = $this->sanitizeStudentId($data['student_id']);

            if (empty($studentId)) {
                return $this->json([
                    'status' => 'error',
                    'message' => 'Invalid ID format. Please try again.'
                ], Response::HTTP_BAD_REQUEST);
            }

            // Find user by student ID
            $user = $this->utilisateurRepository->findOneBy(['studentId' => $studentId]);
            
            // If not found, try fuzzy matching for common OCR errors
            if (!$user) {
                $user = $this->findUserWithFuzzyMatching($studentId);
            }

            if (!$user) {
                return $this->json([
                    'status' => 'error',
                    'message' => 'ID not recognized. Please register this ID on your profile page first.'
                ], Response::HTTP_NOT_FOUND);
            }

            // Check if account is active
            if ($user->getEtatCompte() !== 'actif') {
                return $this->json([
                    'status' => 'error',
                    'message' => 'Account is not active. Please contact support.'
                ], Response::HTTP_FORBIDDEN);
            }

            // Store user ID in session and redirect to login callback
            $request->getSession()->set('scan_login_user_id', $user->getId());
            
            return $this->json([
                'status' => 'success',
                'redirect' => $this->generateUrl('scan_login_complete'),
                'message' => 'ID verified! Redirecting...'
            ]);
        } catch (\Exception $e) {
            return $this->json([
                'status' => 'error',
                'message' => 'Scan failed. Please try again or use email login.'
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Complete the scan-based login by authenticating the user
     */
    #[Route('/scan-login-complete', name: 'scan_login_complete', methods: ['GET'])]
    public function scanLoginComplete(Request $request): Response
    {
        $session = $request->getSession();
        $userId = $session->get('scan_login_user_id');
        
        if (!$userId) {
            $this->addFlash('error', 'Invalid login session.');
            return $this->redirectToRoute('app_login');
        }

        $user = $this->utilisateurRepository->find($userId);
        
        if (!$user) {
            $this->addFlash('error', 'User not found.');
            return $this->redirectToRoute('app_login');
        }

        // Clear the session variable
        $session->remove('scan_login_user_id');
        
        // Create and store the authentication token
        $token = new UsernamePasswordToken(
            $user,
            'main',
            $user->getRoles()
        );
        
        // Set token in security context
        $this->container->get('security.token_storage')->setToken($token);
        
        // Store in session for persistence
        $session->set('_security_main', serialize($token));
        
        $this->addFlash('success', 'Welcome back, ' . $user->getPrenom() . '!');
        
        // Redirect based on user role
        if (in_array('ROLE_ADMIN', $user->getRoles(), true)) {
            return $this->redirectToRoute('admin_dashboard');
        }
        
        return $this->redirectToRoute('app_home');
    }

    /**
     * Upload ID card from profile page
     */
    #[Route('/profile/id-card/upload', name: 'profile_id_card_upload', methods: ['POST'])]
    public function uploadIdCard(Request $request): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return $this->json([
                'status' => 'error',
                'message' => 'You must be logged in.'
            ], Response::HTTP_UNAUTHORIZED);
        }

        /** @var \Symfony\Component\HttpFoundation\File\UploadedFile $file */
        $file = $request->files->get('id_card');
        
        if (!$file) {
            return $this->json([
                'status' => 'error',
                'message' => 'No file uploaded.'
            ], Response::HTTP_BAD_REQUEST);
        }

        // Validate file type
        $allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
        if (!in_array($file->getMimeType(), $allowedTypes)) {
            return $this->json([
                'status' => 'error',
                'message' => 'Only image files (JPEG, PNG, WebP) are allowed.'
            ], Response::HTTP_BAD_REQUEST);
        }

        // Validate file size (5MB max)
        $maxSize = 5 * 1024 * 1024; // 5MB in bytes
        if ($file->getSize() > $maxSize) {
            return $this->json([
                'status' => 'error',
                'message' => 'File size must be less than 5MB.'
            ], Response::HTTP_BAD_REQUEST);
        }

        // Generate unique filename
        $originalFilename = pathinfo($file->getClientOriginalName(), PATHINFO_FILENAME);
        $safeFilename = $this->slugger->slug($originalFilename);
        $newFilename = $safeFilename . '-' . uniqid() . '.' . $file->guessExtension();

        // Upload directory
        $projectDirParam = $this->getParameter('kernel.project_dir');
        $projectDir = is_string($projectDirParam) ? $projectDirParam : '';
        $uploadDir = $projectDir . '/public/uploads/id_cards';
        
        // Create directory if it doesn't exist
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        try {
            $file->move($uploadDir, $newFilename);
            
            // Update user entity
            $user->setIdCardImage('uploads/id_cards/' . $newFilename);
            $this->entityManager->flush();

            return $this->json([
                'status' => 'success',
                'message' => 'ID card uploaded successfully.',
                'image_path' => 'uploads/id_cards/' . $newFilename
            ]);
        } catch (\Exception $e) {
            return $this->json([
                'status' => 'error',
                'message' => 'Failed to upload file. Please try again.'
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Save student ID extracted from OCR
     */
    #[Route('/profile/student-id/save', name: 'profile_student_id_save', methods: ['POST'])]
    public function saveStudentId(Request $request): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return $this->json([
                'status' => 'error',
                'message' => 'You must be logged in.'
            ], Response::HTTP_UNAUTHORIZED);
        }

        $data = json_decode($request->getContent(), true);
        
        if (!isset($data['student_id']) || empty($data['student_id'])) {
            return $this->json([
                'status' => 'error',
                'message' => 'No student ID provided.'
            ], Response::HTTP_BAD_REQUEST);
        }

        $studentId = $this->sanitizeStudentId($data['student_id']);

        if (empty($studentId)) {
            return $this->json([
                'status' => 'error',
                'message' => 'Invalid student ID format.'
            ], Response::HTTP_BAD_REQUEST);
        }

        // Check if student ID is already used by another user
        $existingUser = $this->utilisateurRepository->findOneBy(['studentId' => $studentId]);
        if ($existingUser && $existingUser->getId() !== $user->getId()) {
            return $this->json([
                'status' => 'error',
                'message' => 'This student ID is already registered to another account.'
            ], Response::HTTP_CONFLICT);
        }

        try {
            $user->setStudentId($studentId);
            $this->entityManager->flush();

            return $this->json([
                'status' => 'success',
                'message' => 'Student ID saved successfully. You can now use ID card scan login.',
                'student_id' => $studentId
            ]);
        } catch (\Exception $e) {
            return $this->json([
                'status' => 'error',
                'message' => 'Failed to save student ID. Please try again.'
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete ID card and student ID
     */
    #[Route('/profile/id-card/delete', name: 'profile_id_card_delete', methods: ['POST'])]
    public function deleteIdCard(): JsonResponse
    {
        /** @var Utilisateur $user */
        $user = $this->getUser();
        
        if (!$user) {
            return $this->json([
                'status' => 'error',
                'message' => 'You must be logged in.'
            ], Response::HTTP_UNAUTHORIZED);
        }

        try {
            // Delete image file if exists
            $imagePath = $user->getIdCardImage();
            if ($imagePath) {
                $projectDirParam = $this->getParameter('kernel.project_dir');
                $projectDir = is_string($projectDirParam) ? $projectDirParam : '';
                $fullPath = $projectDir . '/public/' . $imagePath;
                if (file_exists($fullPath)) {
                    unlink($fullPath);
                }
            }

            // Clear database fields
            $user->setIdCardImage(null);
            $user->setStudentId(null);
            $this->entityManager->flush();

            return $this->json([
                'status' => 'success',
                'message' => 'ID card removed successfully.'
            ]);
        } catch (\Exception $e) {
            return $this->json([
                'status' => 'error',
                'message' => 'Failed to remove ID card. Please try again.'
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Sanitize student ID input
     */
    private function sanitizeStudentId(string $studentId): string
    {
        // Remove any non-alphanumeric characters except dashes and underscores
        $sanitized = preg_replace('/[^a-zA-Z0-9_-]/', '', $studentId);
        return strtoupper(trim((string) $sanitized));
    }

    /**
     * Find user with fuzzy matching for common OCR errors
     * Handles misreadings like: 3→J, 0→O, 1→I, 5→S, etc.
     */
    private function findUserWithFuzzyMatching(string $studentId): ?Utilisateur
    {
        // Common OCR misreadings mapping
        $ocrVariations = [
            '3' => ['J'],
            '0' => ['O'],
            '1' => ['I', 'L'],
            '5' => ['S'],
            '8' => ['B'],
            '6' => ['G'],
            'J' => ['3'],
            'O' => ['0'],
            'I' => ['1'],
            'L' => ['1'],
            'S' => ['5'],
            'B' => ['8'],
            'G' => ['6'],
            'M' => ['N', 'H'],
            'N' => ['M'],
            'T' => ['I', '7'],
        ];

        // Try each position with variations
        $chars = str_split($studentId);
        $variations = [$studentId]; // Start with original

        foreach ($chars as $pos => $char) {
            if (isset($ocrVariations[$char])) {
                foreach ($ocrVariations[$char] as $replacement) {
                    $newVariation = $chars;
                    $newVariation[$pos] = $replacement;
                    $variations[] = implode('', $newVariation);
                }
            }
        }

        // Remove duplicates and try each variation
        $variations = array_unique($variations);
        
        foreach ($variations as $variation) {
            $user = $this->utilisateurRepository->findOneBy(['studentId' => $variation]);
            if ($user) {
                return $user;
            }
        }

        // Try more aggressive fuzzy matching - positions that commonly cause issues
        // Handle cases like: 231IMT040S → 231IMT0405 (5 read as S at the end)
        $specificReplacements = [
            str_replace('3', 'J', $studentId, $count1),
            str_replace('0', 'O', $studentId, $count2),
            str_replace('1', 'I', $studentId, $count3),
            str_replace('5', 'S', $studentId, $count4),
            str_replace('S', '5', $studentId, $count5),
        ];

        foreach ($specificReplacements as $replacement) {
            if ($replacement !== $studentId) {
                $user = $this->utilisateurRepository->findOneBy(['studentId' => $replacement]);
                if ($user) {
                    return $user;
                }
            }
        }

        // Try removing common extra characters that OCR adds
        $cleanedVariations = [
            str_replace(['O', '0'], '', $studentId),  // Remove extra O/0
            preg_replace('/(\d{3})([A-Z])O([A-Z])(\d{4})/', '$1$2$3$4', $studentId), // 231IMO405 → 231IM405
            preg_replace('/(\d{3}[A-Z]{2})O(\d{4})/', '$1$2', $studentId), // 231IMTO405 → 231IMT405
        ];

        foreach ($cleanedVariations as $variation) {
            if ($variation !== $studentId && strlen((string) $variation) >= 8) {
                $user = $this->utilisateurRepository->findOneBy(['studentId' => $variation]);
                if ($user) {
                    return $user;
                }
            }
        }

        return null;
    }

    /**
     * Handle contact admin form submission from stress scan results
     * Sends email to admin with user's message
     */
    #[Route('/api/stress-scan/contact-admin', name: 'stress_scan_contact_admin', methods: ['POST'])]
    public function contactAdmin(Request $request): JsonResponse
    {
        try {
            /** @var Utilisateur|null $user */
            $user = $this->getUser();
            
            if (!$user) {
                return $this->json([
                    'success' => false,
                    'error' => 'You must be logged in to send a message.'
                ], Response::HTTP_UNAUTHORIZED);
            }

            $data = json_decode($request->getContent(), true);
            
            if (!isset($data['message']) || empty(trim($data['message']))) {
                return $this->json([
                    'success' => false,
                    'error' => 'Message is required.'
                ], Response::HTTP_BAD_REQUEST);
            }

            $message = trim($data['message']);
            $recommendedProduct = $data['recommendedProduct'] ?? 'Not specified';
            $stressLevel = $data['stressLevel'] ?? 'Not specified';

            // Build email content
            $emailSubject = sprintf(
                '[AI Stress Scan] Message from %s %s',
                $user->getPrenom(),
                $user->getNom()
            );

            $emailBody = sprintf(
                "New message from AI Stress Scan feature\n\n" .
                "User Information:\n" .
                "- Name: %s %s\n" .
                "- Email: %s\n" .
                "- Stress Level: %s\n" .
                "- Recommended Product: %s\n\n" .
                "Message:\n%s\n\n" .
                "---\nSent from Curavita AI Stress Scan",
                $user->getPrenom(),
                $user->getNom(),
                $user->getEmail(),
                $stressLevel,
                $recommendedProduct,
                $message
            );

            // Send email using MailerService
            $this->mailerService->sendRawEmail(
                'ihebjbir10@gmail.com',
                $emailSubject,
                $emailBody
            );

            return $this->json([
                'success' => true,
                'message' => 'Your message has been sent to the admin successfully.'
            ]);

        } catch (\Exception $e) {
            error_log('Error sending stress scan contact email: ' . $e->getMessage());
            return $this->json([
                'success' => false,
                'error' => 'Failed to send message. Please try again later.'
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
}
