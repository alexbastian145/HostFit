package com.hostfit.service;

import com.hostfit.core.PlanType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Admin form for adding or editing a hosting plan. */
public class PlanForm {

    @NotNull(message = "Choose a provider")
    private Long providerId;

    @NotBlank(message = "Plan name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Choose a plan type")
    private PlanType planType;

    @NotNull(message = "vCPU count is required")
    @Min(value = 1, message = "At least 1 vCPU")
    @Max(512)
    private Integer vcpu;

    @NotNull(message = "RAM is required")
    @DecimalMin(value = "0.25", message = "At least 0.25 GB")
    @DecimalMax("4096")
    private Double ramGb;

    @NotNull(message = "Storage is required")
    @Min(value = 1, message = "At least 1 GB")
    @Max(1_000_000)
    private Integer storageGb;

    @Size(max = 30)
    private String storageType = "SSD";

    @Min(0)
    private Integer bandwidthGb;

    private boolean unmetered;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "1", message = "Price must be at least ₹1")
    @DecimalMax(value = "1000000", message = "Price must be at most ₹10,00,000")
    private BigDecimal priceInr;

    private boolean managed;

    @Size(max = 500)
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Must be an http(s) URL")
    private String url;

    private boolean active = true;

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public Integer getVcpu() {
        return vcpu;
    }

    public void setVcpu(Integer vcpu) {
        this.vcpu = vcpu;
    }

    public Double getRamGb() {
        return ramGb;
    }

    public void setRamGb(Double ramGb) {
        this.ramGb = ramGb;
    }

    public Integer getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(Integer storageGb) {
        this.storageGb = storageGb;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Integer getBandwidthGb() {
        return bandwidthGb;
    }

    public void setBandwidthGb(Integer bandwidthGb) {
        this.bandwidthGb = bandwidthGb;
    }

    public boolean isUnmetered() {
        return unmetered;
    }

    public void setUnmetered(boolean unmetered) {
        this.unmetered = unmetered;
    }

    public BigDecimal getPriceInr() {
        return priceInr;
    }

    public void setPriceInr(BigDecimal priceInr) {
        this.priceInr = priceInr;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
