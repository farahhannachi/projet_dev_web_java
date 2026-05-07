<?php

namespace App\Service;

use App\Entity\Address;
use App\Entity\Commande;
use App\Entity\OrderShipment;

class OrderSplitService
{
    /**
     * @param array<int, array{address: Address, items: array<int, array{id:int,quantity:int,unitPrice:float}>}> $allocations
     */
    public function splitOrderByAddress(
        Commande $commande,
        array $allocations,
        ShippingCalculatorService $shippingCalculatorService
    ): float {
        foreach ($commande->getShipments() as $existing) {
            $commande->removeShipment($existing);
        }

        $totalShipping = 0.0;

        foreach ($allocations as $allocation) {
            $address = $allocation['address'];
            $items = $allocation['items'] ?? [];
            $subtotal = 0.0;
            $count = 0;
            foreach ($items as $item) {
                $qty = max(1, (int) ($item['quantity'] ?? 1));
                $unit = (float) ($item['unitPrice'] ?? 0);
                $subtotal += $qty * $unit;
                $count += $qty;
            }

            $shippingCost = $shippingCalculatorService->calculateForAddress($address, $count, $subtotal);

            $shipment = new OrderShipment();
            $shipment->setCommande($commande);
            $shipment->setAddress($address);
            $shipment->setItems($items);
            $shipment->setShippingCost($shippingCost);
            $commande->addShipment($shipment);

            $totalShipping += $shippingCost;
        }

        $commande->setBaseShippingCost(round($totalShipping, 2));
        return round($totalShipping, 2);
    }
}

