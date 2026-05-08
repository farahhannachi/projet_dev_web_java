package org.example.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class TwoFactorAuthService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_LENGTH = 20;
    private static final int TIME_STEP_SECONDS = 30;

    public String generateSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] buffer = new byte[SECRET_LENGTH];
        secureRandom.nextBytes(buffer);
        return base32Encode(buffer);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            String expected = generateCurrentCode(secret, currentWindow + offset);
            if (code.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    public String buildManualEntryKey(String secret) {
        return formatSecretForDisplay(secret);
    }

    public String buildOtpAuthUri(String accountName, String secret) {
        String safeAccountName = accountName == null || accountName.isBlank() ? "user" : accountName.trim();
        return "otpauth://totp/CuraVita:" + safeAccountName + "?secret=" + secret + "&issuer=CuraVita";
    }

    private String generateCurrentCode(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception exception) {
            return "";
        }
    }

    private String formatSecretForDisplay(String secret) {
        String clean = secret.replace(" ", "");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < clean.length(); index++) {
            if (index > 0 && index % 4 == 0) {
                builder.append(' ');
            }
            builder.append(clean.charAt(index));
        }
        return builder.toString();
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int current = 0;
        int bits = 0;

        for (byte datum : data) {
            current = (current << 8) | (datum & 0xFF);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32_ALPHABET.charAt((current >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }

        if (bits > 0) {
            result.append(BASE32_ALPHABET.charAt((current << (5 - bits)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String secret) {
        String normalized = secret.toUpperCase().replace(" ", "").replace("=", "");
        ByteBuffer buffer = ByteBuffer.allocate((normalized.length() * 5) / 8 + 1);

        int current = 0;
        int bits = 0;
        for (char character : normalized.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(character);
            if (value < 0) {
                continue;
            }
            current = (current << 5) | value;
            bits += 5;
            if (bits >= 8) {
                buffer.put((byte) ((current >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }

        byte[] decoded = new byte[buffer.position()];
        buffer.flip();
        buffer.get(decoded);
        return decoded;
    }
}
