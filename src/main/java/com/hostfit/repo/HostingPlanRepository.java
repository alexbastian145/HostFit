package com.hostfit.repo;

import com.hostfit.domain.HostingPlan;
import com.hostfit.domain.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostingPlanRepository extends JpaRepository<HostingPlan, Long> {

    @Query("select p from HostingPlan p join fetch p.provider where p.active = true and p.provider.active = true")
    List<HostingPlan> findAllActive();

    @Query("select p from HostingPlan p join fetch p.provider")
    List<HostingPlan> findAllWithProvider();

    @Query("select p from HostingPlan p join fetch p.provider where p.id = :id")
    Optional<HostingPlan> findWithProviderById(@Param("id") Long id);

    @Query("select p from HostingPlan p join fetch p.provider where p.id in :ids")
    List<HostingPlan> findAllWithProviderByIdIn(@Param("ids") Collection<Long> ids);

    Optional<HostingPlan> findByProviderAndExternalKey(Provider provider, String externalKey);

    long countByProvider(Provider provider);

    long countByActiveTrue();
}
