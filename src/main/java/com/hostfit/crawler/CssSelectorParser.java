package com.hostfit.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostfit.core.PlanType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic HTML scraper driven by {@link CssScrapeConfig}. Works for pricing pages that are rendered
 * server-side; pages that build their plan cards with JavaScript need a different source type.
 */
@Component
public class CssSelectorParser implements PriceParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public CrawlSourceType type() {
        return CrawlSourceType.CSS_SCRAPER;
    }

    @Override
    public List<ParsedPlan> parse(String body, String config, String url) throws ParseException {
        CssScrapeConfig cfg = readConfig(config);
        Document doc = Jsoup.parse(body, url == null ? "" : url);
        Elements cards;
        try {
            cards = doc.select(cfg.getCardSelector());
        } catch (Exception e) {
            throw new ParseException("Invalid cardSelector: " + e.getMessage(), e);
        }
        if (cards.isEmpty()) {
            throw new ParseException("No plan cards matched '" + cfg.getCardSelector()
                    + "'. The page layout may have changed or the plans are rendered by JavaScript.");
        }

        PlanType planType = parsePlanType(cfg.getPlanType());
        List<ParsedPlan> out = new ArrayList<>();
        for (Element card : cards) {
            String name = text(card, cfg.getNameSelector());
            BigDecimal price = TextParsers.price(text(card, cfg.getPriceSelector()), cfg.getPricePick());
            if (name == null || name.isBlank() || price == null || price.signum() <= 0) {
                continue;
            }
            Double vcpu = number(text(card, cfg.getVcpuSelector()));
            Double ram = TextParsers.gigabytes(text(card, cfg.getRamSelector()));
            Double storage = TextParsers.gigabytes(text(card, cfg.getStorageSelector()));
            String bandwidthText = text(card, cfg.getBandwidthSelector());
            boolean unmetered = TextParsers.isUnmetered(bandwidthText);
            Double bandwidth = unmetered ? null : TextParsers.gigabytes(bandwidthText);

            out.add(new ParsedPlan(
                    "css:" + TextParsers.slug(name),
                    name.trim(),
                    planType,
                    vcpu == null ? null : (int) Math.round(vcpu),
                    ram,
                    storage == null ? null : (int) Math.round(storage),
                    cfg.getStorageType(),
                    bandwidth == null ? null : (int) Math.round(bandwidth),
                    unmetered,
                    price,
                    cfg.getCurrency() == null ? "INR" : cfg.getCurrency().toUpperCase(),
                    cfg.isManaged(),
                    url));
        }
        if (out.isEmpty()) {
            throw new ParseException("Plan cards were found but none had both a name and a price. "
                    + "Check nameSelector and priceSelector.");
        }
        return out;
    }

    public static CssScrapeConfig readConfig(String config) throws ParseException {
        if (config == null || config.isBlank()) {
            throw new ParseException("This source needs a JSON config with cardSelector, nameSelector and priceSelector.");
        }
        CssScrapeConfig cfg;
        try {
            cfg = MAPPER.readValue(config, CssScrapeConfig.class);
        } catch (Exception e) {
            throw new ParseException("Config is not valid JSON: " + e.getMessage(), e);
        }
        if (isBlank(cfg.getCardSelector()) || isBlank(cfg.getNameSelector()) || isBlank(cfg.getPriceSelector())) {
            throw new ParseException("Config must set cardSelector, nameSelector and priceSelector.");
        }
        return cfg;
    }

    private static PlanType parsePlanType(String value) throws ParseException {
        try {
            return PlanType.valueOf(value == null ? "SHARED" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParseException("Unknown planType '" + value + "' (use SHARED, VPS, CLOUD or DEDICATED).");
        }
    }

    private static Double number(String text) {
        List<BigDecimal> nums = TextParsers.numbers(text);
        return nums.isEmpty() ? null : nums.get(0).doubleValue();
    }

    private static String text(Element card, String selector) {
        if (isBlank(selector)) {
            return null;
        }
        Element el = card.selectFirst(selector);
        return el == null ? null : el.text();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
