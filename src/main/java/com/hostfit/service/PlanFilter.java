package com.hostfit.service;

import com.hostfit.core.PlanType;
import java.math.BigDecimal;

/** Filters for the public plan browser. Every field is optional. */
public record PlanFilter(PlanType type, Long providerId, BigDecimal maxPrice, String query, String sort) {

    public static final String SORT_PRICE = "price";
    public static final String SORT_PRICE_DESC = "price_desc";
    public static final String SORT_RAM = "ram";
    public static final String SORT_PROVIDER = "provider";
}
