package org.example.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.model.Commande;
import org.example.model.Reservation;
import org.example.model.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MailerService {
    private String smtpHost;
    private String smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String fromEmail;
    private String fromName;
    private String adminEmail;
    private String lastError;

    public MailerService() {
        Map<String, String> fileEnv = loadDotEnv();

        smtpHost = pick("SMTP_HOST", fileEnv, "smtp.gmail.com");
        smtpPort = pick("SMTP_PORT", fileEnv, "587");
        smtpUser = pick("SMTP_USER", fileEnv, "");
        smtpPassword = pick("SMTP_PASS", fileEnv, "");
        fromEmail = pick("MAIL_FROM", fileEnv, "curavita123@gmail.com");
        fromName = pick("MAIL_FROM_NAME", fileEnv, "CURAVITA Support");
        adminEmail = pick("MAIL_ADMIN", fileEnv, "curavita123@gmail.com");

        // Optional MAILER_DSN from .env: prefer MAILER_DSN_Yassine when present, then MAILER_DSN.
        String dsn = resolveMailerDsn(fileEnv);
        if (dsn != null && !dsn.isBlank() && !"null://null".equalsIgnoreCase(dsn)) {
            applyMailerDsn(dsn);
        }

        if ((fromEmail == null || fromEmail.isBlank()) && smtpUser != null && !smtpUser.isBlank()) {
            fromEmail = smtpUser;
        }
        if ((adminEmail == null || adminEmail.isBlank()) && smtpUser != null && !smtpUser.isBlank()) {
            adminEmail = smtpUser;
        }
    }

    public boolean canSend() {
        return !smtpHost.isBlank() && !smtpPort.isBlank() && !smtpUser.isBlank() && !smtpPassword.isBlank();
    }

    public String getConfigurationStatus() {
        if (canSend()) {
            return "OK";
        }
        return "SMTP non configure. Definir SMTP_USER/SMTP_PASS (ou MAILER_DSN smtp://...)";
    }

    public String getLastError() {
        return lastError;
    }

    public void clearLastError() {
        lastError = null;
    }

    public void sendTicketCreatedEmail(String toEmail, String userName, int ticketId) {
        String subject = "Ticket #" + ticketId + " cree - CURAVITA";
        String content = "<p style=\"margin:0 0 14px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
            + safe(userName)
            + "</strong>, votre demande de support a bien ete enregistree.</p>"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0fdf4;border:1px solid #d1fae5;border-radius:12px;margin:12px 0 18px;\">"
            + "<tr><td style=\"padding:16px;color:#14532d;font-size:14px;\"><strong>Numero du ticket:</strong> #"
            + ticketId
            + "</td></tr></table>"
            + "<p style=\"margin:0;color:#6b7280;font-size:14px;line-height:1.6;\">Notre equipe reviendra vers vous rapidement.</p>";
        String html = buildMailLayout("Ticket cree", "Ticket cree avec succes", content);
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendTicketResponseEmail(String toEmail, String userName, int ticketId, String responseText) {
        String subject = "Nouvelle reponse a votre ticket #" + ticketId + " - CURAVITA";
        String content = "<p style=\"margin:0 0 14px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
            + safe(userName)
            + "</strong>, nous avons ajoute une nouvelle reponse a votre ticket #"
            + ticketId
            + ".</p>"
            + "<div style=\"background:#f9fafb;border:1px solid #e5e7eb;border-radius:12px;padding:14px 16px;margin:10px 0 18px;\">"
            + "<p style=\"margin:0;color:#1f2937;font-size:14px;line-height:1.6;\">"
            + safe(responseText)
            + "</p></div>"
            + "<p style=\"margin:0;color:#6b7280;font-size:14px;\">Merci pour votre confiance.</p>";
        String html = buildMailLayout("Reponse support", "Mise a jour de votre ticket", content);
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendCommandeInvoiceEmail(Commande commande, List<FrontPanierService.CartItem> items) {
        if (commande.getEmail() == null || commande.getEmail().isBlank()) {
            return;
        }

        StringBuilder lines = new StringBuilder("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;margin-top:8px;\">")
            .append("<thead><tr>")
            .append("<th style=\"text-align:left;background:#f8fafc;padding:10px 12px;border-bottom:1px solid #e5e7eb;color:#334155;font-size:12px;\">Produit</th>")
            .append("<th style=\"text-align:right;background:#f8fafc;padding:10px 12px;border-bottom:1px solid #e5e7eb;color:#334155;font-size:12px;\">Qt</th>")
            .append("<th style=\"text-align:right;background:#f8fafc;padding:10px 12px;border-bottom:1px solid #e5e7eb;color:#334155;font-size:12px;\">Total</th>")
            .append("</tr></thead><tbody>");
        if (items != null) {
            for (FrontPanierService.CartItem item : items) {
            lines.append("<tr>")
                .append("<td style=\"padding:10px 12px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\">")
                        .append(safe(item.getNom()))
                .append("</td>")
                .append("<td style=\"padding:10px 12px;border-bottom:1px solid #eef2f7;text-align:right;color:#374151;font-size:14px;\">")
                        .append(item.getQuantite())
                .append("</td>")
                .append("<td style=\"padding:10px 12px;border-bottom:1px solid #eef2f7;text-align:right;color:#111827;font-size:14px;\">")
                .append(String.format("%.2f DT", item.getTotalLigne()))
                .append("</td></tr>");
            }
        }
        lines.append("</tbody></table>");

        String subject = "Facture commande #" + commande.getId() + " - CURAVITA";
        String content = "<p style=\"margin:0 0 10px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
            + safe(commande.getNom())
            + "</strong>, votre commande a ete enregistree.</p>"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f8fafc;border:1px solid #e5e7eb;border-radius:12px;margin:10px 0 14px;\">"
            + "<tr><td style=\"padding:10px 14px;color:#334155;font-size:14px;\"><strong>Commande:</strong> #" + commande.getId() + "</td></tr>"
            + "<tr><td style=\"padding:0 14px 10px;color:#334155;font-size:14px;\"><strong>Paiement:</strong> " + safe(commande.getModePaiement()) + "</td></tr>"
            + "<tr><td style=\"padding:0 14px 14px;color:#334155;font-size:14px;\"><strong>Livraison:</strong> " + safe(commande.getAdresseLivraison()) + "</td></tr>"
            + "</table>"
                + lines
            + "<p style=\"margin:16px 0 0;text-align:right;color:#111827;font-size:18px;\"><strong>Total: "
            + String.format("%.2f DT", commande.getTotal())
            + "</strong></p>";

        String html = buildMailLayout("Facture commande", "Confirmation de commande #" + commande.getId(), content);

        sendHtmlEmail(commande.getEmail(), subject, html);
    }

    public void sendAdminCommandeNotification(Commande commande, List<FrontPanierService.CartItem> items) {
        String subject = "Nouvelle commande #" + commande.getId();
        StringBuilder itemsHtml = new StringBuilder("<ul style=\"margin:8px 0 0;padding-left:18px;color:#374151;\">");
        if (items != null) {
            for (FrontPanierService.CartItem item : items) {
                itemsHtml.append("<li style=\"margin-bottom:6px;\">")
                        .append(safe(item.getNom()))
                        .append(" x")
                        .append(item.getQuantite())
                        .append(" - ")
                        .append(String.format("%.2f DT", item.getTotalLigne()))
                        .append("</li>");
            }
        }
        itemsHtml.append("</ul>");

        String content = "<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;\">Une nouvelle commande vient d'etre enregistree.</p>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #e5e7eb;border-radius:12px;background:#ffffff;\">"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>ID:</strong> #" + commande.getId() + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Client:</strong> " + safe(commande.getNom()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Email:</strong> " + safe(commande.getEmail()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;color:#111827;font-size:14px;\"><strong>Total:</strong> " + String.format("%.2f DT", commande.getTotal()) + "</td></tr>"
                + "</table>"
                + "<p style=\"margin:14px 0 6px;color:#334155;font-size:14px;\"><strong>Produits:</strong></p>"
                + itemsHtml;
        String html = buildMailLayout("Admin commande", "Nouvelle commande #" + commande.getId(), content);
        sendHtmlEmail(adminEmail, subject, html);
    }

    public void sendCommandeStatusUpdateEmail(Commande commande, String oldStatus, String newStatus) {
        if (commande.getEmail() == null || commande.getEmail().isBlank()) {
            return;
        }

        String subject = "Mise a jour de votre commande #" + commande.getId();
        String content = "<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
            + safe(commande.getNom())
            + "</strong>, le statut de votre commande a ete modifie.</p>"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
            + "<tr><td style=\"padding:10px 14px;background:#f8fafc;border-bottom:1px solid #e5e7eb;color:#334155;font-size:14px;\"><strong>Ancien statut</strong></td><td style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;color:#111827;font-size:14px;\">" + safe(label(oldStatus)) + "</td></tr>"
            + "<tr><td style=\"padding:10px 14px;background:#f8fafc;border-bottom:1px solid #e5e7eb;color:#334155;font-size:14px;\"><strong>Nouveau statut</strong></td><td style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;color:#111827;font-size:14px;\">" + safe(label(newStatus)) + "</td></tr>"
            + "<tr><td style=\"padding:10px 14px;background:#f8fafc;color:#334155;font-size:14px;\"><strong>Total</strong></td><td style=\"padding:10px 14px;color:#111827;font-size:14px;\">" + String.format("%.2f DT", commande.getTotal()) + "</td></tr>"
            + "</table>";

        String html = buildMailLayout("Statut commande", "Mise a jour commande #" + commande.getId(), content);

        sendHtmlEmail(commande.getEmail(), subject, html);
    }

    public void sendReservationEmail(Reservation reservation) {
        Service service = ServiceService.getInstance().getById(reservation.getServiceId());
        String subject = "Nouvelle reservation #" + reservation.getId() + " - CURAVITA";
        String content = "<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;\">Une nouvelle reservation a ete enregistree.</p>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #e5e7eb;border-radius:12px;background:#ffffff;\">"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>ID:</strong> #" + reservation.getId() + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Client:</strong> " + safe(reservation.getNomClient()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Email:</strong> " + safe(reservation.getEmailClient()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Service:</strong> " + safe(service == null ? "" : service.getNom()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #eef2f7;color:#111827;font-size:14px;\"><strong>Date:</strong> " + safe(reservation.getDateRendezVous() == null ? "" : reservation.getDateRendezVous().toString()) + "</td></tr>"
                + "<tr><td style=\"padding:10px 14px;color:#111827;font-size:14px;\"><strong>Motif:</strong> " + safe(reservation.getMotif()) + "</td></tr>"
                + "</table>";
        sendHtmlEmail(adminEmail, subject, buildMailLayout("Reservation", "Nouvelle reservation #" + reservation.getId(), content));
    }

    public void sendPatientConfirmation(Reservation reservation) {
        if (reservation.getEmailClient() == null || reservation.getEmailClient().isBlank()) {
            return;
        }

        Service service = ServiceService.getInstance().getById(reservation.getServiceId());
        String subject = "Votre reservation #" + reservation.getId() + " - CURAVITA";
        String content = "<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
                + safe(reservation.getNomClient())
                + "</strong>, votre demande de reservation a bien ete enregistree.</p>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f8fafc;border:1px solid #e5e7eb;border-radius:12px;margin:10px 0 14px;\">"
                + "<tr><td style=\"padding:10px 14px;color:#334155;font-size:14px;\"><strong>Reservation:</strong> #" + reservation.getId() + "</td></tr>"
                + "<tr><td style=\"padding:0 14px 10px;color:#334155;font-size:14px;\"><strong>Service:</strong> " + safe(service == null ? "" : service.getNom()) + "</td></tr>"
                + "<tr><td style=\"padding:0 14px 14px;color:#334155;font-size:14px;\"><strong>Date:</strong> " + safe(reservation.getDateRendezVous() == null ? "" : reservation.getDateRendezVous().toString()) + "</td></tr>"
                + "</table>"
                + "<p style=\"margin:0;color:#6b7280;font-size:14px;\">Vous recevrez une mise a jour apres validation par notre equipe.</p>";
        sendHtmlEmail(reservation.getEmailClient(), subject, buildMailLayout("Reservation", "Demande recue", content));
    }

    public void sendStatusEmail(Reservation reservation) {
        if (reservation.getEmailClient() == null || reservation.getEmailClient().isBlank()) {
            return;
        }

        String subject = "Statut reservation #" + reservation.getId() + " - CURAVITA";
        String content = "<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;line-height:1.6;\">Bonjour <strong>"
                + safe(reservation.getNomClient())
                + "</strong>, le statut de votre reservation est maintenant <strong>"
                + safe(label(reservation.getStatut()))
                + "</strong>.</p>";
        sendHtmlEmail(reservation.getEmailClient(), subject, buildMailLayout("Statut reservation", "Mise a jour reservation #" + reservation.getId(), content));
    }

    public void sendChatbotTranscriptEmail(String toEmail, String userName, List<String> messages) {
        if (toEmail == null || toEmail.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }

        List<String> sanitized = new ArrayList<>();
        for (String message : messages) {
            sanitized.add(safe(message));
        }

        StringBuilder content = new StringBuilder("<p style=\"margin:0 0 12px;color:#4b5563;font-size:15px;\"><strong>Utilisateur:</strong> ")
                .append(safe(userName))
                .append("</p><ul style=\"margin:0;padding-left:18px;color:#374151;line-height:1.6;\">");
        for (String line : sanitized) {
            content.append("<li>").append(line).append("</li>");
        }
        content.append("</ul>");

        String html = buildMailLayout("Transcript chatbot", "Conversation chatbot CURAVITA", content.toString());

        sendHtmlEmail(toEmail, "Transcript chatbot - CURAVITA", html);
        sendHtmlEmail(adminEmail, "Transcript chatbot (admin copy)", html);
    }

    private String buildMailLayout(String eyebrow, String title, String content) {
        return "<!doctype html><html><body style=\"margin:0;padding:0;background:#f4f7fa;font-family:Segoe UI,Tahoma,Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:28px 12px;background:#f4f7fa;\"><tr><td align=\"center\">"
                + "<table width=\"680\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:680px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;\">"
                + "<tr><td style=\"padding:24px 26px;background:linear-gradient(135deg,#16563f 0%,#28a745 100%);\">"
                + "<p style=\"margin:0 0 6px;color:rgba(255,255,255,0.88);font-size:12px;letter-spacing:.4px;text-transform:uppercase;\">" + safe(eyebrow) + "</p>"
                + "<h1 style=\"margin:0;color:#ffffff;font-size:22px;line-height:1.3;\">" + safe(title) + "</h1>"
                + "</td></tr>"
                + "<tr><td style=\"padding:22px 26px;\">" + content + "</td></tr>"
                + "<tr><td style=\"padding:16px 26px;background:#f9fafb;border-top:1px solid #e5e7eb;\">"
                + "<p style=\"margin:0;color:#6b7280;font-size:12px;\">CURAVITA • Votre sante, notre priorite</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!canSend() || to == null || to.isBlank()) {
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (Exception e) {
            // Non-blocking like Symfony: mailing failure should not break main business flow.
            lastError = e.getMessage();
            System.err.println("[MAILER] Echec envoi email: " + e.getMessage());
        }
    }

    private String label(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "en_attente" -> "En attente";
            case "confirmee" -> "Confirmee";
            case "annulee" -> "Annulee";
            case "livree" -> "Livree";
            case "review" -> "En revue anti-fraude";
            case "bloquee" -> "Bloquee";
            default -> status;
        };
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String pick(String key, Map<String, String> fileEnv, String fallback) {
        String sysValue = System.getenv(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue.trim();
        }
        String fileValue = fileEnv.get(key);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }
        return fallback;
    }

    private Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path[] candidates = new Path[] {
                Path.of(System.getProperty("user.dir"), ".env"),
        };

        for (Path path : candidates) {
            if (!Files.exists(path)) {
                continue;
            }
            try {
                for (String rawLine : Files.readAllLines(path)) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    values.putIfAbsent(key, value);
                }
            } catch (IOException ignored) {
                // Ignore malformed/unreadable files and keep env-based config.
            }
        }

        return values;
    }

    private void applyMailerDsn(String dsn) {
        try {
            URI uri = URI.create(dsn);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                smtpHost = uri.getHost();
            }
            if (uri.getPort() > 0) {
                smtpPort = String.valueOf(uri.getPort());
            }

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                smtpUser = decode(parts[0]);
                if (parts.length > 1) {
                    smtpPassword = decode(parts[1]);
                }
            }

            if (fromEmail == null || fromEmail.isBlank()) {
                fromEmail = smtpUser;
            }
            if (adminEmail == null || adminEmail.isBlank()) {
                adminEmail = smtpUser;
            }
        } catch (Exception ignored) {
            // Keep existing values when DSN cannot be parsed.
        }
    }

    private String resolveMailerDsn(Map<String, String> fileEnv) {
        String preferred = pick("MAILER_DSN_Yassine", fileEnv, "");
        if (preferred != null && !preferred.isBlank() && !"null://null".equalsIgnoreCase(preferred)) {
            return preferred;
        }

        String standard = pick("MAILER_DSN", fileEnv, "");
        if (standard != null && !standard.isBlank() && !"null://null".equalsIgnoreCase(standard)) {
            return standard;
        }

        return "";
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }
}
