package ru.fitnesscrm.memberships.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;

import java.util.List;

public interface MembershipTemplateRepository extends JpaRepository<MembershipTemplate, Long> {

    List<MembershipTemplate> findByActiveTrue();
}
