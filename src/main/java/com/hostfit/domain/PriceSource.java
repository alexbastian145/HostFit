package com.hostfit.domain;

/** Where a plan's current price came from. */
public enum PriceSource {
    SEED("Estimate"),
    MANUAL("Admin entered"),
    CRAWLED("Crawled");

    private final String label;

    PriceSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
