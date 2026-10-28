package com.hostfit.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed view of the {@code hostfit.*} settings in application.yml. */
@ConfigurationProperties(prefix = "hostfit")
public record AppProperties(
        @DefaultValue Admin admin,
        @DefaultValue Fx fx,
        @DefaultValue Crawler crawler,
        @DefaultValue Seed seed) {

    public record Admin(
            @DefaultValue("admin") String username,
            @DefaultValue("admin123") String password) {
    }

    public record Fx(
            @DefaultValue("88.0") BigDecimal usdInr,
            @DefaultValue("false") boolean autoRefresh,
            @DefaultValue("https://api.frankfurter.app/latest?from=USD&to=INR") String rateUrl) {
    }

    public record Crawler(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 */6 * * *") String cron,
            @DefaultValue("HostFitBot/1.0 (student project; price comparison)") String userAgent,
            @DefaultValue("20") int timeoutSeconds,
            @DefaultValue("1500") long delayMs,
            @DefaultValue("true") boolean respectRobots,
            @DefaultValue("14") int staleDays,
            @DefaultValue("false") boolean allowPrivateHosts) {
    }

    public record Seed(@DefaultValue("true") boolean enabled) {
    }
}
