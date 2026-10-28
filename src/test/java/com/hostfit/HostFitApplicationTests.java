package com.hostfit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hostfit.domain.HostingPlan;
import com.hostfit.repo.AdminUserRepository;
import com.hostfit.repo.HostingPlanRepository;
import com.hostfit.repo.ProviderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HostFitApplicationTests {

    @Autowired
    private ProviderRepository providers;
    @Autowired
    private HostingPlanRepository plans;
    @Autowired
    private AdminUserRepository admins;

    @Test
    void contextLoadsAndSeedsStarterData() {
        assertEquals(11, providers.count());
        assertTrue(plans.count() >= 40);
        assertTrue(admins.findByUsername("admin").isPresent());
    }

    @Test
    void everySeededPlanHasSaneValues() {
        List<HostingPlan> all = plans.findAllWithProvider();
        for (HostingPlan p : all) {
            assertTrue(p.getPriceInr().signum() > 0, p.getName());
            assertTrue(p.getVcpu() >= 1, p.getName());
            assertTrue(p.getRamGb() > 0, p.getName());
            assertTrue(p.getStorageGb() >= 1, p.getName());
            assertTrue(p.isUnmetered() || p.getBandwidthGb() != null, p.getName() + " needs bandwidth info");
        }
    }
}
