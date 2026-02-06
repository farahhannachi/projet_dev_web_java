<?php

namespace App\Entity;

use App\Repository\TraitementRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: TraitementRepository::class)]
#[ORM\Table(name: 'traitement')]
class Traitement
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_traitement')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Ordonnance::class)]
    #[ORM\JoinColumn(name: 'id_ordonnance', referencedColumnName: 'id_ordonnance', nullable: false)]
    private ?Ordonnance $ordonnance = null;

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur', referencedColumnName: 'id_utilisateur', nullable: false)]
    private ?Utilisateur $utilisateur = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $dosage = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $frequence = null;

    #[ORM\Column(name: 'duree_jours', nullable: true)]
    private ?int $dureeJours = null;

    #[ORM\Column(name: 'date_debut', nullable: true)]
    private ?\DateTime $dateDebut = null;

    #[ORM\Column(name: 'date_fin', nullable: true)]
    private ?\DateTime $dateFin = null;

    #[ORM\Column(length: 50)]
    private ?string $status = 'actif';

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $notes = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getOrdonnance(): ?Ordonnance
    {
        return $this->ordonnance;
    }

    public function setOrdonnance(?Ordonnance $ordonnance): static
    {
        $this->ordonnance = $ordonnance;
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

    public function getDosage(): ?string
    {
        return $this->dosage;
    }

    public function setDosage(?string $dosage): static
    {
        $this->dosage = $dosage;
        return $this;
    }

    public function getFrequence(): ?string
    {
        return $this->frequence;
    }

    public function setFrequence(?string $frequence): static
    {
        $this->frequence = $frequence;
        return $this;
    }

    public function getDureeJours(): ?int
    {
        return $this->dureeJours;
    }

    public function setDureeJours(?int $dureeJours): static
    {
        $this->dureeJours = $dureeJours;
        return $this;
    }

    public function getDateDebut(): ?\DateTime
    {
        return $this->dateDebut;
    }

    public function setDateDebut(?\DateTime $dateDebut): static
    {
        $this->dateDebut = $dateDebut;
        return $this;
    }

    public function getDateFin(): ?\DateTime
    {
        return $this->dateFin;
    }

    public function setDateFin(?\DateTime $dateFin): static
    {
        $this->dateFin = $dateFin;
        return $this;
    }

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;
        return $this;
    }

    public function getNotes(): ?string
    {
        return $this->notes;
    }

    public function setNotes(?string $notes): static
    {
        $this->notes = $notes;
        return $this;
    }
}
