package ru.fitnesscrm.scheduling.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.finance.facade.FinanceFacade;
import ru.fitnesscrm.memberships.facade.MembershipFacade;
import ru.fitnesscrm.scheduling.api.dto.request.CreateBookingRequest;

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

    public void book(CreateBookingRequest request) {
        if (!membershipFacade.canBookClasses(request.clientId())) {
            throw new BusinessException("Client has no active membership with remaining classes");
        }
        if (!financeFacade.canBookClasses(request.clientId())) {
            throw new BusinessException("Client account has negative balance");
        }
        throw new BusinessException("Booking not implemented yet — add optimistic locking and capacity check");
    }
}
