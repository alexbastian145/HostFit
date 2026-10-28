package com.hostfit.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A plan that satisfies every hard requirement, with its match score.
 *
 * @param score     0-100 match score
 * @param breakdown points earned per scoring component (sums to {@code score}, up to rounding)
 * @param reasons   short human-readable explanations
 * @param headroom  geometric mean of plan resources / required resources (1.0 = exactly enough)
 */
public record Recommendation(
        PlanSpec plan,
        int score,
        Map<String, Integer> breakdown,
        List<String> reasons,
        double headroom,
        BigDecimal yearlyCost,
        BigDecimal costPer1kVisits) {
}
