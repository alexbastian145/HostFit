package com.hostfit.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hostfit.core.PlanType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Parser tests run against small fixtures shaped like the providers' public API responses. */
class ApiParsersTest {

    private static final String LINODE = """
            {"data":[
              {"id":"g6-nanode-1","label":"Nanode 1GB","class":"nanode","memory":1024,"disk":25600,"transfer":1000,"vcpus":1,
               "price":{"hourly":0.0075,"monthly":5.0}},
              {"id":"g6-standard-2","label":"Linode 4GB","class":"standard","memory":4096,"disk":81920,"transfer":4000,"vcpus":2,
               "price":{"hourly":0.036,"monthly":24.0}},
              {"id":"g6-dedicated-4","label":"Dedicated 8GB","class":"dedicated","memory":8192,"disk":163840,"transfer":5000,"vcpus":4,
               "price":{"hourly":0.108,"monthly":72.0}},
              {"id":"g1-gpu-rtx6000-1","label":"RTX6000 GPU x1","class":"gpu","memory":16384,"disk":655360,"transfer":16000,"vcpus":8,
               "price":{"hourly":1.5,"monthly":1000.0}}
            ],"page":1,"pages":1,"results":4}
            """;

    private static final String VULTR = """
            {"plans":[
              {"id":"vc2-1c-1gb","vcpu_count":1,"ram":1024,"disk":25,"disk_count":1,"bandwidth":1024,"monthly_cost":5,"type":"vc2"},
              {"id":"vc2-2c-4gb","vcpu_count":2,"ram":4096,"disk":80,"disk_count":1,"bandwidth":3072,"monthly_cost":20,"type":"vc2"},
              {"id":"vdc-4vcpu-16gb","vcpu_count":4,"ram":16384,"disk":150,"disk_count":1,"bandwidth":5120,"monthly_cost":120,"type":"vdc"},
              {"id":"vbm-4c-32gb","vcpu_count":4,"ram":32768,"disk":480,"disk_count":2,"bandwidth":5120,"monthly_cost":120,"type":"vbm"},
              {"id":"vcg-a100-1c-6g","vcpu_count":12,"ram":61440,"disk":350,"disk_count":1,"bandwidth":10240,"monthly_cost":900,"type":"vcg"}
            ],"meta":{"total":5}}
            """;

    @Test
    void linodeParserMapsClassesAndConvertsUnits() throws Exception {
        List<ParsedPlan> plans = new LinodeApiParser().parse(LINODE, null, LinodeApiParser.DEFAULT_URL);

        assertEquals(3, plans.size(), "GPU plans are skipped");
        ParsedPlan nanode = plans.get(0);
        assertEquals("linode:g6-nanode-1", nanode.externalKey());
        assertEquals("Nanode 1GB", nanode.name());
        assertEquals(PlanType.VPS, nanode.planType());
        assertEquals(1, nanode.vcpu());
        assertEquals(1.0, nanode.ramGb(), 1e-9);
        assertEquals(25, nanode.storageGb());
        assertEquals(1000, nanode.bandwidthGb());
        assertEquals(new BigDecimal("5.0"), nanode.price());
        assertEquals("USD", nanode.currency());

        assertEquals(PlanType.CLOUD, plans.get(2).planType());
        assertEquals(160, plans.get(2).storageGb());
    }

    @Test
    void linodeParserRejectsUnexpectedPayloads() {
        assertThrows(ParseException.class, () -> new LinodeApiParser().parse("not json", null, null));
        assertThrows(ParseException.class, () -> new LinodeApiParser().parse("{\"errors\":[]}", null, null));
    }

    @Test
    void vultrParserMapsFamiliesAndMultiplesDisks() throws Exception {
        List<ParsedPlan> plans = new VultrApiParser().parse(VULTR, null, VultrApiParser.DEFAULT_URL);

        assertEquals(4, plans.size(), "GPU family is skipped");
        ParsedPlan small = plans.get(0);
        assertEquals("vultr:vc2-1c-1gb", small.externalKey());
        assertEquals("Cloud Compute 1 vCPU / 1 GB", small.name());
        assertEquals(PlanType.VPS, small.planType());
        assertEquals(1024, small.bandwidthGb());

        assertEquals(PlanType.CLOUD, plans.get(2).planType());
        ParsedPlan metal = plans.get(3);
        assertEquals(PlanType.DEDICATED, metal.planType());
        assertEquals(960, metal.storageGb(), "two 480 GB disks");
    }

    @Test
    void vultrParserRejectsUnexpectedPayloads() {
        assertThrows(ParseException.class, () -> new VultrApiParser().parse("<html>", null, null));
        assertThrows(ParseException.class, () -> new VultrApiParser().parse("{\"error\":\"x\"}", null, null));
    }

    @Test
    void unknownClassesAreIgnored() {
        assertNull(LinodeApiParser.mapClass("highmem"));
        assertNull(LinodeApiParser.mapClass(null));
        assertNull(VultrApiParser.mapFamily("vcg"));
        assertTrue(VultrApiParser.mapFamily("vhf") == PlanType.VPS);
    }
}
