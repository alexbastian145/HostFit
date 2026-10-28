package com.hostfit.web;

import com.hostfit.core.PlanType;
import com.hostfit.domain.HostingPlan;
import com.hostfit.service.PlanForm;
import com.hostfit.service.PlanService;
import com.hostfit.service.ProviderService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin: add, update, remove plans and update prices. */
@Controller
@org.springframework.web.bind.annotation.RequestMapping("/admin/plans")
public class AdminPlanController {

    private final PlanService plans;
    private final ProviderService providers;

    public AdminPlanController(PlanService plans, ProviderService providers) {
        this.plans = plans;
        this.providers = providers;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<HostingPlan> all = plans.findAll().stream()
                .filter(p -> needle.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || p.getProvider().getName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        model.addAttribute("plans", all);
        model.addAttribute("q", q);
        return "admin/plans";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PlanForm());
        model.addAttribute("planId", null);
        addOptions(model);
        return "admin/plan-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PlanForm form, BindingResult errors, Model model,
                         RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("planId", null);
            addOptions(model);
            return "admin/plan-form";
        }
        HostingPlan saved = plans.create(form);
        flash.addFlashAttribute("flash", "Added " + saved.getName() + ".");
        return "redirect:/admin/plans";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", plans.toForm(plans.get(id)));
        model.addAttribute("planId", id);
        addOptions(model);
        return "admin/plan-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PlanForm form, BindingResult errors,
                         Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("planId", id);
            addOptions(model);
            return "admin/plan-form";
        }
        HostingPlan saved = plans.update(id, form);
        flash.addFlashAttribute("flash", "Updated " + saved.getName() + ".");
        return "redirect:/admin/plans";
    }

    @PostMapping("/{id}/price")
    public String updatePrice(@PathVariable Long id, @RequestParam("price") BigDecimal price, RedirectAttributes flash) {
        if (price == null || price.compareTo(BigDecimal.ONE) < 0 || price.compareTo(new BigDecimal("1000000")) > 0) {
            flash.addFlashAttribute("flashError", "Price must be between ₹1 and ₹10,00,000.");
            return "redirect:/admin/plans";
        }
        HostingPlan saved = plans.updatePrice(id, price);
        flash.addFlashAttribute("flash", "Price of " + saved.getName() + " set to ₹" + price.stripTrailingZeros().toPlainString() + ".");
        return "redirect:/admin/plans";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        HostingPlan p = plans.get(id);
        plans.setActive(id, !p.isActive());
        flash.addFlashAttribute("flash", p.getName() + (p.isActive() ? " hidden from recommendations." : " is active again."));
        return "redirect:/admin/plans";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        HostingPlan p = plans.get(id);
        plans.delete(id);
        flash.addFlashAttribute("flash", "Removed " + p.getName() + ".");
        return "redirect:/admin/plans";
    }

    private void addOptions(Model model) {
        model.addAttribute("providers", providers.findAll());
        model.addAttribute("planTypes", PlanType.values());
    }
}
