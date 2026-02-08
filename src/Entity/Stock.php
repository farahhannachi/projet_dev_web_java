<?php

namespace App\Entity;

use App\Repository\StockRepository;
use App\Entity\Produit;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: StockRepository::class)]
#[ORM\Table(name: 'stock')]
class Stock
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_stock')]
    private ?int $id = null;

    #[ORM\Column(name: 'seuil_alerte')]
    private ?int $seuilAlerte = null;

    #[ORM\Column(name: 'date_expiration')]
    private ?\DateTime $dateExpiration = null;

    #[ORM\Column(name: 'etat_stock', length: 50)]
    private ?string $etatStock = 'disponible';

    #[ORM\Column(name: 'date_derniere_mise_ajour')]
    private ?\DateTime $dateDerniereMiseAJour = null;

    #[ORM\ManyToOne(targetEntity: Produit::class, inversedBy: 'stocks')]
    #[ORM\JoinColumn(name: 'produit_id', referencedColumnName: 'id_produit')]
    private ?Produit $produit = null;

    #[ORM\Column(name: 'quantite')]
    private ?int $quantite = 0;

    public function __construct()
    {
        $this->dateDerniereMiseAJour = new \DateTime();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getSeuilAlerte(): ?int
    {
        return $this->seuilAlerte;
    }

    public function setSeuilAlerte(int $seuilAlerte): static
    {
        $this->seuilAlerte = $seuilAlerte;

        return $this;
    }

    public function getDateExpiration(): ?\DateTime
    {
        return $this->dateExpiration;
    }

    public function setDateExpiration(\DateTime $dateExpiration): static
    {
        $this->dateExpiration = $dateExpiration;

        return $this;
    }

    public function getEtatStock(): ?string
    {
        return $this->etatStock;
    }

    public function setEtatStock(string $etatStock): static
    {
        $this->etatStock = $etatStock;

        return $this;
    }

    public function getDateDerniereMiseAJour(): ?\DateTime
    {
        return $this->dateDerniereMiseAJour;
    }

    public function setDateDerniereMiseAJour(\DateTime $dateDerniereMiseAJour): static
    {
        $this->dateDerniereMiseAJour = $dateDerniereMiseAJour;

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

    public function getQuantite(): ?int
    {
        return $this->quantite;
    }

    public function setQuantite(int $quantite): static
    {
        $this->quantite = $quantite;
        
        // Mettre à jour automatiquement le statut du produit
        if ($this->produit) {
            $this->produit->updateStatutFromStock($this->quantite);
        }

        return $this;
    }
}
