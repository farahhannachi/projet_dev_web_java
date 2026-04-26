package org.example.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Utility class for sending emails using JavaMail SMTP
 * Simple configuration - just add your email and password
 */
public class EmailUtil {
    
    // SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    // Email credentials - CONFIGURE THESE
    // For Gmail: Use your Gmail address and password
    // Example:
    //   FROM_EMAIL = "your.email@gmail.com"
    //   EMAIL_PASSWORD = "your-actual-password"
    private static final String FROM_EMAIL = "ihebjbir10@gmail.com";     // ← SET YOUR EMAIL
    private static final String EMAIL_PASSWORD = "yhgakgrgwwqgikag";         // ← SET YOUR PASSWORD
    
    /**
     * Send password reset email
     * @param toEmail Recipient email
     * @param resetToken The reset token (UUID)
     * @return true if email sent successfully
     */
    public static boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Setup SMTP properties
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            
            // Create authenticator
            Authenticator auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            };
            
            // Get session
            Session session = Session.getInstance(props, auth);
            session.setDebug(false); // Set to true for debugging
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Password Reset - CuraVita Pharmacy");
            
            // Create email content - use HTTP link for clickability + custom scheme as fallback
            String httpLink = "http://localhost:8080/reset?token=" + resetToken;
            String customLink = "curavita://reset?token=" + resetToken;
            String htmlContent = buildResetEmailHTML(httpLink, customLink);
            
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Send email
            Transport.send(message);
            
            System.out.println("[EMAIL] Password reset email sent to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Build HTML content for password reset email
     * @param httpLink Clickable HTTP link for the button
     * @param customLink Custom scheme link (curavita://) as fallback for copying
     */
    private static String buildResetEmailHTML(String httpLink, String customLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; background-color: #f4f4f4; }" +
                "        .container { max-width: 600px; margin: 50px auto; background-color: white; " +
                "                      padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        .header { color: #27ae60; text-align: center; margin-bottom: 30px; }" +
                "        .content { color: #333; line-height: 1.6; margin-bottom: 20px; }" +
                "        .button { display: inline-block; background-color: #27ae60; color: white; " +
                "                  padding: 12px 30px; border-radius: 5px; text-decoration: none; margin: 20px 0; }" +
                "        .fallback-section { margin-top: 25px; padding: 15px; background-color: #f9f9f9; border-radius: 5px; }" +
                "        .fallback-label { font-size: 13px; color: #666; margin-bottom: 8px; }" +
                "        .custom-link { word-break: break-all; font-family: monospace; font-size: 12px; color: #27ae60; }" +
                "        .footer { color: #999; font-size: 12px; text-align: center; margin-top: 30px; border-top: 1px solid #ddd; padding-top: 20px; }" +
                "        .warning { color: #e74c3c; font-size: 12px; margin-top: 20px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h2>🔑 Password Reset Request</h2>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Hello,</p>" +
                "            <p>We received a request to reset your password. Click the button below to reset it:</p>" +
                "            <a href='" + httpLink + "' class='button'>Reset Password</a>" +
                "            <div class='fallback-section'>" +
                "                <div class='fallback-label'>If the button doesn't work, copy and paste this link into your app:</div>" +
                "                <div class='custom-link'>" + customLink + "</div>" +
                "            </div>" +
                "            <p>This link will expire in 24 hours.</p>" +
                "        </div>" +
                "        <div class='warning'>" +
                "            ⚠️ If you did not request a password reset, please ignore this email or contact support." +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>CuraVita Pharmacy Management System</p>" +
                "            <p>&copy; 2026 All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Verify email configuration
     */
    public static boolean testEmailConfiguration() {
        boolean isConfigured = !FROM_EMAIL.equals("your.email@gmail.com") && 
                              !EMAIL_PASSWORD.equals("your-password");
        
        if (!isConfigured) {
            System.err.println("[EMAIL CONFIG] ⚠️ Email credentials not configured!");
            System.err.println("[EMAIL CONFIG] Please edit EmailUtil.java and set:");
            System.err.println("  - FROM_EMAIL: your Gmail address");
            System.err.println("  - EMAIL_PASSWORD: your password");
            return false;
        }
        
        System.out.println("[EMAIL CONFIG] ✓ Email configured for: " + FROM_EMAIL);
        return true;
    }
}

