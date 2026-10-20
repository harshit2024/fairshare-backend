
package com.fairShare.fairshare_backend.expenses.dto;

import com.fairShare.fairshare_backend.expenses.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotBlank @Size(max = 255)
        String description,

        @NotNull @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Pattern(regexp = "[A-Za-z]{3}")
        String currency,

        @NotNull UUID paidBy,

        @NotNull ExpenseCategory category,

        @NotNull SplitType splitType,

        LocalDateTime expenseDate,

        @Size(max = 5000)
        String notes,

        @NotEmpty @Size(min = 1, max = 100)
        @Valid
        List<SplitRequest> splits
) {}