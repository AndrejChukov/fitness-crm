package ru.fitnesscrm.finance.facade;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.finance.service.FinanceService;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class FinanceFacade {

    private final FinanceService financeService;

    public void createInvoiceForMembership(Long membershipId, Long clientId, BigDecimal amount) {
        financeService.makeInvoice(membershipId, clientId, amount);
    }

    public boolean canBookClasses(Long clientId) {
        return financeService.hasNonNegativeBalance(clientId);
    }
}
