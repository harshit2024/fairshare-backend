package com.fairShare.fairshare_backend.settlements.dto;

import com.fairShare.fairshare_backend.settlements.Settlement;
import com.fairShare.fairshare_backend.settlements.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID groupId,
        UUID paidBy,
        UUID paidTo,
        BigDecimal amount,
        String currency,
        SettlementStatus status,
        String paymentMethod,
        String transactionReference,
        String notes,
        LocalDateTime settledAt,
        LocalDateTime createdAt
) {
    public static SettlementResponse fromEntity(Settlement s) {
        return new SettlementResponse(
                s.getId(),
                s.getGroupId(),
                s.getPaidBy(),
                s.getPaidTo(),
                s.getAmount(),
                s.getCurrency(),
                s.getStatus(),
                s.getPaymentMethod(),
                s.getTransactionReference(),
                s.getNotes(),
                s.getSettledAt(),
                s.getCreatedAt()
        );
    }
}