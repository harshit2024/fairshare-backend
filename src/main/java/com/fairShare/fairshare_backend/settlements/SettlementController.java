package com.fairShare.fairshare_backend.settlements;

import com.fairShare.fairshare_backend.settlements.dto.CreateSettlementRequest;
import com.fairShare.fairshare_backend.settlements.dto.SettlementResponse;
import com.fairShare.fairshare_backend.settlements.dto.UpdateSettlementStatusRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/groups/{groupId}/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSettlementRequest request) {

        return settlementService.create(groupId, request);
    }

    @GetMapping("/groups/{groupId}/settlements")
    public List<SettlementResponse> getGroupSettlements(
            @PathVariable UUID groupId) {

        return settlementService.getGroupSettlements(groupId);
    }

    @GetMapping("/settlements/{settlementId}")
    public SettlementResponse getById(
            @PathVariable UUID settlementId) {

        return settlementService.getById(settlementId);
    }

    @PatchMapping("/settlements/{settlementId}/status")
    public SettlementResponse updateStatus(
            @PathVariable UUID settlementId,
            @Valid @RequestBody UpdateSettlementStatusRequest request) {

        return settlementService.updateStatus(settlementId, request);
    }
}