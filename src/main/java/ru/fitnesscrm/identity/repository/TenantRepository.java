package ru.fitnesscrm.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.identity.domain.Tenant;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
