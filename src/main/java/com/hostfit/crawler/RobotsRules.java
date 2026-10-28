package com.hostfit.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal robots.txt interpreter: honours Allow/Disallow for the group that matches our user-agent
 * token (falling back to "*"), with longest-match precedence and Allow winning ties.
 */
public final class RobotsRules {

    private final List<String> allow;
    private final List<String> disallow;

    private RobotsRules(List<String> allow, List<String> disallow) {
        this.allow = allow;
        this.disallow = disallow;
    }

    public static RobotsRules allowAll() {
        return new RobotsRules(List.of(), List.of());
    }

    public static RobotsRules parse(String robotsTxt, String agentToken) {
        if (robotsTxt == null || robotsTxt.isBlank()) {
            return allowAll();
        }
        String token = agentToken.toLowerCase(Locale.ROOT);
        List<String> specificAllow = new ArrayList<>();
        List<String> specificDisallow = new ArrayList<>();
        List<String> starAllow = new ArrayList<>();
        List<String> starDisallow = new ArrayList<>();
        boolean sawSpecific = false;

        List<String> currentAgents = new ArrayList<>();
        boolean lastWasAgent = false;
        for (String rawLine : robotsTxt.split("\\r?\\n")) {
            String line = rawLine;
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if ("user-agent".equals(field)) {
                if (!lastWasAgent) {
                    currentAgents = new ArrayList<>();
                }
                currentAgents.add(value.toLowerCase(Locale.ROOT));
                lastWasAgent = true;
                continue;
            }
            lastWasAgent = false;
            boolean isAllow = "allow".equals(field);
            boolean isDisallow = "disallow".equals(field);
            if (!isAllow && !isDisallow) {
                continue;
            }
            boolean forSpecific = currentAgents.stream().anyMatch(a -> !a.equals("*") && token.contains(a));
            boolean forStar = currentAgents.contains("*");
            if (forSpecific) {
                sawSpecific = true;
                (isAllow ? specificAllow : specificDisallow).add(value);
            } else if (forStar) {
                (isAllow ? starAllow : starDisallow).add(value);
            }
        }
        return sawSpecific ? new RobotsRules(specificAllow, specificDisallow) : new RobotsRules(starAllow, starDisallow);
    }

    public boolean isAllowed(String path) {
        String p = (path == null || path.isEmpty()) ? "/" : path;
        int bestAllow = longestMatch(allow, p);
        int bestDisallow = longestMatch(disallow, p);
        if (bestDisallow < 0) {
            return true;
        }
        return bestAllow >= bestDisallow;
    }

    private static int longestMatch(List<String> rules, String path) {
        int best = -1;
        for (String rule : rules) {
            if (rule.isEmpty()) {
                continue; // "Disallow:" with no value means allow everything
            }
            String r = rule;
            boolean anchored = r.endsWith("$");
            if (anchored) {
                r = r.substring(0, r.length() - 1);
            }
            boolean matches;
            if (r.contains("*")) {
                matches = wildcardMatch(r, path, anchored);
            } else {
                matches = anchored ? path.equals(r) : path.startsWith(r);
            }
            if (matches) {
                best = Math.max(best, r.length());
            }
        }
        return best;
    }

    private static boolean wildcardMatch(String pattern, String path, boolean anchored) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
            }
        }
        if (anchored) {
            regex.append('$');
        }
        return java.util.regex.Pattern.compile(regex.toString()).matcher(path).find();
    }
}
