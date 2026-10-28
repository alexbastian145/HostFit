package com.hostfit.crawler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RobotsRulesTest {

    private static final String ROBOTS = """
            # comment
            User-agent: *
            Disallow: /private/
            Disallow: /checkout
            Allow: /private/public-info
            Disallow: /*.pdf$

            User-agent: BadBot
            Disallow: /
            """;

    @Test
    void honoursDisallowAndLongestMatchAllow() {
        RobotsRules rules = RobotsRules.parse(ROBOTS, "HostFitBot");

        assertTrue(rules.isAllowed("/pricing"));
        assertFalse(rules.isAllowed("/private/data"));
        assertFalse(rules.isAllowed("/checkout/cart"));
        assertTrue(rules.isAllowed("/private/public-info"));
        assertFalse(rules.isAllowed("/files/brochure.pdf"));
        assertTrue(rules.isAllowed("/files/brochure.pdf.html"));
    }

    @Test
    void specificGroupOverridesWildcardGroup() {
        RobotsRules rules = RobotsRules.parse(ROBOTS, "BadBot/1.0");

        assertFalse(rules.isAllowed("/pricing"));
    }

    @Test
    void emptyOrMissingRobotsAllowsEverything() {
        assertTrue(RobotsRules.parse("", "HostFitBot").isAllowed("/anything"));
        assertTrue(RobotsRules.parse(null, "HostFitBot").isAllowed("/anything"));
        assertTrue(RobotsRules.parse("User-agent: *\nDisallow:\n", "HostFitBot").isAllowed("/anything"));
        assertTrue(RobotsRules.allowAll().isAllowed("/"));
    }
}
