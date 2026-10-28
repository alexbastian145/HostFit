package com.hostfit.core;

/** How much server administration the user is willing to do. */
public enum Expertise {
    MANAGED("Managed", "The provider handles updates, security and backups for me."),
    SELF_MANAGED("Self-managed", "I'm comfortable running and securing my own server.");

    private final String label;
    private final String description;

    Expertise(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
