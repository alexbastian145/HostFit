package com.hostfit.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a {@link Requirement} into a {@link ResourceNeed} using transparent
 * rule-of-thumb formulas defined by the {@link UseCase} workload profile.
 */
public class CapacityEstimator {

    /** Extra bandwidth for bots, spikes and backups. */
    static final double BANDWIDTH_HEADROOM = 1.25;
    /** Largest storage-based RAM allowance for database workloads. */
    static final double DB_RAM_CAP_GB = 64;
    /** Database RAM allowance as a fraction of data size. */
    static final double DB_RAM_PER_STORAGE_GB = 0.1;

    public ResourceNeed estimate(Requirement req) {
        UseCase u = req.useCase();
        long visits = req.monthlyVisits();

        int vcpu = (int) Math.max(1, Math.ceil(visits / (double) u.getVisitsPerVcpu()));

        double ram = u.getBaseRamGb() + visits / (double) u.getVisitsPerGbRam();
        if (u == UseCase.DATABASE) {
            ram += Math.min(DB_RAM_CAP_GB, req.storageGb() * DB_RAM_PER_STORAGE_GB);
        }
        ram = roundUpToHalf(ram);

        double bandwidth = visits * u.getPagesPerVisit() * u.getPageWeightMb() / 1024.0 * BANDWIDTH_HEADROOM;
        bandwidth = Math.ceil(bandwidth);

        Set<PlanType> excluded = EnumSet.noneOf(PlanType.class);
        boolean sharedAllowed = u.getSharedVisitCeiling() > 0 && visits <= u.getSharedVisitCeiling();
        if (!sharedAllowed) {
            excluded.add(PlanType.SHARED);
        }

        Set<PlanType> preferred = EnumSet.noneOf(PlanType.class);
        Set<PlanType> acceptable = EnumSet.noneOf(PlanType.class);
        boolean light = sharedAllowed && vcpu <= 1 && ram <= 2;

        if (vcpu >= 8 || ram >= 32) {
            preferred.add(PlanType.CLOUD);
            preferred.add(PlanType.DEDICATED);
            acceptable.add(PlanType.VPS);
        } else if (vcpu >= 4 || ram >= 8) {
            preferred.add(PlanType.CLOUD);
            preferred.add(PlanType.VPS);
            acceptable.add(PlanType.DEDICATED);
        } else if (light) {
            preferred.add(PlanType.SHARED);
            acceptable.add(PlanType.VPS);
        } else {
            preferred.add(PlanType.VPS);
            acceptable.add(PlanType.CLOUD);
            acceptable.add(PlanType.SHARED);
        }
        preferred.removeAll(excluded);
        acceptable.removeAll(excluded);

        List<String> notes = new ArrayList<>();
        notes.add(String.format(Locale.ROOT, "%,d visits/month ≈ %,.0f %s per month.",
                visits, visits * u.getPagesPerVisit(), u.getPagesPerVisit() > 5 ? "requests" : "page views"));
        notes.add(String.format(Locale.ROOT, "Estimated transfer ≈ %,.0f GB/month (includes 25%% headroom for bots and spikes).", bandwidth));
        if (excluded.contains(PlanType.SHARED)) {
            if (u.getSharedVisitCeiling() == 0) {
                notes.add("Shared hosting is excluded: it cannot run custom backends or database servers.");
            } else {
                notes.add(String.format(Locale.ROOT, "Shared hosting is excluded: above ~%,d visits/month a %s needs dedicated resources.",
                        u.getSharedVisitCeiling(), u.getLabel().toLowerCase(Locale.ROOT)));
            }
        }
        if (u == UseCase.DATABASE) {
            notes.add("Database RAM includes about 10% of your data size for caching.");
        }

        return new ResourceNeed(vcpu, ram, req.storageGb(), bandwidth, preferred, acceptable, excluded, List.copyOf(notes));
    }

    static double roundUpToHalf(double value) {
        return Math.ceil(value * 2.0) / 2.0;
    }
}
