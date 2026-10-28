package com.hostfit.web;

import com.hostfit.core.Alternative;
import com.hostfit.core.PlanType;
import com.hostfit.core.Recommendation;
import com.hostfit.core.RecommendationResult;
import com.hostfit.core.ResourceNeed;
import com.hostfit.domain.HostingPlan;
import com.hostfit.service.PlanFilter;
import com.hostfit.service.PlanService;
import com.hostfit.service.RecommendationService;
import com.hostfit.service.RequirementForm;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only JSON API: browse plans and request recommendations programmatically. */
@RestController
@RequestMapping("/api")
public class ApiController {

    public record PlanDto(Long id, String provider, String name, PlanType type, int vcpu, double ramGb, int storageGb,
                          String storageType, Integer bandwidthGb, boolean unmetered, BigDecimal priceInr,
                          boolean managed, String url, String priceSource, Instant priceVerifiedAt) {
        static PlanDto of(HostingPlan p) {
            return new PlanDto(p.getId(), p.getProvider().getName(), p.getName(), p.getPlanType(), p.getVcpu(),
                    p.getRamGb(), p.getStorageGb(), p.getStorageType(), p.getBandwidthGb(), p.isUnmetered(),
                    p.getPriceInr(), p.isManaged(), p.getUrl(), p.getPriceSource().name(), p.getPriceVerifiedAt());
        }
    }

    public record RecommendationDto(ResourceNeed need, List<Recommendation> recommendations,
                                    List<Alternative> alternatives, BigDecimal minimumBudgetNeeded) {
        static RecommendationDto of(RecommendationResult r) {
            return new RecommendationDto(r.need(), r.recommendations(), r.alternatives(), r.minimumBudgetNeeded());
        }
    }

    private final PlanService plans;
    private final RecommendationService recommendations;

    public ApiController(PlanService plans, RecommendationService recommendations) {
        this.plans = plans;
        this.recommendations = recommendations;
    }

    @GetMapping("/plans")
    public List<PlanDto> plans(@RequestParam(value = "type", required = false) PlanType type,
                               @RequestParam(value = "provider", required = false) Long providerId,
                               @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice) {
        return plans.browse(new PlanFilter(type, providerId, maxPrice, null, PlanFilter.SORT_PRICE))
                .stream().map(PlanDto::of).toList();
    }

    @GetMapping("/plans/{id}")
    public PlanDto plan(@PathVariable Long id) {
        return PlanDto.of(plans.get(id));
    }

    @PostMapping("/recommendations")
    public RecommendationDto recommend(@Valid @RequestBody RequirementForm form) {
        return RecommendationDto.of(recommendations.recommend(form.toRequirement(), RecommendationService.DEFAULT_LIMIT));
    }
}
