package com.hostfit.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostfit.core.PlanType;
import com.hostfit.crawler.CrawlSourceType;
import com.hostfit.domain.CrawlSource;
import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PricePoint;
import com.hostfit.domain.PriceSource;
import com.hostfit.domain.Provider;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.PricePointRepository;
import com.hostfit.repo.ProviderRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the starter catalogue (providers, plans, crawl sources) from {@code seed-data.json} into an empty
 * database. Seeded prices are rough estimates (price source "Estimate") until a crawl or an administrator
 * verifies them.
 */
@Component
@Order(2)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    record SeedProvider(String name, String website, String description) {
    }

    record SeedPlan(String provider, String name, PlanType type, int vcpu, double ramGb, int storageGb,
                    String storageType, Integer bandwidthGb, boolean unmetered, BigDecimal priceInr,
                    boolean managed, String url, String externalKey) {
    }

    record SeedSource(String provider, String name, CrawlSourceType type, String url, boolean enabled) {
    }

    record SeedData(List<SeedProvider> providers, List<SeedPlan> plans, List<SeedSource> sources) {
    }

    private final ProviderRepository providers;
    private final HostingPlanRepository plans;
    private final PricePointRepository history;
    private final CrawlSourceRepository sources;
    private final AppProperties props;

    public DataSeeder(ProviderRepository providers, HostingPlanRepository plans, PricePointRepository history,
                      CrawlSourceRepository sources, AppProperties props) {
        this.providers = providers;
        this.plans = plans;
        this.history = history;
        this.sources = sources;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!props.seed().enabled() || providers.count() > 0) {
            return;
        }
        SeedData data = read();
        Map<String, Provider> byName = new HashMap<>();
        for (SeedProvider sp : data.providers()) {
            byName.put(sp.name(), providers.save(new Provider(sp.name(), sp.website(), sp.description())));
        }
        for (SeedPlan sp : data.plans()) {
            Provider provider = requireProvider(byName, sp.provider());
            HostingPlan p = new HostingPlan();
            p.setProvider(provider);
            p.setName(sp.name());
            p.setPlanType(sp.type());
            p.setVcpu(sp.vcpu());
            p.setRamGb(sp.ramGb());
            p.setStorageGb(sp.storageGb());
            p.setStorageType(sp.storageType());
            p.setBandwidthGb(sp.unmetered() ? null : sp.bandwidthGb());
            p.setUnmetered(sp.unmetered());
            p.setPriceInr(sp.priceInr());
            p.setManaged(sp.managed());
            p.setUrl(sp.url());
            p.setExternalKey(sp.externalKey());
            p.setPriceSource(PriceSource.SEED);
            HostingPlan saved = plans.save(p);
            history.save(new PricePoint(saved, saved.getPriceInr(), PriceSource.SEED));
        }
        for (SeedSource ss : data.sources()) {
            CrawlSource s = new CrawlSource();
            s.setProvider(requireProvider(byName, ss.provider()));
            s.setName(ss.name());
            s.setType(ss.type());
            s.setUrl(ss.url());
            s.setEnabled(ss.enabled());
            sources.save(s);
        }
        log.info("Seeded {} providers, {} plans and {} crawl sources", data.providers().size(),
                data.plans().size(), data.sources().size());
    }

    private static Provider requireProvider(Map<String, Provider> byName, String name) {
        Provider p = byName.get(name);
        if (p == null) {
            throw new IllegalStateException("seed-data.json references unknown provider '" + name + "'");
        }
        return p;
    }

    private static SeedData read() throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource("seed-data.json").getInputStream()) {
            return mapper.readValue(in, SeedData.class);
        }
    }
}
