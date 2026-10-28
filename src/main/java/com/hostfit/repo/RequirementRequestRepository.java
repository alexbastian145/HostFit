package com.hostfit.repo;

import com.hostfit.domain.RequirementRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRequestRepository extends JpaRepository<RequirementRequest, Long> {
}
