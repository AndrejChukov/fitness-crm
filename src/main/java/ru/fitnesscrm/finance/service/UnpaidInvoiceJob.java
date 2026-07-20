package ru.fitnesscrm.finance.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class UnpaidInvoiceJob {

    private final InvoiceRepository invoiceRepository;
    private final ClientAccountRepository clientAccountRepository;

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void processUnpaidInvoice() {
        TenantContext.executeWithoutFilter(() -> {
            List<Invoice> unpaidInvoices = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.UNPAID, LocalDate.now());

            for (Invoice unpaidInvoice : unpaidInvoices) {
                clientAccountRepository.applyDebt(
                        unpaidInvoice.getClientId(),
                        unpaidInvoice.getAmount().negate()
                );
            }
            return null;
        });
    }

}
