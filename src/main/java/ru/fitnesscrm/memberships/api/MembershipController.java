package ru.fitnesscrm.memberships.api;

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
import ru.fitnesscrm.memberships.api.dto.request.AssignMembershipRequest;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.api.dto.request.CreateMembershipTemplateRequest;
import ru.fitnesscrm.memberships.api.dto.response.MembershipTemplateResponse;
import ru.fitnesscrm.memberships.service.ClientMembershipService;
import ru.fitnesscrm.memberships.service.MembershipTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memberships")
@AllArgsConstructor
public class MembershipController {

    private final MembershipTemplateService templateService;
    private final ClientMembershipService clientMembershipService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<List<MembershipTemplateResponse>> findTemplates() {
        return ApiResponse.ok(templateService.findAllActive());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ApiResponse<MembershipTemplateResponse> createTemplate(
            @Valid @RequestBody CreateMembershipTemplateRequest request
    ) {
        return ApiResponse.ok(templateService.create(request));
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    public ApiResponse<MembershipTemplateResponse> findTemplate(@PathVariable Long id) {
        return ApiResponse.ok(templateService.findById(id));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER')")
    public ApiResponse<ClientMembershipResponse> assign(@Valid @RequestBody AssignMembershipRequest request) {
        return ApiResponse.ok(clientMembershipService.assign(request));
    }
}
