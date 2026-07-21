package ru.fitnesscrm.finance.api.request;

import jakarta.validation.constraints.NotNull;
import ru.fitnesscrm.finance.domain.PaymentMethod;

public record PayInvoiceRequest(@NotNull PaymentMethod method) {
}
