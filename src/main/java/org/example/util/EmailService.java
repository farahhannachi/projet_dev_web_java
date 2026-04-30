package org.example.util; // Package "util" — services transversaux

// Bibliothèque Jakarta Mail (anciennement JavaMail) pour l'envoi d'emails
import jakarta.mail.*;           // Classes de base : Session, Message, Transport, Authenticator
import jakarta.mail.internet.*; // Classes pour les emails Internet : MimeMessage, InternetAddress
import java.util.Properties;    // Classe Java standard pour stocker des propriétés clé=valeur

/**
 * EmailService — Service d'envoi d'emails via SMTP (Gmail).
 *
 * Rôle : envoyer des emails HTML aux patients (confirmation de traitement,
 * notification d'ordonnance, rappels de prise de médicaments...).
 *
 * Protocole utilisé : SMTP avec STARTTLS
 *   - SMTP (Simple Mail Transfer Protocol) : protocole standard d'envoi d'emails
 *   - STARTTLS : extension qui chiffre la connexion SMTP (port 587)
 *   - Différent de SSL/TLS direct (port 465) : STARTTLS commence en clair puis chiffre
 *
 * Authentification : mot de passe d'application Gmail
 *   (pas le mot de passe principal du compte, mais un mot de passe généré spécialement
 *   pour les applications tierces — plus sécurisé)
 *
 * Envoi asynchrone : dans un Thread séparé pour ne pas bloquer l'interface JavaFX.
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class EmailService {

    // Instance unique (Singleton)
    private static EmailService instance;

    // ── Configuration SMTP ────────────────────────────────────────────────
    // Serveur SMTP de Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";

    // Port 587 : port standard SMTP avec STARTTLS (chiffrement opportuniste)
    private static final int SMTP_PORT = 587;

    // Email de l'expéditeur (compte Gmail configuré pour l'application)
    private static final String SENDER_EMAIL = "emnabenaissa554@gmail.com";

    // Mot de passe d'application Gmail (16 caractères, généré dans les paramètres Google)
    // Format : "xxxx xxxx xxxx xxxx" (avec espaces pour la lisibilité)
    private static final String SENDER_PASS = "bzqe ynyj qikb ivoz";

    // Nom affiché comme expéditeur dans la boîte mail du destinataire
    private static final String SENDER_NAME = "CuraVita Pharmacie";

    // Constructeur privé
    private EmailService() {}

    /**
     * Retourne l'instance unique. La crée si elle n'existe pas encore.
     */
    public static EmailService getInstance() {
        if (instance == null) instance = new EmailService();
        return instance;
    }

    /**
     * Envoie un email HTML au destinataire de façon asynchrone.
     *
     * Asynchrone = dans un Thread séparé → l'interface JavaFX ne se bloque pas
     * pendant l'envoi (qui peut prendre 1-3 secondes selon la connexion).
     *
     * @param toEmail  Adresse email du destinataire (ex: "patient@gmail.com")
     * @param subject  Sujet de l'email (ex: "Votre ordonnance CuraVita")
     * @param htmlBody Contenu HTML de l'email (peut contenir des balises <b>, <p>, etc.)
     */
    public void send(String toEmail, String subject, String htmlBody) {
        // Créer un nouveau Thread pour l'envoi (non-bloquant pour l'UI)
        new Thread(() -> {
            try {
                // ── Configurer les propriétés SMTP ────────────────────────
                Properties props = new Properties();

                // Activer l'authentification SMTP (login/mot de passe requis)
                props.put("mail.smtp.auth", "true");

                // Activer STARTTLS : la connexion commence en clair puis est chiffrée
                props.put("mail.smtp.starttls.enable", "true");

                // Adresse du serveur SMTP
                props.put("mail.smtp.host", SMTP_HOST);

                // Port SMTP (587 pour STARTTLS)
                props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

                // Faire confiance au certificat SSL de Gmail (évite les erreurs de certificat)
                props.put("mail.smtp.ssl.trust", SMTP_HOST);

                // ── Créer la session SMTP avec authentification ───────────
                // Session.getInstance() : crée une session avec les propriétés et l'authentificateur
                // Authenticator : classe anonyme qui fournit les credentials (email + mot de passe)
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        // Retourner les credentials pour l'authentification SMTP
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
                    }
                });

                // ── Construire le message email ───────────────────────────
                Message message = new MimeMessage(session); // Créer un message MIME (format email standard)

                // Expéditeur : "CuraVita Pharmacie <emnabenaissa554@gmail.com>"
                message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));

                // Destinataire : InternetAddress.parse() accepte "email@domain.com"
                // Message.RecipientType.TO : destinataire principal (pas CC ni BCC)
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));

                // Sujet de l'email
                message.setSubject(subject);

                // Corps de l'email en HTML avec charset UTF-8 (pour les accents)
                // "text/html; charset=UTF-8" : indique au client mail que c'est du HTML
                message.setContent(htmlBody, "text/html; charset=UTF-8");

                // ── Envoyer l'email ───────────────────────────────────────
                // Transport.send() : se connecte au serveur SMTP et envoie le message
                Transport.send(message);

                System.out.println("[Email] Envoyé à " + toEmail); // Log de succès

            } catch (Exception e) {
                // Log l'erreur sans planter l'application
                System.err.println("[Email] Erreur envoi : " + e.getMessage());
            }
        }).start(); // Démarrer le thread immédiatement
    }
}
