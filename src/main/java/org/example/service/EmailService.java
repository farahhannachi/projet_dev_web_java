package org.example.service;

import org.example.model.Reservation;

public class EmailService {
    private static EmailService instance;
    private final MailerService mailerService = new MailerService();

    private EmailService() {
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public void sendReservationEmail(Reservation reservation) {
        sendIfConfigured(() -> mailerService.sendReservationEmail(reservation));
    }

    public void sendPatientConfirmation(Reservation reservation) {
        sendIfConfigured(() -> mailerService.sendPatientConfirmation(reservation));
    }

    public void sendStatusEmail(Reservation reservation) {
        sendIfConfigured(() -> mailerService.sendStatusEmail(reservation));
    }

    private void sendIfConfigured(Runnable mailAction) {
        if (!mailerService.canSend()) {
            System.out.println("Email non envoye: SMTP non configure.");
            return;
        }
        mailAction.run();
    }
}
