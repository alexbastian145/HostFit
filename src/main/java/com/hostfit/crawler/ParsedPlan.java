package com.hostfit.crawler;

import com.hostfit.core.PlanType;

import java.math.BigDecimal;

/**
 * One plan extracted from a provider page or API. Nullable spec fields mean
 * "not found on the page": existing plans keep their stored value, and new
 * plans are only created when vcpu, ramGb and storageGb are all known.
 *
 * @param externalKey stable identifier used to match the plan on later crawls (e.g. {@code linode:g6-nanode-1})
 * @param price       monthly price in {@code currency}
 * @param currency    ISO code, "USD" or "INR"
 */
public record ParsedPlan(
        String externalKey,
        String name,
        PlanType planType,
        Integer vcpu,
        Double ramGb,
        Integer storageGb,
        String storageType,
        Integer bandwidthGb,
        boolean unmetered,
        BigDecimal price,
        String currency,
        Boolean managed,
        String url) {
}
