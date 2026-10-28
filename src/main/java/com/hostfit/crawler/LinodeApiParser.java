package com.hostfit.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostfit.core.PlanType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Linode's public instance-type catalogue
 * ({@code GET https://api.linode.com/v4/linode/types}, no API key needed).
 * Shared-CPU classes map to VPS, dedicated-CPU classes to CLOUD.
 */
@Component
public class LinodeApiParser implements PriceParser {

    public static final String DEFAULT_URL = "https://api.linode.com/v4/linode/types?page_size=200";
    private static final String PRICING_PAGE = "https://www.linode.com/pricing/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public CrawlSourceType type() {
        return CrawlSourceType.LINODE_API;
    }

    @Override
    public List<ParsedPlan> parse(String body, String config, String url) throws ParseException {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            throw new ParseException("Linode response is not valid JSON", e);
        }
        JsonNode data = root == null ? null : root.get("data");
        if (data == null || !data.isArray()) {
            throw new ParseException("Linode response has no 'data' array");
        }

        List<ParsedPlan> plans = new ArrayList<>();
        for (JsonNode n : data) {
            String id = text(n, "id");
            String cls = text(n, "class");
            PlanType type = mapClass(cls);
            JsonNode price = n.get("price");
            if (id == null || type == null || price == null || !price.hasNonNull("monthly")) {
                continue;
            }
            BigDecimal monthly = BigDecimal.valueOf(price.get("monthly").asDouble());
            if (monthly.signum() <= 0 || !n.hasNonNull("vcpus") || !n.hasNonNull("memory") || !n.hasNonNull("disk")) {
                continue;
            }
            Integer transfer = n.hasNonNull("transfer") ? n.get("transfer").asInt() : null;
            String label = text(n, "label");
            plans.add(new ParsedPlan(
                    "linode:" + id,
                    label == null ? id : label,
                    type,
                    n.get("vcpus").asInt(),
                    n.get("memory").asDouble() / 1024.0,
                    (int) Math.round(n.get("disk").asDouble() / 1024.0),
                    "SSD",
                    transfer,
                    false,
                    monthly,
                    "USD",
                    Boolean.FALSE,
                    PRICING_PAGE));
        }
        return plans;
    }

    static PlanType mapClass(String cls) {
        if (cls == null) {
            return null;
        }
        switch (cls) {
            case "nanode":
            case "standard":
                return PlanType.VPS;
            case "dedicated":
            case "premium":
                return PlanType.CLOUD;
            default:
                return null; // gpu, highmem, accelerated... not relevant for general hosting
        }
    }

    private static String text(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asText() : null;
    }
}
