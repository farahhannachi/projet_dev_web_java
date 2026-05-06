package org.example.service;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmsService {
    // No hardcoded credentials here. To use SMS locally, set environment variables
    // TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER, or configure
    // them in your runtime environment. Do NOT commit secrets to source control.
    private static final String DEFAULT_ACCOUNT_SID = "";
    private static final String DEFAULT_AUTH_TOKEN = "";
    private static final String DEFAULT_FROM_NUMBER = "";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsService() {
        // Use the hardcoded credentials directly
        this.accountSid = DEFAULT_ACCOUNT_SID;
        this.authToken = DEFAULT_AUTH_TOKEN;
        this.fromNumber = DEFAULT_FROM_NUMBER;

        // Diagnostic: print whether credentials were found (mask sensitive parts)
        try {
            String sidMask = (this.accountSid == null) ? "<missing>" : (this.accountSid.length() > 6 ? this.accountSid.substring(0, 6) + "..." : this.accountSid);
            String tokenMask = (this.authToken == null) ? "<missing>" : (this.authToken.length() > 6 ? this.authToken.substring(0, 6) + "..." : this.authToken);
            String fromMask = (this.fromNumber == null) ? "<missing>" : this.fromNumber;
            System.out.println("[SMS] Using Twilio SID=" + sidMask + " From=" + fromMask + " Token=" + tokenMask);
        } catch (Exception ignored) {}
    }

    /**
     * Send a welcome SMS using Twilio REST API. Returns true if request accepted.
     * This method is defensive: if Twilio credentials are missing, it logs and returns false.
     */
    public boolean sendWelcomeSMS(String toNumber) {
        if (accountSid == null || authToken == null || fromNumber == null) {
            System.out.println("[SMS] Twilio credentials not configured (TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_FROM_NUMBER)");
            return false;
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            String body = "Welcome to CuraVita! Your account has been successfully created.";

            String payload = "To=" + encode(toNumber) + "&From=" + encode(fromNumber) + "&Body=" + encode(body);

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

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
