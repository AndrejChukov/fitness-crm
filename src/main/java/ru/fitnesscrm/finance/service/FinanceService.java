package ru.fitnesscrm.finance.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;

import java.math.BigDecimal;

/**
 * TODO (your exercise): when membership is assigned, create Invoice with 3-day due date.
 * If unpaid after 3 days, set ClientAccount balance negative and block bookings.
 * TODO: async trainer payroll — base $10/class + $2 per attended client at end of day.
 */
@Service
@AllArgsConstructor
public class FinanceService {

    private final ClientAccountRepository clientAccountRepository;

    public boolean hasNonNegativeBalance(Long clientId) {
        return clientAccountRepository.findByClientId(clientId)
                .map(account -> account.getBalance().compareTo(BigDecimal.ZERO) >= 0)
                .orElse(true);
    }
}
