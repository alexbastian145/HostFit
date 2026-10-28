package com.hostfit.core;

import java.math.BigDecimal;
import java.util.Objects;

/** The user's hosting requirements (Module 1 output). */
public record Requirement(UseCase useCase, long monthlyVisits, int storageGb, BigDecimal budgetInr, Expertise expertise) {

    public Requirement {
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(budgetInr, "budgetInr");
        Objects.requireNonNull(expertise, "expertise");
        if (monthlyVisits < 1) {
            throw new IllegalArgumentException("monthlyVisits must be >= 1");
        }
        if (storageGb < 1) {
            throw new IllegalArgumentException("storageGb must be >= 1");
        }
        if (budgetInr.signum() <= 0) {
            throw new IllegalArgumentException("budgetInr must be > 0");
        }
    }
}
