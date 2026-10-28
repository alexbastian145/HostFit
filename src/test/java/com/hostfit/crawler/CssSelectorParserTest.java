package com.hostfit.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hostfit.core.PlanType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CssSelectorParserTest {

    private static final String PAGE = """
            <html><body>
              <div class="plan">
                <h3>Premium Web Hosting</h3>
                <div class="price"><s>₹599</s> ₹1,299/mo</div>
                <ul><li class="cpu">1 vCPU</li><li class="ram">2 GB RAM</li>
                    <li class="disk">100 GB SSD</li><li class="bw">Unmetered bandwidth</li></ul>
              </div>
              <div class="plan">
                <h3>Business Hosting</h3>
                <div class="price">₹2,499/mo</div>
                <ul><li class="cpu">2 vCPU</li><li class="ram">4096 MB</li>
                    <li class="disk">1 TB NVMe</li><li class="bw">3 TB</li></ul>
              </div>
              <div class="plan"><h3>Call us</h3><div class="price">Contact sales</div></div>
            </body></html>
            """;

    private static final String CONFIG = """
            {"cardSelector":".plan","nameSelector":"h3","priceSelector":".price","vcpuSelector":".cpu",
             "ramSelector":".ram","storageSelector":".disk","bandwidthSelector":".bw",
             "planType":"shared","currency":"inr","managed":true}
            """;

    @Test
    void extractsPlansUsingConfiguredSelectors() throws Exception {
        List<ParsedPlan> plans = new CssSelectorParser().parse(PAGE, CONFIG, "https://example.test/pricing");

        assertEquals(2, plans.size(), "card without a numeric price is skipped");
        ParsedPlan first = plans.get(0);
        assertEquals("css:premium-web-hosting", first.externalKey());
        assertEquals("Premium Web Hosting", first.name());
        assertEquals(PlanType.SHARED, first.planType());
        assertEquals(new BigDecimal("1299"), first.price(), "last number wins by default");
        assertEquals("INR", first.currency());
        assertEquals(1, first.vcpu());
        assertEquals(2.0, first.ramGb(), 1e-9);
        assertEquals(100, first.storageGb());
        assertTrue(first.unmetered());
        assertNull(first.bandwidthGb());
        assertTrue(first.managed());
        assertEquals("https://example.test/pricing", first.url());

        ParsedPlan second = plans.get(1);
        assertEquals(4.0, second.ramGb(), 1e-9);
        assertEquals(1024, second.storageGb());
        assertEquals(3072, second.bandwidthGb());
    }

    @Test
    void failsLoudlyWhenLayoutChanges() {
        String config = "{\"cardSelector\":\".gone\",\"nameSelector\":\"h3\",\"priceSelector\":\".price\"}";
        ParseException e = assertThrows(ParseException.class, () -> new CssSelectorParser().parse(PAGE, config, null));
        assertTrue(e.getMessage().contains(".gone"));
    }

    @Test
    void failsWhenNoCardHasNameAndPrice() {
        String config = "{\"cardSelector\":\".plan\",\"nameSelector\":\".nope\",\"priceSelector\":\".price\"}";
        assertThrows(ParseException.class, () -> new CssSelectorParser().parse(PAGE, config, null));
    }

    @Test
    void validatesConfiguration() {
        CssSelectorParser parser = new CssSelectorParser();
        assertThrows(ParseException.class, () -> parser.parse(PAGE, null, null));
        assertThrows(ParseException.class, () -> parser.parse(PAGE, "{oops", null));
        assertThrows(ParseException.class, () -> parser.parse(PAGE, "{\"cardSelector\":\".plan\"}", null));
        String badType = "{\"cardSelector\":\".plan\",\"nameSelector\":\"h3\",\"priceSelector\":\".price\",\"planType\":\"HUGE\"}";
        assertThrows(ParseException.class, () -> parser.parse(PAGE, badType, null));
    }
}
