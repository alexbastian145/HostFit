package com.hostfit.domain;

import com.hostfit.core.Expertise;
import com.hostfit.core.Requirement;
import com.hostfit.core.UseCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** A saved set of user requirements; its id makes a results page shareable by link. */
@Entity
@Table(name = "requirement_requests")
public class RequirementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false, length = 30)
    private UseCase useCase;

    @Column(name = "monthly_visits", nullable = false)
    private long monthlyVisits;

    @Column(name = "storage_gb", nullable = false)
    private int storageGb;

    @Column(name = "budget_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetInr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Expertise expertise;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Requirement toRequirement() {
        return new Requirement(useCase, monthlyVisits, storageGb, budgetInr, expertise);
    }

    public Long getId() {
        return id;
    }

    public UseCase getUseCase() {
        return useCase;
    }

    public void setUseCase(UseCase useCase) {
        this.useCase = useCase;
    }

    public long getMonthlyVisits() {
        return monthlyVisits;
    }

    public void setMonthlyVisits(long monthlyVisits) {
        this.monthlyVisits = monthlyVisits;
    }

    public int getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(int storageGb) {
        this.storageGb = storageGb;
    }

    public BigDecimal getBudgetInr() {
        return budgetInr;
    }

    public void setBudgetInr(BigDecimal budgetInr) {
        this.budgetInr = budgetInr;
    }

    public Expertise getExpertise() {
        return expertise;
    }

    public void setExpertise(Expertise expertise) {
        this.expertise = expertise;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
