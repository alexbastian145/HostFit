package com.hostfit.core;

import java.util.List;
import java.util.Set;

/**
 * Estimated minimum resources for a {@link Requirement}, plus the plan tiers
 * that suit it. This is the "preliminary capacity guidance" shown to the user.
 */
public record ResourceNeed(
        int minVcpu,
        double minRamGb,
        int minStorageGb,
        double minBandwidthGb,
        Set<PlanType> preferredTiers,
        Set<PlanType> acceptableTiers,
        Set<PlanType> excludedTiers,
        List<String> notes) {
}
