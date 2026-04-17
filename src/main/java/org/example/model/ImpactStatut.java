package org.example.model;

public enum ImpactStatut {
    AUCUN("aucun", "Aucun"),
    EN_COURS("en_cours", "En cours"),
    RESOLU("resolu", "Resolu"),
    FERME("ferme", "Ferme");

    private final String dbValue;
    private final String label;

    ImpactStatut(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static ImpactStatut fromDb(String value) {
        for (ImpactStatut statut : values()) {
            if (statut.dbValue.equalsIgnoreCase(value)) {
                return statut;
            }
        }
        return null;
    }
}

