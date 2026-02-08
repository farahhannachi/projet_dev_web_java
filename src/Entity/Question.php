<?php

namespace App\Entity;

use App\Repository\QuestionRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: QuestionRepository::class)]
#[ORM\Table(name: 'question')]
class Question
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_question')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur', referencedColumnName: 'id_utilisateur', nullable: false)]
    #[Assert\NotNull(message: "L'utilisateur est obligatoire")]
    private ?Utilisateur $utilisateur = null;

    #[ORM\Column(name: 'type_ticket', length: 20)]
    #[Assert\NotBlank(message: "Le type de ticket est obligatoire")]
    #[Assert\Choice(
        choices: ['support', 'reclamation', 'retour'],
        message: "Le type de ticket doit être support, réclamation ou retour"
    )]
    private ?string $typeTicket = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: "L'objet est obligatoire")]
    #[Assert\Length(
        min: 3,
        max: 255,
        minMessage: "L'objet doit contenir au moins {{ limit }} caractères",
        maxMessage: "L'objet ne peut pas dépasser {{ limit }} caractères"
    )]
    private ?string $objet = null;

    #[ORM\Column(type: 'text')]
    #[Assert\NotBlank(message: "La description est obligatoire")]
    #[Assert\Length(
        min: 5,
        minMessage: "La description doit contenir au moins {{ limit }} caractères"
    )]
    private ?string $description = null;

    #[ORM\Column(length: 20)]
    #[Assert\NotBlank(message: "La priorité est obligatoire")]
    #[Assert\Choice(
        choices: ['basse', 'normale', 'haute'],
        message: "La priorité doit être basse, normale ou haute"
    )]
    private ?string $priorite = 'normale';

    #[ORM\Column(length: 20)]
    #[Assert\NotBlank(message: "Le statut est obligatoire")]
    #[Assert\Choice(
        choices: ['ouvert', 'en_cours', 'resolu', 'ferme'],
        message: "Le statut doit être ouvert, en_cours, resolu ou ferme"
    )]
    private ?string $statut = 'ouvert';

    #[ORM\Column(name: 'file_name', length: 255, nullable: true)]
    private ?string $fileName = null;

    #[ORM\Column(name: 'file_path', length: 255, nullable: true)]
    private ?string $filePath = null;

    #[ORM\Column(name: 'file_type', length: 100, nullable: true)]
    private ?string $fileType = null;

    #[ORM\Column(name: 'file_size', nullable: true)]
    private ?int $fileSize = null;

    #[ORM\Column(name: 'created_at')]
    private ?\DateTimeImmutable $createdAt = null;

    #[ORM\OneToMany(targetEntity: ResponseQuestion::class, mappedBy: 'question', cascade: ['persist', 'remove'])]
    private Collection $reponses;

    public function __construct()
    {
        $this->reponses = new ArrayCollection();
        $this->createdAt = new \DateTimeImmutable();
    }

    public function getId(): ?int
    {
        return $this->id;
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

    public function getTypeTicket(): ?string
    {
        return $this->typeTicket;
    }

    public function setTypeTicket(string $typeTicket): static
    {
        $allowedTypes = ['support', 'reclamation', 'retour'];
        if (!in_array($typeTicket, $allowedTypes)) {
            throw new \InvalidArgumentException("Type de ticket invalide");
        }
        $this->typeTicket = $typeTicket;
        return $this;
    }

    public function getObjet(): ?string
    {
        return $this->objet;
    }

    public function setObjet(string $objet): static
    {
        $this->objet = $objet;
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

    public function getPriorite(): ?string
    {
        return $this->priorite;
    }

    public function setPriorite(string $priorite): static
    {
        $allowedPriorites = ['basse', 'normale', 'haute'];
        if (!in_array($priorite, $allowedPriorites)) {
            throw new \InvalidArgumentException("Priorité invalide");
        }
        $this->priorite = $priorite;
        return $this;
    }

    public function getStatut(): ?string
    {
        return $this->statut;
    }

    public function setStatut(string $statut): static
    {
        $allowedStatuts = ['ouvert', 'en_cours', 'resolu', 'ferme'];
        if (!in_array($statut, $allowedStatuts)) {
            throw new \InvalidArgumentException("Statut invalide");
        }
        $this->statut = $statut;
        return $this;
    }

    public function getFileName(): ?string
    {
        return $this->fileName;
    }

    public function setFileName(?string $fileName): static
    {
        $this->fileName = $fileName;
        return $this;
    }

    public function getFilePath(): ?string
    {
        return $this->filePath;
    }

    public function setFilePath(?string $filePath): static
    {
        $this->filePath = $filePath;
        return $this;
    }

    public function getFileType(): ?string
    {
        return $this->fileType;
    }

    public function setFileType(?string $fileType): static
    {
        $this->fileType = $fileType;
        return $this;
    }

    public function getFileSize(): ?int
    {
        return $this->fileSize;
    }

    public function setFileSize(?int $fileSize): static
    {
        $this->fileSize = $fileSize;
        return $this;
    }

    public function getCreatedAt(): ?\DateTimeImmutable
    {
        return $this->createdAt;
    }

    public function setCreatedAt(\DateTimeImmutable $createdAt): static
    {
        $this->createdAt = $createdAt;
        return $this;
    }

    /**
     * @return Collection<int, ResponseQuestion>
     */
    public function getReponses(): Collection
    {
        return $this->reponses;
    }

    public function addReponse(ResponseQuestion $reponse): static
    {
        if (!$this->reponses->contains($reponse)) {
            $this->reponses->add($reponse);
            $reponse->setQuestion($this);
        }
        return $this;
    }

    public function removeReponse(ResponseQuestion $reponse): static
    {
        if ($this->reponses->removeElement($reponse)) {
            if ($reponse->getQuestion() === $this) {
                $reponse->setQuestion(null);
            }
        }
        return $this;
    }
}
