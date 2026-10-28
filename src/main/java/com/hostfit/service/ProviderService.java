package com.hostfit.service;

import com.hostfit.domain.Provider;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.ProviderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hosting Plan &amp; Provider module (providers). */
@Service
@Transactional(readOnly = true)
public class ProviderService {

    private final ProviderRepository providers;
    private final HostingPlanRepository plans;
    private final CrawlSourceRepository sources;

    public ProviderService(ProviderRepository providers, HostingPlanRepository plans, CrawlSourceRepository sources) {
        this.providers = providers;
        this.plans = plans;
        this.sources = sources;
    }

    public List<Provider> findAll() {
        return providers.findAllByOrderByNameAsc();
    }

    public Provider get(Long id) {
        return providers.findById(id).orElseThrow(() -> new NotFoundException("Provider " + id + " not found"));
    }

    public long planCount(Provider p) {
        return plans.countByProvider(p);
    }

    public ProviderForm toForm(Provider p) {
        ProviderForm f = new ProviderForm();
        f.setName(p.getName());
        f.setWebsite(p.getWebsite());
        f.setDescription(p.getDescription());
        f.setActive(p.isActive());
        return f;
    }

    /** @throws IllegalArgumentException if another provider already uses the name */
    @Transactional
    public Provider create(ProviderForm f) {
        String name = f.getName().trim();
        if (providers.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A provider named '" + name + "' already exists");
        }
        Provider p = new Provider();
        copy(p, f, name);
        return providers.save(p);
    }

    @Transactional
    public Provider update(Long id, ProviderForm f) {
        Provider p = get(id);
        String name = f.getName().trim();
        providers.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("A provider named '" + name + "' already exists");
            }
        });
        copy(p, f, name);
        return providers.save(p);
    }

    /** @throws IllegalStateException if the provider still has plans or crawl sources */
    @Transactional
    public void delete(Long id) {
        Provider p = get(id);
        long planCount = plans.countByProvider(p);
        long sourceCount = sources.countByProviderId(id);
        if (planCount > 0 || sourceCount > 0) {
            throw new IllegalStateException("Cannot delete " + p.getName() + ": it still has " + planCount
                    + " plan(s) and " + sourceCount + " crawl source(s). Remove those first, or deactivate the provider.");
        }
        providers.delete(p);
    }

    private static void copy(Provider p, ProviderForm f, String name) {
        p.setName(name);
        p.setWebsite(f.getWebsite() == null || f.getWebsite().isBlank() ? null : f.getWebsite().trim());
        p.setDescription(f.getDescription() == null || f.getDescription().isBlank() ? null : f.getDescription().trim());
        p.setActive(f.isActive());
    }
}
