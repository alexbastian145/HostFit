package com.hostfit.crawler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small helpers for pulling numbers and sizes out of marketing text such as "₹1,499/mo" or "4 GB RAM". */
public final class TextParsers {

    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final Pattern SIZE = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)\\s*(tb|tib|gb|gib|mb|mib)?", Pattern.CASE_INSENSITIVE);

    private TextParsers() {
    }

    /** All numbers in the text, in order; thousands separators are removed. */
    public static List<BigDecimal> numbers(String text) {
        List<BigDecimal> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String raw = m.group().replace(",", "");
            try {
                out.add(new BigDecimal(raw));
            } catch (NumberFormatException ignored) {
                // skip tokens such as "1.2.3"
            }
        }
        return out;
    }

    /** Picks a price out of text like "₹499 ₹299/mo": strategy is "first", "last" (default) or "min". */
    public static BigDecimal price(String text, String strategy) {
        List<BigDecimal> nums = numbers(text);
        if (nums.isEmpty()) {
            return null;
        }
        if ("first".equalsIgnoreCase(strategy)) {
            return nums.get(0);
        }
        if ("min".equalsIgnoreCase(strategy)) {
            return nums.stream().min(BigDecimal::compareTo).orElse(null);
        }
        return nums.get(nums.size() - 1);
    }

    /**
     * Parses a size such as "4 GB", "4096 MB" or "1.5 TB" and returns gigabytes.
     * A bare number is taken to already be in gigabytes. Returns null if no number is present.
     */
    public static Double gigabytes(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = SIZE.matcher(text);
        if (!m.find()) {
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        String unit = m.group(2) == null ? "gb" : m.group(2).toLowerCase(Locale.ROOT);
        if (unit.startsWith("t")) {
            return value * 1024.0;
        }
        if (unit.startsWith("m")) {
            return value / 1024.0;
        }
        return value;
    }

    public static boolean isUnmetered(String text) {
        if (text == null) {
            return false;
        }
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("unmetered") || t.contains("unlimited");
    }

    /** Lower-case, hyphen-separated identifier derived from a plan name. */
    public static String slug(String text) {
        String s = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "plan" : s;
    }
}
