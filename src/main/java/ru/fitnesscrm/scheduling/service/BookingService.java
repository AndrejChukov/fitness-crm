package ru.fitnesscrm.scheduling.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.finance.facade.FinanceFacade;
import ru.fitnesscrm.memberships.facade.MembershipFacade;
import ru.fitnesscrm.scheduling.api.dto.request.CreateBookingRequest;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.domain.BookingStatus;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.repository.BookingRepository;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;

import java.time.Instant;

/**
 * TODO (your exercise): implement booking validation with:
 * - ACTIVE membership and remaining_classes &gt; 0 (MembershipFacade)
 * - client balance not negative (FinanceFacade)
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

    public void book(CreateBookingRequest request) {
        if (!membershipFacade.canBookClasses(request.clientId())) {
            throw new BusinessException("Client has no active membership with remaining classes");
        }
        if (!financeFacade.canBookClasses(request.clientId())) {
            throw new BusinessException("Client account has negative balance");
        }
        ClassSession classSession = classSessionRepository.findById(request.classSessionId()).orElseThrow(() ->
                new ResourceNotFoundException("Class session not found"));

        if (bookingRepository.countByClassSessionIdAndStatus(classSession.getId(),
                BookingStatus.CONFIRMED) < classSession.getMaxCapacity()) {
            Booking booking = new Booking();
            booking.setTenantId(TenantContext.getTenantId());
            booking.setClassSessionId(classSession.getId());
            booking.setClientId(TenantContext.getUserId());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setBookedAt(Instant.now());
            bookingRepository.save(booking);
        }
    }
}
