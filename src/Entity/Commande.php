<?php

namespace App\Entity;

use App\Repository\CommandeRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: CommandeRepository::class)]
#[ORM\Table(name: 'commande')]
class Commande
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_commande')]
    private ?int $id = null;

    #[ORM\Column(name: 'date_commande', type: 'datetime_immutable')]
    private \DateTimeImmutable $dateCommande;

    #[ORM\Column(length: 50, options: ['default' => 'en_attente'])]
    private string $statut = 'en_attente';

    #[ORM\Column(type: 'decimal', precision: 10, scale: 2, options: ['default' => 0])]
    private string $total = '0.00';

    #[ORM\Column(name: 'mode_paiement', length: 50)]
    private string $modePaiement = '';

    #[ORM\Column(name: 'adresse_livraison', length: 255)]
    private string $adresseLivraison = '';

    #[ORM\Column(length: 20)]
    private string $telephone = '';

    #[ORM\Column(length: 255)]
    private string $nom = '';

    #[ORM\Column(length: 255)]
    private string $email = '';

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $message = null;

    // Temporary solution: store product IDs as JSON string
    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $produitsIds = null;

    #[ORM\Column(name: 'coupon_code', length: 64, nullable: true)]
    private ?string $couponCode = null;

    #[ORM\Column(name: 'coupon_discount', type: 'decimal', precision: 10, scale: 2, options: ['default' => 0])]
    private string $couponDiscount = '0.00';

    #[ORM\Column(name: 'estimated_delivery_date', nullable: true)]
    private ?\DateTimeImmutable $estimatedDeliveryDate = null;

    #[ORM\Column(name: 'fraud_score', options: ['default' => 0])]
    private int $fraudScore = 0;

    #[ORM\Column(name: 'base_shipping_cost', type: 'decimal', precision: 10, scale: 2, options: ['default' => 0])]
    private string $baseShippingCost = '0.00';

    #[ORM\ManyToOne(targetEntity: Utilisateur::class)]
    #[ORM\JoinColumn(name: 'id_utilisateur_id', referencedColumnName: 'id_utilisateur', nullable: true, onDelete: 'SET NULL')]
    private ?Utilisateur $utilisateur = null;

    #[ORM\OneToMany(mappedBy: 'commande', targetEntity: OrderShipment::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $shipments;

    public function __construct()
    {
        $this->dateCommande = new \DateTimeImmutable();
        $this->statut = 'en_attente';
        $this->total = '0.00';
        $this->couponDiscount = '0.00';
        $this->fraudScore = 0;
        $this->baseShippingCost = '0.00';
        $this->shipments = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getDateCommande(): \DateTimeImmutable
    {
        return $this->dateCommande;
    }

    protected function setDateCommande(\DateTimeImmutable $dateCommande): static
    {
        $this->dateCommande = $dateCommande;
        return $this;
    }

    public function getStatut(): string
    {
        return $this->statut;
    }

    public function setStatut(string $statut): static
    {
        $this->statut = $statut;
        return $this;
    }

    public function getTotal(): float
    {
        return (float) $this->total;
    }

    public function setTotal(float $total): static
    {
        $this->total = (string) $total;
        return $this;
    }

    public function getModePaiement(): string
    {
        return $this->modePaiement;
    }

    public function setModePaiement(string $modePaiement): static
    {
        $this->modePaiement = $modePaiement;
        return $this;
    }

    public function getAdresseLivraison(): string
    {
        return $this->adresseLivraison;
    }

    public function setAdresseLivraison(string $adresseLivraison): static
    {
        $this->adresseLivraison = $adresseLivraison;
        return $this;
    }

    public function getTelephone(): string
    {
        return $this->telephone;
    }

    public function setTelephone(string $telephone): static
    {
        $this->telephone = $telephone;
        return $this;
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

    public function getEmail(): string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = $email;
        return $this;
    }

    public function getMessage(): ?string
    {
        return $this->message;
    }

    public function setMessage(?string $message): static
    {
        $this->message = $message;
        return $this;
    }

    public function getProduitsIds(): ?string
    {
        return $this->produitsIds;
    }

    public function setProduitsIds(?string $produitsIds): static
    {
        $this->produitsIds = $produitsIds;
        return $this;
    }

    /**
     * Get products array from JSON string
     */
    public function getProduitsArray(): array
    {
        return $this->produitsIds ? json_decode($this->produitsIds, true) : [];
    }

    /**
     * Set products from array
     */
    public function setProduitsFromArray(array $produits): static
    {
        $this->produitsIds = json_encode(array_values($produits));
        return $this;
    }

    /**
     * Calculate total based on selected products (requires repository)
     */
    public function calculateTotalFromProducts(array $products): float
    {
        $total = 0.0;
        foreach ($products as $product) {
            $total += $product->getPrix();
        }
        return $total;
    }

    public function getCouponCode(): ?string
    {
        return $this->couponCode;
    }

    public function setCouponCode(?string $couponCode): static
    {
        $this->couponCode = $couponCode;
        return $this;
    }

    public function getCouponDiscount(): float
    {
        return (float) ($this->couponDiscount ?? 0.0);
    }

    public function setCouponDiscount(float $couponDiscount): static
    {
        $this->couponDiscount = max(0.0, $couponDiscount);
        return $this;
    }

    public function getEstimatedDeliveryDate(): ?\DateTimeImmutable
    {
        return $this->estimatedDeliveryDate;
    }

    protected function setEstimatedDeliveryDate(?\DateTimeImmutable $estimatedDeliveryDate): static
    {
        $this->estimatedDeliveryDate = $estimatedDeliveryDate;
        return $this;
    }

    public function getFraudScore(): int
    {
        return $this->fraudScore;
    }

    public function setFraudScore(int $fraudScore): static
    {
        $this->fraudScore = max(0, min(100, $fraudScore));
        return $this;
    }

    public function getBaseShippingCost(): float
    {
        return (float) ($this->baseShippingCost ?? 0.0);
    }

    public function setBaseShippingCost(float $baseShippingCost): static
    {
        $this->baseShippingCost = max(0.0, $baseShippingCost);
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
     * @return Collection<int, OrderShipment>
     */
    public function getShipments(): Collection
    {
        return $this->shipments;
    }

    public function addShipment(OrderShipment $shipment): static
    {
        if (!$this->shipments->contains($shipment)) {
            $this->shipments->add($shipment);
            $shipment->setCommande($this);
        }
        return $this;
    }

    public function removeShipment(OrderShipment $shipment): static
    {
        if ($this->shipments->removeElement($shipment)) {
            if ($shipment->getCommande() === $this) {
                $shipment->setCommande(null);
            }
        }
        return $this;
    }
}
