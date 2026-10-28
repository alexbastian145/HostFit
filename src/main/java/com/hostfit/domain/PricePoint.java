package com.hostfit.domain;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** One entry in a plan's price history. */
@Entity
@Table(name = "price_points")
public class PricePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private HostingPlan plan;

    @Column(name = "price_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceInr;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceSource source;

    public PricePoint() {
    }

    public PricePoint(HostingPlan plan, BigDecimal priceInr, PriceSource source) {
        this.plan = plan;
        this.priceInr = priceInr;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public HostingPlan getPlan() {
        return plan;
    }

    public BigDecimal getPriceInr() {
        return priceInr;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public PriceSource getSource() {
        return source;
    }
}
