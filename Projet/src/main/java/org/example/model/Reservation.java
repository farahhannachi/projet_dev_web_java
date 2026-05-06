package org.example.model;

import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private int serviceId;
    private String nomClient;
    private String emailClient;
    private String telephoneClient;
    private LocalDateTime dateReservation;
    private LocalDateTime dateRendezVous;
    private String motif;
    private String statut; // "PENDING", "CONFIRMED", "REJECTED"
    private LocalDateTime dateCreation;

    public Reservation() {}

    public Reservation(int id, int serviceId, String nomClient, String emailClient, String telephoneClient,
                      LocalDateTime dateReservation, LocalDateTime dateRendezVous, String motif,
                      String statut, LocalDateTime dateCreation) {
        this.id = id;
        this.serviceId = serviceId;
        this.nomClient = nomClient;
        this.emailClient = emailClient;
        this.telephoneClient = telephoneClient;
        this.dateReservation = dateReservation;
        this.dateRendezVous = dateRendezVous;
        this.motif = motif;
        this.statut = statut;
        this.dateCreation = dateCreation;
    }

    public Reservation(int serviceId, String nomClient, String emailClient, String telephoneClient,
                      LocalDateTime dateReservation, String motif) {
        this(0, serviceId, nomClient, emailClient, telephoneClient, dateReservation, dateReservation,
             motif, "PENDING", LocalDateTime.now());
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }

    public String getEmailClient() { return emailClient; }
    public void setEmailClient(String emailClient) { this.emailClient = emailClient; }

    public String getTelephoneClient() { return telephoneClient; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public LocalDateTime getDateRendezVous() { return dateRendezVous; }
    public void setDateRendezVous(LocalDateTime dateRendezVous) { this.dateRendezVous = dateRendezVous; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "Réservation{" +
                "id=" + id +
                ", nomClient='" + nomClient + '\'' +
                ", dateRendezVous=" + dateRendezVous +
                ", statut='" + statut + '\'' +
                '}';
    }
}
