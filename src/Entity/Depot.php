<?php

namespace App\Entity;

use App\Repository\DepotRepository;
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

    #[ORM\Column(name: 'capacite_depot')]
    private ?int $capaciteDepot = null;

    #[ORM\Column(name: 'responsable_depot', length: 255)]
    private ?string $responsableDepot = null;

    #[ORM\Column(name: 'date_creation')]
    private ?\DateTimeImmutable $dateCreation = null;

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

    public function getDateCreation(): ?\DateTimeImmutable
    {
        return $this->dateCreation;
    }

    public function setDateCreation(\DateTimeImmutable $dateCreation): static
    {
        $this->dateCreation = $dateCreation;

        return $this;
    }
}
