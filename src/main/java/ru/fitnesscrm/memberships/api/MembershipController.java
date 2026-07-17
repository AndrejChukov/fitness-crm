package ru.fitnesscrm.memberships.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.fitnesscrm.memberships.api.dto.request.CreateMembershipTemplateRequest;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.api.dto.response.MembershipTemplateResponse;
import ru.fitnesscrm.memberships.service.ClientMembershipService;
import ru.fitnesscrm.memberships.service.MembershipTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memberships")
@AllArgsConstructor
@Tag(name = "Memberships", description = "Membership templates and client membership lifecycle")
public class MembershipController {

    private final MembershipTemplateService templateService;
    private final ClientMembershipService clientMembershipService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    @Operation(summary = "List active membership templates")
    public ApiResponse<List<MembershipTemplateResponse>> findTemplates() {
        return ApiResponse.ok(templateService.findAllActive());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Create a membership template")
    public ApiResponse<MembershipTemplateResponse> createTemplate(
            @Valid @RequestBody CreateMembershipTemplateRequest request
    ) {
        return ApiResponse.ok(templateService.create(request));
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    @Operation(summary = "Get membership template by id")
    public ApiResponse<MembershipTemplateResponse> findTemplate(@PathVariable Long id) {
        return ApiResponse.ok(templateService.findById(id));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER')")
    @Operation(summary = "Assign a membership template to a client")
    public ApiResponse<ClientMembershipResponse> assign(@Valid @RequestBody AssignMembershipRequest request) {
        return ApiResponse.ok(clientMembershipService.assign(request));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    @Operation(
            summary = "List ACTIVE memberships for a client",
            description = "CLIENT may list only their own memberships. Staff may list any client in the current tenant."
    )
    public ApiResponse<List<ClientMembershipResponse>> findClientMemberships(@PathVariable Long clientId) {
        return ApiResponse.ok(clientMembershipService.findClientMemberships(clientId));
    }

    @PostMapping("/{id}/deduct-class")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER')")
    @Operation(
            summary = "Deduct one class from a membership",
            description = "Internal/staff operation. Unlimited memberships are unchanged. Last class → DEPLETED."
    )
    public ApiResponse<ClientMembershipResponse> deductClass(@PathVariable("id") Long id) {
        return ApiResponse.ok(clientMembershipService.deductClass(id));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    @Operation(
            summary = "Freeze a client membership",
            description = """
                    Transitions ACTIVE → FROZEN.
                    Clients may freeze only their own membership.
                    Staff (TENANT_ADMIN / TRAINER) may freeze any membership in their tenant.
                    Other-tenant memberships return 404. Another client's membership for role CLIENT returns 403.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Membership frozen"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state (not ACTIVE, expired, freeze budget exhausted)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Client tried to freeze another client's membership"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Membership not found or belongs to another tenant")
    })
    public ApiResponse<ClientMembershipResponse> freeze(@PathVariable("id") Long id) {
        return ApiResponse.ok(clientMembershipService.freeze(id));
    }

    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TRAINER', 'CLIENT')")
    @Operation(
            summary = "Unfreeze a client membership",
            description = """
                    Transitions FROZEN → ACTIVE (or DEPLETED if remainingClasses == 0).
                    Uses cap policy: at most 14 freeze days total; excess freeze days are not charged,
                    but the membership is always unfrozen so it cannot stay FROZEN forever.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Membership unfrozen"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Membership is not frozen"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Client tried to unfreeze another client's membership"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Membership not found or belongs to another tenant")
    })
    public ApiResponse<ClientMembershipResponse> unfreeze(@PathVariable("id") Long id) {
        return ApiResponse.ok(clientMembershipService.unfreeze(id));
    }
}
