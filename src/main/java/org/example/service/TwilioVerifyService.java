package org.example.service;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
/**
 * Twilio Verify (SMS OTP) via l’API REST v2 — même style d’auth Basic que {@link SmsService}.
 * Variables : {@code TWILIO_ACCOUNT_SID}, {@code TWILIO_AUTH_TOKEN}, {@code TWILIO_VERIFY_SERVICE_SID}.
 * Dev local sans Twilio : {@code TWILIO_VERIFY_SKIP=true} (ou propriété système {@code twilio.verify.skip=true}) :
 * l’envoi est simulé et tout code à 6 chiffres est accepté à la vérification.
 */
public final class TwilioVerifyService {

    private static TwilioVerifyService instance;

    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;
    private final boolean skipVerify;

    private TwilioVerifyService() {
        this.accountSid = firstNonBlank(System.getenv("TWILIO_ACCOUNT_SID"), System.getProperty("twilio.account.sid"), "");
        this.authToken = firstNonBlank(System.getenv("TWILIO_AUTH_TOKEN"), System.getProperty("twilio.auth.token"), "");
        this.verifyServiceSid = firstNonBlank(
                System.getenv("TWILIO_VERIFY_SERVICE_SID"),
                System.getProperty("twilio.verify.service.sid"),
                "");
        String skipEnv = System.getenv("TWILIO_VERIFY_SKIP");
        this.skipVerify = "true".equalsIgnoreCase(skipEnv)
                || "true".equalsIgnoreCase(System.getProperty("twilio.verify.skip", ""));
    }

    public static TwilioVerifyService getInstance() {
        if (instance == null) {
            instance = new TwilioVerifyService();
        }
        return instance;
    }

    public boolean isSkipMode() {
        return skipVerify;
    }

    /** SID + token + service Verify renseignés (ignoré si {@link #isSkipMode()}). */
    public boolean isConfigured() {
        return skipVerify || (notBlank(accountSid) && notBlank(authToken) && notBlank(verifyServiceSid));
    }

    /**
     * Numéro au format E.164 pour Twilio Verify (Tunisie : +216… si l’utilisateur saisit 8 chiffres locaux).
     */
    public static String toE164(String normalizedDigits) {
        if (normalizedDigits == null || normalizedDigits.isBlank()) {
            return "";
        }
        String n = normalizedDigits.trim();
        if (n.startsWith("+")) {
            return n;
        }
        if (n.startsWith("216") && n.length() >= 11) {
            return "+" + n;
        }
        if (n.matches("[0-9]{8}")) {
            return "+216" + n;
        }
        if (n.matches("0[0-9]{8}")) {
            return "+216" + n.substring(1);
        }
        return "+" + n;
    }

    /** Démarre une vérification SMS ; retourne succès si Twilio accepte la requête (statut pending). */
    public VerifySendResult sendSmsVerification(String toE164) {
        if (toE164 == null || !toE164.startsWith("+")) {
            return VerifySendResult.failure("Numéro invalide (format international requis, ex. +216…).");
        }
        if (skipVerify) {
            System.out.println("[TwilioVerify] SKIP mode — aucun SMS envoyé pour " + toE164);
            return VerifySendResult.ok();
        }
        if (!notBlank(accountSid) || !notBlank(authToken) || !notBlank(verifyServiceSid)) {
            return VerifySendResult.failure(
                    "Twilio Verify non configuré : renseignez TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN et TWILIO_VERIFY_SERVICE_SID.");
        }

        try {
            String url = "https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/Verifications";
            String payload = "To=" + encode(toE164) + "&Channel=sms";
            String resp = postForm(url, payload);
            int http = lastHttpStatus;
            if (http >= 200 && http < 300) {
                return VerifySendResult.ok();
            }
            return VerifySendResult.failure("Twilio Verify (" + http + "): " + truncate(resp, 400));
        } catch (Exception e) {
            return VerifySendResult.failure(e.getMessage() != null ? e.getMessage() : "Erreur réseau");
        }
    }

    private static int lastHttpStatus;

    /** Vérifie le code saisi par l’utilisateur pour ce numéro. */
    public boolean checkVerificationCode(String toE164, String code) {
        if (toE164 == null || code == null || code.isBlank()) {
            return false;
        }
        String clean = code.trim();
        if (!clean.matches("\\d{4,10}")) {
            return false;
        }
        if (skipVerify) {
            System.out.println("[TwilioVerify] SKIP mode — acceptation du code pour " + toE164);
            return clean.matches("\\d{6}");
        }
        if (!notBlank(accountSid) || !notBlank(authToken) || !notBlank(verifyServiceSid)) {
            return false;
        }
        try {
            String url = "https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/VerificationCheck";
            String payload = "To=" + encode(toE164) + "&Code=" + encode(clean);
            String resp = postForm(url, payload);
            int http = lastHttpStatus;
            if (http < 200 || http >= 300) {
                System.out.println("[TwilioVerify] Check failed HTTP " + http + ": " + truncate(resp, 500));
                return false;
            }
            return responseIndicatesApproved(resp);
        } catch (Exception e) {
            System.out.println("[TwilioVerify] Check exception: " + e.getMessage());
            return false;
        }
    }

    private String postForm(String urlStr, String payload) throws Exception {
        byte[] postData = payload.getBytes(StandardCharsets.UTF_8);
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        String userpass = accountSid + ":" + authToken;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes(StandardCharsets.UTF_8));
        con.setRequestProperty("Authorization", basicAuth);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write(postData);
            wr.flush();
        }
        lastHttpStatus = con.getResponseCode();
        InputStream is = (lastHttpStatus >= 200 && lastHttpStatus < 300) ? con.getInputStream() : con.getErrorStream();
        byte[] resp = is.readAllBytes();
        return new String(resp, StandardCharsets.UTF_8);
    }

    private static boolean responseIndicatesApproved(String resp) {
        if (resp == null || resp.isBlank()) {
            return false;
        }
        String c = resp.replace(" ", "").replace("\n", "");
        return c.contains("\"status\":\"approved\"") || c.contains("\"valid\":true");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return "";
    }

    public static record VerifySendResult(boolean success, String message) {
        public static VerifySendResult ok() {
            return new VerifySendResult(true, "");
        }

        public static VerifySendResult failure(String msg) {
            return new VerifySendResult(false, msg != null ? msg : "Erreur");
        }
    }
}
