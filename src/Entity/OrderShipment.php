<?php

namespace App\Entity;

use App\Repository\OrderShipmentRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: OrderShipmentRepository::class)]
#[ORM\Table(name: 'order_shipment')]
class OrderShipment
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Commande::class, inversedBy: 'shipments')]
    #[ORM\JoinColumn(name: 'id_commande', referencedColumnName: 'id_commande', nullable: false, onDelete: 'CASCADE')]
    private ?Commande $commande = null;

    #[ORM\ManyToOne(targetEntity: Address::class)]
    #[ORM\JoinColumn(name: 'address_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?Address $address = null;

    #[ORM\Column(name: 'items_json', type: 'text')]
    private string $itemsJson = '[]';

    #[ORM\Column(name: 'shipping_cost', type: 'decimal', precision: 10, scale: 2, options: ['default' => 0])]
    private string $shippingCost = '0.00';

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getCommande(): ?Commande
    {
        return $this->commande;
    }

    public function setCommande(?Commande $commande): static
    {
        $this->commande = $commande;
        return $this;
    }

    public function getAddress(): ?Address
    {
        return $this->address;
    }

    public function setAddress(?Address $address): static
    {
        $this->address = $address;
        return $this;
    }

    public function getItemsJson(): string
    {
        return $this->itemsJson;
    }

    public function setItemsJson(string $itemsJson): static
    {
        $this->itemsJson = $itemsJson;
        return $this;
    }

    public function getItems(): array
    {
        $decoded = json_decode($this->itemsJson, true);
        return is_array($decoded) ? $decoded : [];
    }

    public function setItems(array $items): static
    {
        $this->itemsJson = json_encode(array_values($items));
        return $this;
    }

    public function getShippingCost(): float
    {
        return $this->shippingCost;
    }

    public function setShippingCost(float $shippingCost): static
    {
        $this->shippingCost = max(0.0, $shippingCost);
        return $this;
    }
}

