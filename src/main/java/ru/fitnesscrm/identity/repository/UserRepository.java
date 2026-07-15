package ru.fitnesscrm.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.identity.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByTenant_IdAndEmail(Long tenantId, String email);

    List<User> findByTenant_Id(Long tenantId);

    boolean existsByTenant_IdAndEmail(Long tenantId, String email);
}
