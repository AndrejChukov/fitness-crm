package ru.fitnesscrm.finance.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.*;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.finance.repository.InvoiceRepository;
import ru.fitnesscrm.finance.repository.TransactionRepository;

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
    private final TransactionRepository transactionRepository;

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

    @Transactional
    public void pay(Long invoiceId, PaymentMethod method) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!requiredTenantId().equals(invoice.getTenantId())) {
            throw new ResourceNotFoundException("Invoice not found");
        }
        if (invoice.getStatus() != InvoiceStatus.UNPAID
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessException("Invoice is already paid");
        }

        ClientAccount account = clientAccountRepository.findByClientId(invoice.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client account not found"));

        account.setBalance(account.getBalance().add(invoice.getAmount()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());

        Transaction transaction = new Transaction();
        transaction.setInvoiceId(invoice.getId());
        transaction.setAmount(invoice.getAmount());
        transaction.setMethod(method);
        transactionRepository.save(transaction);
    }

    public boolean hasNonNegativeBalance(Long clientId) {
        return clientAccountRepository.findByClientId(clientId)
                .map(account -> account.getBalance().compareTo(BigDecimal.ZERO) >= 0)
                .orElse(true);
    }

    private Long requiredTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("Tenant id is required");
        return tenantId;
    }
}
