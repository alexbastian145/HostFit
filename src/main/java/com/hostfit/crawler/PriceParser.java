package com.hostfit.crawler;

import java.util.List;

/** Turns a fetched document into plans. Implementations are pure functions of their input. */
public interface PriceParser {

    CrawlSourceType type();

    /**
     * @param body   the fetched response body
     * @param config source-specific configuration (JSON), may be null for API parsers
     * @param url    the URL the body came from, used to build absolute plan links
     */
    List<ParsedPlan> parse(String body, String config, String url) throws ParseException;
}
