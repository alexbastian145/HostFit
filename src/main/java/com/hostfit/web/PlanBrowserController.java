package com.hostfit.web;

import com.hostfit.core.PlanType;
import com.hostfit.domain.HostingPlan;
import com.hostfit.service.PlanFilter;
import com.hostfit.service.PlanService;
import com.hostfit.service.ProviderService;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Public "View available plans" pages. */
@Controller
public class PlanBrowserController {

    private final PlanService plans;
    private final ProviderService providers;

    public PlanBrowserController(PlanService plans, ProviderService providers) {
        this.plans = plans;
        this.providers = providers;
    }

    @GetMapping("/plans")
    public String list(@RequestParam(value = "type", required = false) PlanType type,
                       @RequestParam(value = "provider", required = false) Long providerId,
                       @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "sort", required = false, defaultValue = "price") String sort,
                       Model model) {
        model.addAttribute("plans", plans.browse(new PlanFilter(type, providerId, maxPrice, q, sort)));
        model.addAttribute("providers", providers.findAll());
        model.addAttribute("planTypes", PlanType.values());
        model.addAttribute("type", type);
        model.addAttribute("providerId", providerId);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        return "plans";
    }

    @GetMapping("/plans/{id}")
    public String detail(@PathVariable Long id, Model model) {
        HostingPlan plan = plans.get(id);
        model.addAttribute("plan", plan);
        model.addAttribute("history", plans.priceHistory(plan));
        return "plan-detail";
    }
}
