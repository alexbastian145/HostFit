package com.hostfit.repo;

import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.PricePoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePointRepository extends JpaRepository<PricePoint, Long> {

    List<PricePoint> findTop30ByPlanOrderByRecordedAtDesc(HostingPlan plan);
}
