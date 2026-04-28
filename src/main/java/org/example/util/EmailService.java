package org.example.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Service d'envoi d'emails via SMTP (Gmail).
 * Le sender s'affiche comme "CuraVita Pharmacie".
 */
public class EmailService {

    private static EmailService instance;

    // Configurer avec un compte Gmail
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String SENDER_EMAIL  = "emnabenaissa554@gmail.com"; // Votre email Gmail
    private static final String SENDER_PASS   = "bzqe ynyj qikb ivoz";       // Mot de passe d'application Gmail
    private static final String SENDER_NAME   = "CuraVita Pharmacie";

    private EmailService() {}

    public static EmailService getInstance() {
        if (instance == null) instance = new EmailService();
        return instance;
    }

    /**
     * Envoie un email HTML au patient
     * @param toEmail   email du destinataire
     * @param subject   sujet de l'email
     * @param htmlBody  contenu HTML
     */
    public void send(String toEmail, String subject, String htmlBody) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
                props.put("mail.smtp.ssl.trust", SMTP_HOST);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=UTF-8");

                Transport.send(message);
                System.out.println("[Email] Envoyé à " + toEmail);
            } catch (Exception e) {
                System.err.println("[Email] Erreur envoi : " + e.getMessage());
            }
        }).start();
    }
}
