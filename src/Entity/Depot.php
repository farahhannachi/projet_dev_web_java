<?php

namespace App\Entity;

use App\Repository\DepotRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: DepotRepository::class)]
#[ORM\Table(name: 'depot')]
class Depot
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_depot')]
    private ?int $id = null;

    #[ORM\Column(name: 'nom_depot', length: 255)]
    private ?string $nomDepot = null;

    #[ORM\Column(name: 'adresse_depot', length: 255)]
    private ?string $adresseDepot = null;

    #[ORM\Column(name: 'ville', length: 100, nullable: true)]
    private ?string $ville = null;

    #[ORM\Column(name: 'capacite_depot')]
    private ?int $capaciteDepot = null;

    #[ORM\Column(name: 'responsable_depot', length: 255)]
    private ?string $responsableDepot = null;

    #[ORM\Column(name: 'responsable_telephone', length: 50, nullable: true)]
    private ?string $responsableTelephone = null;

    #[ORM\Column(name: 'date_creation')]
    private ?\DateTimeImmutable $dateCreation = null;

    #[ORM\Column(type: 'decimal', precision: 10, scale: 7, nullable: true)]
    private ?string $latitude = null;

    #[ORM\Column(type: 'decimal', precision: 10, scale: 7, nullable: true)]
    private ?string $longitude = null;

    #[ORM\OneToMany(targetEntity: Stock::class, mappedBy: 'depot')]
    private Collection $stocks;

    public function __construct()
    {
        $this->stocks = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNomDepot(): ?string
    {
        return $this->nomDepot;
    }

    public function setNomDepot(string $nomDepot): static
    {
        $this->nomDepot = $nomDepot;

        return $this;
    }

    public function getAdresseDepot(): ?string
    {
        return $this->adresseDepot;
    }

    public function setAdresseDepot(string $adresseDepot): static
    {
        $this->adresseDepot = $adresseDepot;

        return $this;
    }

    public function getVille(): ?string
    {
        return $this->ville;
    }

    public function setVille(string $ville): static
    {
        $this->ville = $ville;

        return $this;
    }

    public function getCapaciteDepot(): ?int
    {
        return $this->capaciteDepot;
    }

    public function setCapaciteDepot(int $capaciteDepot): static
    {
        $this->capaciteDepot = $capaciteDepot;

        return $this;
    }

    public function getResponsableDepot(): ?string
    {
        return $this->responsableDepot;
    }

    public function setResponsableDepot(string $responsableDepot): static
    {
        $this->responsableDepot = $responsableDepot;

        return $this;
    }

    public function getResponsableTelephone(): ?string
    {
        return $this->responsableTelephone;
    }

    public function setResponsableTelephone(?string $responsableTelephone): static
    {
        $this->responsableTelephone = $responsableTelephone;
        return $this;
    }

    public function getDateCreation(): ?\DateTimeImmutable
    {
        return $this->dateCreation;
    }

    public function setDateCreation(\DateTimeImmutable $dateCreation): static
    {
        $this->dateCreation = $dateCreation;

        return $this;
    }

    public function getLatitude(): ?float
    {
        return $this->latitude !== null ? (float) $this->latitude : null;
    }

    public function setLatitude(?float $latitude): static
    {
        $this->latitude = $latitude !== null ? (string) $latitude : null;
        return $this;
    }

    public function getLongitude(): ?float
    {
        return $this->longitude !== null ? (float) $this->longitude : null;
    }

    public function setLongitude(?float $longitude): static
    {
        $this->longitude = $longitude !== null ? (string) $longitude : null;
        return $this;
    }

    /**
     * @return Collection<int, Stock>
     */
    public function getStocks(): Collection
    {
        return $this->stocks;
    }

    public function addStock(Stock $stock): static
    {
        if (!$this->stocks->contains($stock)) {
            $this->stocks->add($stock);
            $stock->setDepot($this);
        }

        return $this;
    }

    public function removeStock(Stock $stock): static
    {
        if ($this->stocks->removeElement($stock)) {
            // set the owning side to null (unless already changed)
            if ($stock->getDepot() === $this) {
                $stock->setDepot(null);
            }
        }

        return $this;
    }

    public function __toString(): string
    {
        return $this->nomDepot;
    }
}
