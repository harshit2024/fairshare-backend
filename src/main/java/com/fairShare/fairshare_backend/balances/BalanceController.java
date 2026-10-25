package com.fairShare.fairshare_backend.balances;

import com.fairShare.fairshare_backend.balances.dto.GroupBalancesResponse;
import com.fairShare.fairshare_backend.balances.dto.SuggestedSettlementResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public List<GroupBalancesResponse> getGroupBalances(
            @PathVariable UUID groupId) {
        return balanceService.getGroupBalances(groupId);
    }

    @GetMapping("/suggestions")
    public List<SuggestedSettlementResponse> getSuggestedSettlements(
            @PathVariable UUID groupId) {
        return balanceService.getSuggestedSettlements(groupId);
    }
}