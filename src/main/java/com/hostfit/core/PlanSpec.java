package com.hostfit.core;

import java.math.BigDecimal;

/**
 * Framework-independent view of a hosting plan, used by the matching engine.
 *
 * @param bandwidthGb monthly transfer in GB; {@code null} when the provider does not publish a figure
 * @param unmetered   true when the plan advertises unmetered / unlimited bandwidth
 */
public record PlanSpec(
        long id,
        String providerName,
        String name,
        PlanType type,
        int vcpu,
        double ramGb,
        int storageGb,
        String storageType,
        Integer bandwidthGb,
        boolean unmetered,
        BigDecimal priceInr,
        boolean managed) {
}
