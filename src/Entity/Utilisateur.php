<?php

namespace App\Entity;

use App\Repository\UtilisateurRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\Serializer\Annotation\Ignore;

#[ORM\Entity(repositoryClass: UtilisateurRepository::class)]
#[ORM\Table(name: 'utilisateur')]
class Utilisateur implements UserInterface, PasswordAuthenticatedUserInterface
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_utilisateur')]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private string $nom = '';

    #[ORM\Column(length: 255)]
    private string $prenom = '';

    #[ORM\Column(length: 255, unique: true)]
    private string $email = '';

    #[ORM\Column(name: 'mot_de_passe', length: 255)]
    private string $motDePasse = '';

    #[ORM\Column(name: 'etat_compte', length: 20)]
    private string $etatCompte = 'actif';

    #[ORM\Column(name: 'date_creation')]
    private \DateTimeImmutable $dateCreation;

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

    #[ORM\Column(name: 'date_naissance', type: 'date', nullable: true)]
    private ?\DateTimeInterface $dateNaissance = null;

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $telephone = null;

    #[ORM\Column(name: 'avatar_url', type: 'text', nullable: true)]
    private ?string $avatarUrl = null;

    #[ORM\Column(name: 'avatar_seed', length: 100, nullable: true)]
    private ?string $avatarSeed = null;

    #[ORM\Column(name: 'has_seen_introduction', type: 'boolean', options: ['default' => false])]
    private bool $hasSeenIntroduction = false;

    #[ORM\Column(name: 'reset_token', length: 255, nullable: true)]
    #[Ignore]
    private ?string $resetToken = null;

    #[ORM\Column(name: 'reset_token_expires_at', type: 'datetime_immutable', nullable: true)]
    #[Ignore]
    private ?\DateTimeImmutable $resetTokenExpiresAt = null;

    // TOTP 2FA Fields
    #[ORM\Column(name: 'totp_secret', type: 'string', length: 255, nullable: true)]
    #[Ignore]
    private ?string $totpSecret = null;

    #[ORM\Column(name: 'totp_enabled', type: 'boolean', options: ['default' => false])]
    private bool $totpEnabled = false;

    #[ORM\Column(name: 'backup_codes', type: 'json', nullable: true)]
    private ?array $backupCodes = null;

    #[ORM\Column(name: 'student_id', length: 100, unique: true, nullable: true)]
    private ?string $studentId = null;

    #[ORM\Column(name: 'id_card_image', length: 255, nullable: true)]
    private ?string $idCardImage = null;

    public function __construct()
    {
        $this->dateCreation = new \DateTimeImmutable();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNom(): string
    {
        return $this->nom;
    }

    public function setNom(string $nom): static
    {
        $this->nom = $nom;

        return $this;
    }

    public function getPrenom(): string
    {
        return $this->prenom;
    }

    public function setPrenom(string $prenom): static
    {
        $this->prenom = $prenom;

        return $this;
    }

    public function getEmail(): string
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

    public function getEtatCompte(): string
    {
        return $this->etatCompte;
    }

    public function setEtatCompte(string $etatCompte): static
    {
        $this->etatCompte = $etatCompte;

        return $this;
    }

    public function getDateCreation(): \DateTimeImmutable
    {
        return $this->dateCreation;
    }

    protected function setDateCreation(\DateTimeImmutable $dateCreation): static
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

    protected function setLastActivityAt(?\DateTimeImmutable $lastActivityAt): static
    {
        $this->lastActivityAt = $lastActivityAt;
        return $this;
    }

    public function getDateNaissance(): ?\DateTimeInterface
    {
        return $this->dateNaissance;
    }

    public function setDateNaissance(?\DateTimeInterface $dateNaissance): static
    {
        $this->dateNaissance = $dateNaissance;
        return $this;
    }

    public function getTelephone(): ?string
    {
        return $this->telephone;
    }

    public function setTelephone(?string $telephone): static
    {
        $this->telephone = $telephone;
        return $this;
    }

    public function getAvatarUrl(): ?string
    {
        return $this->avatarUrl;
    }

    public function setAvatarUrl(?string $avatarUrl): static
    {
        $this->avatarUrl = $avatarUrl;
        return $this;
    }

    public function getAvatarSeed(): ?string
    {
        return $this->avatarSeed;
    }

    public function setAvatarSeed(?string $avatarSeed): static
    {
        $this->avatarSeed = $avatarSeed;
        return $this;
    }

    public function hasSeenIntroduction(): bool
    {
        return $this->hasSeenIntroduction;
    }

    public function setHasSeenIntroduction(bool $hasSeenIntroduction): static
    {
        $this->hasSeenIntroduction = $hasSeenIntroduction;
        return $this;
    }

    // Reset Token getters and setters
    public function getResetToken(): ?string
    {
        return $this->resetToken;
    }

    public function setResetToken(#[\SensitiveParameter] ?string $resetToken): static
    {
        $this->resetToken = $resetToken;
        return $this;
    }

    public function getResetTokenExpiresAt(): ?\DateTimeImmutable
    {
        return $this->resetTokenExpiresAt;
    }

    protected function setResetTokenExpiresAt(#[\SensitiveParameter] ?\DateTimeImmutable $resetTokenExpiresAt): static
    {
        $this->resetTokenExpiresAt = $resetTokenExpiresAt;
        return $this;
    }

    public function isResetTokenValid(): bool
    {
        return $this->resetToken !== null && $this->resetTokenExpiresAt !== null && $this->resetTokenExpiresAt > new \DateTimeImmutable();
    }

    // ===== TOTP Two-Factor Interface Methods =====

    /**
     * Return the TOTP secret for the user.
     */
    public function getTotpSecret(): ?string
    {
        return $this->totpSecret;
    }

    /**
     * Set the TOTP secret for the user.
     */
    public function setTotpSecret(#[\SensitiveParameter] ?string $totpSecret): self
    {
        $this->totpSecret = $totpSecret;
        return $this;
    }

    /**
     * Check if TOTP 2FA is enabled for this user.
     */
    public function isTotpEnabled(): bool
    {
        return $this->totpEnabled;
    }

    /**
     * Enable TOTP 2FA for this user.
     */
    public function setTotpEnabled(bool $totpEnabled): self
    {
        $this->totpEnabled = $totpEnabled;
        return $this;
    }

    /**
     * Return the backup codes for the user.
     */
    public function getBackupCodes(): ?array
    {
        return $this->backupCodes;
    }

    /**
     * Set the backup codes for the user.
     */
    public function setBackupCodes(?array $backupCodes): self
    {
        $this->backupCodes = $backupCodes;
        return $this;
    }

    /**
     * This method is required by the interface.
     */
    public function getTotpIssuer(): string
    {
        return 'CURAVITA';
    }

    /**
     * Get the student ID for ID card login.
     */
    public function getStudentId(): ?string
    {
        return $this->studentId;
    }

    /**
     * Set the student ID for ID card login.
     */
    public function setStudentId(?string $studentId): self
    {
        $this->studentId = $studentId;
        return $this;
    }

    /**
     * Get the ID card image path.
     */
    public function getIdCardImage(): ?string
    {
        return $this->idCardImage;
    }

    /**
     * Set the ID card image path.
     */
    public function setIdCardImage(?string $idCardImage): self
    {
        $this->idCardImage = $idCardImage;
        return $this;
    }
}
