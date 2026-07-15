package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.finance.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
