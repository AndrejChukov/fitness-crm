package ru.fitnesscrm.scheduling.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fitnesscrm.common.exception.ResourceNotFoundException;
import ru.fitnesscrm.common.tenant.TenantContext;
import ru.fitnesscrm.scheduling.api.dto.request.CreateFacilityRequest;
import ru.fitnesscrm.scheduling.api.dto.response.FacilityResponse;
import ru.fitnesscrm.scheduling.domain.Facility;
import ru.fitnesscrm.scheduling.repository.FacilityRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;

    @Transactional(readOnly = true)
    public List<FacilityResponse> findAllActive() {
        return facilityRepository.findByActiveTrue().stream()
                .map(FacilityResponse::from)
                .toList();
    }

    @Transactional
    public FacilityResponse create(CreateFacilityRequest request) {
        Facility facility = new Facility();
        facility.setTenantId(TenantContext.getTenantId());
        facility.setName(request.name());
        facility.setCapacity(request.capacity());
        facility.setActive(true);
        return FacilityResponse.from(facilityRepository.save(facility));
    }

    @Transactional(readOnly = true)
    public FacilityResponse findById(Long id) {
        return facilityRepository.findById(id)
                .map(FacilityResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + id));
    }
}
