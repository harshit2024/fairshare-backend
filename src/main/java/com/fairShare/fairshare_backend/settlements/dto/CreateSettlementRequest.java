
package com.fairShare.fairshare_backend.settlements.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateSettlementRequest(
        @NotNull UUID paidTo,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency,

        @Size(max = 100)
        String paymentMethod,

        @Size(max = 255)
        String transactionReference,

        @Size(max = 5000)
        String notes
) {}
