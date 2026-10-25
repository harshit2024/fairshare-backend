package com.fairShare.fairshare_backend.balances.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedSettlementResponse(
        UUID fromUserId,
        String fromName,
        UUID toUserId,
        String toName,
        BigDecimal amount,
        String currency
) {}