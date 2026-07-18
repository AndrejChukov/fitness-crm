package ru.fitnesscrm.scheduling.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.facade.FinanceFacade;
import ru.fitnesscrm.identity.domain.Role;
import ru.fitnesscrm.memberships.facade.MembershipFacade;
import ru.fitnesscrm.scheduling.api.dto.request.CreateBookingRequest;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.domain.BookingStatus;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.repository.BookingRepository;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Booking with membership/finance/capacity checks and ClassSession {@code @Version} optimistic locking.
 * Cancel policy: more than 12h before start → {@link BookingStatus#CANCELLED} (no deduct);
 * 12h or less → {@link BookingStatus#LATE_CANCELED} + deduct one class via {@link MembershipFacade}.
 */
@Service
@AllArgsConstructor
public class BookingService {

    private static final long CANCEL_FREE_HOURS = 12;

    private final MembershipFacade membershipFacade;
    private final FinanceFacade financeFacade;
    private final ClassSessionRepository classSessionRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public void book(CreateBookingRequest request) {
        Long tenantId = requireTenantId();
        Long clientId = resolveClientId(request.clientId());

        if (!membershipFacade.canBookClasses(clientId)) {
            throw new BusinessException("Client has no active membership with remaining classes");
        }
        if (!financeFacade.canBookClasses(clientId)) {
            throw new BusinessException("Client account has negative balance");
        }

        ClassSession classSession = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        if (!tenantId.equals(classSession.getTenantId())) {
            throw new ResourceNotFoundException("Class session not found");
        }

        long confirmed = bookingRepository.countByClassSessionIdAndStatus(
                classSession.getId(),
                BookingStatus.CONFIRMED
        );
        if (confirmed >= classSession.getMaxCapacity()) {
            throw new BusinessException("Class session is full");
        }

        classSession.setUpdatedAt(Instant.now());
        classSessionRepository.saveAndFlush(classSession);

        Booking booking = new Booking();
        booking.setTenantId(tenantId);
        booking.setClassSessionId(classSession.getId());
        booking.setClientId(clientId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(Instant.now());
        bookingRepository.save(booking);
    }

    @Transactional
    public void cancel(Long bookingId) {
        Long tenantId = requireTenantId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Long clientId = resolveClientId(booking.getClientId());

        if (!tenantId.equals(booking.getTenantId())) {
            throw new ResourceNotFoundException("Booking not found");
        }
        if (!booking.getClientId().equals(clientId)) {
            throw new BusinessException("This booking belongs to another user");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed bookings can be cancelled");
        }

        ClassSession session = classSessionRepository.findById(booking.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        if (isLateCancel(session.getStartTime())) {
            booking.setStatus(BookingStatus.LATE_CANCELED);
            membershipFacade.deductClassForClient(clientId);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
        }
        booking.setCancelledAt(Instant.now());
    }

    /**
     * Late cancel when remaining time until session start is {@code <= 12} hours
     * (boundary: exactly 12h is LATE_CANCELED).
     */
    private boolean isLateCancel(Instant startTime) {
        return !startTime.isAfter(Instant.now().plus(CANCEL_FREE_HOURS, ChronoUnit.HOURS));
    }

    private Long resolveClientId(Long requestedClientId) {
        if (TenantContext.getRole() == Role.CLIENT) {
            return TenantContext.getUserId();
        }
        if (requestedClientId == null) {
            throw new BusinessException("clientId is required");
        }
        return requestedClientId;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return tenantId;
    }
}
