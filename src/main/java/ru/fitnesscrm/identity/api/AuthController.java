package ru.fitnesscrm.identity.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fitnesscrm.common.api.ApiResponse;
import ru.fitnesscrm.identity.api.dto.response.AuthResponse;
import ru.fitnesscrm.identity.api.dto.request.LoginRequest;
import ru.fitnesscrm.identity.api.dto.request.RegisterTenantRequest;
import ru.fitnesscrm.identity.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        return ApiResponse.ok(authService.registerTenant(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
