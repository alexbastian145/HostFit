package com.hostfit.web;

import com.hostfit.crawler.CrawlService;
import com.hostfit.crawler.CrawlSourceType;
import com.hostfit.crawler.ParsedPlan;
import com.hostfit.domain.CrawlRun;
import com.hostfit.domain.CrawlSource;
import com.hostfit.service.ProviderService;
import com.hostfit.service.SourceForm;
import com.hostfit.service.SourceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin: crawl sources, manual runs, selector testing and run history. */
@Controller
@RequestMapping("/admin/crawl")
public class AdminCrawlController {

    private static final String SAMPLE_CONFIG = """
            {
              "cardSelector": ".plan-card",
              "nameSelector": "h3",
              "priceSelector": ".price",
              "vcpuSelector": ".cpu",
              "ramSelector": ".ram",
              "storageSelector": ".storage",
              "bandwidthSelector": ".bandwidth",
              "planType": "SHARED",
              "currency": "INR",
              "managed": true,
              "pricePick": "last"
            }""";

    private final SourceService sources;
    private final ProviderService providers;
    private final CrawlService crawler;

    public AdminCrawlController(SourceService sources, ProviderService providers, CrawlService crawler) {
        this.sources = sources;
        this.providers = providers;
        this.crawler = crawler;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("sources", sources.findAll());
        model.addAttribute("runs", sources.recentRuns(25));
        model.addAttribute("running", crawler.isRunningAll());
        return "admin/crawl";
    }

    @GetMapping("/sources/new")
    public String newSource(Model model) {
        SourceForm form = new SourceForm();
        form.setType(CrawlSourceType.CSS_SCRAPER);
        form.setConfig(SAMPLE_CONFIG);
        model.addAttribute("form", form);
        model.addAttribute("sourceId", null);
        addOptions(model);
        return "admin/source-form";
    }

    @PostMapping("/sources")
    public String create(@Valid @ModelAttribute("form") SourceForm form, BindingResult errors, Model model,
                         RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                CrawlSource s = sources.create(form);
                flash.addFlashAttribute("flash", "Added source " + s.getName() + ". Use Test to check it before the next crawl.");
                return "redirect:/admin/crawl";
            } catch (IllegalArgumentException e) {
                errors.rejectValue("config", "invalid", e.getMessage());
            }
        }
        model.addAttribute("sourceId", null);
        addOptions(model);
        return "admin/source-form";
    }

    @GetMapping("/sources/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", sources.toForm(sources.get(id)));
        model.addAttribute("sourceId", id);
        addOptions(model);
        return "admin/source-form";
    }

    @PostMapping("/sources/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SourceForm form,
                         BindingResult errors, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                CrawlSource s = sources.update(id, form);
                flash.addFlashAttribute("flash", "Updated source " + s.getName() + ".");
                return "redirect:/admin/crawl";
            } catch (IllegalArgumentException e) {
                errors.rejectValue("config", "invalid", e.getMessage());
            }
        }
        model.addAttribute("sourceId", id);
        addOptions(model);
        return "admin/source-form";
    }

    @PostMapping("/sources/{id}/run")
    public String run(@PathVariable Long id, RedirectAttributes flash) {
        CrawlRun run = crawler.runSource(id);
        String text = run.getSourceName() + ": " + run.getStatus() + ". " + (run.getMessage() == null ? "" : run.getMessage());
        flash.addFlashAttribute(run.getStatus().name().equals("FAILED") ? "flashError" : "flash", text);
        return "redirect:/admin/crawl";
    }

    @PostMapping("/run-all")
    public String runAll(RedirectAttributes flash) {
        List<CrawlRun> runs = crawler.runAllEnabled();
        if (runs.isEmpty()) {
            flash.addFlashAttribute("flashError", "Nothing was crawled: either no source is enabled or a crawl is already running.");
        } else {
            long failed = runs.stream().filter(r -> r.getStatus().name().equals("FAILED")).count();
            flash.addFlashAttribute(failed == 0 ? "flash" : "flashError",
                    "Crawled " + runs.size() + " source(s), " + failed + " failed. See the history below.");
        }
        return "redirect:/admin/crawl";
    }

    @PostMapping("/sources/{id}/preview")
    public String preview(@PathVariable Long id, Model model) {
        CrawlSource source = sources.get(id);
        model.addAttribute("source", source);
        try {
            List<ParsedPlan> parsed = crawler.preview(id);
            model.addAttribute("parsed", parsed);
        } catch (Exception e) {
            model.addAttribute("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return "admin/preview";
    }

    @PostMapping("/sources/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        CrawlSource s = sources.get(id);
        sources.setEnabled(id, !s.isEnabled());
        flash.addFlashAttribute("flash", s.getName() + (s.isEnabled() ? " disabled." : " enabled."));
        return "redirect:/admin/crawl";
    }

    @PostMapping("/sources/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        CrawlSource s = sources.get(id);
        sources.delete(id);
        flash.addFlashAttribute("flash", "Removed source " + s.getName() + ". Plans it created are kept.");
        return "redirect:/admin/crawl";
    }

    private void addOptions(Model model) {
        model.addAttribute("providers", providers.findAll());
        model.addAttribute("sourceTypes", CrawlSourceType.values());
    }
}
