<?php

namespace App\Entity;

use App\Repository\UtilisateurRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;

#[ORM\Entity(repositoryClass: UtilisateurRepository::class)]
#[ORM\Table(name: 'utilisateur')]
class Utilisateur implements UserInterface, PasswordAuthenticatedUserInterface
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_utilisateur')]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private ?string $nom = null;

    #[ORM\Column(length: 255)]
    private ?string $prenom = null;

    #[ORM\Column(length: 255, unique: true)]
    private ?string $email = null;

    #[ORM\Column(name: 'mot_de_passe', length: 255)]
    private ?string $motDePasse = null;

    #[ORM\Column(name: 'etat_compte', length: 20)]
    private ?string $etatCompte = 'actif';

    #[ORM\Column(name: 'date_creation')]
    private ?\DateTimeImmutable $dateCreation = null;

    #[ORM\Column(type: 'json')]
    private array $roles = [];

    #[ORM\Column(name: 'loyalty_points', options: ['default' => 0])]
    private int $loyaltyPoints = 0;

    #[ORM\Column(name: 'loyalty_level', length: 20, options: ['default' => 'BRONZE'])]
    private string $loyaltyLevel = 'BRONZE';

    #[ORM\Column(length: 30, options: ['default' => 'NEW_CUSTOMER'])]
    private string $segment = 'NEW_CUSTOMER';

    #[ORM\Column(name: 'last_activity_at', nullable: true)]
    private ?\DateTimeImmutable $lastActivityAt = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNom(): ?string
    {
        return $this->nom;
    }

    public function setNom(string $nom): static
    {
        $this->nom = $nom;

        return $this;
    }

    public function getPrenom(): ?string
    {
        return $this->prenom;
    }

    public function setPrenom(string $prenom): static
    {
        $this->prenom = $prenom;

        return $this;
    }

    public function getEmail(): ?string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = $email;

        return $this;
    }

    public function getMotDePasse(): ?string
    {
        return $this->motDePasse;
    }

    public function setMotDePasse(string $motDePasse): static
    {
        $this->motDePasse = $motDePasse;

        return $this;
    }

    public function getEtatCompte(): ?string
    {
        return $this->etatCompte;
    }

    public function setEtatCompte(string $etatCompte): static
    {
        $this->etatCompte = $etatCompte;

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

    public function getRoles(): array
    {
        $roles = $this->roles;
        
        // guarantee every user at least has ROLE_USER
        if (!in_array('ROLE_USER', $roles)) {
            $roles[] = 'ROLE_USER';
        }

        return array_unique($roles);
    }

    public function setRoles(array $roles): static
    {
        $this->roles = $roles;

        return $this;
    }

    public function eraseCredentials(): void
    {
        // If you store any temporary, sensitive data on the user, clear it here
        // $this->plainPassword = null;
    }

    public function getUserIdentifier(): string
    {
        return (string) $this->email;
    }

    public function getPassword(): string
    {
        return $this->motDePasse;
    }

    public function getLoyaltyPoints(): int
    {
        return $this->loyaltyPoints;
    }

    public function setLoyaltyPoints(int $loyaltyPoints): static
    {
        $this->loyaltyPoints = max(0, $loyaltyPoints);
        return $this;
    }

    public function getLoyaltyLevel(): string
    {
        return $this->loyaltyLevel;
    }

    public function setLoyaltyLevel(string $loyaltyLevel): static
    {
        $this->loyaltyLevel = $loyaltyLevel;
        return $this;
    }

    public function getSegment(): string
    {
        return $this->segment;
    }

    public function setSegment(string $segment): static
    {
        $this->segment = $segment;
        return $this;
    }

    public function getLastActivityAt(): ?\DateTimeImmutable
    {
        return $this->lastActivityAt;
    }

    public function setLastActivityAt(?\DateTimeImmutable $lastActivityAt): static
    {
        $this->lastActivityAt = $lastActivityAt;
        return $this;
    }
}
