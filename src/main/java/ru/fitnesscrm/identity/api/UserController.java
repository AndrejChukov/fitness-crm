package ru.fitnesscrm.identity.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fitnesscrm.common.api.ApiResponse;
import ru.fitnesscrm.identity.api.dto.request.CreateUserRequest;
import ru.fitnesscrm.identity.api.dto.response.UserResponse;
import ru.fitnesscrm.identity.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.ok(userService.findAllForCurrentTenant());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }
}
