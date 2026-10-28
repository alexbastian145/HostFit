package com.hostfit.web;

import com.hostfit.domain.HostingPlan;
import com.hostfit.service.PlanService;
import com.hostfit.service.ProviderService;
import com.hostfit.service.SourceService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Admin dashboard: data health at a glance. */
@Controller
public class AdminController {

    private final PlanService plans;
    private final ProviderService providers;
    private final SourceService sources;
    private final UiHelper ui;

    public AdminController(PlanService plans, ProviderService providers, SourceService sources, UiHelper ui) {
        this.plans = plans;
        this.providers = providers;
        this.sources = sources;
        this.ui = ui;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        List<HostingPlan> all = plans.findAll();
        List<HostingPlan> attention = all.stream().filter(p -> p.isActive() && ui.needsAttention(p)).toList();
        model.addAttribute("planCount", all.size());
        model.addAttribute("activeCount", all.stream().filter(HostingPlan::isActive).count());
        model.addAttribute("providerCount", providers.findAll().size());
        model.addAttribute("sourceCount", sources.findAll().size());
        model.addAttribute("attentionCount", attention.size());
        model.addAttribute("attention", attention.stream().limit(10).toList());
        model.addAttribute("runs", sources.recentRuns(6));
        return "admin/dashboard";
    }
}
