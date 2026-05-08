package org.example.model;

public enum AuteurType {
    CLIENT("client", "Client"),
    AGENT("agent", "Agent"),
    BOT("bot", "Bot");

    private final String dbValue;
    private final String label;

    AuteurType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static AuteurType fromDb(String value) {
        for (AuteurType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}

