package com.hostfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.hostfit.crawler.CrawlService;
import com.hostfit.domain.CrawlRun;
import com.hostfit.domain.CrawlSource;
import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PriceSource;
import com.hostfit.domain.RunStatus;
import com.hostfit.crawler.PageFetcher;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.ProviderRepository;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/** Runs the whole crawl pipeline (fetch -> parse -> upsert -> history) with the network mocked out. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:crawl-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class CrawlPipelineTest {

    private static final String LINODE = """
            {"data":[
              {"id":"g6-nanode-1","label":"Nanode 1GB","class":"nanode","memory":1024,"disk":25600,"transfer":1000,"vcpus":1,
               "price":{"hourly":0.0075,"monthly":6.0}},
              {"id":"g6-standard-16","label":"Linode 96GB","class":"standard","memory":98304,"disk":2097152,"transfer":20000,"vcpus":20,
               "price":{"hourly":0.9,"monthly":600.0}}
            ]}
            """;

    @Autowired
    private CrawlService crawler;
    @Autowired
    private CrawlSourceRepository sources;
    @Autowired
    private HostingPlanRepository plans;
    @Autowired
    private ProviderRepository providers;
    @MockBean
    private PageFetcher fetcher;

    private CrawlSource linodeSource() {
        return sources.findAllWithProvider().stream()
                .filter(s -> s.getName().equals("Linode public API")).findFirst().orElseThrow();
    }

    @Test
    void crawlUpdatesKnownPlanCreatesNewOneAndRecordsHistory() throws Exception {
        when(fetcher.fetch(anyString(), anyBoolean())).thenReturn(LINODE);
        CrawlSource source = linodeSource();

        CrawlRun run = crawler.runSource(source.getId());

        assertEquals(RunStatus.SUCCESS, run.getStatus(), run.getMessage());
        assertEquals(2, run.getPlansFound());
        assertEquals(1, run.getPlansCreated());
        assertEquals(1, run.getPlansUpdated());

        HostingPlan nanode = plans.findByProviderAndExternalKey(source.getProvider(), "linode:g6-nanode-1").orElseThrow();
        assertEquals(0, new BigDecimal("528.00").compareTo(nanode.getPriceInr()), "6 USD x 88");
        assertEquals(PriceSource.CRAWLED, nanode.getPriceSource());
        assertNotNull(nanode.getPriceVerifiedAt());

        HostingPlan created = plans.findByProviderAndExternalKey(source.getProvider(), "linode:g6-standard-16").orElseThrow();
        assertEquals(20, created.getVcpu());
        assertEquals(96.0, created.getRamGb(), 1e-9);

        CrawlSource reloaded = sources.findWithProviderById(source.getId()).orElseThrow();
        assertEquals("SUCCESS", reloaded.getLastStatus());
    }

    @Test
    void failedFetchIsRecordedAndLeavesPricesUntouched() throws Exception {
        when(fetcher.fetch(anyString(), anyBoolean())).thenThrow(new IOException("HTTP 503 from api.linode.com"));
        CrawlSource source = linodeSource();
        BigDecimal before = plans.findByProviderAndExternalKey(source.getProvider(), "linode:g6-nanode-1")
                .orElseThrow().getPriceInr();

        CrawlRun run = crawler.runSource(source.getId());

        assertEquals(RunStatus.FAILED, run.getStatus());
        assertTrue(run.getMessage().contains("503"));
        BigDecimal after = plans.findByProviderAndExternalKey(source.getProvider(), "linode:g6-nanode-1")
                .orElseThrow().getPriceInr();
        assertEquals(0, before.compareTo(after));
    }

    @Test
    void garbageResponseFailsCleanly() throws Exception {
        when(fetcher.fetch(anyString(), anyBoolean())).thenReturn("<html>Access denied</html>");

        CrawlRun run = crawler.runSource(linodeSource().getId());

        assertEquals(RunStatus.FAILED, run.getStatus());
        assertFalse(run.getMessage().isBlank());
    }
}
