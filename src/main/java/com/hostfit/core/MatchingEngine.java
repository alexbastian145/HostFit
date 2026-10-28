package com.hostfit.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, rule-based matching of requirements to hosting plans.
 *
 * <ol>
 *   <li>Budget &amp; spec check: hard filters (budget, storage, bandwidth, vCPU, RAM, tier).</li>
 *   <li>Requirement matching: plans passing every filter are scored 0-100.</li>
 *   <li>Compare: eligible plans are ranked; near-miss plans (over budget only) become alternatives.</li>
 * </ol>
 *
 * Score weights: resource fit 35, price 30, plan type 15, bandwidth 10, management 10.
 */
public class MatchingEngine {

    public static final String RESOURCE_FIT = "Resource fit";
    public static final String PRICE = "Price";
    public static final String PLAN_TYPE = "Plan type";
    public static final String BANDWIDTH = "Bandwidth";
    public static final String MANAGEMENT = "Management";

    static final double W_FIT = 35;
    static final double W_PRICE = 30;
    static final double W_TIER = 15;
    static final double W_BANDWIDTH = 10;
    static final double W_MANAGEMENT = 10;

    /** Plans costing up to this multiple of the budget are shown as "slightly over budget" alternatives. */
    static final double ALTERNATIVE_BUDGET_MARGIN = 1.5;
    static final int MAX_ALTERNATIVES = 3;

    private final CapacityEstimator estimator;

    /** Maximum points per scoring component, in display order (sums to 100). */
    public static Map<String, Integer> weights() {
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put(RESOURCE_FIT, (int) W_FIT);
        w.put(PRICE, (int) W_PRICE);
        w.put(PLAN_TYPE, (int) W_TIER);
        w.put(BANDWIDTH, (int) W_BANDWIDTH);
        w.put(MANAGEMENT, (int) W_MANAGEMENT);
        return w;
    }

    public MatchingEngine() {
        this(new CapacityEstimator());
    }

    public MatchingEngine(CapacityEstimator estimator) {
        this.estimator = estimator;
    }

    public RecommendationResult recommend(Requirement req, Collection<PlanSpec> plans, int limit) {
        ResourceNeed need = estimator.estimate(req);

        Map<PlanSpec, Set<Violation>> violationsByPlan = new LinkedHashMap<>();
        for (PlanSpec p : plans) {
            violationsByPlan.put(p, violations(req, need, p));
        }

        List<PlanSpec> eligible = new ArrayList<>();
        for (Map.Entry<PlanSpec, Set<Violation>> e : violationsByPlan.entrySet()) {
            if (e.getValue().isEmpty()) {
                eligible.add(e.getKey());
            }
        }

        BigDecimal cheapestEligible = eligible.stream()
                .map(PlanSpec::priceInr).min(Comparator.naturalOrder()).orElse(null);

        Map<Long, Evaluation> evaluations = new LinkedHashMap<>();
        List<Recommendation> ranked = new ArrayList<>();
        for (Map.Entry<PlanSpec, Set<Violation>> e : violationsByPlan.entrySet()) {
            PlanSpec p = e.getKey();
            if (e.getValue().isEmpty()) {
                Recommendation r = score(req, need, p, cheapestEligible);
                ranked.add(r);
                evaluations.put(p.id(), new Evaluation(p, Set.of(), r));
            } else {
                evaluations.put(p.id(), new Evaluation(p, Set.copyOf(e.getValue()), null));
            }
        }
        ranked.sort(Comparator
                .comparingInt(Recommendation::score).reversed()
                .thenComparing(r -> r.plan().priceInr())
                .thenComparingLong(r -> r.plan().id()));

        List<Recommendation> top = ranked.size() > limit ? List.copyOf(ranked.subList(0, Math.max(limit, 0))) : List.copyOf(ranked);

        List<Alternative> alternatives = new ArrayList<>();
        BigDecimal minimumBudget = null;
        for (Map.Entry<PlanSpec, Set<Violation>> e : violationsByPlan.entrySet()) {
            Set<Violation> v = e.getValue();
            if (v.size() == 1 && v.contains(Violation.BUDGET)) {
                PlanSpec p = e.getKey();
                if (minimumBudget == null || p.priceInr().compareTo(minimumBudget) < 0) {
                    minimumBudget = p.priceInr();
                }
                BigDecimal ceiling = req.budgetInr().multiply(BigDecimal.valueOf(ALTERNATIVE_BUDGET_MARGIN));
                if (p.priceInr().compareTo(ceiling) <= 0) {
                    alternatives.add(new Alternative(p, p.priceInr().subtract(req.budgetInr())));
                }
            }
        }
        alternatives.sort(Comparator.comparing((Alternative a) -> a.plan().priceInr())
                .thenComparingLong(a -> a.plan().id()));
        List<Alternative> topAlternatives = alternatives.size() > MAX_ALTERNATIVES
                ? List.copyOf(alternatives.subList(0, MAX_ALTERNATIVES)) : List.copyOf(alternatives);

        // Only meaningful when nothing is eligible; otherwise the user's budget already works.
        BigDecimal minimumBudgetNeeded = eligible.isEmpty() ? minimumBudget : null;

        return new RecommendationResult(need, top, topAlternatives, minimumBudgetNeeded, evaluations);
    }

    Set<Violation> violations(Requirement req, ResourceNeed need, PlanSpec p) {
        Set<Violation> v = EnumSet.noneOf(Violation.class);
        if (p.priceInr().compareTo(req.budgetInr()) > 0) {
            v.add(Violation.BUDGET);
        }
        if (p.storageGb() < need.minStorageGb()) {
            v.add(Violation.STORAGE);
        }
        if (!p.unmetered() && p.bandwidthGb() != null && p.bandwidthGb() < need.minBandwidthGb()) {
            v.add(Violation.BANDWIDTH);
        }
        if (p.vcpu() < need.minVcpu()) {
            v.add(Violation.CPU);
        }
        if (p.ramGb() + 1e-9 < need.minRamGb()) {
            v.add(Violation.RAM);
        }
        if (need.excludedTiers().contains(p.type())) {
            v.add(Violation.TIER);
        }
        return v;
    }

    Recommendation score(Requirement req, ResourceNeed need, PlanSpec p, BigDecimal cheapestEligible) {
        double headroom = headroom(need, p);
        double fit = fitScore(headroom);

        double budget = req.budgetInr().doubleValue();
        double price = p.priceInr().doubleValue();
        double cheapest = cheapestEligible == null ? price : cheapestEligible.doubleValue();
        double affordability = 1.0 - 0.8 * Math.min(1.0, price / budget);
        double relative = price <= 0 ? 1.0 : Math.min(1.0, cheapest / price);
        double priceScore = 0.5 * affordability + 0.5 * relative;

        double tier;
        if (need.preferredTiers().contains(p.type())) {
            tier = 1.0;
        } else if (need.acceptableTiers().contains(p.type())) {
            tier = 0.6;
        } else {
            tier = 0.2;
        }

        double bandwidth = bandwidthScore(need, p);

        double management;
        if (req.expertise() == Expertise.MANAGED) {
            management = p.managed() ? 1.0 : 0.1;
        } else {
            management = p.managed() ? 0.9 : 1.0;
        }

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put(RESOURCE_FIT, (int) Math.round(fit * W_FIT));
        breakdown.put(PRICE, (int) Math.round(priceScore * W_PRICE));
        breakdown.put(PLAN_TYPE, (int) Math.round(tier * W_TIER));
        breakdown.put(BANDWIDTH, (int) Math.round(bandwidth * W_BANDWIDTH));
        breakdown.put(MANAGEMENT, (int) Math.round(management * W_MANAGEMENT));
        int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
        total = Math.max(0, Math.min(100, total));

        BigDecimal yearly = p.priceInr().multiply(BigDecimal.valueOf(12)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal per1k = p.priceInr()
                .divide(BigDecimal.valueOf(req.monthlyVisits()).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP),
                        2, RoundingMode.HALF_UP);

        return new Recommendation(p, total, breakdown, reasons(req, need, p, headroom), headroom, yearly, per1k);
    }

    static double headroom(ResourceNeed need, PlanSpec p) {
        double cpu = p.vcpu() / (double) need.minVcpu();
        double ram = p.ramGb() / need.minRamGb();
        double storage = p.storageGb() / (double) need.minStorageGb();
        return Math.exp((Math.log(cpu) + Math.log(ram) + Math.log(storage)) / 3.0);
    }

    /** 1.0 for 1.2x-2x headroom, a little less when tight, fading to 0 at 8x over-provisioning. */
    static double fitScore(double headroom) {
        if (headroom < 1.2) {
            return 0.75 + 0.25 * Math.max(0.0, (headroom - 1.0) / 0.2);
        }
        if (headroom <= 2.0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - (headroom - 2.0) / 6.0);
    }

    static double bandwidthScore(ResourceNeed need, PlanSpec p) {
        if (p.unmetered() || p.bandwidthGb() == null) {
            return 1.0;
        }
        double ratio = p.bandwidthGb() / Math.max(1.0, need.minBandwidthGb());
        if (ratio >= 2.0) {
            return 1.0;
        }
        return Math.max(0.0, 0.6 + 0.4 * (ratio - 1.0));
    }

    private static List<String> reasons(Requirement req, ResourceNeed need, PlanSpec p, double headroom) {
        List<String> out = new ArrayList<>();
        out.add(String.format(Locale.ROOT, "%d vCPU and %s GB RAM cover the estimated need of %d vCPU / %s GB.",
                p.vcpu(), trim(p.ramGb()), need.minVcpu(), trim(need.minRamGb())));
        out.add(String.format(Locale.ROOT, "%d GB storage vs %d GB required.", p.storageGb(), need.minStorageGb()));

        double pctUnder = (1.0 - p.priceInr().doubleValue() / req.budgetInr().doubleValue()) * 100.0;
        if (pctUnder >= 1.0) {
            out.add(String.format(Locale.ROOT, "Costs %.0f%% less than your budget.", pctUnder));
        } else {
            out.add("Uses essentially your full budget.");
        }

        if (p.unmetered()) {
            out.add("Unmetered bandwidth.");
        } else if (p.bandwidthGb() != null) {
            out.add(String.format(Locale.ROOT, "%,d GB transfer vs ~%,.0f GB estimated.", p.bandwidthGb(), need.minBandwidthGb()));
        }

        if (need.preferredTiers().contains(p.type())) {
            out.add(p.type().getLabel() + " is the ideal tier for this workload.");
        } else if (need.acceptableTiers().contains(p.type())) {
            out.add(p.type().getLabel() + " works, though another tier is a closer fit.");
        } else {
            out.add(p.type().getLabel() + " is usable but not the typical choice for this workload.");
        }

        if (req.expertise() == Expertise.MANAGED && !p.managed()) {
            out.add("Not managed: you would handle updates, security and backups yourself.");
        } else if (req.expertise() == Expertise.MANAGED) {
            out.add("Managed: the provider handles updates and security.");
        }
        if (headroom > 4.0) {
            out.add(String.format(Locale.ROOT, "Heavily over-provisioned (%.1fx what you need).", headroom));
        }
        return List.copyOf(out);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
