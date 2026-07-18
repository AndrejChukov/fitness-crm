package ru.fitnesscrm.scheduling.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.scheduling.api.dto.request.CreateClassSessionRequest;
import ru.fitnesscrm.scheduling.api.dto.response.ClassSessionResponse;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.domain.Facility;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;
import ru.fitnesscrm.scheduling.repository.FacilityRepository;
import ru.fitnesscrm.support.AbstractIntegrationTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ClassSessionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClassSessionRepository classSessionRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private FacilityRepository facilityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClassSessionService classSessionService;

    private Tenant tenant;
    private Facility facility;
    private Facility otherFacility;
    private User trainer;
    private User otherTrainer;

    /** Existing session: [T+1d, T+5d) */
    private Instant existingStart;
    private Instant existingEnd;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("GYM");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        facility = saveFacility("Main Hall");
        otherFacility = saveFacility("Pool");

        trainer = saveTrainer("trainer-" + System.nanoTime() + "@mail.ru");
        otherTrainer = saveTrainer("other-trainer-" + System.nanoTime() + "@mail.ru");

        TenantContext.set(tenant.getId(), trainer.getId(), Role.TRAINER);

        existingStart = Instant.now().plus(1, ChronoUnit.DAYS);
        existingEnd = Instant.now().plus(5, ChronoUnit.DAYS);

        ClassSession existing = new ClassSession();
        existing.setTenantId(tenant.getId());
        existing.setFacilityId(facility.getId());
        existing.setTrainerId(trainer.getId());
        existing.setTitle("Existing Yoga");
        existing.setStartTime(existingStart);
        existing.setEndTime(existingEnd);
        existing.setMaxCapacity(10);
        classSessionRepository.save(existing);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Partial overlap on same trainer+facility → conflict")
    void create_shouldReject_whenPartialOverlap() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                trainer.getId(),
                existingStart.plus(1, ChronoUnit.DAYS),
                existingEnd.minus(1, ChronoUnit.DAYS)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> classSessionService.create(request));
        assertTrue(
                ex.getMessage().contains("Trainer") || ex.getMessage().contains("Facility"),
                "Expected trainer or facility conflict message, got: " + ex.getMessage()
        );
    }

    @Test
    @DisplayName("Complete enclosing overlap → conflict")
    void create_shouldReject_whenCompleteOverlap() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                trainer.getId(),
                existingStart.minus(12, ChronoUnit.HOURS),
                existingEnd.plus(12, ChronoUnit.HOURS)
        );

        assertThrows(BusinessException.class, () -> classSessionService.create(request));
    }

    @Test
    @DisplayName("Same facility, different trainer, overlapping time → facility conflict")
    void create_shouldReject_whenFacilityConflictOnly() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                otherTrainer.getId(),
                existingStart.plus(12, ChronoUnit.HOURS),
                existingEnd.minus(12, ChronoUnit.HOURS)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> classSessionService.create(request));
        assertEquals("Facility is already booked at this time", ex.getMessage());
    }

    @Test
    @DisplayName("Same trainer, different facility, overlapping time → trainer conflict")
    void create_shouldReject_whenTrainerConflictOnly() {
        CreateClassSessionRequest request = request(
                otherFacility.getId(),
                trainer.getId(),
                existingStart.plus(12, ChronoUnit.HOURS),
                existingEnd.minus(12, ChronoUnit.HOURS)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> classSessionService.create(request));
        assertEquals("Trainer is already assigned to another class at this time", ex.getMessage());
    }

    @Test
    @DisplayName("Adjacent intervals (end == next start) do NOT conflict")
    void create_shouldAllow_whenIntervalsAreAdjacent() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                trainer.getId(),
                existingEnd,
                existingEnd.plus(2, ChronoUnit.HOURS)
        );

        ClassSessionResponse response = assertDoesNotThrow(() -> classSessionService.create(request));
        assertEquals(existingEnd, response.startTime());
        assertEquals(trainer.getId(), response.trainerId());
        assertEquals(facility.getId(), response.facilityId());
    }

    @Test
    @DisplayName("Non-overlapping later slot is allowed")
    void create_shouldSucceed_whenNoOverlap() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                trainer.getId(),
                existingEnd.plus(1, ChronoUnit.DAYS),
                existingEnd.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS)
        );

        ClassSessionResponse response = classSessionService.create(request);

        assertEquals("New Class", response.title());
        assertEquals(10, response.maxCapacity());
    }

    @Test
    @DisplayName("Different trainer + different facility at same time is allowed")
    void create_shouldSucceed_whenDifferentTrainerAndFacility() {
        CreateClassSessionRequest request = request(
                otherFacility.getId(),
                otherTrainer.getId(),
                existingStart,
                existingEnd
        );

        ClassSessionResponse response = assertDoesNotThrow(() -> classSessionService.create(request));
        assertEquals(otherFacility.getId(), response.facilityId());
        assertEquals(otherTrainer.getId(), response.trainerId());
    }

    @Test
    @DisplayName("endTime must be after startTime")
    void create_shouldReject_whenEndNotAfterStart() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateClassSessionRequest request = request(facility.getId(), trainer.getId(), start, start);

        BusinessException ex = assertThrows(BusinessException.class, () -> classSessionService.create(request));
        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    @DisplayName("startTime must be in the future")
    void create_shouldReject_whenStartIsInThePast() {
        CreateClassSessionRequest request = request(
                facility.getId(),
                trainer.getId(),
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> classSessionService.create(request));
        assertEquals("Start time must be in the future", ex.getMessage());
    }

    private CreateClassSessionRequest request(
            Long facilityId,
            Long trainerId,
            Instant start,
            Instant end
    ) {
        return new CreateClassSessionRequest(
                facilityId,
                trainerId,
                "New Class",
                "desc",
                start,
                end,
                10
        );
    }

    private Facility saveFacility(String name) {
        Facility f = new Facility();
        f.setTenantId(tenant.getId());
        f.setName(name);
        f.setCapacity(10);
        f.setActive(true);
        return facilityRepository.save(f);
    }

    private User saveTrainer(String email) {
        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setPasswordHash("hashedPas");
        user.setRole(Role.TRAINER);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setActive(true);
        return userRepository.save(user);
    }
}
