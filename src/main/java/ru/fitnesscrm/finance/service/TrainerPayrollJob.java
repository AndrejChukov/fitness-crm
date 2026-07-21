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
import ru.fitnesscrm.scheduling.domain.ClassSession;
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

    private final static BigDecimal BASE_AMOUNT = BigDecimal.TEN;
    private final static BigDecimal BONUS_AMOUNT = BigDecimal.TWO;

    @Scheduled(cron = "0 0 18 * * *")
    @Transactional
    public void calculatePayroll() {
        TenantContext.executeWithoutFilter(() -> {
            Instant now = Instant.now();
            List<ClassSession> classSessions = schedulingFacade.getEndedClassSessions(now);

            for (ClassSession classSession : classSessions) {
                if (trainerPayrollRepository.existsByClassSessionId(classSession.getId())) {
                    continue;
                }
                long countBookings = schedulingFacade.countAttendedBookingsBySessionId(classSession.getId());

                BigDecimal bonusAmount = BigDecimal.valueOf(countBookings).multiply(BONUS_AMOUNT);

                TrainerPayroll trainerPayroll = new TrainerPayroll();
                trainerPayroll.setTenantId(classSession.getTenantId());
                trainerPayroll.setTrainerId(classSession.getTrainerId());
                trainerPayroll.setClassSessionId(classSession.getId());
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

















