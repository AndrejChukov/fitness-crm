package ru.fitnesscrm.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.scheduling.domain.Facility;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByActiveTrue();
}
