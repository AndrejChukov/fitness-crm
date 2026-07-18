package ru.fitnesscrm.scheduling.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.domain.ClientAccount;
import ru.fitnesscrm.finance.repository.ClientAccountRepository;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.identity.domain.Tenant;
import ru.fitnesscrm.identity.domain.User;
import ru.fitnesscrm.identity.repository.TenantRepository;
import ru.fitnesscrm.identity.repository.UserRepository;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipStatus;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;
import ru.fitnesscrm.memberships.repository.ClientMembershipRepository;
import ru.fitnesscrm.memberships.repository.MembershipTemplateRepository;
import ru.fitnesscrm.scheduling.api.dto.request.CreateBookingRequest;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class BookingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired BookingService bookingService;

    @Autowired ClassSessionRepository classSessionRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired MembershipTemplateRepository membershipTemplateRepository;
    @Autowired ClientMembershipRepository clientMembershipRepository;
    @Autowired ClientAccountRepository clientAccountRepository;

    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;

    private Tenant tenant;
    private ClassSession classSession;
    private Facility facility;
    private User client;
    private User trainer;
    private MembershipTemplate membershipTemplate;
    private ClientMembership membership;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Gym");
        tenant.setSlug("gym-" + System.nanoTime());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        trainer = saveUser("trainer-" + System.nanoTime() + "@mail.ru", Role.TRAINER);
        client = saveUser("client-" + System.nanoTime() + "@mail.ru", Role.CLIENT);

        facility = new Facility();
        facility.setTenantId(tenant.getId());
        facility.setName("gymgym");
        facility.setActive(true);
        facility.setCapacity(10);
        facility = facilityRepository.save(facility);

        classSession = new ClassSession();
        classSession.setTenantId(tenant.getId());
        classSession.setFacilityId(facility.getId());
        classSession.setTrainerId(trainer.getId());
        classSession.setTitle("title");
        classSession.setStartTime(Instant.now().plus(10, ChronoUnit.HOURS));
        classSession.setEndTime(Instant.now().plus(20, ChronoUnit.HOURS));
        classSession.setMaxCapacity(1);
        classSession = classSessionRepository.save(classSession);

        membershipTemplate = new MembershipTemplate();
        membershipTemplate.setTenantId(tenant.getId());
        membershipTemplate.setName("templ");
        membershipTemplate.setClassLimit(100);
        membershipTemplate.setPrice(BigDecimal.TEN);
        membershipTemplate.setDurationDays(100);
        membershipTemplate.setActive(true);
        membershipTemplate = membershipTemplateRepository.save(membershipTemplate);

        membership = saveMembership(client.getId(), 10);

        TenantContext.set(tenant.getId(), client.getId(), Role.CLIENT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void book_shouldCreateConfirmedBooking() {
        Long versionBefore = classSessionRepository.findById(classSession.getId()).orElseThrow().getVersion();

        bookingService.book(new CreateBookingRequest(classSession.getId(), client.getId()));

        List<Booking> bookings = bookingRepository.findAll();
        assertEquals(1, bookings.size());
        assertEquals(BookingStatus.CONFIRMED, bookings.getFirst().getStatus());
        assertEquals(tenant.getId(), bookings.getFirst().getTenantId());
        assertEquals(client.getId(), bookings.getFirst().getClientId());
        assertEquals(classSession.getId(), bookings.getFirst().getClassSessionId());

        Long versionAfter = classSessionRepository.findById(classSession.getId()).orElseThrow().getVersion();
        assertTrue(versionAfter > versionBefore);
    }

    @Test
    void book_shouldThrowException_whenCapacityIsOver() {
        bookingService.book(new CreateBookingRequest(classSession.getId(), client.getId()));

        User otherClient = saveUser("other-" + System.nanoTime() + "@mail.ru", Role.CLIENT);
        saveMembership(otherClient.getId(), 5);
        TenantContext.set(tenant.getId(), otherClient.getId(), Role.CLIENT);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingService.book(new CreateBookingRequest(classSession.getId(), otherClient.getId()))
        );
        assertEquals("Class session is full", ex.getMessage());
        assertEquals(1, bookingRepository.countByClassSessionIdAndStatus(classSession.getId(), BookingStatus.CONFIRMED));
    }

    @Test
    void book_shouldReject_whenNoActiveMembership() {
        membership.setStatus(MembershipStatus.EXPIRED);
        clientMembershipRepository.save(membership);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingService.book(new CreateBookingRequest(classSession.getId(), client.getId()))
        );
        assertEquals("Client has no active membership with remaining classes", ex.getMessage());
        assertEquals(0, bookingRepository.countByClassSessionIdAndStatus(classSession.getId(), BookingStatus.CONFIRMED));
    }

    @Test
    void book_shouldReject_whenAccountBalanceIsNegative() {
        ClientAccount account = new ClientAccount();
        account.setTenantId(tenant.getId());
        account.setClientId(client.getId());
        account.setBalance(new BigDecimal("-10.00"));
        clientAccountRepository.save(account);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingService.book(new CreateBookingRequest(classSession.getId(), client.getId()))
        );
        assertEquals("Client account has negative balance", ex.getMessage());
        assertEquals(0, bookingRepository.countByClassSessionIdAndStatus(classSession.getId(), BookingStatus.CONFIRMED));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void book_shouldBumpVersion_soStaleClassSessionUpdateFails() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long sessionId = classSession.getId();
        Long tenantId = tenant.getId();
        Long clientId = client.getId();

        ClassSession staleSession = tx.execute(status ->
                classSessionRepository.findById(sessionId).orElseThrow()
        );
        Long oldVersion = staleSession.getVersion();
        entityManager.detach(staleSession);

        tx.executeWithoutResult(status -> {
            TenantContext.set(tenantId, clientId, Role.CLIENT);
            try {
                bookingService.book(new CreateBookingRequest(sessionId, clientId));
            } finally {
                TenantContext.clear();
            }
        });

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                tx.executeWithoutResult(status -> {
                    staleSession.setTitle(staleSession.getTitle() + "-stale");
                    classSessionRepository.saveAndFlush(staleSession);
                })
        );

        ClassSession freshSession = classSessionRepository.findById(sessionId).orElseThrow();
        assertTrue(freshSession.getVersion() > oldVersion);
    }

    /**
     * Acceptance: two concurrent books against capacity=1 must not create two CONFIRMED bookings.
     * Loser gets ObjectOptimisticLockingFailureException or BusinessException ("full"), depending on timing.
     * HTTP maps the lock failure via {@code GlobalExceptionHandler} → 409.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void book_shouldNotExceedCapacity_whenTwoClientsBookConcurrently() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long sessionId = classSession.getId();
        Long tenantId = tenant.getId();
        Long clientId = client.getId();

        User otherClient = tx.execute(status -> {
            User u = saveUser("race-" + System.nanoTime() + "@mail.ru", Role.CLIENT);
            saveMembership(u.getId(), 5);
            return u;
        });
        Long otherClientId = otherClient.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> bookWhenReady(ready, start, tenantId, clientId, sessionId, successes, failures));
            Future<?> second = pool.submit(() -> bookWhenReady(ready, start, tenantId, otherClientId, sessionId, successes, failures));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        long confirmed = bookingRepository.countByClassSessionIdAndStatus(sessionId, BookingStatus.CONFIRMED);
        assertEquals(1, confirmed, "capacity=1 must not be exceeded under concurrent booking");
        assertEquals(1, successes.get());
        assertEquals(1, failures.get());
    }

    @Test
    void cancel_shouldBeFree_whenMoreThan12HoursBeforeStart() {
        ClassSession farSession = saveSession(
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(25, ChronoUnit.HOURS),
                5
        );
        bookingService.book(new CreateBookingRequest(farSession.getId(), client.getId()));
        Booking booking = requireBooking(farSession.getId());
        int remainingBefore = membership.getRemainingClasses();

        bookingService.cancel(booking.getId());

        entityManager.flush();
        entityManager.clear();

        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());

        ClientMembership refreshed = clientMembershipRepository.findById(membership.getId()).orElseThrow();
        assertEquals(remainingBefore, refreshed.getRemainingClasses());
    }

    @Test
    void cancel_shouldDeductClass_whenLessThan12HoursBeforeStart() {
        // setUp session starts in 10h → late cancel
        bookingService.book(new CreateBookingRequest(classSession.getId(), client.getId()));
        Booking booking = requireBooking(classSession.getId());
        int remainingBefore = membership.getRemainingClasses();

        bookingService.cancel(booking.getId());

        entityManager.flush();
        entityManager.clear();

        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.LATE_CANCELED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());

        ClientMembership refreshed = clientMembershipRepository.findById(membership.getId()).orElseThrow();
        assertEquals(remainingBefore - 1, refreshed.getRemainingClasses());
    }

    /**
     * Boundary: remaining time until start == 12h → LATE_CANCELED + deduct.
     * Free cancel requires a strictly later start ({@code > 12h}).
     */
    @Test
    void cancel_shouldDeductClass_whenExactly12HoursBeforeStart() {
        Instant start = Instant.now().plus(12, ChronoUnit.HOURS);
        ClassSession boundarySession = saveSession(start, start.plus(1, ChronoUnit.HOURS), 5);
        bookingService.book(new CreateBookingRequest(boundarySession.getId(), client.getId()));
        Booking booking = requireBooking(boundarySession.getId());
        int remainingBefore = membership.getRemainingClasses();

        bookingService.cancel(booking.getId());

        entityManager.flush();
        entityManager.clear();

        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.LATE_CANCELED, cancelled.getStatus());

        ClientMembership refreshed = clientMembershipRepository.findById(membership.getId()).orElseThrow();
        assertEquals(remainingBefore - 1, refreshed.getRemainingClasses());
    }

    @Test
    void cancel_shouldReject_whenBookingAlreadyCancelled() {
        ClassSession farSession = saveSession(
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(25, ChronoUnit.HOURS),
                5
        );
        bookingService.book(new CreateBookingRequest(farSession.getId(), client.getId()));
        Booking booking = requireBooking(farSession.getId());
        bookingService.cancel(booking.getId());

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.cancel(booking.getId()));
        assertEquals("Only confirmed bookings can be cancelled", ex.getMessage());
    }

    private void bookWhenReady(
            CountDownLatch ready,
            CountDownLatch start,
            Long tenantId,
            Long clientId,
            Long sessionId,
            AtomicInteger successes,
            AtomicInteger failures
    ) {
        try {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            TenantContext.set(tenantId, clientId, Role.CLIENT);
            try {
                bookingService.book(new CreateBookingRequest(sessionId, clientId));
                successes.incrementAndGet();
            } catch (BusinessException | ObjectOptimisticLockingFailureException e) {
                failures.incrementAndGet();
            } finally {
                TenantContext.clear();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Booking requireBooking(Long sessionId) {
        return bookingRepository.findAll().stream()
                .filter(b -> sessionId.equals(b.getClassSessionId()))
                .findFirst()
                .orElseThrow();
    }

    private ClassSession saveSession(Instant start, Instant end, int maxCapacity) {
        ClassSession session = new ClassSession();
        session.setTenantId(tenant.getId());
        session.setFacilityId(facility.getId());
        session.setTrainerId(trainer.getId());
        session.setTitle("session-" + System.nanoTime());
        session.setStartTime(start);
        session.setEndTime(end);
        session.setMaxCapacity(maxCapacity);
        return classSessionRepository.save(session);
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

    private ClientMembership saveMembership(Long clientId, int remainingClasses) {
        ClientMembership m = new ClientMembership();
        m.setTenantId(tenant.getId());
        m.setTemplate(membershipTemplate);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setClientId(clientId);
        m.setRemainingClasses(remainingClasses);
        m.setStartDate(LocalDate.now().minus(100, ChronoUnit.DAYS));
        m.setEndDate(LocalDate.now().plus(30, ChronoUnit.DAYS));
        return clientMembershipRepository.save(m);
    }
}
