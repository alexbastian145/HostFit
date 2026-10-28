package com.hostfit.repo;

import com.hostfit.domain.Provider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    List<Provider> findAllByOrderByNameAsc();

    Optional<Provider> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
