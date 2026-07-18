package ru.fitnesscrm.scheduling.api;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fitnesscrm.common.api.ApiResponse;
import ru.fitnesscrm.scheduling.api.dto.request.CreateClassSessionRequest;
import ru.fitnesscrm.scheduling.api.dto.request.CreateFacilityRequest;
import ru.fitnesscrm.scheduling.api.dto.response.ClassSessionResponse;
import ru.fitnesscrm.scheduling.api.dto.request.CreateBookingRequest;
import ru.fitnesscrm.scheduling.api.dto.response.FacilityResponse;
import ru.fitnesscrm.scheduling.domain.Booking;
import ru.fitnesscrm.scheduling.service.BookingService;
import ru.fitnesscrm.scheduling.service.ClassSessionService;
import ru.fitnesscrm.scheduling.service.FacilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduling")
@AllArgsConstructor
public class SchedulingController {

    private final FacilityService facilityService;
    private final ClassSessionService classSessionService;
    private final BookingService bookingService;

    @GetMapping("/facilities")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<List<FacilityResponse>> findFacilities() {
        return ApiResponse.ok(facilityService.findAllActive());
    }

    @PostMapping("/facilities")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ApiResponse<FacilityResponse> createFacility(@Valid @RequestBody CreateFacilityRequest request) {
        return ApiResponse.ok(facilityService.create(request));
    }

    @GetMapping("/facilities/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<FacilityResponse> findFacility(@PathVariable Long id) {
        return ApiResponse.ok(facilityService.findById(id));
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER')")
    public ApiResponse<ClassSessionResponse> createSession(@Valid @RequestBody CreateClassSessionRequest request) {
        return ApiResponse.ok(classSessionService.create(request));
    }

    @PostMapping("/bookings")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<Void> book(@Valid @RequestBody CreateBookingRequest request) {
        bookingService.book(request);
        return ApiResponse.okMessage("Booking created");
    }

    @PostMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        bookingService.cancel(id);
        return ApiResponse.okMessage("Booking cancelled");
    }
}
