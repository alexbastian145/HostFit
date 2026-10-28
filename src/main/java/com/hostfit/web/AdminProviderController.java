package com.hostfit.web;

import com.hostfit.domain.Provider;
import com.hostfit.service.ProviderForm;
import com.hostfit.service.ProviderService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin: manage hosting providers. */
@Controller
@RequestMapping("/admin/providers")
public class AdminProviderController {

    private final ProviderService providers;

    public AdminProviderController(ProviderService providers) {
        this.providers = providers;
    }

    @GetMapping
    public String list(Model model) {
        Map<Long, Long> planCounts = new LinkedHashMap<>();
        for (Provider p : providers.findAll()) {
            planCounts.put(p.getId(), providers.planCount(p));
        }
        model.addAttribute("providers", providers.findAll());
        model.addAttribute("planCounts", planCounts);
        return "admin/providers";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProviderForm());
        model.addAttribute("providerId", null);
        return "admin/provider-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProviderForm form, BindingResult errors, Model model,
                         RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Provider p = providers.create(form);
                flash.addFlashAttribute("flash", "Added provider " + p.getName() + ".");
                return "redirect:/admin/providers";
            } catch (IllegalArgumentException e) {
                errors.rejectValue("name", "duplicate", e.getMessage());
            }
        }
        model.addAttribute("providerId", null);
        return "admin/provider-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", providers.toForm(providers.get(id)));
        model.addAttribute("providerId", id);
        return "admin/provider-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProviderForm form,
                         BindingResult errors, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Provider p = providers.update(id, form);
                flash.addFlashAttribute("flash", "Updated provider " + p.getName() + ".");
                return "redirect:/admin/providers";
            } catch (IllegalArgumentException e) {
                errors.rejectValue("name", "duplicate", e.getMessage());
            }
        }
        model.addAttribute("providerId", id);
        return "admin/provider-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Provider p = providers.get(id);
            providers.delete(id);
            flash.addFlashAttribute("flash", "Removed provider " + p.getName() + ".");
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/admin/providers";
    }
}
