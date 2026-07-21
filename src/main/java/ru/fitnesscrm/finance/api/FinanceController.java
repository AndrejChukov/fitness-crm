package ru.fitnesscrm.finance.api;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.fitnesscrm.common.api.ApiResponse;
import ru.fitnesscrm.finance.api.request.PayInvoiceRequest;
import ru.fitnesscrm.finance.service.FinanceService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/invoices/{id}/pay")
    public ApiResponse<Void> pay(@PathVariable Long id, @Valid @RequestBody PayInvoiceRequest request) {
        financeService.pay(id, request.method());
        return ApiResponse.okMessage("Paid");
    }
}
