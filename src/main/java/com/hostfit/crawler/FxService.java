package com.hostfit.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostfit.config.AppProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** USD to INR conversion for providers that price in dollars. The rate is configurable and optionally auto-refreshed. */
@Service
public class FxService {

    private static final Logger log = LoggerFactory.getLogger(FxService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AppProperties props;
    private final PageFetcher fetcher;
    private volatile BigDecimal usdInr;

    public FxService(AppProperties props, PageFetcher fetcher) {
        this.props = props;
        this.fetcher = fetcher;
        this.usdInr = props.fx().usdInr();
    }

    public BigDecimal usdInr() {
        return usdInr;
    }

    /** Fetches a fresh rate when {@code hostfit.fx.auto-refresh} is on; keeps the previous rate on any failure. */
    public void refreshIfEnabled() {
        if (!props.fx().autoRefresh()) {
            return;
        }
        try {
            JsonNode rate = MAPPER.readTree(fetcher.fetch(props.fx().rateUrl(), false)).path("rates").path("INR");
            if (rate.isNumber() && rate.asDouble() > 0) {
                usdInr = BigDecimal.valueOf(rate.asDouble());
                log.info("USD/INR rate refreshed to {}", usdInr);
            }
        } catch (Exception e) {
            log.warn("Could not refresh USD/INR rate, keeping {}: {}", usdInr, e.getMessage());
        }
    }

    public BigDecimal toInr(BigDecimal amount, String currency) {
        if (currency == null || "INR".equalsIgnoreCase(currency)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if ("USD".equalsIgnoreCase(currency)) {
            return amount.multiply(usdInr).setScale(2, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("Unsupported currency " + currency);
    }
}
