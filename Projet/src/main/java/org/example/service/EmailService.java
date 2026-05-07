package org.example.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.model.Reservation;
import org.example.model.Service;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    private static EmailService instance;
    private final String ADMIN_EMAIL = "farah.hannachi@esprit.tn";
    // IMPORTANT: Configure with actual Gmail App Password (NOT normal password)
    // Steps: 1. Enable 2FA on Gmail, 2. Generate App Password at https://myaccount.google.com/apppasswords
    private final String GMAIL_USERNAME = "ihebjbir57@gmail.com";
    private final String GMAIL_APP_PASSWORD = "xwqaadixgvoberte"; // 16-character Gmail App Password

    private EmailService() {}

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    /**
     * Configure SMTP properties for Gmail with timeout and fallback support
     */
    private Properties getMailProperties() {
        Properties props = new Properties();

        // Basic SMTP configuration
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Connection timeouts (10 seconds)
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        // Additional properties for better compatibility
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false"); // Set to true for debugging SMTP issues

        return props;
    }

    /**
     * Send reservation confirmation email to admin
     */
    public void sendReservationEmail(Reservation reservation) {
        // Email is now configured with real Gmail App Password

        try {
            // Get service details
            ServiceService serviceService = ServiceService.getInstance();
            Service service = serviceService.getById(reservation.getServiceId());

            if (service == null) {
                System.err.println("Erreur: Service non trouvé pour l'ID " + reservation.getServiceId());
                return;
            }

            // Configure mail session
            Properties props = getMailProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@curavita.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADMIN_EMAIL));
            message.setSubject("Reservation Confirmation - CuraVita");

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = reservation.getDateRendezVous().format(formatter);

            // Create HTML email body
            String htmlBody = "<html><body>" +
                "<h2>New Reservation Created</h2>" +
                "<p><strong>Reservation ID:</strong> #" + reservation.getId() + "</p>" +
                "<p><strong>Client:</strong> " + reservation.getNomClient() + "</p>" +
                "<p><strong>Service:</strong> " + service.getNom() + " (" + service.getType() + ")</p>" +
                "<p><strong>Date:</strong> " + formattedDate + "</p>" +
                "<p><strong>Status:</strong> " + reservation.getStatut() + "</p>" +
                "<p><strong>Motif:</strong> " + reservation.getMotif() + "</p>" +
                "<br>" +
                "<p>Thank you,<br>CuraVita System</p>" +
                "</body></html>";

            message.setContent(htmlBody, "text/html; charset=utf-8");

            // Try to send email with fallback configurations
            if (sendEmailWithFallback(session, message)) {
                System.out.println("✓ Email de confirmation envoyé pour la réservation #" + reservation.getId());
            } else {
                System.err.println("❌ Échec d'envoi d'email pour la réservation #" + reservation.getId());
                System.err.println("   Raison possible: Connexion réseau bloquée (firewall/université/ISP)");
                System.err.println("   Solutions: Vérifier firewall ou utiliser hotspot mobile");
            }

        } catch (Exception e) {
            // Enhanced error handling for different types of failures
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("Connection timed out") || errorMessage.contains("connect")) {
                    System.err.println("🚫 Erreur réseau SMTP: Connexion impossible à smtp.gmail.com");
                    System.err.println("   Cause probable: Firewall, réseau universitaire, ou ISP bloque le port SMTP");
                    System.err.println("   Solution: Vérifier les paramètres réseau ou utiliser une connexion différente");
                } else if (errorMessage.contains("Authentication failed") || errorMessage.contains("535")) {
                    System.err.println("🔐 Erreur d'authentification Gmail: Vérifier l'App Password");
                    System.err.println("   Solution: Régénérer l'App Password Gmail (voir GMAIL_APP_PASSWORD_SETUP.md)");
                } else if (errorMessage.contains("Invalid Addresses")) {
                    System.err.println("📧 Erreur d'adresse email: Vérifier l'adresse destinataire");
                } else {
                    System.err.println("❌ Erreur email inattendue: " + errorMessage);
                }
            } else {
                System.err.println("❌ Erreur email inconnue: " + e.getClass().getSimpleName());
            }

            // Don't throw exception to avoid breaking reservation flow
            System.err.println("ℹ️ La réservation a été enregistrée malgré l'échec d'email");
        }
    }

    /**
     * Send patient confirmation email after reservation creation
     */
    public void sendPatientConfirmation(Reservation reservation) {
        try {
            // Get service details
            ServiceService serviceService = ServiceService.getInstance();
            Service service = serviceService.getById(reservation.getServiceId());

            if (service == null) {
                System.err.println("Erreur: Service non trouvé pour l'ID " + reservation.getServiceId());
                return;
            }

            // Configure mail session
            Properties props = getMailProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@curavita.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getEmailClient()));
            message.setSubject("Your Reservation Request - CuraVita");

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = reservation.getDateRendezVous().format(formatter);

            // Create HTML email body
            String htmlBody = "<html><body>" +
                "<h2>Your Reservation Request Received</h2>" +
                "<p>Dear " + reservation.getNomClient() + ",</p>" +
                "<p>Your reservation request has been received and is currently <strong>" + reservation.getStatut() + "</strong>.</p>" +
                "<p><strong>Reservation ID:</strong> #" + reservation.getId() + "</p>" +
                "<p><strong>Service:</strong> " + service.getNom() + " (" + service.getType() + ")</p>" +
                "<p><strong>Date:</strong> " + formattedDate + "</p>" +
                "<p><strong>Motif:</strong> " + reservation.getMotif() + "</p>" +
                "<br>" +
                "<p>You will receive an email notification once your reservation is reviewed by our team.</p>" +
                "<br>" +
                "<p>Thank you,<br>CuraVita Team</p>" +
                "</body></html>";

            message.setContent(htmlBody, "text/html; charset=utf-8");

            // Send email
            if (sendEmailWithFallback(session, message)) {
                System.out.println("✓ Patient confirmation email sent for reservation #" + reservation.getId());
            } else {
                System.err.println("❌ Failed to send patient confirmation email for reservation #" + reservation.getId());
            }

        } catch (Exception e) {
            System.err.println("❌ Error sending patient confirmation email: " + e.getMessage());
        }
    }

    /**
     * Send status update email to patient after admin decision
     */
    public void sendStatusEmail(Reservation reservation) {
        try {
            // Get service details
            ServiceService serviceService = ServiceService.getInstance();
            Service service = serviceService.getById(reservation.getServiceId());

            if (service == null) {
                System.err.println("Erreur: Service non trouvé pour l'ID " + reservation.getServiceId());
                return;
            }

            // Configure mail session
            Properties props = getMailProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@curavita.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getEmailClient()));
            message.setSubject("Your Reservation Status - CuraVita");

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = reservation.getDateRendezVous().format(formatter);

            // Create HTML email body
            String statusMessage = "";
            if ("CONFIRMED".equals(reservation.getStatut())) {
                statusMessage = "<p style='color: green; font-weight: bold;'>Your reservation has been CONFIRMED!</p>";
            } else if ("REJECTED".equals(reservation.getStatut())) {
                statusMessage = "<p style='color: red; font-weight: bold;'>Unfortunately, your reservation has been REJECTED.</p>";
            }

            String htmlBody = "<html><body>" +
                "<h2>Reservation Status Update</h2>" +
                "<p>Dear " + reservation.getNomClient() + ",</p>" +
                statusMessage +
                "<p><strong>Reservation ID:</strong> #" + reservation.getId() + "</p>" +
                "<p><strong>Service:</strong> " + service.getNom() + " (" + service.getType() + ")</p>" +
                "<p><strong>Date:</strong> " + formattedDate + "</p>" +
                "<p><strong>Status:</strong> " + reservation.getStatut() + "</p>" +
                "<br>" +
                "<p>If you have any questions, please contact us.</p>" +
                "<br>" +
                "<p>Thank you,<br>CuraVita Team</p>" +
                "</body></html>";

            message.setContent(htmlBody, "text/html; charset=utf-8");

            // Send email
            if (sendEmailWithFallback(session, message)) {
                System.out.println("✓ Status update email sent for reservation #" + reservation.getId());
            } else {
                System.err.println("❌ Failed to send status update email for reservation #" + reservation.getId());
            }

        } catch (Exception e) {
            System.err.println("❌ Error sending status update email: " + e.getMessage());
        }
    }

    /**
     * Try to send email with fallback configurations
     */
    private boolean sendEmailWithFallback(Session session, Message message) {
        // Try primary configuration (STARTTLS on port 587)
        try {
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("Primary SMTP configuration failed: " + e.getMessage());

            // Try fallback configuration (SSL on port 465)
            try {
                Properties fallbackProps = new Properties();
                fallbackProps.put("mail.smtp.auth", "true");
                fallbackProps.put("mail.smtp.socketFactory.port", "465");
                fallbackProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                fallbackProps.put("mail.smtp.host", "smtp.gmail.com");
                fallbackProps.put("mail.smtp.port", "465");
                fallbackProps.put("mail.smtp.connectiontimeout", "10000");
                fallbackProps.put("mail.smtp.timeout", "10000");

                Session fallbackSession = Session.getInstance(fallbackProps, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
                    }
                });

                Message fallbackMessage = new MimeMessage(fallbackSession);
                fallbackMessage.setFrom(message.getFrom()[0]);
                fallbackMessage.setRecipients(Message.RecipientType.TO, message.getRecipients(Message.RecipientType.TO));
                fallbackMessage.setSubject(message.getSubject());
                try {
                    fallbackMessage.setContent(message.getContent(), message.getContentType());
                } catch (java.io.IOException ioException) {
                    System.err.println("Error copying message content: " + ioException.getMessage());
                    return false;
                }

                Transport.send(fallbackMessage);
                System.out.println("Email sent successfully using SSL fallback configuration");
                return true;

            } catch (MessagingException fallbackException) {
                System.err.println("Fallback SMTP configuration also failed: " + fallbackException.getMessage());
                return false;
            }
        }
    }
}
