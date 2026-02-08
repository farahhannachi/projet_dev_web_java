<?php

namespace App\Entity;

use App\Repository\ResponseQuestionRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: ResponseQuestionRepository::class)]
#[ORM\Table(name: 'response_question')]
class ResponseQuestion
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_reponse')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Question::class, inversedBy: 'reponses')]
    #[ORM\JoinColumn(name: 'id_question', referencedColumnName: 'id_question', nullable: false)]
    #[Assert\NotNull(message: "La question est obligatoire")]
    private ?Question $question = null;

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur', referencedColumnName: 'id_utilisateur', nullable: true)]
    private ?Utilisateur $utilisateur = null;

    #[ORM\Column(name: 'auteur_type', length: 20)]
    #[Assert\NotBlank(message: "Le type d'auteur est obligatoire")]
    #[Assert\Choice(
        choices: ['client', 'agent', 'bot'],
        message: "Le type d'auteur doit être client, agent ou bot"
    )]
    private ?string $auteurType = null;

    #[ORM\Column(name: 'reponse_text', type: 'text')]
    #[Assert\NotBlank(message: "Le texte de la réponse est obligatoire")]
    #[Assert\Length(
        min: 3,
        minMessage: "La réponse doit contenir au moins {{ limit }} caractères"
    )]
    private ?string $reponseText = null;

    #[ORM\Column(name: 'reponse_role', length: 30)]
    #[Assert\NotBlank(message: "Le rôle de la réponse est obligatoire")]
    #[Assert\Choice(
        choices: ['question', 'info', 'demande_preuve', 'solution', 'decision'],
        message: "Le rôle de réponse invalide"
    )]
    private ?string $reponseRole = null;

    #[ORM\Column(name: 'action_type', length: 30)]
    #[Assert\NotBlank(message: "Le type d'action est obligatoire")]
    #[Assert\Choice(
        choices: ['aucune', 'remboursement', 'remplacement', 'retour_accepte', 'retour_refuse', 'escalade'],
        message: "Type d'action invalide"
    )]
    private ?string $actionType = 'aucune';

    #[ORM\Column(name: 'impact_statut', length: 20)]
    #[Assert\NotBlank(message: "L'impact statut est obligatoire")]
    #[Assert\Choice(
        choices: ['aucun', 'en_cours', 'resolu', 'ferme'],
        message: "Impact statut invalide"
    )]
    private ?string $impactStatut = 'aucun';

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

    #[ORM\Column(name: 'lu_par_client', type: 'boolean', options: ['default' => false])]
    private bool $luParClient = false;

    public function __construct()
    {
        $this->createdAt = new \DateTimeImmutable();
        $this->luParClient = false;
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getQuestion(): ?Question
    {
        return $this->question;
    }

    public function setQuestion(?Question $question): static
    {
        $this->question = $question;
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

    public function getAuteurType(): ?string
    {
        return $this->auteurType;
    }

    public function setAuteurType(string $auteurType): static
    {
        $allowedTypes = ['client', 'agent', 'bot'];
        if (!in_array($auteurType, $allowedTypes)) {
            throw new \InvalidArgumentException("Type d'auteur invalide");
        }
        $this->auteurType = $auteurType;
        return $this;
    }

    public function getReponseText(): ?string
    {
        return $this->reponseText;
    }

    public function setReponseText(string $reponseText): static
    {
        $this->reponseText = $reponseText;
        return $this;
    }

    public function getReponseRole(): ?string
    {
        return $this->reponseRole;
    }

    public function setReponseRole(string $reponseRole): static
    {
        $allowedRoles = ['question', 'info', 'demande_preuve', 'solution', 'decision'];
        if (!in_array($reponseRole, $allowedRoles)) {
            throw new \InvalidArgumentException("Rôle de réponse invalide");
        }
        $this->reponseRole = $reponseRole;
        return $this;
    }

    public function getActionType(): ?string
    {
        return $this->actionType;
    }

    public function setActionType(string $actionType): static
    {
        $allowedActions = ['aucune', 'remboursement', 'remplacement', 'retour_accepte', 'retour_refuse', 'escalade'];
        if (!in_array($actionType, $allowedActions)) {
            throw new \InvalidArgumentException("Type d'action invalide");
        }
        $this->actionType = $actionType;
        return $this;
    }

    public function getImpactStatut(): ?string
    {
        return $this->impactStatut;
    }

    public function setImpactStatut(string $impactStatut): static
    {
        $allowedImpacts = ['aucun', 'en_cours', 'resolu', 'ferme'];
        if (!in_array($impactStatut, $allowedImpacts)) {
            throw new \InvalidArgumentException("Impact statut invalide");
        }
        $this->impactStatut = $impactStatut;
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

    public function isLuParClient(): bool
    {
        return $this->luParClient;
    }

    public function setLuParClient(bool $luParClient): static
    {
        $this->luParClient = $luParClient;
        return $this;
    }
}
