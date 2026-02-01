<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: 'App\Repository\TraitementRepository')]
#[ORM\Table(name: 'traitement')]
class Traitement
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null;

    #[ORM\Column(type: 'string', length: 255)]
    #[Assert\NotBlank]
    private string $dosage;

    #[ORM\Column(type: 'string', length: 255)]
    #[Assert\NotBlank]
    private string $frequency;

    #[ORM\Column(type: 'integer')]
    #[Assert\NotBlank]
    #[Assert\Positive]
    private int $durationDays;

    #[ORM\Column(type: 'datetime')]
    #[Assert\NotNull]
    private \DateTimeInterface $startDate;

    #[ORM\Column(type: 'datetime')]
    #[Assert\NotNull]
    private \DateTimeInterface $endDate;

    #[ORM\Column(type: 'boolean')]
    private bool $isActive = true;

    #[ORM\Column(type: 'boolean')]
    private bool $isCompleted = false;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $notes = null;

    #[ORM\ManyToOne(targetEntity: Ordonnance::class, inversedBy: 'traitements')]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    #[Assert\NotNull(message: 'Ordonnance is required')]
    private ?Ordonnance $ordonnance = null;

    #[ORM\Column(type: 'integer')]
    #[Assert\NotNull]
    private int $clientId;

    // Note: Produit relationship will be fully implemented when Produit entity is created in task 4.1
    // #[ORM\ManyToOne(targetEntity: 'App\Entity\Produit')]
    // #[ORM\JoinColumn(nullable: true, onDelete: 'SET NULL')]
    // private $produit = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getDosage(): string
    {
        return $this->dosage;
    }

    public function setDosage(string $dosage): self
    {
        $this->dosage = $dosage;
        return $this;
    }

    public function getFrequency(): string
    {
        return $this->frequency;
    }

    public function setFrequency(string $frequency): self
    {
        $this->frequency = $frequency;
        return $this;
    }

    public function getDurationDays(): int
    {
        return $this->durationDays;
    }

    public function setDurationDays(int $durationDays): self
    {
        $this->durationDays = $durationDays;
        return $this;
    }

    public function getStartDate(): \DateTimeInterface
    {
        return $this->startDate;
    }

    public function setStartDate(\DateTimeInterface $startDate): self
    {
        $this->startDate = $startDate;
        return $this;
    }

    public function getEndDate(): \DateTimeInterface
    {
        return $this->endDate;
    }

    public function setEndDate(\DateTimeInterface $endDate): self
    {
        $this->endDate = $endDate;
        return $this;
    }

    public function isActive(): bool
    {
        return $this->isActive;
    }

    public function setIsActive(bool $isActive): self
    {
        $this->isActive = $isActive;
        return $this;
    }

    public function isCompleted(): bool
    {
        return $this->isCompleted;
    }

    public function setIsCompleted(bool $isCompleted): self
    {
        $this->isCompleted = $isCompleted;
        return $this;
    }

    public function getNotes(): ?string
    {
        return $this->notes;
    }

    public function setNotes(?string $notes): self
    {
        $this->notes = $notes;
        return $this;
    }

    public function getOrdonnance(): ?Ordonnance
    {
        return $this->ordonnance;
    }

    public function setOrdonnance(?Ordonnance $ordonnance): self
    {
        $this->ordonnance = $ordonnance;
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

    // Note: Produit getter/setter will be fully implemented when Produit entity is created in task 4.1
    // public function getProduit()
    // {
    //     return $this->produit;
    // }

    // public function setProduit($produit): self
    // {
    //     $this->produit = $produit;
    //     return $this;
    // }
}
