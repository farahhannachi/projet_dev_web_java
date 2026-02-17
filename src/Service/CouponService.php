<?php

namespace App\Service;

use App\Entity\Coupon;
use App\Repository\CouponRepository;

class CouponService
{
    public function __construct(private readonly CouponRepository $couponRepository)
    {
    }

    /**
     * @return array{valid: bool, message: string, coupon: ?Coupon}
     */
    public function validateCoupon(string $code, float $cartTotal): array
    {
        $normalized = strtoupper(trim($code));
        if ($normalized === '') {
            return ['valid' => false, 'message' => 'Code promo vide.', 'coupon' => null];
        }

        $coupon = $this->couponRepository->findOneBy(['code' => $normalized]);
        if (!$coupon) {
            return ['valid' => false, 'message' => 'Coupon introuvable.', 'coupon' => null];
        }

        if (!$coupon->isActif()) {
            return ['valid' => false, 'message' => 'Coupon inactif.', 'coupon' => null];
        }

        if ($coupon->getDateExpiration() && $coupon->getDateExpiration() < new \DateTimeImmutable('now')) {
            return ['valid' => false, 'message' => 'Coupon expiré.', 'coupon' => null];
        }

        if ($coupon->getUsageCount() >= $coupon->getUsageMax()) {
            return ['valid' => false, 'message' => 'Coupon épuisé.', 'coupon' => null];
        }

        if ($cartTotal < $coupon->getMontantMinimumPanier()) {
            return [
                'valid' => false,
                'message' => sprintf('Montant minimum requis: %.2f DT.', $coupon->getMontantMinimumPanier()),
                'coupon' => null,
            ];
        }

        return ['valid' => true, 'message' => 'Coupon valide.', 'coupon' => $coupon];
    }

    /**
     * @return array{discount: float, finalTotal: float}
     */
    public function applyCoupon(float $cartTotal, Coupon $coupon): array
    {
        $discount = 0.0;

        if ($coupon->getType() === Coupon::TYPE_PERCENTAGE) {
            $discount = $cartTotal * ($coupon->getValeur() / 100);
        } else {
            $discount = $coupon->getValeur();
        }

        $discount = min($cartTotal, max(0.0, $discount));
        $finalTotal = max(0.0, $cartTotal - $discount);

        return [
            'discount' => round($discount, 2),
            'finalTotal' => round($finalTotal, 2),
        ];
    }
}

