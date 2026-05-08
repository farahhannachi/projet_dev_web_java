package org.example.model;

import java.time.LocalDateTime;

public class ResponseQuestion {
    private int id;
    private int questionId;
    private String questionObjet;
    private Integer utilisateurId;
    private String utilisateurNom;
    private AuteurType auteurType;
    private ReponseRole reponseRole;
    private ActionType actionType;
    private ImpactStatut impactStatut;
    private String reponseText;
    private boolean luParClient;
    private String fileName;
    private String filePath;
    private String fileType;
    private Integer fileSize;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionObjet() {
        return questionObjet;
    }

    public void setQuestionObjet(String questionObjet) {
        this.questionObjet = questionObjet;
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

    public AuteurType getAuteurType() {
        return auteurType;
    }

    public void setAuteurType(AuteurType auteurType) {
        this.auteurType = auteurType;
    }

    public ReponseRole getReponseRole() {
        return reponseRole;
    }

    public void setReponseRole(ReponseRole reponseRole) {
        this.reponseRole = reponseRole;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public ImpactStatut getImpactStatut() {
        return impactStatut;
    }

    public void setImpactStatut(ImpactStatut impactStatut) {
        this.impactStatut = impactStatut;
    }

    public String getReponseText() {
        return reponseText;
    }

    public void setReponseText(String reponseText) {
        this.reponseText = reponseText;
    }

    public boolean isLuParClient() {
        return luParClient;
    }

    public void setLuParClient(boolean luParClient) {
        this.luParClient = luParClient;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getShortReponseText() {
        if (reponseText == null) {
            return "";
        }
        if (reponseText.length() <= 45) {
            return reponseText;
        }
        return reponseText.substring(0, 42) + "...";
    }
}

