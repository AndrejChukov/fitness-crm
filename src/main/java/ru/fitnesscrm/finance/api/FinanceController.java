package ru.fitnesscrm.finance.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fitnesscrm.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/finance")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class FinanceController {

    @GetMapping("/status")
    public ApiResponse<String> status() {
        return ApiResponse.ok(
                "Finance module scaffolded. Implement invoices, debt tracking, and trainer payroll."
        );
    }
}
