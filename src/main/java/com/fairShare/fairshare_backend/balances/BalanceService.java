package com.fairShare.fairshare_backend.balances;

import com.fairShare.fairshare_backend.balances.dto.GroupBalancesResponse;
import com.fairShare.fairshare_backend.balances.dto.MemberBalanceResponse;
import com.fairShare.fairshare_backend.balances.dto.SuggestedSettlementResponse;
import com.fairShare.fairshare_backend.groups.GroupMemberRepository;
import com.fairShare.fairshare_backend.security.CurrentUserService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BalanceService {

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    private static final int MONEY_SCALE = 2;

    private final BalanceRepository balanceRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CurrentUserService currentUserService;

    public BalanceService(
            BalanceRepository balanceRepository,
            GroupMemberRepository groupMemberRepository,
            CurrentUserService currentUserService) {
        this.balanceRepository = balanceRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<GroupBalancesResponse> getGroupBalances(UUID groupId) {
        requireGroupMember(groupId);

        Map<String, CurrencyLedger> ledgers = calculateLedgers(groupId);

        return ledgers.entrySet()
                .stream()
                .map(entry -> {
                    String currency = entry.getKey();
                    CurrencyLedger ledger = entry.getValue();

                    List<MemberBalanceResponse> members =
                            ledger.members.values()
                                    .stream()
                                    .map(this::toMemberResponse)
                                    .toList();

                    List<SuggestedSettlementResponse> suggestions =
                            simplifyDebts(currency, ledger);

                    return new GroupBalancesResponse(
                            groupId,
                            currency,
                            members,
                            suggestions
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestedSettlementResponse> getSuggestedSettlements(
            UUID groupId) {
        requireGroupMember(groupId);

        Map<String, CurrencyLedger> ledgers = calculateLedgers(groupId);

        return ledgers.entrySet()
                .stream()
                .flatMap(entry ->
                        simplifyDebts(entry.getKey(), entry.getValue())
                                .stream())
                .toList();
    }

    private Map<String, CurrencyLedger> calculateLedgers(UUID groupId) {
        List<BalanceRepository.GroupMemberRow> groupMembers =
                balanceRepository.findGroupMembers(groupId);

        if (groupMembers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Group has no members or does not exist"
            );
        }

        Map<String, CurrencyLedger> ledgers = new TreeMap<>();

        // Create a ledger for every currency found in expenses.
        for (BalanceRepository.ExpenseRow expense
                : balanceRepository.findGroupExpenses(groupId)) {

            CurrencyLedger ledger =
                    ledgerFor(ledgers, expense.currency(), groupMembers);

            MutableBalance payer = ledger.members.get(expense.paidBy());

            if (payer == null) {
                throw inconsistentData(
                        "An expense payer is not a member of the group");
            }

            BigDecimal amount = money(expense.amount());
            payer.amountPaid = payer.amountPaid.add(amount);
            payer.netBalance = payer.netBalance.add(amount);
        }

        // A participant owes their recorded share of an expense.
        for (BalanceRepository.SplitRow split
                : balanceRepository.findGroupSplits(groupId)) {

            CurrencyLedger ledger =
                    ledgerFor(ledgers, split.currency(), groupMembers);

            MutableBalance participant =
                    ledger.members.get(split.userId());

            if (participant == null) {
                throw inconsistentData(
                        "An expense participant is not a member of the group");
            }

            BigDecimal share = money(split.shareAmount());

            participant.amountOwed =
                    participant.amountOwed.add(share);

            participant.netBalance =
                    participant.netBalance.subtract(share);
        }

        // Completed repayments reduce outstanding debts.
        for (BalanceRepository.SettlementRow settlement
                : balanceRepository.findCompletedSettlements(groupId)) {

            CurrencyLedger ledger =
                    ledgerFor(ledgers, settlement.currency(), groupMembers);

            MutableBalance payer =
                    ledger.members.get(settlement.paidByUserId());

            MutableBalance receiver =
                    ledger.members.get(settlement.paidToUserId());

            if (payer == null || receiver == null) {
                throw inconsistentData(
                        "A settlement participant is not a member of the group");
            }

            BigDecimal amount = money(settlement.amount());

            // The payer's negative balance moves toward zero.
            payer.netBalance = payer.netBalance.add(amount);

            // The receiver's positive balance moves toward zero.
            receiver.netBalance = receiver.netBalance.subtract(amount);
        }

        return ledgers;
    }

    private BalanceRepository.ExpenseRow findExpense(
            UUID groupId, UUID expenseId) {

        /*
         * This is replaced below by a lookup cache in the same service.
         * See the optimized implementation in the next section.
         */
        throw new UnsupportedOperationException(
                "Use the expense lookup cache implementation below");
    }

    private CurrencyLedger ledgerFor(
            Map<String, CurrencyLedger> ledgers,
            String currency,
            List<BalanceRepository.GroupMemberRow> groupMembers) {

        String normalizedCurrency = currency == null
                ? "INR"
                : currency.trim().toUpperCase(Locale.ROOT);

        return ledgers.computeIfAbsent(
                normalizedCurrency,
                key -> {
                    CurrencyLedger ledger = new CurrencyLedger();

                    for (BalanceRepository.GroupMemberRow member
                            : groupMembers) {
                        ledger.members.put(
                                member.userId(),
                                new MutableBalance(
                                        member.userId(),
                                        member.name(),
                                        member.email()
                                )
                        );
                    }

                    return ledger;
                }
        );
    }

    private MemberBalanceResponse toMemberResponse(
            MutableBalance member) {

        BigDecimal net = money(member.netBalance);

        String status;

        if (net.signum() > 0) {
            status = "RECEIVES";
        } else if (net.signum() < 0) {
            status = "OWES";
        } else {
            status = "SETTLED";
        }

        return new MemberBalanceResponse(
                member.userId,
                member.name,
                member.email,
                money(member.amountPaid),
                money(member.amountOwed),
                net,
                status
        );
    }

    private List<SuggestedSettlementResponse> simplifyDebts(
            String currency,
            CurrencyLedger ledger) {

        List<MutableBalance> debtors = new ArrayList<>();
        List<MutableBalance> creditors = new ArrayList<>();

        for (MutableBalance member : ledger.members.values()) {
            BigDecimal net = money(member.netBalance);

            if (net.signum() < 0) {
                debtors.add(member.copyWithBalance(net.abs()));
            } else if (net.signum() > 0) {
                creditors.add(member.copyWithBalance(net));
            }
        }

        // Largest debt/credit first reduces the number of transfers.
        debtors.sort(
                Comparator.comparing(
                        (MutableBalance b) -> b.netBalance
                ).reversed()
        );

        creditors.sort(
                Comparator.comparing(
                        (MutableBalance b) -> b.netBalance
                ).reversed()
        );

        List<SuggestedSettlementResponse> result = new ArrayList<>();

        int debtorIndex = 0;
        int creditorIndex = 0;

        while (debtorIndex < debtors.size()
                && creditorIndex < creditors.size()) {

            MutableBalance debtor = debtors.get(debtorIndex);
            MutableBalance creditor = creditors.get(creditorIndex);

            BigDecimal transfer = debtor.netBalance.min(
                    creditor.netBalance
            );

            transfer = money(transfer);

            if (transfer.signum() > 0) {
                result.add(new SuggestedSettlementResponse(
                        debtor.userId,
                        debtor.name,
                        creditor.userId,
                        creditor.name,
                        transfer,
                        currency
                ));
            }

            debtor.netBalance =
                    money(debtor.netBalance.subtract(transfer));

            creditor.netBalance =
                    money(creditor.netBalance.subtract(transfer));

            if (debtor.netBalance.signum() == 0) {
                debtorIndex++;
            }

            if (creditor.netBalance.signum() == 0) {
                creditorIndex++;
            }
        }

        return result;
    }

    private void requireGroupMember(UUID groupId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        if (!groupMemberRepository.existsByGroupIdAndUserId(
                groupId, currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group"
            );
        }
    }

    private ResponseStatusException inconsistentData(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot calculate balances: " + message
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? ZERO
                : value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static class CurrencyLedger {
        private final Map<UUID, MutableBalance> members =
                new LinkedHashMap<>();
    }

    private static class MutableBalance {
        private final UUID userId;
        private final String name;
        private final String email;

        private BigDecimal amountPaid = ZERO;
        private BigDecimal amountOwed = ZERO;
        private BigDecimal netBalance = ZERO;

        private MutableBalance(
                UUID userId,
                String name,
                String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }

        private MutableBalance copyWithBalance(BigDecimal balance) {
            MutableBalance copy =
                    new MutableBalance(userId, name, email);
            copy.netBalance = balance;
            return copy;
        }
    }
}