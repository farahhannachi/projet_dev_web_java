<?php

namespace App\Entity;

use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: 'App\Repository\OrdonnanceRepository')]
#[ORM\Table(name: 'ordonnance')]
class Ordonnance
{
    public const STATUS_PENDING_VALIDATION = 'pending_validation';
    public const STATUS_VALIDATED = 'validated';
    public const STATUS_REJECTED = 'rejected';

    public const ALLOWED_FILE_TYPES = ['pdf', 'jpg', 'jpeg', 'png'];
    public const ALLOWED_MIME_TYPES = [
        'application/pdf',
        'image/jpeg',
        'image/jpg',
        'image/png'
    ];

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null;

    #[ORM\Column(type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'File name is required')]
    private string $fileName;

    #[ORM\Column(type: 'string', length: 500)]
    #[Assert\NotBlank(message: 'File path is required')]
    private string $filePath;

    #[ORM\Column(type: 'string', length: 50)]
    #[Assert\NotBlank]
    #[Assert\Choice(
        choices: [
            self::STATUS_PENDING_VALIDATION,
            self::STATUS_VALIDATED,
            self::STATUS_REJECTED
        ],
        message: 'Invalid status. Must be one of: {{ choices }}'
    )]
    private string $status = self::STATUS_PENDING_VALIDATION;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $rejectionReason = null;

    #[ORM\Column(type: 'datetime')]
    private \DateTimeInterface $uploadedAt;

    #[ORM\Column(type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $validatedAt = null;

    #[ORM\Column(type: 'integer')]
    #[Assert\NotNull(message: 'Client ID is required')]
    private int $clientId;

    #[ORM\Column(type: 'integer', nullable: true)]
    private ?int $validatedById = null;

    #[ORM\OneToMany(targetEntity: Traitement::class, mappedBy: 'ordonnance', cascade: ['persist', 'remove'])]
    private Collection $traitements;

    public function __construct()
    {
        $this->uploadedAt = new \DateTime();
        $this->traitements = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getFileName(): string
    {
        return $this->fileName;
    }

    public function setFileName(string $fileName): self
    {
        $this->fileName = $fileName;
        return $this;
    }

    public function getFilePath(): string
    {
        return $this->filePath;
    }

    public function setFilePath(string $filePath): self
    {
        $this->filePath = $filePath;
        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): self
    {
        $this->status = $status;
        return $this;
    }

    public function getRejectionReason(): ?string
    {
        return $this->rejectionReason;
    }

    public function setRejectionReason(?string $rejectionReason): self
    {
        $this->rejectionReason = $rejectionReason;
        return $this;
    }

    public function getUploadedAt(): \DateTimeInterface
    {
        return $this->uploadedAt;
    }

    public function setUploadedAt(\DateTimeInterface $uploadedAt): self
    {
        $this->uploadedAt = $uploadedAt;
        return $this;
    }

    public function getValidatedAt(): ?\DateTimeInterface
    {
        return $this->validatedAt;
    }

    public function setValidatedAt(?\DateTimeInterface $validatedAt): self
    {
        $this->validatedAt = $validatedAt;
        return $this;
    }

    public function getClientId(): int
    {
        return $this->clientId;
    }

    public function setClientId(int $clientId): self
    {
        $this->clientId = $clientId;
        return $this;
    }

    public function getValidatedById(): ?int
    {
        return $this->validatedById;
    }

    public function setValidatedById(?int $validatedById): self
    {
        $this->validatedById = $validatedById;
        return $this;
    }

    public function isPendingValidation(): bool
    {
        return $this->status === self::STATUS_PENDING_VALIDATION;
    }

    public function isValidated(): bool
    {
        return $this->status === self::STATUS_VALIDATED;
    }

    public function isRejected(): bool
    {
        return $this->status === self::STATUS_REJECTED;
    }

    /**
     * Get the full file path for storage
     */
    public function getFullFilePath(string $uploadDirectory): string
    {
        return $uploadDirectory . '/' . $this->filePath;
    }

    /**
     * Validate file extension
     */
    public static function isValidFileExtension(string $extension): bool
    {
        return in_array(strtolower($extension), self::ALLOWED_FILE_TYPES, true);
    }

    /**
     * Validate MIME type
     */
    public static function isValidMimeType(string $mimeType): bool
    {
        return in_array($mimeType, self::ALLOWED_MIME_TYPES, true);
    }

    /**
     * Get allowed file extensions as string
     */
    public static function getAllowedExtensionsString(): string
    {
        return implode(', ', array_map('strtoupper', self::ALLOWED_FILE_TYPES));
    }

    /**
     * @return Collection<int, Traitement>
     */
    public function getTraitements(): Collection
    {
        return $this->traitements;
    }

    public function addTraitement(Traitement $traitement): self
    {
        if (!$this->traitements->contains($traitement)) {
            $this->traitements->add($traitement);
            $traitement->setOrdonnance($this);
        }

        return $this;
    }

    public function removeTraitement(Traitement $traitement): self
    {
        if ($this->traitements->removeElement($traitement)) {
            // set the owning side to null (unless already changed)
            if ($traitement->getOrdonnance() === $this) {
                $traitement->setOrdonnance(null);
            }
        }

        return $this;
    }
}
