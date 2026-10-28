package com.hostfit.service;

import com.hostfit.crawler.CrawlSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin form for a crawl source. */
public class SourceForm {

    @NotNull(message = "Choose a provider")
    private Long providerId;

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @NotNull(message = "Choose a source type")
    private CrawlSourceType type;

    @NotBlank(message = "URL is required")
    @Size(max = 1000)
    @Pattern(regexp = "^https?://\\S+$", message = "Must be an http(s) URL")
    private String url;

    @Size(max = 4000)
    private String config;

    private boolean enabled = true;

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

    public CrawlSourceType getType() {
        return type;
    }

    public void setType(CrawlSourceType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
