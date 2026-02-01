<?php

namespace App\Service;

use App\Entity\Ordonnance;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\String\Slugger\SluggerInterface;

class FileUploadService
{
    private string $prescriptionsDirectory;
    private SluggerInterface $slugger;

    public function __construct(
        string $prescriptionsDirectory,
        SluggerInterface $slugger
    ) {
        $this->prescriptionsDirectory = $prescriptionsDirectory;
        $this->slugger = $slugger;
    }

    /**
     * Upload a prescription file
     * 
     * @param UploadedFile $file The uploaded file
     * @return array Array with 'fileName' and 'filePath' keys
     * @throws FileException If upload fails
     * @throws \InvalidArgumentException If file type is not allowed
     */
    public function uploadPrescription(UploadedFile $file): array
    {
        // Validate file type
        $this->validateFile($file);

        // Generate a unique filename
        $originalFilename = pathinfo($file->getClientOriginalName(), PATHINFO_FILENAME);
        $safeFilename = $this->slugger->slug($originalFilename);
        $extension = $file->guessExtension();
        
        // Add timestamp and random string for uniqueness
        $newFilename = $safeFilename . '-' . uniqid() . '-' . time() . '.' . $extension;

        // Ensure the upload directory exists
        if (!is_dir($this->prescriptionsDirectory)) {
            mkdir($this->prescriptionsDirectory, 0755, true);
        }

        // Move the file to the upload directory
        try {
            $file->move($this->prescriptionsDirectory, $newFilename);
        } catch (FileException $e) {
            throw new FileException('Failed to upload prescription file: ' . $e->getMessage());
        }

        return [
            'fileName' => $file->getClientOriginalName(),
            'filePath' => $newFilename
        ];
    }

    /**
     * Validate uploaded file
     * 
     * @param UploadedFile $file
     * @throws \InvalidArgumentException If file is invalid
     */
    private function validateFile(UploadedFile $file): void
    {
        // Check if file was uploaded successfully
        if (!$file->isValid()) {
            throw new \InvalidArgumentException('The uploaded file is not valid.');
        }

        // Validate file extension
        $extension = $file->guessExtension();
        if (!Ordonnance::isValidFileExtension($extension)) {
            throw new \InvalidArgumentException(
                sprintf(
                    'Invalid file type. Allowed types: %s',
                    Ordonnance::getAllowedExtensionsString()
                )
            );
        }

        // Validate MIME type
        $mimeType = $file->getMimeType();
        if (!Ordonnance::isValidMimeType($mimeType)) {
            throw new \InvalidArgumentException(
                sprintf(
                    'Invalid file MIME type: %s. Allowed types: %s',
                    $mimeType,
                    Ordonnance::getAllowedExtensionsString()
                )
            );
        }

        // Check file size (max 10MB)
        $maxSize = 10 * 1024 * 1024; // 10MB in bytes
        if ($file->getSize() > $maxSize) {
            throw new \InvalidArgumentException(
                sprintf(
                    'File size exceeds maximum allowed size of %d MB',
                    $maxSize / (1024 * 1024)
                )
            );
        }
    }

    /**
     * Delete a prescription file
     * 
     * @param string $filePath The relative file path
     * @return bool True if deleted successfully, false otherwise
     */
    public function deletePrescription(string $filePath): bool
    {
        $fullPath = $this->prescriptionsDirectory . '/' . $filePath;
        
        if (file_exists($fullPath)) {
            return unlink($fullPath);
        }
        
        return false;
    }

    /**
     * Get the full path to a prescription file
     * 
     * @param string $filePath The relative file path
     * @return string The full file path
     */
    public function getFullPath(string $filePath): string
    {
        return $this->prescriptionsDirectory . '/' . $filePath;
    }

    /**
     * Check if a prescription file exists
     * 
     * @param string $filePath The relative file path
     * @return bool True if file exists, false otherwise
     */
    public function fileExists(string $filePath): bool
    {
        return file_exists($this->getFullPath($filePath));
    }

    /**
     * Get the prescriptions directory path
     * 
     * @return string
     */
    public function getPrescriptionsDirectory(): string
    {
        return $this->prescriptionsDirectory;
    }
}
