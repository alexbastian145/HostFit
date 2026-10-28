package com.hostfit.core;

import java.math.BigDecimal;

/** A plan that meets every requirement except the budget, and by how much it is over. */
public record Alternative(PlanSpec plan, BigDecimal overBudgetBy) {
}
