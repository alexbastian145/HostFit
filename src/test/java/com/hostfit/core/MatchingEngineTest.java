package com.hostfit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchingEngineTest {

    private MatchingEngine engine;
    private List<PlanSpec> catalogue;

    private static PlanSpec plan(long id, String provider, PlanType type, int vcpu, double ram, int storage,
                                 Integer bandwidth, boolean unmetered, String price, boolean managed) {
        return new PlanSpec(id, provider, provider + " " + id, type, vcpu, ram, storage, "SSD",
                bandwidth, unmetered, new BigDecimal(price), managed);
    }

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
        catalogue = new ArrayList<>();
        // Mirrors the example table in the Review 1 deck.
        catalogue.add(plan(1, "Provider A", PlanType.SHARED, 1, 1, 50, null, true, "300", true));
        catalogue.add(plan(2, "Provider B", PlanType.VPS, 2, 4, 80, 3000, false, "1500", false));
        catalogue.add(plan(3, "Provider C", PlanType.CLOUD, 4, 8, 160, 5000, false, "4000", false));
        catalogue.add(plan(4, "Provider D", PlanType.VPS, 2, 4, 80, 3000, false, "1400", true));
        catalogue.add(plan(5, "Provider E", PlanType.VPS, 8, 32, 640, 10000, false, "1900", false));
    }

    private static Requirement ecommerceExample(Expertise expertise) {
        return new Requirement(UseCase.ECOMMERCE, 50_000, 50, new BigDecimal("2000"), expertise);
    }

    @Test
    void reviewExampleRecommendsRightSizedVpsAndExcludesShared() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 5);

        assertFalse(result.recommendations().isEmpty());
        for (Recommendation r : result.recommendations()) {
            assertTrue(r.plan().priceInr().compareTo(new BigDecimal("2000")) <= 0, "within budget");
            assertTrue(r.plan().type() != PlanType.SHARED, "shared excluded for a 50k visit store");
        }
        Recommendation best = result.recommendations().get(0);
        assertEquals(2, best.plan().vcpu());
        assertEquals(4.0, best.plan().ramGb(), 1e-9);
        assertEquals(PlanType.VPS, best.plan().type());
    }

    @Test
    void overProvisionedPlanScoresLowerThanRightSizedOne() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 5);

        Recommendation rightSized = find(result, 2);
        Recommendation oversized = find(result, 5);
        assertTrue(rightSized.score() > oversized.score(),
                "2 vCPU / 4 GB (" + rightSized.score() + ") should beat 8 vCPU / 32 GB (" + oversized.score() + ")");
    }

    @Test
    void managedPreferenceRanksManagedPlanFirstAtSimilarSpecs() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.MANAGED), catalogue, 5);

        Recommendation managed = find(result, 4);
        Recommendation unmanaged = find(result, 2);
        assertTrue(managed.score() > unmanaged.score());
        assertEquals(4, result.recommendations().get(0).plan().id());
    }

    @Test
    void scoresAreBoundedAndBreakdownMatchesTotal() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 10);

        for (Recommendation r : result.recommendations()) {
            assertTrue(r.score() >= 0 && r.score() <= 100);
            int sum = r.breakdown().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(Math.min(100, sum), r.score());
            assertFalse(r.reasons().isEmpty());
            assertEquals(5, r.breakdown().size());
        }
    }

    @Test
    void recommendationsAreSortedByScoreDescending() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 10);

        int previous = Integer.MAX_VALUE;
        for (Recommendation r : result.recommendations()) {
            assertTrue(r.score() <= previous);
            previous = r.score();
        }
    }

    @Test
    void limitCapsNumberOfRecommendations() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 1);

        assertEquals(1, result.recommendations().size());
    }

    @Test
    void whenBudgetIsTooLowReportsMinimumBudgetAndAlternatives() {
        Requirement tight = new Requirement(UseCase.ECOMMERCE, 50_000, 50, new BigDecimal("1200"), Expertise.SELF_MANAGED);

        RecommendationResult result = engine.recommend(tight, catalogue, 5);

        assertTrue(result.recommendations().isEmpty());
        assertEquals(new BigDecimal("1400"), result.minimumBudgetNeeded());
        assertFalse(result.alternatives().isEmpty());
        Alternative cheapest = result.alternatives().get(0);
        assertEquals(4, cheapest.plan().id());
        assertEquals(new BigDecimal("200"), cheapest.overBudgetBy());
    }

    @Test
    void minimumBudgetIsNullWhenSomethingFitsTheBudget() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 5);

        assertNull(result.minimumBudgetNeeded());
    }

    @Test
    void sharedHostingNeverRecommendedForAppBackend() {
        Requirement backend = new Requirement(UseCase.APP_BACKEND, 5_000, 10, new BigDecimal("5000"), Expertise.SELF_MANAGED);

        RecommendationResult result = engine.recommend(backend, catalogue, 10);

        Evaluation shared = result.evaluations().get(1L);
        assertTrue(shared.violations().contains(Violation.TIER));
        assertNull(shared.recommendation());
        for (Recommendation r : result.recommendations()) {
            assertTrue(r.plan().type() != PlanType.SHARED);
        }
    }

    @Test
    void unmeteredBandwidthSatisfiesBandwidthRequirement() {
        Requirement blog = new Requirement(UseCase.BLOG, 50_000, 10, new BigDecimal("1000"), Expertise.MANAGED);
        List<PlanSpec> plans = List.of(
                plan(10, "Unmetered Co", PlanType.SHARED, 1, 1, 50, null, true, "300", true),
                plan(11, "Capped Co", PlanType.SHARED, 1, 1, 50, 10, false, "250", true));

        RecommendationResult result = engine.recommend(blog, plans, 5);

        assertEquals(1, result.recommendations().size());
        assertEquals(10, result.recommendations().get(0).plan().id());
        assertTrue(result.evaluations().get(11L).violations().contains(Violation.BANDWIDTH));
    }

    @Test
    void everyHardConstraintProducesItsViolation() {
        Requirement r = new Requirement(UseCase.ECOMMERCE, 50_000, 500, new BigDecimal("100"), Expertise.SELF_MANAGED);
        ResourceNeed need = new CapacityEstimator().estimate(r);
        PlanSpec weak = plan(20, "Weak", PlanType.SHARED, 1, 1, 10, 5, false, "999", false);

        Set<Violation> v = engine.violations(r, need, weak);

        assertEquals(Set.of(Violation.BUDGET, Violation.STORAGE, Violation.BANDWIDTH, Violation.CPU,
                Violation.RAM, Violation.TIER), v);
    }

    @Test
    void emptyCatalogueGivesEmptyResult() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.MANAGED), List.of(), 5);

        assertTrue(result.recommendations().isEmpty());
        assertTrue(result.alternatives().isEmpty());
        assertNull(result.minimumBudgetNeeded());
        assertNotNull(result.need());
    }

    @Test
    void costEstimatesAreDerivedFromMonthlyPrice() {
        RecommendationResult result = engine.recommend(ecommerceExample(Expertise.SELF_MANAGED), catalogue, 5);

        Recommendation r = find(result, 2);
        assertEquals(new BigDecimal("18000"), r.yearlyCost());
        assertEquals(new BigDecimal("30.00"), r.costPer1kVisits());
    }

    @Test
    void fitScoreShape() {
        assertEquals(0.75, MatchingEngine.fitScore(1.0), 1e-9);
        assertEquals(1.0, MatchingEngine.fitScore(1.5), 1e-9);
        assertEquals(0.5, MatchingEngine.fitScore(5.0), 1e-9);
        assertEquals(0.0, MatchingEngine.fitScore(20.0), 1e-9);
    }

    private static Recommendation find(RecommendationResult result, long id) {
        Evaluation e = result.evaluations().get(id);
        assertNotNull(e, "plan " + id + " evaluated");
        assertNotNull(e.recommendation(), "plan " + id + " should be eligible but had " + e.violations());
        return e.recommendation();
    }
}
