package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.finance.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
