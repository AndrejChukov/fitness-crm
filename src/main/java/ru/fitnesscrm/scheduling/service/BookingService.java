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

/**
 * TODO (your exercise): complete booking validation with:
 * - ClassSession capacity check with @Version optimistic locking
 * - cancellation policy: &gt;12h = CANCELLED, &lt;12h = LATE_CANCELED + deduct class
 */
@Service
@AllArgsConstructor
public class BookingService {

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

        Booking booking = new Booking();
        booking.setTenantId(tenantId);
        booking.setClassSessionId(classSession.getId());
        booking.setClientId(clientId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(Instant.now());
        bookingRepository.save(booking);
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
