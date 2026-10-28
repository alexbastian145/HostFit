package com.hostfit.crawler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class PageFetcherTest {

    @Test
    void privateAndLocalAddressesAreRejected() throws Exception {
        for (String ip : new String[] {"127.0.0.1", "10.1.2.3", "172.16.5.4", "192.168.0.10", "169.254.169.254",
                "100.64.0.1", "0.0.0.0", "::1", "fd00::1"}) {
            assertTrue(PageFetcher.isPrivate(InetAddress.getByName(ip)), ip + " should be private");
        }
    }

    @Test
    void publicAddressesAreAllowed() throws Exception {
        for (String ip : new String[] {"8.8.8.8", "1.1.1.1", "172.32.0.1", "2606:4700:4700::1111"}) {
            assertFalse(PageFetcher.isPrivate(InetAddress.getByName(ip)), ip + " should be public");
        }
    }
}
