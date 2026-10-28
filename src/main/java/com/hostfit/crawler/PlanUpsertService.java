package com.hostfit.crawler;

import com.hostfit.domain.CrawlSource;
import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PricePoint;
import com.hostfit.domain.PriceSource;
import com.hostfit.domain.Provider;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.PricePointRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies crawled plans to the database: updates prices of known plans and creates new ones. */
@Service
public class PlanUpsertService {

    static final BigDecimal MIN_CHANGE = new BigDecimal("0.50");
    static final BigDecimal MAX_PRICE = new BigDecimal("1000000");
    static final double SUSPICIOUS_CHANGE = 0.6;

    public record Outcome(int found, int created, int updated, int unchanged, int skipped, List<String> notes) {
    }

    private final HostingPlanRepository plans;
    private final PricePointRepository history;
    private final CrawlSourceRepository sources;
    private final FxService fx;

    public PlanUpsertService(HostingPlanRepository plans, PricePointRepository history,
                             CrawlSourceRepository sources, FxService fx) {
        this.plans = plans;
        this.history = history;
        this.sources = sources;
        this.fx = fx;
    }

    @Transactional
    public Outcome apply(Long sourceId, List<ParsedPlan> parsed) {
        CrawlSource source = sources.findWithProviderById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Crawl source " + sourceId + " no longer exists"));
        Provider provider = source.getProvider();
        Instant now = Instant.now();

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;
        List<String> notes = new ArrayList<>();

        for (ParsedPlan p : parsed) {
            BigDecimal inr;
            try {
                inr = fx.toInr(p.price(), p.currency());
            } catch (IllegalArgumentException e) {
                skipped++;
                notes.add(p.name() + ": " + e.getMessage());
                continue;
            }
            if (inr.signum() <= 0 || inr.compareTo(MAX_PRICE) > 0) {
                skipped++;
                notes.add(p.name() + ": implausible price " + inr);
                continue;
            }

            Optional<HostingPlan> existing = plans.findByProviderAndExternalKey(provider, p.externalKey());
            if (existing.isPresent()) {
                HostingPlan plan = existing.get();
                BigDecimal old = plan.getPriceInr();
                if (old.subtract(inr).abs().compareTo(MIN_CHANGE) > 0) {
                    if (old.signum() > 0 && inr.subtract(old).abs()
                            .divide(old, 4, RoundingMode.HALF_UP).doubleValue() > SUSPICIOUS_CHANGE) {
                        notes.add("Large price change for " + plan.getName() + ": " + old + " -> " + inr + " (check source)");
                    }
                    plan.setPriceInr(inr);
                    history.save(new PricePoint(plan, inr, PriceSource.CRAWLED));
                    updated++;
                } else {
                    unchanged++;
                }
                plan.setOriginalPrice(p.price());
                plan.setOriginalCurrency(p.currency());
                plan.setPriceSource(PriceSource.CRAWLED);
                plan.setPriceVerifiedAt(now);
                refreshSpecs(plan, p);
                plans.save(plan);
            } else if (p.vcpu() == null || p.ramGb() == null || p.storageGb() == null) {
                skipped++;
                notes.add(p.name() + ": new plan skipped, vCPU/RAM/storage not found on the page");
            } else {
                HostingPlan plan = new HostingPlan();
                plan.setProvider(provider);
                plan.setName(p.name());
                plan.setPlanType(p.planType());
                plan.setVcpu(p.vcpu());
                plan.setRamGb(p.ramGb());
                plan.setStorageGb(p.storageGb());
                plan.setStorageType(p.storageType() == null ? "SSD" : p.storageType());
                plan.setBandwidthGb(p.bandwidthGb());
                plan.setUnmetered(p.unmetered());
                plan.setManaged(Boolean.TRUE.equals(p.managed()));
                plan.setPriceInr(inr);
                plan.setOriginalPrice(p.price());
                plan.setOriginalCurrency(p.currency());
                plan.setUrl(p.url());
                plan.setExternalKey(p.externalKey());
                plan.setPriceSource(PriceSource.CRAWLED);
                plan.setPriceVerifiedAt(now);
                plan.setActive(true);
                HostingPlan saved = plans.save(plan);
                history.save(new PricePoint(saved, inr, PriceSource.CRAWLED));
                created++;
            }
        }
        return new Outcome(parsed.size(), created, updated, unchanged, skipped, List.copyOf(notes));
    }

    /** Specs found on the page overwrite stored ones; anything the page did not show is left alone. */
    private static void refreshSpecs(HostingPlan plan, ParsedPlan p) {
        if (p.vcpu() != null) {
            plan.setVcpu(p.vcpu());
        }
        if (p.ramGb() != null) {
            plan.setRamGb(p.ramGb());
        }
        if (p.storageGb() != null) {
            plan.setStorageGb(p.storageGb());
        }
        if (p.unmetered()) {
            plan.setUnmetered(true);
        } else if (p.bandwidthGb() != null) {
            plan.setUnmetered(false);
            plan.setBandwidthGb(p.bandwidthGb());
        }
    }
}
