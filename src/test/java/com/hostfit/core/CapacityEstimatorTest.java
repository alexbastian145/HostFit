package com.hostfit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CapacityEstimatorTest {

    private final CapacityEstimator estimator = new CapacityEstimator();

    private static Requirement req(UseCase useCase, long visits, int storage) {
        return new Requirement(useCase, visits, storage, new BigDecimal("2000"), Expertise.SELF_MANAGED);
    }

    @Test
    void ecommerceExampleFromReview1NeedsTwoVcpuAndFourGbClass() {
        ResourceNeed need = estimator.estimate(req(UseCase.ECOMMERCE, 50_000, 50));

        assertEquals(2, need.minVcpu());
        assertEquals(3.5, need.minRamGb(), 1e-9);
        assertEquals(50, need.minStorageGb());
        assertEquals(458.0, need.minBandwidthGb(), 1e-9);
        assertTrue(need.preferredTiers().contains(PlanType.VPS));
        assertTrue(need.excludedTiers().contains(PlanType.SHARED));
    }

    @Test
    void smallBlogPrefersSharedHosting() {
        ResourceNeed need = estimator.estimate(req(UseCase.BLOG, 10_000, 5));

        assertEquals(1, need.minVcpu());
        assertEquals(1.0, need.minRamGb(), 1e-9);
        assertTrue(need.preferredTiers().contains(PlanType.SHARED));
        assertTrue(need.acceptableTiers().contains(PlanType.VPS));
        assertTrue(need.excludedTiers().isEmpty());
    }

    @Test
    void sharedHostingIsNeverSuitableForBackendsOrDatabases() {
        for (UseCase useCase : new UseCase[] {UseCase.APP_BACKEND, UseCase.DATABASE}) {
            ResourceNeed need = estimator.estimate(req(useCase, 1_000, 5));
            assertTrue(need.excludedTiers().contains(PlanType.SHARED), useCase + " should exclude shared");
            assertFalse(need.preferredTiers().contains(PlanType.SHARED));
            assertFalse(need.acceptableTiers().contains(PlanType.SHARED));
        }
    }

    @Test
    void databaseRamGrowsWithDataSize() {
        ResourceNeed small = estimator.estimate(req(UseCase.DATABASE, 10_000, 10));
        ResourceNeed large = estimator.estimate(req(UseCase.DATABASE, 10_000, 500));

        assertTrue(large.minRamGb() > small.minRamGb());
        assertEquals(2.5, small.minRamGb(), 1e-9); // base 1 + 10k/40k + 10GB*0.1 = 2.25, rounded up to 2.5
    }

    @Test
    void heavyTrafficPrefersCloudOrDedicated() {
        ResourceNeed need = estimator.estimate(req(UseCase.APP_BACKEND, 400_000, 200));

        assertEquals(20, need.minVcpu());
        assertTrue(need.preferredTiers().contains(PlanType.CLOUD));
        assertTrue(need.preferredTiers().contains(PlanType.DEDICATED));
    }

    @Test
    void ramIsRoundedUpToHalfGigabytes() {
        assertEquals(3.5, CapacityEstimator.roundUpToHalf(3.5), 1e-9);
        assertEquals(4.0, CapacityEstimator.roundUpToHalf(3.51), 1e-9);
        assertEquals(0.5, CapacityEstimator.roundUpToHalf(0.1), 1e-9);
    }

    @Test
    void requirementRejectsInvalidValues() {
        BigDecimal budget = new BigDecimal("100");
        assertThrows(IllegalArgumentException.class,
                () -> new Requirement(UseCase.BLOG, 0, 5, budget, Expertise.MANAGED));
        assertThrows(IllegalArgumentException.class,
                () -> new Requirement(UseCase.BLOG, 10, 0, budget, Expertise.MANAGED));
        assertThrows(IllegalArgumentException.class,
                () -> new Requirement(UseCase.BLOG, 10, 5, BigDecimal.ZERO, Expertise.MANAGED));
    }
}
