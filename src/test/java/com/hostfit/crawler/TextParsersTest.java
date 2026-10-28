package com.hostfit.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TextParsersTest {

    @Test
    void extractsNumbersWithThousandsSeparators() {
        assertEquals(java.util.List.of(new BigDecimal("1499"), new BigDecimal("299.50")),
                TextParsers.numbers("₹1,499 now only ₹299.50/mo"));
        assertTrue(TextParsers.numbers("no digits here").isEmpty());
        assertTrue(TextParsers.numbers(null).isEmpty());
    }

    @Test
    void priceStrategies() {
        String text = "₹ 599 ₹ 299/mo";
        assertEquals(new BigDecimal("299"), TextParsers.price(text, "last"));
        assertEquals(new BigDecimal("299"), TextParsers.price(text, null));
        assertEquals(new BigDecimal("599"), TextParsers.price(text, "first"));
        assertEquals(new BigDecimal("299"), TextParsers.price(text, "min"));
        assertNull(TextParsers.price("Contact sales", "last"));
    }

    @Test
    void convertsSizesToGigabytes() {
        assertEquals(4.0, TextParsers.gigabytes("4 GB RAM"), 1e-9);
        assertEquals(4.0, TextParsers.gigabytes("4096 MB"), 1e-9);
        assertEquals(2048.0, TextParsers.gigabytes("2 TB"), 1e-9);
        assertEquals(100.0, TextParsers.gigabytes("100"), 1e-9);
        assertEquals(1500.0, TextParsers.gigabytes("1,500GB"), 1e-9);
        assertNull(TextParsers.gigabytes("n/a"));
        assertNull(TextParsers.gigabytes(null));
    }

    @Test
    void detectsUnmeteredBandwidth() {
        assertTrue(TextParsers.isUnmetered("Unmetered bandwidth"));
        assertTrue(TextParsers.isUnmetered("UNLIMITED"));
        assertFalse(TextParsers.isUnmetered("3 TB"));
        assertFalse(TextParsers.isUnmetered(null));
    }

    @Test
    void slugifiesNames() {
        assertEquals("premium-web-hosting", TextParsers.slug("  Premium Web Hosting! "));
        assertEquals("kvm-2", TextParsers.slug("KVM 2"));
        assertEquals("plan", TextParsers.slug("***"));
    }
}
