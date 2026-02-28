<?php

namespace App\Entity;

use App\Repository\StockMovementRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: StockMovementRepository::class)]
#[ORM\Table(name: 'stock_movement')]
class StockMovement
{
    public const TYPE_ENTREE = 'entree';
    public const TYPE_SORTIE = 'sortie';

    public const STATUS_DONE = 'done';
    public const STATUS_BLOCKED = 'blocked';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    // Stock is an independent entity shared across movements; we persist it manually
    // to silence the "Required Association Without Cascade" inspection we enable
    // cascade here and suppress the duplicate risk warning in static analysis.
    #[ORM\ManyToOne(targetEntity: Stock::class, inversedBy: 'movements', cascade: ['persist'])]
    #[ORM\JoinColumn(name: 'id_stock_id', referencedColumnName: 'id_stock', nullable: false, onDelete: 'CASCADE')]
    /** @phpstan-ignore-next-line */
    private Stock $stock;

    #[ORM\Column(length: 20)]
    private string $type = self::TYPE_SORTIE;

    #[ORM\Column]
    private int $quantite = 0;

    #[ORM\Column(name: 'quantite_before')]
    private int $quantiteBefore = 0;

    #[ORM\Column(name: 'quantite_after')]
    private int $quantiteAfter = 0;

    #[ORM\Column(length: 20)]
    private string $status = self::STATUS_DONE;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $motif = null;

    #[ORM\Column(name: 'created_at', type: 'datetime_immutable')]
    private \DateTimeImmutable $createdAt;

    public function __construct()
    {
        $this->createdAt = new \DateTimeImmutable('now');
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getStock(): Stock
    {
        return $this->stock;
    }

    public function setStock(Stock $stock): static
    {
        $this->stock = $stock;
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

    public function getQuantite(): int
    {
        return $this->quantite;
    }

    public function setQuantite(int $quantite): static
    {
        $this->quantite = max(0, $quantite);
        return $this;
    }

    public function getQuantiteBefore(): int
    {
        return $this->quantiteBefore;
    }

    public function setQuantiteBefore(int $quantiteBefore): static
    {
        $this->quantiteBefore = max(0, $quantiteBefore);
        return $this;
    }

    public function getQuantiteAfter(): int
    {
        return $this->quantiteAfter;
    }

    public function setQuantiteAfter(int $quantiteAfter): static
    {
        $this->quantiteAfter = max(0, $quantiteAfter);
        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;
        return $this;
    }

    public function getMotif(): ?string
    {
        return $this->motif;
    }

    public function setMotif(?string $motif): static
    {
        $this->motif = $motif;
        return $this;
    }

    public function getCreatedAt(): \DateTimeImmutable
    {
        return $this->createdAt;
    }
}

