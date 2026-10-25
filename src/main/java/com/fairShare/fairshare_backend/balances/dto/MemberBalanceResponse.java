package com.fairShare.fairshare_backend.balances.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalanceResponse(
        UUID userId,
        String name,
        String email,
        BigDecimal amountPaid,
        BigDecimal amountOwed,
        BigDecimal netBalance,
        String balanceStatus
) {}
