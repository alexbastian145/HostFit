package com.hostfit.service;

import com.hostfit.core.Expertise;
import com.hostfit.core.Requirement;
import com.hostfit.core.UseCase;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** User & Requirement module input, shared by the web form and the REST API. Validation = "Validate input". */
public class RequirementForm {

    @NotNull(message = "Choose what you want to host")
    private UseCase useCase;

    @NotNull(message = "Enter your expected monthly visits")
    @Min(value = 1, message = "Monthly visits must be at least 1")
    @Max(value = 50_000_000, message = "For more than 50 million visits a month, talk to a provider directly")
    private Long monthlyVisits;

    @NotNull(message = "Enter the storage you need in GB")
    @Min(value = 1, message = "Storage must be at least 1 GB")
    @Max(value = 100_000, message = "Storage above 100,000 GB is outside HostFit's range")
    private Integer storageGb;

    @NotNull(message = "Enter your monthly budget in ₹")
    @DecimalMin(value = "50", message = "Budget must be at least ₹50 per month")
    @DecimalMax(value = "1000000", message = "Budget must be at most ₹10,00,000 per month")
    private BigDecimal budgetInr;

    @NotNull(message = "Choose how much server management you want")
    private Expertise expertise;

    public Requirement toRequirement() {
        return new Requirement(useCase, monthlyVisits, storageGb, budgetInr, expertise);
    }

    public UseCase getUseCase() {
        return useCase;
    }

    public void setUseCase(UseCase useCase) {
        this.useCase = useCase;
    }

    public Long getMonthlyVisits() {
        return monthlyVisits;
    }

    public void setMonthlyVisits(Long monthlyVisits) {
        this.monthlyVisits = monthlyVisits;
    }

    public Integer getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(Integer storageGb) {
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
}
