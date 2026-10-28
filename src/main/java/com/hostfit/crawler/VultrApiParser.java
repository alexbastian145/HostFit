package com.hostfit.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostfit.core.PlanType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses Vultr's public plan catalogue
 * ({@code GET https://api.vultr.com/v2/plans}, no API key needed).
 * Shared-CPU families map to VPS, dedicated-CPU families to CLOUD, bare metal to DEDICATED.
 */
@Component
public class VultrApiParser implements PriceParser {

    public static final String DEFAULT_URL = "https://api.vultr.com/v2/plans?type=all&per_page=500";
    private static final String PRICING_PAGE = "https://www.vultr.com/pricing/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public CrawlSourceType type() {
        return CrawlSourceType.VULTR_API;
    }

    @Override
    public List<ParsedPlan> parse(String body, String config, String url) throws ParseException {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            throw new ParseException("Vultr response is not valid JSON", e);
        }
        JsonNode plans = root == null ? null : root.get("plans");
        if (plans == null || !plans.isArray()) {
            throw new ParseException("Vultr response has no 'plans' array");
        }

        List<ParsedPlan> out = new ArrayList<>();
        for (JsonNode n : plans) {
            String id = n.hasNonNull("id") ? n.get("id").asText() : null;
            String family = n.hasNonNull("type") ? n.get("type").asText() : null;
            PlanType type = mapFamily(family);
            if (id == null || type == null || !n.hasNonNull("monthly_cost")
                    || !n.hasNonNull("vcpu_count") || !n.hasNonNull("ram") || !n.hasNonNull("disk")) {
                continue;
            }
            BigDecimal monthly = BigDecimal.valueOf(n.get("monthly_cost").asDouble());
            if (monthly.signum() <= 0) {
                continue;
            }
            int vcpu = n.get("vcpu_count").asInt();
            double ramGb = n.get("ram").asDouble() / 1024.0;
            int disks = Math.max(1, n.hasNonNull("disk_count") ? n.get("disk_count").asInt() : 1);
            int storage = n.get("disk").asInt() * disks;
            Integer bandwidth = n.hasNonNull("bandwidth") ? n.get("bandwidth").asInt() : null;
            String storageType = "NVMe";

            out.add(new ParsedPlan(
                    "vultr:" + id,
                    friendlyName(family, vcpu, ramGb),
                    type,
                    vcpu,
                    ramGb,
                    storage,
                    storageType,
                    bandwidth,
                    false,
                    monthly,
                    "USD",
                    Boolean.FALSE,
                    PRICING_PAGE));
        }
        return out;
    }

    static PlanType mapFamily(String family) {
        if (family == null) {
            return null;
        }
        switch (family) {
            case "vc2":
            case "vhf":
            case "vhp":
                return PlanType.VPS;
            case "vdc":
            case "voc":
                return PlanType.CLOUD;
            case "vbm":
                return PlanType.DEDICATED;
            default:
                return null; // vcg (GPU) and anything new are skipped
        }
    }

    static String friendlyName(String family, int vcpu, double ramGb) {
        String label;
        switch (family) {
            case "vc2":
                label = "Cloud Compute";
                break;
            case "vhf":
                label = "High Frequency";
                break;
            case "vhp":
                label = "High Performance";
                break;
            case "vdc":
                label = "Dedicated Cloud";
                break;
            case "voc":
                label = "Optimized Cloud";
                break;
            default:
                label = "Bare Metal";
                break;
        }
        String ram = ramGb == Math.rint(ramGb) ? String.valueOf((long) ramGb) : String.format(Locale.ROOT, "%.1f", ramGb);
        return label + " " + vcpu + " vCPU / " + ram + " GB";
    }
}
