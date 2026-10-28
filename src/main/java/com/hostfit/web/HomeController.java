package com.hostfit.web;

import com.hostfit.core.Expertise;
import com.hostfit.core.MatchingEngine;
import com.hostfit.core.UseCase;
import com.hostfit.domain.HostingPlan;
import com.hostfit.service.PlanService;
import com.hostfit.service.RecommendationService;
import com.hostfit.service.RequirementForm;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** User &amp; Requirement module, Recommendation Result module and side-by-side comparison. */
@Controller
public class HomeController {

    private static final int MAX_COMPARE = 4;

    private final RecommendationService recommendations;
    private final PlanService plans;

    public HomeController(RecommendationService recommendations, PlanService plans) {
        this.recommendations = recommendations;
        this.plans = plans;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "example", required = false) String example, Model model) {
        RequirementForm form = new RequirementForm();
        if (example != null) {
            // The worked example from the Review 1 deck.
            form.setUseCase(UseCase.ECOMMERCE);
            form.setMonthlyVisits(50_000L);
            form.setStorageGb(50);
            form.setBudgetInr(new BigDecimal("2000"));
            form.setExpertise(Expertise.SELF_MANAGED);
        } else {
            form.setExpertise(Expertise.SELF_MANAGED);
        }
        model.addAttribute("form", form);
        addFormOptions(model);
        List<HostingPlan> active = plans.findActive();
        model.addAttribute("planCount", active.size());
        model.addAttribute("providerCount", active.stream().map(p -> p.getProvider().getId()).distinct().count());
        return "index";
    }

    @PostMapping("/recommend")
    public String recommend(@Valid @ModelAttribute("form") RequirementForm form, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            addFormOptions(model);
            List<HostingPlan> active = plans.findActive();
            model.addAttribute("planCount", active.size());
            model.addAttribute("providerCount", active.stream().map(p -> p.getProvider().getId()).distinct().count());
            return "index";
        }
        Long id = recommendations.save(form).getId();
        return "redirect:/results/" + id;
    }

    @GetMapping("/results/{id}")
    public String results(@PathVariable Long id, Model model) {
        RecommendationService.ResultView view = recommendations.resultFor(id);
        model.addAttribute("search", view.request());
        model.addAttribute("result", view.result());
        model.addAttribute("need", view.result().need());
        model.addAttribute("plans", view.plans());
        model.addAttribute("weights", MatchingEngine.weights());
        model.addAttribute("maxCompare", MAX_COMPARE);
        return "results";
    }

    @GetMapping("/compare")
    public String compare(@RequestParam(value = "ids", required = false) List<Long> ids,
                          @RequestParam(value = "req", required = false) Long requestId, Model model) {
        List<Long> distinct = ids == null ? List.of() : ids.stream().distinct().limit(MAX_COMPARE).toList();
        List<HostingPlan> found = plans.getAllById(distinct);
        Map<Long, HostingPlan> byId = new LinkedHashMap<>();
        for (HostingPlan p : found) {
            byId.put(p.getId(), p);
        }
        List<HostingPlan> ordered = new ArrayList<>();
        for (Long id : distinct) {
            HostingPlan p = byId.get(id);
            if (p != null) {
                ordered.add(p);
            }
        }
        model.addAttribute("selected", ordered);
        ordered.stream().min(java.util.Comparator.comparing(HostingPlan::getPriceInr))
                .ifPresent(p -> model.addAttribute("cheapestId", p.getId()));
        model.addAttribute("enough", ordered.size() >= 2);
        if (requestId != null && !ordered.isEmpty()) {
            RecommendationService.ResultView view = recommendations.resultFor(requestId);
            model.addAttribute("search", view.request());
            model.addAttribute("evaluations", view.result().evaluations());
            ordered.stream()
                    .map(p -> view.result().evaluations().get(p.getId()))
                    .filter(e -> e != null && e.recommendation() != null)
                    .mapToInt(e -> e.recommendation().score())
                    .max()
                    .ifPresent(best -> model.addAttribute("bestScore", best));
        }
        return "compare";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("weights", MatchingEngine.weights());
        model.addAttribute("useCases", UseCase.values());
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private static void addFormOptions(Model model) {
        model.addAttribute("useCases", UseCase.values());
        model.addAttribute("expertiseOptions", Expertise.values());
    }
}
