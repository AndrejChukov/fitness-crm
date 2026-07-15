package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.finance.domain.TrainerPayroll;

public interface TrainerPayrollRepository extends JpaRepository<TrainerPayroll, Long> {
}
