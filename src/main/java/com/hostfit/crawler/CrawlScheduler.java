package com.hostfit.crawler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Refreshes prices on the schedule {@code hostfit.crawler.cron} (default: every 6 hours). */
@Component
@ConditionalOnProperty(prefix = "hostfit.crawler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CrawlScheduler {

    private final CrawlService crawlService;

    public CrawlScheduler(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Scheduled(cron = "${hostfit.crawler.cron:0 0 */6 * * *}")
    public void refreshPrices() {
        crawlService.runAllEnabled();
    }
}
