<?php

namespace App\Entity;

use App\Repository\OrdonnanceRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: OrdonnanceRepository::class)]
#[ORM\Table(name: 'ordonnance')]
class Ordonnance
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_ordonnance')]
    private ?int $id = null;

    #[ORM\Column(name: 'numero_ordonnance', length: 100)]
    #[Assert\NotBlank(message: 'Le numéro d\'ordonnance est obligatoire')]
    #[Assert\Length(
        max: 100,
        maxMessage: 'Le numéro ne peut pas dépasser {{ limit }} caractères'
    )]
    private ?string $numeroOrdonnance = null;

    #[ORM\Column(name: 'date_ordonnance')]
    #[Assert\NotBlank(message: 'La date de l\'ordonnance est obligatoire')]
    private ?\DateTime $dateOrdonnance = null;

    #[ORM\Column(name: 'date_expiration')]
    #[Assert\NotBlank(message: 'La date d\'expiration est obligatoire')]
    private ?\DateTime $dateExpiration = null;

    #[ORM\Column(length: 50)]
    #[Assert\NotBlank(message: 'Le statut est obligatoire')]
    #[Assert\Choice(
        choices: ['en_attente', 'valide', 'rejete', 'en attente', 'validé', 'rejeté'],
        message: 'Statut invalide'
    )]
    private ?string $statut = 'en_attente';

    #[ORM\Column(name: 'note_medical', type: 'text', nullable: true)]
    #[Assert\Length(
        max: 5000,
        maxMessage: 'La note ne peut pas dépasser {{ limit }} caractères'
    )]
    private ?string $noteMedical = null;

    #[ORM\Column(name: 'signature_electronique', type: 'boolean', options: ['default' => false])]
    private bool $signatureElectronique = false;

    #[ORM\Column(name: 'signature_date', type: 'datetime', nullable: true)]
    private ?\DateTime $signatureDate = null;

    #[ORM\Column(name: 'signature_medecin', length: 255, nullable: true)]
    private ?string $signatureMedecin = null;

    #[ORM\Column(name: 'docusign_envelope_id', length: 255, nullable: true)]
    private ?string $docusignEnvelopeId = null;

    #[ORM\Column(name: 'docusign_status', length: 50, nullable: true)]
    private ?string $docusignStatus = null;

    #[ORM\Column(name: 'signature_document_path', length: 500, nullable: true)]
    private ?string $signatureDocumentPath = null;

    #[ORM\Column(name: 'signature_patient', type: 'text', nullable: true)]
    private ?string $signaturePatient = null;

    #[ORM\Column(name: 'signature_patient_date', type: 'datetime', nullable: true)]
    private ?\DateTime $signaturePatientDate = null;

    #[ORM\Column(name: 'signature_patient_ip', length: 45, nullable: true)]
    private ?string $signaturePatientIp = null;

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur', referencedColumnName: 'id_utilisateur', nullable: false, onDelete: 'CASCADE')]
    #[Assert\NotBlank(message: 'Le patient est obligatoire')]
    private ?Utilisateur $utilisateur = null;

    #[ORM\OneToMany(targetEntity: Traitement::class, mappedBy: 'ordonnance', cascade: ['persist', 'remove'])]
    private Collection $traitements;

    public function __construct()
    {
        $this->traitements = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNumeroOrdonnance(): ?string
    {
        return $this->numeroOrdonnance;
    }

    public function setNumeroOrdonnance(?string $numeroOrdonnance): static
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

    /**
     * @return Collection<int, Traitement>
     */
    public function getTraitements(): Collection
    {
        return $this->traitements;
    }

    public function addTraitement(Traitement $traitement): static
    {
        if (!$this->traitements->contains($traitement)) {
            $this->traitements->add($traitement);
            $traitement->setOrdonnance($this);
        }

        return $this;
    }

    public function removeTraitement(Traitement $traitement): static
    {
        if ($this->traitements->removeElement($traitement)) {
            // set the owning side to null (unless already changed)
            if ($traitement->getOrdonnance() === $this) {
                $traitement->setOrdonnance(null);
            }
        }

        return $this;
    }

    public function isSignatureElectronique(): bool
    {
        return $this->signatureElectronique;
    }

    public function setSignatureElectronique(bool $signatureElectronique): static
    {
        $this->signatureElectronique = $signatureElectronique;
        return $this;
    }

    public function getSignatureDate(): ?\DateTime
    {
        return $this->signatureDate;
    }

    public function setSignatureDate(?\DateTime $signatureDate): static
    {
        $this->signatureDate = $signatureDate;
        return $this;
    }

    public function getSignatureMedecin(): ?string
    {
        return $this->signatureMedecin;
    }

    public function setSignatureMedecin(?string $signatureMedecin): static
    {
        $this->signatureMedecin = $signatureMedecin;
        return $this;
    }

    public function getDocusignEnvelopeId(): ?string
    {
        return $this->docusignEnvelopeId;
    }

    public function setDocusignEnvelopeId(?string $docusignEnvelopeId): static
    {
        $this->docusignEnvelopeId = $docusignEnvelopeId;
        return $this;
    }

    public function getDocusignStatus(): ?string
    {
        return $this->docusignStatus;
    }

    public function setDocusignStatus(?string $docusignStatus): static
    {
        $this->docusignStatus = $docusignStatus;
        return $this;
    }

    public function getSignatureDocumentPath(): ?string
    {
        return $this->signatureDocumentPath;
    }

    public function setSignatureDocumentPath(?string $signatureDocumentPath): static
    {
        $this->signatureDocumentPath = $signatureDocumentPath;
        return $this;
    }

    public function getSignaturePatient(): ?string
    {
        return $this->signaturePatient;
    }

    public function setSignaturePatient(?string $signaturePatient): static
    {
        $this->signaturePatient = $signaturePatient;
        return $this;
    }

    public function getSignaturePatientDate(): ?\DateTime
    {
        return $this->signaturePatientDate;
    }

    public function setSignaturePatientDate(?\DateTime $signaturePatientDate): static
    {
        $this->signaturePatientDate = $signaturePatientDate;
        return $this;
    }

    public function getSignaturePatientIp(): ?string
    {
        return $this->signaturePatientIp;
    }

    public function setSignaturePatientIp(?string $signaturePatientIp): static
    {
        $this->signaturePatientIp = $signaturePatientIp;
        return $this;
    }
}
