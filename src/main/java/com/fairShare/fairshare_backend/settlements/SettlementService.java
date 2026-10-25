package com.fairShare.fairshare_backend.settlements;

import com.fairShare.fairshare_backend.groups.GroupMemberRepository;
import com.fairShare.fairshare_backend.security.CurrentUserService;
import com.fairShare.fairshare_backend.settlements.dto.CreateSettlementRequest;
import com.fairShare.fairshare_backend.settlements.dto.SettlementResponse;
import com.fairShare.fairshare_backend.settlements.dto.UpdateSettlementStatusRequest;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CurrentUserService currentUserService;

    public SettlementService(
            SettlementRepository settlementRepository,
            GroupMemberRepository groupMemberRepository,
            CurrentUserService currentUserService) {
        this.settlementRepository = settlementRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public SettlementResponse create(
            UUID groupId,
            CreateSettlementRequest request) {

        UUID payerId = currentUserService.getCurrentUserId();
        UUID receiverId = request.paidTo();

        requireMember(groupId, payerId);
        requireMember(groupId, receiverId);

        if (payerId.equals(receiverId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot settle with yourself");
        }

        Settlement settlement = new Settlement();
        settlement.setGroupId(groupId);
        settlement.setPaidBy(payerId);
        settlement.setPaidTo(receiverId);
        settlement.setAmount(request.amount());

        String currency = request.currency() == null
                || request.currency().isBlank()
                ? "INR"
                : request.currency().toUpperCase(Locale.ROOT);

        settlement.setCurrency(currency);
        settlement.setPaymentMethod(request.paymentMethod());
        settlement.setTransactionReference(
                request.transactionReference());
        settlement.setNotes(request.notes());

        // Never trust a client-supplied status.
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setSettledAt(null);

        return SettlementResponse.fromEntity(
                settlementRepository.save(settlement));
    }

    @Transactional
    public SettlementResponse updateStatus(
            UUID settlementId,
            UpdateSettlementStatusRequest request) {

        UUID currentUserId = currentUserService.getCurrentUserId();

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Settlement not found"));

        requireMember(settlement.getGroupId(), currentUserId);

        SettlementStatus requestedStatus = request.status();

        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending settlements can be updated");
        }

        if (requestedStatus == SettlementStatus.COMPLETED) {
            if (!currentUserId.equals(settlement.getPaidTo())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the payment receiver can confirm completion");
            }

            settlement.setSettledAt(LocalDateTime.now());

        } else if (requestedStatus == SettlementStatus.CANCELLED) {
            if (!currentUserId.equals(settlement.getPaidBy())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the payer can cancel a pending settlement");
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allowed status transitions are PENDING to COMPLETED "
                            + "or PENDING to CANCELLED");
        }

        settlement.setStatus(requestedStatus);

        return SettlementResponse.fromEntity(
                settlementRepository.save(settlement));
    }

    @Transactional
    public SettlementResponse getById(UUID settlementId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Settlement not found"));

        requireMember(settlement.getGroupId(), currentUserId);

        return SettlementResponse.fromEntity(settlement);
    }

    @Transactional
    public List<SettlementResponse> getGroupSettlements(UUID groupId) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        requireMember(groupId, currentUserId);

        return settlementRepository
                .findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(SettlementResponse::fromEntity)
                .toList();
    }

    private void requireMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository
                .existsByGroupIdAndUserId(groupId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You must be a group member to access settlements");
        }
    }
}