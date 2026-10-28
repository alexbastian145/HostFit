package com.hostfit.crawler;

import com.hostfit.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Polite HTTP client for the crawler: identifies itself, honours robots.txt for HTML pages,
 * limits response size, and refuses to contact private/loopback addresses (SSRF protection,
 * since administrators supply the URLs).
 */
@Component
public class PageFetcher {

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private final AppProperties props;
    private final HttpClient client;

    public PageFetcher(AppProperties props) {
        this.props = props;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(Math.max(1, props.crawler().timeoutSeconds())))
                .build();
    }

    /**
     * @param checkRobots whether to consult robots.txt first (used for HTML scraping, not documented public APIs)
     */
    public String fetch(String url, boolean checkRobots) throws IOException {
        URI uri = validate(url);
        if (checkRobots && props.crawler().respectRobots()) {
            enforceRobots(uri);
        }
        return get(uri);
    }

    URI validate(String url) throws IOException {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL: " + url);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("Only http and https URLs can be crawled");
        }
        if (uri.getHost() == null) {
            throw new IOException("URL has no host");
        }
        if (!props.crawler().allowPrivateHosts()) {
            rejectPrivateHost(uri.getHost());
        }
        return uri;
    }

    static void rejectPrivateHost(String host) throws IOException {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("Cannot resolve host " + host);
        }
        for (InetAddress a : addresses) {
            if (isPrivate(a)) {
                throw new IOException("Refusing to crawl private or local address " + a.getHostAddress());
            }
        }
    }

    static boolean isPrivate(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 16 && (b[0] & 0xFE) == 0xFC) {
            return true; // IPv6 unique local fc00::/7
        }
        return b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64; // carrier-grade NAT 100.64/10
    }

    private void enforceRobots(URI uri) throws IOException {
        URI robotsUri = URI.create(uri.getScheme() + "://" + uri.getRawAuthority() + "/robots.txt");
        String robots;
        try {
            robots = get(robotsUri);
        } catch (IOException e) {
            return; // no readable robots.txt means no restrictions
        }
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        RobotsRules rules = RobotsRules.parse(robots, agentToken());
        if (!rules.isAllowed(path)) {
            throw new IOException("robots.txt disallows crawling " + path + " on " + uri.getHost());
        }
    }

    String agentToken() {
        String ua = props.crawler().userAgent();
        int slash = ua.indexOf('/');
        int space = ua.indexOf(' ');
        int end = slash > 0 ? slash : (space > 0 ? space : ua.length());
        return ua.substring(0, end);
    }

    private String get(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, props.crawler().timeoutSeconds())))
                .header("User-Agent", props.crawler().userAgent())
                .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            try (InputStream in = response.body()) {
                if (status < 200 || status >= 300) {
                    throw new IOException("HTTP " + status + " from " + uri.getHost());
                }
                return readLimited(in);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + uri.getHost(), e);
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            if (out.size() > MAX_BYTES) {
                throw new IOException("Response larger than " + (MAX_BYTES / (1024 * 1024)) + " MB");
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
