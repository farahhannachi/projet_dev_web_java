package org.example.model;

public enum ActionType {
    AUCUNE("aucune", "Aucune"),
    REMBOURSEMENT("remboursement", "Remboursement"),
    REMPLACEMENT("remplacement", "Remplacement"),
    RETOUR_ACCEPTE("retour_accepte", "Retour accepte"),
    RETOUR_REFUSE("retour_refuse", "Retour refuse"),
    ESCALADE("escalade", "Escalade");

    private final String dbValue;
    private final String label;

    ActionType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static ActionType fromDb(String value) {
        for (ActionType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}

