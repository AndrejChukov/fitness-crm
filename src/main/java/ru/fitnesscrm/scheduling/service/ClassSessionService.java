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
import ru.fitnesscrm.scheduling.repository.FacilityRepository;

import java.time.Instant;

@Service
@AllArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final FacilityRepository facilityRepository;

    @Transactional
    public ClassSessionResponse create(CreateClassSessionRequest request) {
        Long tenantId = requireTenantId();

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
        if (!request.startTime().isAfter(Instant.now())) {
            throw new BusinessException("Start time must be in the future");
        }

        facilityRepository.findById(request.facilityId())
                .filter(facility -> tenantId.equals(facility.getTenantId()))
                .orElseThrow(() -> new BusinessException("Facility not found in current tenant"));

        boolean trainerConflict = !classSessionRepository
                .findTrainerConflicts(request.trainerId(), request.startTime(), request.endTime())
                .isEmpty();
        boolean facilityConflict = !classSessionRepository
                .findFacilityConflicts(request.facilityId(), request.startTime(), request.endTime())
                .isEmpty();

        if (trainerConflict) {
            throw new BusinessException("Trainer is already assigned to another class at this time");
        }
        if (facilityConflict) {
            throw new BusinessException("Facility is already booked at this time");
        }

        ClassSession session = new ClassSession();
        session.setTenantId(tenantId);
        session.setFacilityId(request.facilityId());
        session.setTrainerId(request.trainerId());
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setMaxCapacity(request.maxCapacity());

        return ClassSessionResponse.from(classSessionRepository.save(session));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        return tenantId;
    }
}
