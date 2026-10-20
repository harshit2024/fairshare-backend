
package com.fairShare.fairshare_backend.expenses.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

public record SplitRequest(
        @NotNull UUID userId,

        @DecimalMin(value = "0.0001")
        BigDecimal value
) {}