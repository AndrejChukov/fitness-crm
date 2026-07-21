package ru.fitnesscrm.finance.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.PayrollStatus;
import ru.fitnesscrm.finance.domain.TrainerPayroll;
import ru.fitnesscrm.finance.repository.TrainerPayrollRepository;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.domain.BookingStatus;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.domain.Facility;
import ru.fitnesscrm.scheduling.repository.BookingRepository;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;
import ru.fitnesscrm.scheduling.repository.FacilityRepository;
import ru.fitnesscrm.support.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class TrainerPayrollJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ClassSessionRepository classSessionRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired TrainerPayrollRepository trainerPayrollRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired TrainerPayrollJob trainerPayrollJob;
    @Autowired EntityManager entityManager;

    private Tenant tenant;
    private User client;
    private User otherClient;
    private User trainer;
    private Facility facility;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Gym");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        client = saveUser("client-" + System.nanoTime() + "@mail.ru", Role.CLIENT);
        otherClient = saveUser("client2-" + System.nanoTime() + "@mail.ru", Role.CLIENT);
        trainer = saveUser("trainer-" + System.nanoTime() + "@mail.ru", Role.TRAINER);

        facility = new Facility();
        facility.setTenantId(tenant.getId());
        facility.setName("Main Hall");
        facility.setActive(true);
        facility.setCapacity(20);
        facility = facilityRepository.save(facility);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void calculatePayroll_shouldBeBase10Plus2PerAttended() {
        ClassSession session = saveEndedSession();
        saveBooking(session.getId(), client.getId(), BookingStatus.ATTENDED);

        trainerPayrollJob.calculatePayroll();
        entityManager.flush();
        entityManager.clear();

        List<TrainerPayroll> payrolls = trainerPayrollRepository.findAll();
        assertEquals(1, payrolls.size());
        TrainerPayroll payroll = payrolls.getFirst();
        assertEquals(PayrollStatus.CALCULATED, payroll.getStatus());
        assertEquals(0, BigDecimal.TEN.compareTo(payroll.getBaseAmount()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(payroll.getBonusAmount()));
        assertEquals(0, BigDecimal.valueOf(12).compareTo(payroll.getTotalAmount()));
        assertEquals(session.getId(), payroll.getClassSessionId());
        assertEquals(trainer.getId(), payroll.getTrainerId());
        assertEquals(tenant.getId(), payroll.getTenantId());
    }

    @Test
    void calculatePayroll_shouldUseOnlyAttendedCount_forThatSession() {
        ClassSession session = saveEndedSession();
        saveBooking(session.getId(), client.getId(), BookingStatus.ATTENDED);
        saveBooking(session.getId(), otherClient.getId(), BookingStatus.ATTENDED);

        ClassSession otherSession = saveEndedSession();
        saveBooking(otherSession.getId(), client.getId(), BookingStatus.CONFIRMED);

        trainerPayrollJob.calculatePayroll();
        entityManager.flush();
        entityManager.clear();

        TrainerPayroll forSession = trainerPayrollRepository.findAll().stream()
                .filter(p -> session.getId().equals(p.getClassSessionId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, BigDecimal.valueOf(4).compareTo(forSession.getBonusAmount()));
        assertEquals(0, BigDecimal.valueOf(14).compareTo(forSession.getTotalAmount()));

        TrainerPayroll forOther = trainerPayrollRepository.findAll().stream()
                .filter(p -> otherSession.getId().equals(p.getClassSessionId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(forOther.getBonusAmount()));
        assertEquals(0, BigDecimal.TEN.compareTo(forOther.getTotalAmount()));
    }

    @Test
    void calculatePayroll_shouldNotCreateDuplicate_whenRunTwice() {
        ClassSession session = saveEndedSession();
        saveBooking(session.getId(), client.getId(), BookingStatus.ATTENDED);

        trainerPayrollJob.calculatePayroll();
        trainerPayrollJob.calculatePayroll();
        entityManager.flush();
        entityManager.clear();

        assertEquals(1, trainerPayrollRepository.findAll().size());
        assertTrue(trainerPayrollRepository.existsByClassSessionId(session.getId()));
    }

    @Test
    void calculatePayroll_shouldIgnoreFutureSessions() {
        ClassSession future = new ClassSession();
        future.setTenantId(tenant.getId());
        future.setFacilityId(facility.getId());
        future.setTrainerId(trainer.getId());
        future.setTitle("Future Yoga");
        future.setStartTime(Instant.now().plus(2, ChronoUnit.HOURS));
        future.setEndTime(Instant.now().plus(3, ChronoUnit.HOURS));
        future.setMaxCapacity(10);
        classSessionRepository.save(future);

        trainerPayrollJob.calculatePayroll();
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, trainerPayrollRepository.findAll().size());
    }

    @Test
    void calculatePayroll_shouldCreateBaseOnly_whenNoAttendedBookings() {
        saveEndedSession();

        trainerPayrollJob.calculatePayroll();
        entityManager.flush();
        entityManager.clear();

        List<TrainerPayroll> payrolls = trainerPayrollRepository.findAll();
        assertEquals(1, payrolls.size());
        assertEquals(0, BigDecimal.TEN.compareTo(payrolls.getFirst().getTotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(payrolls.getFirst().getBonusAmount()));
    }

    private ClassSession saveEndedSession() {
        ClassSession session = new ClassSession();
        session.setTenantId(tenant.getId());
        session.setFacilityId(facility.getId());
        session.setTrainerId(trainer.getId());
        session.setTitle("Ended-" + System.nanoTime());
        session.setStartTime(Instant.now().minus(3, ChronoUnit.HOURS));
        session.setEndTime(Instant.now().minus(2, ChronoUnit.HOURS));
        session.setMaxCapacity(10);
        return classSessionRepository.save(session);
    }

    private void saveBooking(Long sessionId, Long clientId, BookingStatus status) {
        Booking booking = new Booking();
        booking.setTenantId(tenant.getId());
        booking.setClassSessionId(sessionId);
        booking.setClientId(clientId);
        booking.setStatus(status);
        booking.setBookedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        bookingRepository.save(booking);
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setPasswordHash("password");
        user.setRole(role);
        user.setFirstName("Andrej");
        user.setLastName("Ivanov");
        user.setActive(true);
        return userRepository.save(user);
    }
}
