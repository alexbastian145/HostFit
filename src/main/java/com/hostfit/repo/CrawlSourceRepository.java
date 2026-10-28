package com.hostfit.repo;

import com.hostfit.domain.CrawlSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrawlSourceRepository extends JpaRepository<CrawlSource, Long> {

    @Query("select s from CrawlSource s join fetch s.provider order by s.id")
    List<CrawlSource> findAllWithProvider();

    @Query("select s from CrawlSource s join fetch s.provider where s.enabled = true order by s.id")
    List<CrawlSource> findEnabledWithProvider();

    @Query("select s from CrawlSource s join fetch s.provider where s.id = :id")
    Optional<CrawlSource> findWithProviderById(@Param("id") Long id);

    long countByProviderId(Long providerId);
}
