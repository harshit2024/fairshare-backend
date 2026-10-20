
package com.fairShare.fairshare_backend.expenses.dto;

import com.fairShare.fairshare_backend.expenses.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        String description,
        BigDecimal amount,
        String currency,
        UUID paidBy,
        UUID createdBy,
        ExpenseCategory category,
        SplitType splitType,
        LocalDateTime expenseDate,
        String notes,
        List<ExpenseSplitResponse> splits
) {
    public static ExpenseResponse from(
            Expense expense, List<ExpenseSplit> splits) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroupId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getPaidBy(),
                expense.getCreatedBy(),
                expense.getCategory(),
                expense.getSplitType(),
                expense.getExpenseDate(),
                expense.getNotes(),
                splits.stream()
                        .map(ExpenseSplitResponse::from)
                        .toList()
        );
    }
}