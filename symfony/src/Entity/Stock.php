<?php

namespace App\Entity;

use App\Repository\StockRepository;
use App\Entity\Produit;
use App\Entity\Depot;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: StockRepository::class)]
#[ORM\Table(name: 'stock')]
class Stock
{
    public const ETAT_DISPONIBLE = 'disponible';
    public const ETAT_ALERTE = 'alerte';
    public const ETAT_RUPTURE = 'rupture';
    public const ETAT_PERIME = 'perime';
    public const ETAT_EXPIRE = 'expire';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_stock')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Produit::class, inversedBy: 'stocks')]
    #[ORM\JoinColumn(name: 'produit_id', referencedColumnName: 'id_produit')]
    private ?Produit $produit = null;

    #[ORM\ManyToOne(targetEntity: Depot::class, inversedBy: 'stocks')]
    #[ORM\JoinColumn(name: 'depot_id', referencedColumnName: 'id_depot')]
    private ?Depot $depot = null;

    #[ORM\Column(name: 'quantite')]
    private ?int $quantite = 0;

    #[ORM\Column(name: 'seuil_alerte')]
    private ?int $seuilAlerte = 10;

    #[ORM\Column(name: 'seuil_critique')]
    private ?int $seuilCritique = 5;

    #[ORM\Column(name: 'date_entree', type: 'datetime')]
    private ?\DateTime $dateEntree = null;

    #[ORM\Column(name: 'date_expiration', type: 'datetime', nullable: true)]
    private ?\DateTime $dateExpiration = null;

    #[ORM\Column(name: 'etat_stock', length: 20)]
    private ?string $etatStock = self::ETAT_DISPONIBLE;

    #[ORM\Column(name: 'date_derniere_mise_a_jour', type: 'datetime')]
    private ?\DateTime $dateDerniereMiseAJour = null;

    #[ORM\Column(name: 'derniere_entree', type: 'datetime', nullable: true)]
    private ?\DateTime $derniereEntree = null;

    #[ORM\Column(name: 'derniere_sortie', type: 'datetime', nullable: true)]
    private ?\DateTime $derniereSortie = null;

    #[ORM\Column(name: 'total_entrees', options: ['default' => 0])]
    private int $totalEntrees = 0;

    #[ORM\Column(name: 'total_sorties', options: ['default' => 0])]
    private int $totalSorties = 0;

    #[ORM\Column(name: 'prix_achat_unitaire', type: 'decimal', precision: 10, scale: 2, nullable: true)]
    private ?float $prixAchatUnitaire = null;

    #[ORM\Column(name: 'prix_vente_unitaire', type: 'decimal', precision: 10, scale: 2, nullable: true)]
    private ?float $prixVenteUnitaire = null;

    #[ORM\Column(name: 'emplacement', length: 100, nullable: true)]
    private ?string $emplacement = null;

    #[ORM\Column(name: 'batch_number', length: 50, nullable: true)]
    private ?string $batchNumber = null;

    #[ORM\Column(name: 'fournisseur', length: 100, nullable: true)]
    private ?string $fournisseur = null;

    #[ORM\Column(name: 'notes', type: 'text', nullable: true)]
    private ?string $notes = null;

    public function __construct()
    {
        $this->dateDerniereMiseAJour = new \DateTime();
        $this->dateEntree = new \DateTime();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getIdStock(): ?int
    {
        return $this->id;
    }

    public function getProduit(): ?Produit
    {
        return $this->produit;
    }

    public function setProduit(?Produit $produit): static
    {
        $this->produit = $produit;
        return $this;
    }

    public function getDepot(): ?Depot
    {
        return $this->depot;
    }

    public function setDepot(?Depot $depot): static
    {
        $this->depot = $depot;
        return $this;
    }

    public function getQuantite(): ?int
    {
        return $this->quantite;
    }

    public function setQuantite(?int $quantite): static
    {
        $this->quantite = $quantite;
        $this->updateEtatStock();
        $this->dateDerniereMiseAJour = new \DateTime();
        return $this;
    }

    public function getSeuilAlerte(): ?int
    {
        return $this->seuilAlerte;
    }

    public function setSeuilAlerte(?int $seuilAlerte): static
    {
        $this->seuilAlerte = $seuilAlerte;
        $this->updateEtatStock();
        return $this;
    }

    public function getSeuilCritique(): ?int
    {
        return $this->seuilCritique;
    }

    public function setSeuilCritique(?int $seuilCritique): static
    {
        $this->seuilCritique = $seuilCritique;
        $this->updateEtatStock();
        return $this;
    }

    public function getDateEntree(): ?\DateTime
    {
        return $this->dateEntree;
    }

    public function setDateEntree(?\DateTime $dateEntree): static
    {
        $this->dateEntree = $dateEntree;
        return $this;
    }

    public function getDateExpiration(): ?\DateTime
    {
        return $this->dateExpiration;
    }

    public function setDateExpiration(?\DateTime $dateExpiration): static
    {
        $this->dateExpiration = $dateExpiration;
        $this->updateEtatStock();
        return $this;
    }

    public function getEtatStock(): ?string
    {
        return $this->etatStock;
    }

    public function setEtatStock(?string $etatStock): static
    {
        $this->etatStock = $etatStock;
        return $this;
    }

    public function getDateDerniereMiseAJour(): ?\DateTime
    {
        return $this->dateDerniereMiseAJour;
    }

    public function setDateDerniereMiseAJour(?\DateTime $dateDerniereMiseAJour): static
    {
        $this->dateDerniereMiseAJour = $dateDerniereMiseAJour;
        return $this;
    }

    public function getDerniereEntree(): ?\DateTime
    {
        return $this->derniereEntree;
    }

    public function setDerniereEntree(?\DateTime $derniereEntree): static
    {
        $this->derniereEntree = $derniereEntree;
        return $this;
    }

    public function getDerniereSortie(): ?\DateTime
    {
        return $this->derniereSortie;
    }

    public function setDerniereSortie(?\DateTime $derniereSortie): static
    {
        $this->derniereSortie = $derniereSortie;
        return $this;
    }

    public function getTotalEntrees(): int
    {
        return $this->totalEntrees;
    }

    public function setTotalEntrees(int $totalEntrees): static
    {
        $this->totalEntrees = $totalEntrees;
        return $this;
    }

    public function getTotalSorties(): int
    {
        return $this->totalSorties;
    }

    public function setTotalSorties(int $totalSorties): static
    {
        $this->totalSorties = $totalSorties;
        return $this;
    }

    public function getPrixAchatUnitaire(): ?float
    {
        return $this->prixAchatUnitaire;
    }

    public function setPrixAchatUnitaire(?float $prixAchatUnitaire): static
    {
        $this->prixAchatUnitaire = $prixAchatUnitaire;
        return $this;
    }

    public function getPrixVenteUnitaire(): ?float
    {
        return $this->prixVenteUnitaire;
    }

    public function setPrixVenteUnitaire(?float $prixVenteUnitaire): static
    {
        $this->prixVenteUnitaire = $prixVenteUnitaire;
        return $this;
    }

    public function getEmplacement(): ?string
    {
        return $this->emplacement;
    }

    public function setEmplacement(?string $emplacement): static
    {
        $this->emplacement = $emplacement;
        return $this;
    }

    public function getBatchNumber(): ?string
    {
        return $this->batchNumber;
    }

    public function setBatchNumber(?string $batchNumber): static
    {
        $this->batchNumber = $batchNumber;
        return $this;
    }

    public function getFournisseur(): ?string
    {
        return $this->fournisseur;
    }

    public function setFournisseur(?string $fournisseur): static
    {
        $this->fournisseur = $fournisseur;
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

    public function getEtatStockAffiche(): string
    {
        return match($this->etatStock) {
            self::ETAT_DISPONIBLE => 'Disponible',
            self::ETAT_ALERTE => 'Alerte',
            self::ETAT_RUPTURE => 'Rupture',
            self::ETAT_PERIME => 'Périmé',
            self::ETAT_EXPIRE => 'Expiré',
            default => 'Inconnu'
        };
    }

    public function updateEtatStock(): void
    {
        if ($this->estPerime()) {
            $this->etatStock = self::ETAT_EXPIRE;
            return;
        }

        if ($this->estProchePeremption()) {
            $this->etatStock = self::ETAT_PERIME;
            return;
        }

        // Vérifier l'état selon la quantité
        if ($this->quantite <= 0) {
            $this->etatStock = self::ETAT_RUPTURE;
        } elseif ($this->quantite <= $this->seuilCritique) {
            $this->etatStock = self::ETAT_RUPTURE;
        } elseif ($this->quantite <= $this->seuilAlerte) {
            $this->etatStock = self::ETAT_ALERTE;
        } else {
            $this->etatStock = self::ETAT_DISPONIBLE;
        }
    }

    public function estPerime(): bool
    {
        if (!$this->dateExpiration) {
            return false;
        }
        return $this->dateExpiration < new \DateTime();
    }

    public function estProchePeremption(): bool
    {
        if (!$this->dateExpiration) {
            return false;
        }
        
        $aujourdHui = new \DateTime();
        $interval = $aujourdHui->diff($this->dateExpiration);
        return $interval->days <= 30 && $this->dateExpiration > $aujourdHui;
    }

    public function getJoursAvantPeremption(): ?int
    {
        if (!$this->dateExpiration) {
            return null;
        }
        
        $aujourdHui = new \DateTime();
        $interval = $aujourdHui->diff($this->dateExpiration);
        
        if ($this->dateExpiration < $aujourdHui) {
            return -$interval->days; // Négatif si déjà périmé
        }
        
        return $interval->days;
    }

    public function getValeurTotale(): float
    {
        $prix = $this->prixVenteUnitaire ?? $this->prixAchatUnitaire ?? 0;
        return $prix * $this->quantite;
    }

    public function getMargeBeneficiaire(): ?float
    {
        if (!$this->prixAchatUnitaire || !$this->prixVenteUnitaire) {
            return null;
        }
        
        $prixAchat = $this->prixAchatUnitaire;
        $prixVente = $this->prixVenteUnitaire;
        
        if ($prixAchat == 0) {
            return null;
        }
        
        return (($prixVente - $prixAchat) / $prixAchat) * 100;
    }

    public function __toString(): string
    {
        $produit = $this->produit ? $this->produit->getNom() : 'Produit inconnu';
        $depot = $this->depot ? $this->depot->getNomDepot() : 'Dépôt inconnu';
        
        return sprintf('%s - %s (%s)', $produit, $depot, $this->quantite);
    }
}
