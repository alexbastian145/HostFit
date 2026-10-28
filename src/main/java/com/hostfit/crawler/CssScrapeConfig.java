package com.hostfit.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Per-source scraping rules for {@link CssSelectorParser}, stored as JSON on the crawl source.
 * Only {@code cardSelector}, {@code nameSelector} and {@code priceSelector} are required; selectors
 * are evaluated relative to each plan card.
 *
 * <pre>{@code
 * {
 *   "cardSelector": ".plan-card", "nameSelector": "h3", "priceSelector": ".price",
 *   "vcpuSelector": ".cpu", "ramSelector": ".ram", "storageSelector": ".disk", "bandwidthSelector": ".bw",
 *   "planType": "SHARED", "currency": "INR", "managed": true, "pricePick": "last"
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CssScrapeConfig {

    private String cardSelector;
    private String nameSelector;
    private String priceSelector;
    private String vcpuSelector;
    private String ramSelector;
    private String storageSelector;
    private String bandwidthSelector;
    private String planType = "SHARED";
    private String currency = "INR";
    private boolean managed = false;
    private String pricePick = "last";
    private String storageType = "SSD";

    public String getCardSelector() {
        return cardSelector;
    }

    public void setCardSelector(String cardSelector) {
        this.cardSelector = cardSelector;
    }

    public String getNameSelector() {
        return nameSelector;
    }

    public void setNameSelector(String nameSelector) {
        this.nameSelector = nameSelector;
    }

    public String getPriceSelector() {
        return priceSelector;
    }

    public void setPriceSelector(String priceSelector) {
        this.priceSelector = priceSelector;
    }

    public String getVcpuSelector() {
        return vcpuSelector;
    }

    public void setVcpuSelector(String vcpuSelector) {
        this.vcpuSelector = vcpuSelector;
    }

    public String getRamSelector() {
        return ramSelector;
    }

    public void setRamSelector(String ramSelector) {
        this.ramSelector = ramSelector;
    }

    public String getStorageSelector() {
        return storageSelector;
    }

    public void setStorageSelector(String storageSelector) {
        this.storageSelector = storageSelector;
    }

    public String getBandwidthSelector() {
        return bandwidthSelector;
    }

    public void setBandwidthSelector(String bandwidthSelector) {
        this.bandwidthSelector = bandwidthSelector;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public String getPricePick() {
        return pricePick;
    }

    public void setPricePick(String pricePick) {
        this.pricePick = pricePick;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
}
