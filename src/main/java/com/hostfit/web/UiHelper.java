package com.hostfit.web;

import com.hostfit.config.AppProperties;
import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PriceSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Formatting helpers exposed to templates as {@code @ui}. */
@Component("ui")
public class UiHelper {

    private static final Locale EN_IN = Locale.forLanguageTag("en-IN");

    /** Badge describing how trustworthy a plan's price is. */
    public record Freshness(String label, String css, String title) {
    }

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final AppProperties props;

    public UiHelper(AppProperties props) {
        this.props = props;
    }

    /** ₹1,499 with Indian digit grouping (₹1,00,000). */
    public String inr(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(EN_IN);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return "₹" + nf.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    /** Yearly cost (12 x monthly) formatted as rupees. */
    public String yearly(BigDecimal monthly) {
        return monthly == null ? "—" : inr(monthly.multiply(BigDecimal.valueOf(12)));
    }

    public String when(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant);
    }

    public String num(Number n) {
        if (n == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(EN_IN);
        return nf.format(Math.round(n.doubleValue()));
    }

    public String ram(double gb) {
        return trim(gb) + " GB";
    }

    public String storage(int gb, String type) {
        String size = gb >= 1024 && gb % 1024 == 0 ? (gb / 1024) + " TB" : num(gb) + " GB";
        return type == null || type.isBlank() ? size : size + " " + type;
    }

    public String bandwidth(Integer gb, boolean unmetered) {
        if (unmetered) {
            return "Unmetered";
        }
        if (gb == null) {
            return "Not stated";
        }
        if (gb >= 1024) {
            return trim(gb / 1024.0) + " TB";
        }
        return num(gb) + " GB";
    }

    public String ago(Instant instant) {
        if (instant == null) {
            return "never";
        }
        Duration d = Duration.between(instant, Instant.now());
        long minutes = Math.max(0, d.toMinutes());
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        if (minutes < 60 * 48) {
            return (minutes / 60) + " h ago";
        }
        return (minutes / (60 * 24)) + " days ago";
    }

    public Freshness freshness(HostingPlan plan) {
        if (plan.getPriceSource() == PriceSource.SEED || plan.getPriceVerifiedAt() == null) {
            return new Freshness("Estimate", "badge-warn",
                    "Starter estimate, not yet verified. Run the price crawler or confirm it in Admin.");
        }
        long ageDays = Duration.between(plan.getPriceVerifiedAt(), Instant.now()).toDays();
        String when = ago(plan.getPriceVerifiedAt());
        if (ageDays > props.crawler().staleDays()) {
            return new Freshness("Stale · " + when, "badge-warn",
                    "This price has not been re-checked for over " + props.crawler().staleDays() + " days.");
        }
        if (plan.getPriceSource() == PriceSource.CRAWLED) {
            return new Freshness("Live · " + when, "badge-ok", "Price fetched automatically from the provider.");
        }
        return new Freshness("Verified · " + when, "badge-ok", "Price confirmed by an administrator.");
    }

    public boolean needsAttention(HostingPlan plan) {
        return !"badge-ok".equals(freshness(plan).css());
    }

    private static String trim(double v) {
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
