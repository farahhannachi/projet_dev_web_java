package org.example.model;

public enum ReponseRole {
    QUESTION("question", "Question"),
    INFO("info", "Info"),
    DEMANDE_PREUVE("demande_preuve", "Demande preuve"),
    SOLUTION("solution", "Solution"),
    DECISION("decision", "Decision");

    private final String dbValue;
    private final String label;

    ReponseRole(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static ReponseRole fromDb(String value) {
        for (ReponseRole role : values()) {
            if (role.dbValue.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }
}

