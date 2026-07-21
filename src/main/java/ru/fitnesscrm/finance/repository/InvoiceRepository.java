package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    @Modifying
    @Query("""
        update Invoice i set i.status = :status
        where i.id in :ids
    """)
    int updateStatusByIds(@Param("status") InvoiceStatus status, @Param("ids") List<Long> ids);

}
