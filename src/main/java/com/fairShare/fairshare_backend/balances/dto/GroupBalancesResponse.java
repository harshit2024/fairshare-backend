package com.fairShare.fairshare_backend.balances.dto;

import java.util.List;
import java.util.UUID;

public record GroupBalancesResponse(
        UUID groupId,
        String currency,
        List<MemberBalanceResponse> members,
        List<SuggestedSettlementResponse> suggestedSettlements
) {}