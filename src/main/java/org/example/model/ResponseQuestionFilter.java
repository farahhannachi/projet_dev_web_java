package org.example.model;

public class ResponseQuestionFilter {
    private Integer questionId;
    private AuteurType auteurType;
    private ReponseRole reponseRole;
    private ActionType actionType;
    private ImpactStatut impactStatut;
    private Boolean luParClient;
    private String searchQuery;

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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

    public Boolean getLuParClient() {
        return luParClient;
    }

    public void setLuParClient(Boolean luParClient) {
        this.luParClient = luParClient;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
}
