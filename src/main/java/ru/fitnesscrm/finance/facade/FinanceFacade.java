package ru.fitnesscrm.finance.facade;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.fitnesscrm.finance.service.FinanceService;

@Component
@AllArgsConstructor
public class FinanceFacade {

    private final FinanceService financeService;

    public boolean canBookClasses(Long clientId) {
        return financeService.hasNonNegativeBalance(clientId);
    }
}
