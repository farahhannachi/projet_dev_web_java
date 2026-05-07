package org.example.service;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmsService {
    private static SmsService instance;
    // Définir TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER (env ou -D) — ne pas committer de secrets.
    private static final String DEFAULT_ACCOUNT_SID = "";
    private static final String DEFAULT_AUTH_TOKEN = "";
    private static final String DEFAULT_FROM_NUMBER = "";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsService() {
        this.accountSid = firstNonBlank(System.getenv("TWILIO_ACCOUNT_SID"), System.getProperty("twilio.account.sid"), DEFAULT_ACCOUNT_SID);
        this.authToken = firstNonBlank(System.getenv("TWILIO_AUTH_TOKEN"), System.getProperty("twilio.auth.token"), DEFAULT_AUTH_TOKEN);
        this.fromNumber = firstNonBlank(System.getenv("TWILIO_FROM_NUMBER"), System.getProperty("twilio.from.number"), DEFAULT_FROM_NUMBER);

        try {
            String sidMask = masked(this.accountSid, "SID");
            String tokenMask = masked(this.authToken, "token");
            String fromMask = (this.fromNumber == null || this.fromNumber.isBlank()) ? "<missing>" : this.fromNumber;
            System.out.println("[SMS] Twilio SID=" + sidMask + " From=" + fromMask + " Token=" + tokenMask);
        } catch (Exception ignored) {}
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

    private static String masked(String value, String label) {
        if (value == null || value.isBlank()) {
            return "<missing " + label + ">";
        }
        return value.length() > 6 ? value.substring(0, 6) + "..." : "***";
    }

    private static boolean credentialsConfigured(String sid, String token, String from) {
        return sid != null && !sid.isBlank()
                && token != null && !token.isBlank()
                && from != null && !from.isBlank();
    }

    public static SmsService getInstance() {
        if (instance == null) {
            instance = new SmsService();
        }
        return instance;
    }

    public boolean send(String toNumber, String message) {
        return sendSms(toNumber, message);
    }

    /**
     * Send a welcome SMS using Twilio REST API. Returns true if request accepted.
     * This method is defensive: if Twilio credentials are missing, it logs and returns false.
     */
    public boolean sendWelcomeSMS(String toNumber) {
        return sendSms(toNumber, "Welcome to CuraVita! Your account has been successfully created.");
    }

    private boolean sendSms(String toNumber, String body) {
        if (!credentialsConfigured(accountSid, authToken, fromNumber)) {
            System.out.println("[SMS] Twilio non configuré : définissez TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN et TWILIO_FROM_NUMBER (non vides).");
            return false;
        }
        if (toNumber == null || toNumber.isBlank()) {
            System.out.println("[SMS] Envoi annulé : numéro du destinataire absent (enregistrez un téléphone pour le patient).");
            return false;
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            String safeBody = body != null ? body : "";
            String payload = "To=" + encode(toNumber.trim()) + "&From=" + encode(fromNumber) + "&Body=" + encode(safeBody);

            byte[] postData = payload.getBytes(StandardCharsets.UTF_8);

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
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

            int responseCode = con.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? con.getInputStream() : con.getErrorStream();
            byte[] resp = is.readAllBytes();
            String respText = new String(resp, StandardCharsets.UTF_8);
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("[SMS] Sent welcome SMS to " + toNumber + ": " + respText);
                return true;
            } else {
                System.out.println("[SMS] Twilio error (" + responseCode + "): " + respText);
                return false;
            }
        } catch (Exception e) {
            System.out.println("[SMS] Exception sending SMS: " + e.getMessage());
            return false;
        }
    }

    private static String encode(String s) {
        if (s == null) {
            return "";
        }
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
