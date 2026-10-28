package com.hostfit.repo;

import com.hostfit.domain.CrawlRun;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {

    @Query("select r from CrawlRun r order by r.startedAt desc")
    List<CrawlRun> findRecent(Pageable pageable);
}
