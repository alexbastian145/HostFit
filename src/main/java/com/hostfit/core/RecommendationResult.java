package com.hostfit.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Output of {@link MatchingEngine}.
 *
 * @param recommendations    best eligible plans, best first (limited)
 * @param alternatives       plans that only miss the budget (within the alternative margin), cheapest first
 * @param minimumBudgetNeeded cheapest price of any plan meeting all non-budget requirements; null if none exists
 * @param evaluations        evaluation of every plan keyed by plan id (used by the compare view)
 */
public record RecommendationResult(
        ResourceNeed need,
        List<Recommendation> recommendations,
        List<Alternative> alternatives,
        BigDecimal minimumBudgetNeeded,
        Map<Long, Evaluation> evaluations) {
}
