package com.hostfit.service;

import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PricePoint;
import com.hostfit.domain.PriceSource;
import com.hostfit.domain.Provider;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.PricePointRepository;
import com.hostfit.repo.ProviderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hosting Plan &amp; Provider module (plans) plus the Admin / Data Management operations on plans. */
@Service
@Transactional(readOnly = true)
public class PlanService {

    private final HostingPlanRepository plans;
    private final ProviderRepository providers;
    private final PricePointRepository history;

    public PlanService(HostingPlanRepository plans, ProviderRepository providers, PricePointRepository history) {
        this.plans = plans;
        this.providers = providers;
        this.history = history;
    }

    /** Active plans of active providers, filtered and sorted for the public browser. */
    public List<HostingPlan> browse(PlanFilter f) {
        Comparator<HostingPlan> order = switch (f.sort() == null ? "" : f.sort()) {
            case PlanFilter.SORT_PRICE_DESC -> Comparator.comparing(HostingPlan::getPriceInr).reversed();
            case PlanFilter.SORT_RAM -> Comparator.comparingDouble(HostingPlan::getRamGb).reversed();
            case PlanFilter.SORT_PROVIDER -> Comparator.comparing((HostingPlan p) -> p.getProvider().getName())
                    .thenComparing(HostingPlan::getPriceInr);
            default -> Comparator.comparing(HostingPlan::getPriceInr);
        };
        String q = f.query() == null ? "" : f.query().trim().toLowerCase(Locale.ROOT);
        return plans.findAllActive().stream()
                .filter(p -> f.type() == null || p.getPlanType() == f.type())
                .filter(p -> f.providerId() == null || p.getProvider().getId().equals(f.providerId()))
                .filter(p -> f.maxPrice() == null || p.getPriceInr().compareTo(f.maxPrice()) <= 0)
                .filter(p -> q.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(q)
                        || p.getProvider().getName().toLowerCase(Locale.ROOT).contains(q))
                .sorted(order.thenComparing(HostingPlan::getId))
                .toList();
    }

    public List<HostingPlan> findActive() {
        return plans.findAllActive();
    }

    public List<HostingPlan> findAll() {
        return plans.findAllWithProvider().stream()
                .sorted(Comparator.comparing((HostingPlan p) -> p.getProvider().getName().toLowerCase(Locale.ROOT))
                        .thenComparing(HostingPlan::getPriceInr))
                .toList();
    }

    public HostingPlan get(Long id) {
        return plans.findWithProviderById(id).orElseThrow(() -> new NotFoundException("Plan " + id + " not found"));
    }

    public List<HostingPlan> getAllById(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return plans.findAllWithProviderByIdIn(ids);
    }

    public List<PricePoint> priceHistory(HostingPlan plan) {
        return history.findTop30ByPlanOrderByRecordedAtDesc(plan);
    }

    public long countActive() {
        return plans.countByActiveTrue();
    }

    public PlanForm toForm(HostingPlan p) {
        PlanForm f = new PlanForm();
        f.setProviderId(p.getProvider().getId());
        f.setName(p.getName());
        f.setPlanType(p.getPlanType());
        f.setVcpu(p.getVcpu());
        f.setRamGb(p.getRamGb());
        f.setStorageGb(p.getStorageGb());
        f.setStorageType(p.getStorageType());
        f.setBandwidthGb(p.getBandwidthGb());
        f.setUnmetered(p.isUnmetered());
        f.setPriceInr(p.getPriceInr());
        f.setManaged(p.isManaged());
        f.setUrl(p.getUrl());
        f.setActive(p.isActive());
        return f;
    }

    @Transactional
    public HostingPlan create(PlanForm f) {
        HostingPlan p = new HostingPlan();
        apply(p, f);
        p.setPriceInr(f.getPriceInr());
        p.setPriceSource(PriceSource.MANUAL);
        p.setPriceVerifiedAt(Instant.now());
        HostingPlan saved = plans.save(p);
        history.save(new PricePoint(saved, saved.getPriceInr(), PriceSource.MANUAL));
        return saved;
    }

    @Transactional
    public HostingPlan update(Long id, PlanForm f) {
        HostingPlan p = get(id);
        apply(p, f);
        if (p.getPriceInr().compareTo(f.getPriceInr()) != 0) {
            recordManualPrice(p, f.getPriceInr());
        }
        return plans.save(p);
    }

    @Transactional
    public HostingPlan updatePrice(Long id, BigDecimal newPrice) {
        HostingPlan p = get(id);
        if (p.getPriceInr().compareTo(newPrice) != 0) {
            recordManualPrice(p, newPrice);
        } else {
            p.setPriceVerifiedAt(Instant.now());
            p.setPriceSource(PriceSource.MANUAL);
        }
        return plans.save(p);
    }

    @Transactional
    public void delete(Long id) {
        plans.delete(get(id));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        HostingPlan p = get(id);
        p.setActive(active);
        plans.save(p);
    }

    private void recordManualPrice(HostingPlan p, BigDecimal newPrice) {
        p.setPriceInr(newPrice);
        p.setOriginalPrice(null);
        p.setOriginalCurrency(null);
        p.setPriceSource(PriceSource.MANUAL);
        p.setPriceVerifiedAt(Instant.now());
        history.save(new PricePoint(p, newPrice, PriceSource.MANUAL));
    }

    private void apply(HostingPlan p, PlanForm f) {
        Provider provider = providers.findById(f.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider " + f.getProviderId() + " not found"));
        p.setProvider(provider);
        p.setName(f.getName().trim());
        p.setPlanType(f.getPlanType());
        p.setVcpu(f.getVcpu());
        p.setRamGb(f.getRamGb());
        p.setStorageGb(f.getStorageGb());
        p.setStorageType(f.getStorageType() == null || f.getStorageType().isBlank() ? "SSD" : f.getStorageType().trim());
        p.setUnmetered(f.isUnmetered());
        p.setBandwidthGb(f.isUnmetered() ? null : f.getBandwidthGb());
        p.setManaged(f.isManaged());
        p.setUrl(f.getUrl() == null || f.getUrl().isBlank() ? null : f.getUrl().trim());
        p.setActive(f.isActive());
    }
}
