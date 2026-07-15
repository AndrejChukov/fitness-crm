package ru.fitnesscrm.identity.api;

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
import ru.fitnesscrm.identity.api.dto.request.CreateTenantRequest;
import ru.fitnesscrm.identity.api.dto.response.TenantResponse;
import ru.fitnesscrm.identity.service.TenantService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@AllArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public ApiResponse<List<TenantResponse>> findAll() {
        return ApiResponse.ok(tenantService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.findById(id));
    }

    @PostMapping
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.ok(tenantService.create(request));
    }
}
