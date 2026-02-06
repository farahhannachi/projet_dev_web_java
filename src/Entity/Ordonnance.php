<?php

namespace App\Entity;

use App\Repository\OrdonnanceRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: OrdonnanceRepository::class)]
#[ORM\Table(name: 'ordonnance')]
class Ordonnance
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_ordonnance')]
    private ?int $id = null;

    #[ORM\Column(name: 'numero_ordonnance', length: 100)]
    private ?string $numeroOrdonnance = null;

    #[ORM\Column(name: 'date_ordonnance')]
    private ?\DateTime $dateOrdonnance = null;

    #[ORM\Column(name: 'date_expiration')]
    private ?\DateTime $dateExpiration = null;

    #[ORM\Column(length: 50)]
    private ?string $statut = 'en attente';

    #[ORM\Column(name: 'note_medical', type: 'text', nullable: true)]
    private ?string $noteMedical = null;

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur', referencedColumnName: 'id_utilisateur', nullable: false)]
    private ?Utilisateur $utilisateur = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNumeroOrdonnance(): ?string
    {
        return $this->numeroOrdonnance;
    }

    public function setNumeroOrdonnance(string $numeroOrdonnance): static
    {
        $this->numeroOrdonnance = $numeroOrdonnance;
        return $this;
    }

    public function getDateOrdonnance(): ?\DateTime
    {
        return $this->dateOrdonnance;
    }

    public function setDateOrdonnance(?\DateTime $dateOrdonnance): static
    {
        $this->dateOrdonnance = $dateOrdonnance;
        return $this;
    }

    public function getDateExpiration(): ?\DateTime
    {
        return $this->dateExpiration;
    }

    public function setDateExpiration(?\DateTime $dateExpiration): static
    {
        $this->dateExpiration = $dateExpiration;
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

    public function getNoteMedical(): ?string
    {
        return $this->noteMedical;
    }

    public function setNoteMedical(?string $noteMedical): static
    {
        $this->noteMedical = $noteMedical;
        return $this;
    }

    public function getUtilisateur(): ?Utilisateur
    {
        return $this->utilisateur;
    }

    public function setUtilisateur(?Utilisateur $utilisateur): static
    {
        $this->utilisateur = $utilisateur;
        return $this;
    }
}
