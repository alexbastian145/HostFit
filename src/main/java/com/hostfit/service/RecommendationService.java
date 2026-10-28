package com.hostfit.service;

import com.hostfit.core.MatchingEngine;
import com.hostfit.core.PlanSpec;
import com.hostfit.core.Requirement;
import com.hostfit.core.RecommendationResult;
import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.RequirementRequest;
import com.hostfit.repo.RequirementRequestRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Glue between stored plans and the framework-free {@link MatchingEngine}. */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    public static final int DEFAULT_LIMIT = 6;

    /** Everything a results page needs: the requirement, the engine output and the plan entities for display. */
    public record ResultView(RequirementRequest request, Requirement requirement, RecommendationResult result,
                             Map<Long, HostingPlan> plans) {
    }

    private final PlanService planService;
    private final RequirementRequestRepository requests;
    private final MatchingEngine engine = new MatchingEngine();

    public RecommendationService(PlanService planService, RequirementRequestRepository requests) {
        this.planService = planService;
        this.requests = requests;
    }

    @Transactional
    public RequirementRequest save(RequirementForm form) {
        RequirementRequest r = new RequirementRequest();
        r.setUseCase(form.getUseCase());
        r.setMonthlyVisits(form.getMonthlyVisits());
        r.setStorageGb(form.getStorageGb());
        r.setBudgetInr(form.getBudgetInr());
        r.setExpertise(form.getExpertise());
        return requests.save(r);
    }

    public RequirementRequest getRequest(Long id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Search " + id + " not found"));
    }

    /** Matches against the current active plans, so results always reflect the latest prices. */
    public RecommendationResult recommend(Requirement requirement, int limit) {
        return compute(requirement, planService.findActive(), limit);
    }

    public ResultView resultFor(Long requestId) {
        RequirementRequest request = getRequest(requestId);
        Requirement requirement = request.toRequirement();
        List<HostingPlan> active = planService.findActive();
        RecommendationResult result = compute(requirement, active, DEFAULT_LIMIT);
        Map<Long, HostingPlan> byId = active.stream().collect(Collectors.toMap(HostingPlan::getId, Function.identity()));
        return new ResultView(request, requirement, result, byId);
    }

    private RecommendationResult compute(Requirement requirement, List<HostingPlan> plans, int limit) {
        List<PlanSpec> specs = plans.stream().map(HostingPlan::toSpec).toList();
        return engine.recommend(requirement, specs, limit);
    }
}
