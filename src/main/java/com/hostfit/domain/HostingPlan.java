package com.hostfit.domain;

import com.hostfit.core.PlanSpec;
import com.hostfit.core.PlanType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A purchasable hosting plan: specs, price and where that price came from. */
@Entity
@Table(name = "hosting_plans", uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "external_key"}))
public class HostingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;

    @Column(nullable = false)
    private int vcpu;

    @Column(name = "ram_gb", nullable = false)
    private double ramGb;

    @Column(name = "storage_gb", nullable = false)
    private int storageGb;

    @Column(name = "storage_type", length = 30)
    private String storageType = "SSD";

    @Column(name = "bandwidth_gb")
    private Integer bandwidthGb;

    @Column(nullable = false)
    private boolean unmetered;

    @Column(name = "price_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceInr;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "original_currency", length = 3)
    private String originalCurrency;

    @Column(nullable = false)
    private boolean managed;

    @Column(length = 500)
    private String url;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", nullable = false, length = 20)
    private PriceSource priceSource = PriceSource.MANUAL;

    @Column(name = "price_verified_at")
    private Instant priceVerifiedAt;

    @Column(name = "external_key", length = 150)
    private String externalKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.REMOVE)
    private List<PricePoint> history = new ArrayList<>();

    public PlanSpec toSpec() {
        return new PlanSpec(id == null ? 0L : id, provider.getName(), name, planType, vcpu, ramGb, storageGb,
                storageType, bandwidthGb, unmetered, priceInr, managed);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
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

    public int getVcpu() {
        return vcpu;
    }

    public void setVcpu(int vcpu) {
        this.vcpu = vcpu;
    }

    public double getRamGb() {
        return ramGb;
    }

    public void setRamGb(double ramGb) {
        this.ramGb = ramGb;
    }

    public int getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(int storageGb) {
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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
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

    public PriceSource getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(PriceSource priceSource) {
        this.priceSource = priceSource;
    }

    public Instant getPriceVerifiedAt() {
        return priceVerifiedAt;
    }

    public void setPriceVerifiedAt(Instant priceVerifiedAt) {
        this.priceVerifiedAt = priceVerifiedAt;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
