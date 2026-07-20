package ru.fitnesscrm.finance.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.api.ApiResponse;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.ClientAccount;
import ru.fitnesscrm.finance.domain.Invoice;
import ru.fitnesscrm.finance.domain.InvoiceStatus;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * TODO (your exercise): when membership is assigned, create Invoice with 3-day due date.
 * If unpaid after 3 days, set ClientAccount balance negative and block bookings.
 * TODO: async trainer payroll — base $10/class + $2 per attended client at end of day.
 */
@Service
@AllArgsConstructor
public class FinanceService {

    private final ClientAccountRepository clientAccountRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void makeInvoice(Long membershipId, Long clientId, BigDecimal amount) {
        Long tenantId = requiredTenantId();

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setClientId(clientId);
        invoice.setClientMembershipId(membershipId);
        invoice.setAmount(amount);
        invoice.setDueDate(LocalDate.now().plusDays(3));
        invoice.setStatus(InvoiceStatus.UNPAID);

        Optional<ClientAccount> byClientId = clientAccountRepository.findByClientId(clientId);
        if (byClientId.isEmpty()) {
            ClientAccount account = new ClientAccount();
            account.setTenantId(tenantId);
            account.setClientId(clientId);
            account.setBalance(BigDecimal.ZERO);
            clientAccountRepository.save(account);
        }

        invoiceRepository.save(invoice);
    }

    private Long requiredTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("Tenant id is required");
        return tenantId;
    }

    public boolean hasNonNegativeBalance(Long clientId) {
        return clientAccountRepository.findByClientId(clientId)
                .map(account -> account.getBalance().compareTo(BigDecimal.ZERO) >= 0)
                .orElse(true);
    }
}
