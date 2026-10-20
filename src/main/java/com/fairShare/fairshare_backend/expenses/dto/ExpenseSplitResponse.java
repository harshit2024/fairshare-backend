
package com.fairShare.fairshare_backend.expenses.dto;

import com.fairShare.fairshare_backend.expenses.ExpenseSplit;
import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSplitResponse(
        UUID id,
        UUID userId,
        BigDecimal shareAmount,
        BigDecimal percentage,
        BigDecimal shares
) {
    public static ExpenseSplitResponse from(ExpenseSplit split) {
        return new ExpenseSplitResponse(
                split.getId(),
                split.getUserId(),
                split.getShareAmount(),
                split.getPercentage(),
                split.getShares()
        );
    }
}