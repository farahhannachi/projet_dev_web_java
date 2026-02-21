<?php

namespace App\Service;

use App\Entity\Utilisateur;
use OTPHP\TOTP;
use Endroid\QrCode\Color\Color;
use Endroid\QrCode\Encoding\Encoding;
use Endroid\QrCode\ErrorCorrectionLevel;
use Endroid\QrCode\QrCode;
use Endroid\QrCode\Writer\SvgWriter;
use Doctrine\ORM\EntityManagerInterface;

class TwoFactorAuthService
{
    private EntityManagerInterface $entityManager;

    public function __construct(EntityManagerInterface $entityManager)
    {
        $this->entityManager = $entityManager;
    }

    /**
     * Generate a new TOTP secret for a user
     */
    public function generateSecret(): string
    {
        $totp = TOTP::create();
        return $totp->getSecret();
    }

    /**
     * Generate QR code URL for Google Authenticator
     */
    public function getQrCodeUrl(Utilisateur $user): string
    {
        $secret = $user->getTotpSecret();
        if (!$secret) {
            $secret = $this->generateSecret();
            $user->setTotpSecret($secret);
        }

        $totp = TOTP::create(
            $secret,
            30,
            'sha1',
            6
        );

        $totp->setIssuer('CURAVITA');
        $totp->setLabel($user->getEmail());

        return $totp->getProvisioningUri();
    }

    /**
     * Generate QR code image as base64 using SVG (no GD required)
     */
    public function getQrCodeImage(Utilisateur $user): string
    {
        $provisioningUri = $this->getQrCodeUrl($user);

        $qrCode = new QrCode(
            data: $provisioningUri,
            encoding: new Encoding('UTF-8'),
            errorCorrectionLevel: ErrorCorrectionLevel::Low,
            size: 300,
            margin: 10,
            foregroundColor: new Color(0, 0, 0),
            backgroundColor: new Color(255, 255, 255)
        );

        $writer = new SvgWriter();
        $result = $writer->write($qrCode);

        // Convert SVG to base64
        $svgString = $result->getString();
        return base64_encode($svgString);
    }

    /**
     * Verify a TOTP code
     */
    public function verifyCode(Utilisateur $user, string $code): bool
    {
        $secret = $user->getTotpSecret();
        if (!$secret) {
            return false;
        }

        $totp = TOTP::create(
            $secret,
            30,
            'sha1',
            6
        );

        return $totp->verify($code);
    }

    /**
     * Enable 2FA for a user after verifying their first code
     */
    public function enable2FA(Utilisateur $user, string $code): bool
    {
        if ($this->verifyCode($user, $code)) {
            $user->setTotpEnabled(true);
            $this->entityManager->flush();
            return true;
        }
        return false;
    }

    /**
     * Disable 2FA for a user
     */
    public function disable2FA(Utilisateur $user): void
    {
        $user->setTotpEnabled(false);
        $user->setTotpSecret(null);
        $user->setBackupCodes(null);
        $this->entityManager->flush();
    }

    /**
     * Check if user has 2FA enabled
     */
    public function is2FAEnabled(Utilisateur $user): bool
    {
        return $user->isTotpEnabled() && $user->getTotpSecret() !== null;
    }
}
