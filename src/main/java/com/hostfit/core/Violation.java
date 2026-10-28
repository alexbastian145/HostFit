package com.hostfit.core;

/** A hard requirement that a plan fails to meet. */
public enum Violation {
    BUDGET("Over budget"),
    STORAGE("Not enough storage"),
    BANDWIDTH("Not enough bandwidth"),
    CPU("Not enough vCPU"),
    RAM("Not enough RAM"),
    TIER("Plan type unsuitable for this workload");

    private final String label;

    Violation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
