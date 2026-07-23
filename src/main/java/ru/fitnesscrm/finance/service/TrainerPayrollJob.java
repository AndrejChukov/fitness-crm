package ru.fitnesscrm.finance.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.PayrollStatus;
import ru.fitnesscrm.finance.domain.TrainerPayroll;
import ru.fitnesscrm.finance.repository.TrainerPayrollRepository;
import ru.fitnesscrm.scheduling.facade.EndedClassSessionView;
import ru.fitnesscrm.scheduling.facade.SchedulingFacade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class TrainerPayrollJob {

    private final SchedulingFacade schedulingFacade;
    private final TrainerPayrollRepository trainerPayrollRepository;

    private static final BigDecimal BASE_AMOUNT = BigDecimal.TEN;
    private static final BigDecimal BONUS_AMOUNT = BigDecimal.TWO;

    @Scheduled(cron = "0 0 18 * * *")
    @Transactional
    public void calculatePayroll() {
        TenantContext.executeWithoutFilter(() -> {
            Instant now = Instant.now();
            List<EndedClassSessionView> classSessions = schedulingFacade.getEndedClassSessions(now);

            for (EndedClassSessionView classSession : classSessions) {
                if (trainerPayrollRepository.existsByClassSessionId(classSession.id())) {
                    continue;
                }
                long countBookings = schedulingFacade.countAttendedBookingsBySessionId(classSession.id());

                BigDecimal bonusAmount = BigDecimal.valueOf(countBookings).multiply(BONUS_AMOUNT);

                TrainerPayroll trainerPayroll = new TrainerPayroll();
                trainerPayroll.setTenantId(classSession.tenantId());
                trainerPayroll.setTrainerId(classSession.trainerId());
                trainerPayroll.setClassSessionId(classSession.id());
                trainerPayroll.setBaseAmount(BASE_AMOUNT);
                trainerPayroll.setBonusAmount(bonusAmount);
                trainerPayroll.setTotalAmount(BASE_AMOUNT.add(bonusAmount));
                trainerPayroll.setStatus(PayrollStatus.CALCULATED);
                trainerPayroll.setCalculatedAt(now);
                trainerPayrollRepository.save(trainerPayroll);
            }
            return null;
        });
    }
}
