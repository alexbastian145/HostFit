package com.hostfit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Audit record of one crawl of one source. Source name is copied so history survives source deletion. */
@Entity
@Table(name = "crawl_runs")
public class CrawlRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_name", nullable = false, length = 120)
    private String sourceName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "plans_found")
    private int plansFound;

    @Column(name = "plans_created")
    private int plansCreated;

    @Column(name = "plans_updated")
    private int plansUpdated;

    @Column(name = "plans_skipped")
    private int plansSkipped;

    @Column(length = 2000)
    private String message;

    public Long getId() {
        return id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public int getPlansFound() {
        return plansFound;
    }

    public void setPlansFound(int plansFound) {
        this.plansFound = plansFound;
    }

    public int getPlansCreated() {
        return plansCreated;
    }

    public void setPlansCreated(int plansCreated) {
        this.plansCreated = plansCreated;
    }

    public int getPlansUpdated() {
        return plansUpdated;
    }

    public void setPlansUpdated(int plansUpdated) {
        this.plansUpdated = plansUpdated;
    }

    public int getPlansSkipped() {
        return plansSkipped;
    }

    public void setPlansSkipped(int plansSkipped) {
        this.plansSkipped = plansSkipped;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? null : (message.length() > 2000 ? message.substring(0, 2000) : message);
    }
}
