package com.hostfit.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** End-to-end tests of the web layer against the seeded in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class WebFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void homePageShowsTheRequirementForm() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recommend plans")));
    }

    @Test
    void exampleLinkPrefillsTheDeckExample() throws Exception {
        mvc.perform(get("/").param("example", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"50000\"")));
    }

    @Test
    void validSubmissionRedirectsToResultsWithRecommendations() throws Exception {
        MvcResult post = mvc.perform(post("/recommend")
                        .with(csrf())
                        .param("useCase", "ECOMMERCE")
                        .param("monthlyVisits", "50000")
                        .param("storageGb", "50")
                        .param("budgetInr", "2000")
                        .param("expertise", "SELF_MANAGED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/results/*"))
                .andReturn();

        mvc.perform(get(post.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Capacity guidance")))
                .andExpect(content().string(containsString("Best match")));
    }

    @Test
    void invalidSubmissionShowsErrorsAndKeepsTheForm() throws Exception {
        mvc.perform(post("/recommend").with(csrf())
                        .param("useCase", "BLOG")
                        .param("monthlyVisits", "0")
                        .param("storageGb", "")
                        .param("budgetInr", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Monthly visits must be at least 1")))
                .andExpect(content().string(containsString("Enter the storage you need")))
                .andExpect(content().string(containsString("Budget must be at least")))
                .andExpect(content().string(containsString("Choose how much server management")));
    }

    @Test
    void impossibleBudgetShowsTheMinimumBudgetNeeded() throws Exception {
        MvcResult post = mvc.perform(post("/recommend").with(csrf())
                        .param("useCase", "DATABASE")
                        .param("monthlyVisits", "200000")
                        .param("storageGb", "500")
                        .param("budgetInr", "100")
                        .param("expertise", "SELF_MANAGED"))
                .andReturn();

        mvc.perform(get(post.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No plan fits all of your requirements")));
    }

    @Test
    void browsePageListsPlansAndFilters() throws Exception {
        mvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hostinger")));
        mvc.perform(get("/plans").param("type", "DEDICATED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AX41-NVMe")));
    }

    @Test
    void unknownPlanIsA404() throws Exception {
        mvc.perform(get("/plans/999999")).andExpect(status().isNotFound());
    }

    @Test
    void compareNeedsTwoPlans() throws Exception {
        mvc.perform(get("/compare").param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Select at least two plans")));
        mvc.perform(get("/compare").param("ids", "1").param("ids", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Price / month")));
    }

    @Test
    void adminAreaRequiresLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
        mvc.perform(get("/admin/plans"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSeeDashboardAndPlans() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin dashboard")));
        mvc.perform(get("/admin/plans")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hosting plans")));
        mvc.perform(get("/admin/crawl")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Price crawler")));
        mvc.perform(get("/admin/providers")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAddAPlanAndValidationIsEnforced() throws Exception {
        mvc.perform(post("/admin/plans").with(csrf())
                        .param("name", "")
                        .param("priceInr", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plan name is required")));

        mvc.perform(post("/admin/plans").with(csrf())
                        .param("providerId", "1")
                        .param("name", "Test Plan")
                        .param("planType", "VPS")
                        .param("vcpu", "2")
                        .param("ramGb", "4")
                        .param("storageGb", "80")
                        .param("priceInr", "999")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/plans"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPostWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/admin/plans/1/toggle")).andExpect(status().isForbidden());
    }

    @Test
    void apiReturnsRecommendationsAsJson() throws Exception {
        String body = "{\"useCase\":\"ECOMMERCE\",\"monthlyVisits\":50000,\"storageGb\":50,"
                + "\"budgetInr\":2000,\"expertise\":\"SELF_MANAGED\"}";
        mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.need.minVcpu").value(2))
                .andExpect(jsonPath("$.recommendations[0].score").isNumber())
                .andExpect(jsonPath("$.recommendations[0].plan.priceInr").isNumber());
    }

    @Test
    void apiRejectsInvalidInput() throws Exception {
        mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"useCase\":\"BLOG\",\"monthlyVisits\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.monthlyVisits").exists());
        mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiListsPlans() throws Exception {
        mvc.perform(get("/api/plans").param("type", "SHARED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SHARED"));
        mvc.perform(get("/api/plans/999999")).andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
