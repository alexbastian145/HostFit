package com.hostfit.crawler;

/** How a crawl source is read. */
public enum CrawlSourceType {
    LINODE_API("Linode public API", true),
    VULTR_API("Vultr public API", true),
    CSS_SCRAPER("HTML page (CSS selectors)", false);

    private final String label;
    private final boolean api;

    CrawlSourceType(String label, boolean api) {
        this.label = label;
        this.api = api;
    }

    public String getLabel() {
        return label;
    }

    /** True for documented public JSON APIs, which skip the robots.txt check used for HTML scraping. */
    public boolean isApi() {
        return api;
    }
}
