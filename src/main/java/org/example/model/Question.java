package org.example.model;

public class Question {
    private int id;
    private String typeTicket;
    private String objet;
    private String description;
    private String priorite;
    private String statut;
    private String fileName;
    private String filePath;
    private String fileType;
    private Integer fileSize;
    private java.time.LocalDateTime createdAt;
    private Integer utilisateurId;
    private String utilisateurNom;
    private String utilisateurEmail;

    public Question() {
    }

    public Question(int id, String objet) {
        this.id = id;
        this.objet = objet;
    }

    public Question(int id, String objet, String typeTicket, String priorite, String statut, java.time.LocalDateTime createdAt) {
        this.id = id;
        this.objet = objet;
        this.typeTicket = typeTicket;
        this.priorite = priorite;
        this.statut = statut;
        this.createdAt = createdAt;
    }

    // ---- getters/setters ----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getTypeTicket() {
        return typeTicket;
    }

    public void setTypeTicket(String typeTicket) {
        this.typeTicket = typeTicket;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Integer utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getUtilisateurNom() {
        return utilisateurNom;
    }

    public void setUtilisateurNom(String utilisateurNom) {
        this.utilisateurNom = utilisateurNom;
    }

    public String getUtilisateurEmail() {
        return utilisateurEmail;
    }

    public void setUtilisateurEmail(String utilisateurEmail) {
        this.utilisateurEmail = utilisateurEmail;
    }

    /**
     * Affichage côté UI.
     * Exemple: "Prenom Nom" ou "Prenom Nom (email)".
     */
    public String getUtilisateurDisplay() {
        String name = utilisateurNom != null ? utilisateurNom.trim() : "";
        String email = utilisateurEmail != null ? utilisateurEmail.trim() : "";
        if (!name.isBlank() && !email.isBlank()) {
            return name + " (" + email + ")";
        }
        if (!name.isBlank()) {
            return name;
        }
        return email;
    }
}
