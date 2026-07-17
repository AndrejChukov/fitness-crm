package ru.fitnesscrm.scheduling.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.BusinessException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.scheduling.api.dto.request.CreateClassSessionRequest;
import ru.fitnesscrm.scheduling.api.dto.response.ClassSessionResponse;
import ru.fitnesscrm.scheduling.domain.ClassSession;
import ru.fitnesscrm.scheduling.repository.ClassSessionRepository;

import java.time.Instant;

/**
 * TODO (your exercise): implement the Conflict Resolver before saving a ClassSession.
 * Query trainer and facility overlapping intervals:
 * (start_time &lt; new_end) AND (end_time &gt; new_start)
 * Throw BusinessException when trainer or facility is already booked.
 */
@Service
@AllArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;

    @Transactional
    public ClassSessionResponse create(CreateClassSessionRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
        if (request.startTime().isBefore(Instant.now())) {
            throw new BusinessException("Start time must be after now");
        }

        boolean isTrainerConflict = !classSessionRepository.findTrainerConflicts(request.trainerId(),
                request.startTime(), request.endTime()).isEmpty();
        boolean isFacilityConflict = !classSessionRepository.findFacilityConflicts(request.facilityId(),
                request.startTime(), request.endTime()).isEmpty();

        if (isFacilityConflict) {
            throw new BusinessException("Facility already booked");
        }
        if (isTrainerConflict) {
            throw new BusinessException("Class on that time already booked by another trainer");
        }

        ClassSession session = new ClassSession();
        session.setTenantId(TenantContext.getTenantId());
        session.setFacilityId(request.facilityId());
        session.setTrainerId(request.trainerId());
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setMaxCapacity(request.maxCapacity());

        return ClassSessionResponse.from(classSessionRepository.save(session));
    }
}
