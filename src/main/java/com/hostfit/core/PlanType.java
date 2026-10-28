package com.hostfit.core;

/** Hosting product tiers used throughout HostFit. */
public enum PlanType {
    SHARED("Shared", "Many sites share one server. Cheapest, least control."),
    VPS("VPS", "A virtual slice of a server with guaranteed RAM and vCPU."),
    CLOUD("Cloud", "Scalable / dedicated-CPU cloud instances or managed cloud stacks."),
    DEDICATED("Dedicated", "A whole physical server for your workload.");

    private final String label;
    private final String description;

    PlanType(String label, String description) {
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
