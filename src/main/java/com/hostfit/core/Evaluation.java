package com.hostfit.core;

import java.util.Set;

/**
 * How one plan fared against a requirement. {@code recommendation} is non-null
 * exactly when {@code violations} is empty.
 */
public record Evaluation(PlanSpec plan, Set<Violation> violations, Recommendation recommendation) {

    public boolean eligible() {
        return violations.isEmpty();
    }
}
