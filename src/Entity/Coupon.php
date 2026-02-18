<?php

namespace App\Entity;

use App\Repository\CouponRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: CouponRepository::class)]
#[ORM\Table(name: 'coupon')]
#[ORM\UniqueConstraint(name: 'uniq_coupon_code', columns: ['code'])]
class Coupon
{
    public const TYPE_PERCENTAGE = 'percentage';
    public const TYPE_FIXED = 'fixed';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 64, unique: true)]
    #[Assert\NotBlank]
    private string $code = '';

    #[ORM\Column(length: 20)]
    #[Assert\Choice(choices: [self::TYPE_PERCENTAGE, self::TYPE_FIXED])]
    private string $type = self::TYPE_PERCENTAGE;

    #[ORM\Column(type: 'decimal', precision: 10, scale: 2)]
    #[Assert\Positive]
    private string $valeur = '0.00';

    #[ORM\Column(name: 'date_expiration', nullable: true)]
    private ?\DateTimeImmutable $dateExpiration = null;

    #[ORM\Column(name: 'usage_max', options: ['default' => 1])]
    private int $usageMax = 1;

    #[ORM\Column(name: 'usage_count', options: ['default' => 0])]
    private int $usageCount = 0;

    #[ORM\Column(options: ['default' => true])]
    private bool $actif = true;

    #[ORM\Column(name: 'montant_minimum_panier', type: 'decimal', precision: 10, scale: 2, options: ['default' => 0])]
    private string $montantMinimumPanier = '0.00';

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getCode(): string
    {
        return $this->code;
    }

    public function setCode(string $code): static
    {
        $this->code = strtoupper(trim($code));
        return $this;
    }

    public function getType(): string
    {
        return $this->type;
    }

    public function setType(string $type): static
    {
        $this->type = $type;
        return $this;
    }

    public function getValeur(): float
    {
        return $this->valeur;
    }

    public function setValeur(float $valeur): static
    {
        $this->valeur = $valeur;
        return $this;
    }

    public function getDateExpiration(): ?\DateTimeImmutable
    {
        return $this->dateExpiration;
    }

    public function setDateExpiration(?\DateTimeImmutable $dateExpiration): static
    {
        $this->dateExpiration = $dateExpiration;
        return $this;
    }

    public function getUsageMax(): int
    {
        return $this->usageMax;
    }

    public function setUsageMax(int $usageMax): static
    {
        $this->usageMax = max(1, $usageMax);
        return $this;
    }

    public function getUsageCount(): int
    {
        return $this->usageCount;
    }

    public function setUsageCount(int $usageCount): static
    {
        $this->usageCount = max(0, $usageCount);
        return $this;
    }

    public function incrementUsage(): static
    {
        $this->usageCount++;
        return $this;
    }

    public function isActif(): bool
    {
        return $this->actif;
    }

    public function setActif(bool $actif): static
    {
        $this->actif = $actif;
        return $this;
    }

    public function getMontantMinimumPanier(): float
    {
        return $this->montantMinimumPanier;
    }

    public function setMontantMinimumPanier(float $montantMinimumPanier): static
    {
        $this->montantMinimumPanier = max(0.0, $montantMinimumPanier);
        return $this;
    }
}

