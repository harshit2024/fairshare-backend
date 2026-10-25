package com.fairShare.fairshare_backend.settlements.dto;

import jakarta.validation.constraints.NotNull;
import com.fairShare.fairshare_backend.settlements.SettlementStatus;

public record UpdateSettlementStatusRequest(
        @NotNull SettlementStatus status
) {}