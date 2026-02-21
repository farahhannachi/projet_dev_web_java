<?php

namespace App\Twig;

use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;

class EncodingExtension extends AbstractExtension
{
    public function getFilters(): array
    {
        return [
            new TwigFilter('fix_encoding', [$this, 'fixEncoding']),
            new TwigFilter('decode_utf8', [$this, 'decodeUtf8']),
        ];
    }

    /**
     * Fix double-encoded UTF-8 text
     * Converts garbled text like "AntÃƒÆ'Ã†â€™Ãƒâ€šÃ‚Â©" back to "Antécédents"
     */
    public function fixEncoding(?string $text): ?string
    {
        if (empty($text)) {
            return $text;
        }

        // Check if text contains mojibake patterns (common in double-encoded UTF-8)
        if (strpos($text, 'Ã') !== false || strpos($text, 'Â') !== false) {
            // Try to fix by converting from Latin1 to UTF-8
            $fixed = mb_convert_encoding($text, 'UTF-8', 'ISO-8859-1');
            
            // If that doesn't work, try the reverse
            if ($fixed === $text || strpos($fixed, 'Ã') !== false) {
                $fixed = mb_convert_encoding($text, 'UTF-8', 'UTF-8');
            }
            
            return $fixed;
        }
        
        return $text;
    }

    /**
     * Alternative method to decode UTF-8 mojibake
     */
    public function decodeUtf8(?string $text): ?string
    {
        if (empty($text)) {
            return $text;
        }

        // Remove BOM if present
        $text = str_replace("\xEF\xBB\xBF", '', $text);

        // Convert from UTF-8 to Latin1 and back to fix double encoding
        $text = mb_convert_encoding($text, 'UTF-8', 'UTF-8');
        
        // If still contains garbled characters, try iconv
        if (strpos($text, 'Ã') !== false || strpos($text, 'Â') !== false) {
            $converted = @iconv('UTF-8', 'ISO-8859-1//IGNORE', $text);
            if ($converted !== false) {
                $text = mb_convert_encoding($converted, 'UTF-8', 'ISO-8859-1');
            }
        }

        return $text;
    }
}
