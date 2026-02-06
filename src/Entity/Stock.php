<?php

namespace App\Entity;

use App\Repository\StockRepository;
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
}
