<?php

namespace App\Entity;

use App\Repository\ProduitRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ProduitRepository::class)]
#[ORM\Table(name: 'produit')]
class Produit
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_produit')]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private string $nom;

    #[ORM\Column(type: 'text')]
    private string $description;

    #[ORM\Column(type: 'decimal', precision: 10, scale: 2)]
    private string $prix;

    #[ORM\Column(name: 'quantite_stock')]
    private int $quantiteStock;

    #[ORM\Column(name: 'date_expiration', type: 'datetime_immutable')]
    private \DateTimeImmutable $dateExpiration;

    #[ORM\Column(length: 100)]
    private string $categorie;

    #[ORM\Column(nullable: true)]
    private ?string $image = null;

    #[ORM\Column(length: 20, options: ['default' => 'disponible'])]
    private string $statut = 'disponible';

    #[ORM\OneToMany(targetEntity: Promotion::class, mappedBy: 'produit')]
    private Collection $promotions;

    #[ORM\OneToMany(targetEntity: Stock::class, mappedBy: 'produit')]
    private Collection $stocks;

    public function __construct()
    {
        $this->promotions = new ArrayCollection();
        $this->stocks = new ArrayCollection();
        $this->statut = 'disponible';
        $this->quantiteStock = 0;
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNom(): string
    {
        return $this->nom;
    }

    public function setNom(string $nom): static
    {
        $this->nom = $nom;
        return $this;
    }

    public function getDescription(): string
    {
        return $this->description;
    }

    public function setDescription(string $description): static
    {
        $this->description = $description;
        return $this;
    }

    public function getPrix(): float
    {
        return (float) $this->prix;
    }

    public function setPrix(float $prix): static
    {
        $this->prix = (string) $prix;
        return $this;
    }

    public function getQuantiteStock(): int
    {
        return $this->quantiteStock;
    }

    public function setQuantiteStock(int $quantiteStock): static
    {
        $this->quantiteStock = $quantiteStock;
        return $this;
    }

    public function getDateExpiration(): \DateTimeImmutable
    {
        return $this->dateExpiration;
    }

    protected function setDateExpiration(\DateTimeImmutable $dateExpiration): static
    {
        $this->dateExpiration = $dateExpiration;
        return $this;
    }

    public function getCategorie(): string
    {
        return $this->categorie;
    }

    public function setCategorie(string $categorie): static
    {
        $this->categorie = $categorie;
        return $this;
    }

    public function getImage(): ?string
    {
        return $this->image;
    }

    public function setImage(?string $image): static
    {
        $this->image = $image;
        return $this;
    }

    public function getStatut(): string
    {
        return $this->statut;
    }

    public function setStatut(string $statut): static
    {
        $this->statut = $statut;
        return $this;
    }

    /**
     * @return Collection<int, Promotion>
     */
    public function getPromotions(): Collection
    {
        return $this->promotions;
    }

    public function addPromotion(Promotion $promotion): static
    {
        if (!$this->promotions->contains($promotion)) {
            $this->promotions->add($promotion);
            $promotion->setProduit($this);
        }

        return $this;
    }

    public function removePromotion(Promotion $promotion): static
    {
        if ($this->promotions->removeElement($promotion)) {
            // set the owning side to null (unless already changed)
            if ($promotion->getProduit() === $this) {
                $promotion->setProduit(null);
            }
        }

        return $this;
    }

    public function getActivePromotion(): ?Promotion
    {
        foreach ($this->promotions as $promotion) {
            if ($promotion->isActive()) {
                return $promotion;
            }
        }
        return null;
    }

    public function getPromotionalPrice(): float
    {
        $activePromotion = $this->getActivePromotion();
        if ($activePromotion) {
            return $activePromotion->calculatePromotionalPrice($this->prix);
        }
        return $this->prix;
    }

    /**
     * @return Collection|Stock[]
     */
    public function getStocks(): Collection
    {
        return $this->stocks;
    }

    public function addStock(Stock $stock): static
    {
        if (!$this->stocks->contains($stock)) {
            $this->stocks->add($stock);
            $stock->setProduit($this);
        }

        return $this;
    }

    public function removeStock(Stock $stock): static
    {
        if ($this->stocks->removeElement($stock)) {
            // set the owning side to null (unless already changed)
            if ($stock->getProduit() === $this) {
                $stock->setProduit(null);
            }
        }

        return $this;
    }

    /**
     * Met à jour le statut du produit en fonction du stock total
     */
    public function updateStatutFromStock(int $stockQuantite): void
    {
        if ($stockQuantite <= 0) {
            $this->statut = 'rupture';
        } elseif ($stockQuantite <= 5) {
            $this->statut = 'stock_critique';
        } else {
            $this->statut = 'disponible';
        }
    }

    /**
     * Calcule le stock total disponible pour ce produit
     */
    public function getStockTotal(): int
    {
        $total = 0;
        foreach ($this->stocks as $stock) {
            $total += $stock->getQuantite();
        }
        return $total;
    }

    /**
     * Vérifie si le produit est disponible en stock
     */
    public function estDisponible(): bool
    {
        return $this->getStockTotal() > 0 && $this->statut === 'disponible';
    }
}
