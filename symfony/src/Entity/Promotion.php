<?php

namespace App\Entity;

use App\Repository\PromotionRepository;
use App\Entity\Produit;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: PromotionRepository::class)]
#[ORM\Table(name: 'promotion')]
class Promotion
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_promotion')]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private ?string $titre = null;

    #[ORM\Column(type: Types::TEXT)]
    private ?string $description = null;

    #[ORM\Column(name: 'valeur_reduction')]
    private ?float $valeurReduction = null;

    #[ORM\Column(name: 'date_debut')]
    private ?\DateTime $dateDebut = null;

    #[ORM\Column(name: 'date_fin')]
    private ?\DateTime $dateFin = null;

    #[ORM\Column(length: 20)]
    private ?string $statut = 'active';

    #[ORM\ManyToOne(targetEntity: Produit::class)]
    #[ORM\JoinColumn(name: 'id_produit', referencedColumnName: 'id_produit', nullable: true)]
    private ?Produit $produit = null;

    #[ORM\Column(name: 'id_admin')]
    private ?int $idAdmin = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTitre(): ?string
    {
        return $this->titre;
    }

    public function setTitre(string $titre): static
    {
        $this->titre = $titre;

        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(string $description): static
    {
        $this->description = $description;

        return $this;
    }

    public function getValeurReduction(): ?float
    {
        return $this->valeurReduction;
    }

    public function setValeurReduction(float $valeurReduction): static
    {
        $this->valeurReduction = $valeurReduction;

        return $this;
    }

    public function getDateDebut(): ?\DateTime
    {
        return $this->dateDebut;
    }

    public function setDateDebut(\DateTime $dateDebut): static
    {
        $this->dateDebut = $dateDebut;

        return $this;
    }

    public function getDateFin(): ?\DateTime
    {
        return $this->dateFin;
    }

    public function setDateFin(\DateTime $dateFin): static
    {
        $this->dateFin = $dateFin;

        return $this;
    }

    public function getStatut(): ?string
    {
        return $this->statut;
    }

    public function setStatut(string $statut): static
    {
        $this->statut = $statut;

        return $this;
    }

    public function getProduit(): ?Produit
    {
        return $this->produit;
    }

    public function setProduit(?Produit $produit): static
    {
        $this->produit = $produit;

        return $this;
    }

    public function getIdAdmin(): ?int
    {
        return $this->idAdmin;
    }

    public function setIdAdmin(int $idAdmin): static
    {
        $this->idAdmin = $idAdmin;

        return $this;
    }

    public function isActive(): bool
    {
        $now = new \DateTime();
        return $this->statut === 'active' && 
               $this->dateDebut <= $now && 
               $this->dateFin >= $now;
    }

    public function calculatePromotionalPrice(float $originalPrice): float
    {
        if (!$this->isActive()) {
            return $originalPrice;
        }

        return $originalPrice * (1 - $this->valeurReduction / 100);
    }
}
