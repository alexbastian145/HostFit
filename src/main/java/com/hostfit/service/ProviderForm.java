package com.hostfit.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin form for a hosting provider. */
public class ProviderForm {

    @NotBlank(message = "Provider name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Must be an http(s) URL")
    private String website;

    @Size(max = 1000)
    private String description;

    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
