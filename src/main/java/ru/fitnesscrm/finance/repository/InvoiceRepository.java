package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

}
